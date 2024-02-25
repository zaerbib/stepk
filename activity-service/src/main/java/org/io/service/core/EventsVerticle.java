package org.io.service.core;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgException;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.RxHelper;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.openapi.RouterBuilder;
import io.vertx.rxjava3.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava3.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducerRecord;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.rxjava3.sqlclient.Tuple;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import lombok.extern.slf4j.Slf4j;
import org.io.service.config.KafkaConfig;
import org.io.service.config.PgConfig;
import org.io.service.config.SqlQueries;
import org.io.service.service.StepAccountActivityService;
import org.reactivestreams.Publisher;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;


@Slf4j
public class EventsVerticle extends AbstractVerticle {
  private KafkaConsumer<String, JsonObject> eventConsumer;
  private KafkaProducer<String, JsonObject> updateProducer;
  private PgPool pgPool;
  private HttpServer httpServer;
  private ServiceBinder serviceBinder;
  private MessageConsumer<JsonObject> consumer;

  @Override
  public Completable rxStart() {
    this.readAndWriteFromAndToKafka();
    this.startStepAccountActivityService();
    this.startHttpServer();
    return Completable.complete();
  }

  @Override
  public Completable rxStop() {
    this.httpServer.close();
    consumer.unregister();

    return Completable.complete();
  }

  private void startHttpServer() {
    RouterBuilder.create(vertx, "openapi.json")
      .doOnError(Throwable::printStackTrace)
      .subscribe(
        routerBuilder -> {
          routerBuilder.mountServicesFromExtensions();
          Router router = Router.newInstance(routerBuilder.createRouter().getDelegate());
          router.errorHandler(400, ctx -> {
            log.debug("Bad request : "+ctx.failure());
          });

          httpServer = vertx.createHttpServer(new HttpServerOptions()
            .setPort(9097)
            .setHost("localhost"));
          httpServer.requestHandler(router);
          httpServer.getDelegate().listen().mapEmpty();
        },
        err -> {
          log.debug("HttpServer Failed to start");
        }
      );
  }

  private void startStepAccountActivityService() {
    serviceBinder = new ServiceBinder(vertx.getDelegate());
    StepAccountActivityService service = StepAccountActivityService.create(pgPool);
    consumer = serviceBinder.setAddress("activity.service.api")
      .register(StepAccountActivityService.class, service);
  }

  private void readAndWriteFromAndToKafka() {
    eventConsumer = KafkaConsumer.create(vertx, KafkaConfig.consumer("activity-service"));
    updateProducer = KafkaProducer.create(vertx, KafkaConfig.producer());
    pgPool = PgPool.pool(vertx, PgConfig.pgConnectOptions(), new PoolOptions());

    eventConsumer.subscribe("incoming.steps");
    eventConsumer.handler(record -> {
      insertRecord(record)
        .flatMap(this::generateActivityUpdate)
        .flatMap(this::commitKafkaConsumerOffset)
        .doOnError(err -> log.error("Woops", err))
        .retryWhen(this::retryLater)
        .subscribe();
    });
  }

  private Flowable<Throwable> retryLater(Flowable<Throwable> errs) {
    return errs.delay(10, TimeUnit.SECONDS, RxHelper.scheduler(vertx));
  }

  private Flowable<KafkaConsumerRecord<String, JsonObject>> insertRecord(KafkaConsumerRecord<String, JsonObject> record) {
    JsonObject data = record.value();

    Tuple values = Tuple.of(
      data.getString("deviceId"),
      data.getLong("deviceSync"),
      data.getInteger("stepsCount")
    );

    return pgPool
      .preparedQuery(SqlQueries.insertStepEvent())
      .rxExecute(values)
      .map(rs -> record)
      .onErrorReturn(err -> {
        if (duplicateKeyInsert(err)) {
          return record;
        } else {
          throw new RuntimeException(err);
        }
      }).toFlowable();
  }

  private boolean duplicateKeyInsert(Throwable err) {
    return (err instanceof PgException) && "23505".equals(((PgException) err).getSqlState());
  }

  private Flowable<KafkaConsumerRecord<String, JsonObject>> generateActivityUpdate(KafkaConsumerRecord<String,
    JsonObject> record) {
    String deviceId = record.value().getString("deviceId");
    LocalDateTime now = LocalDateTime.now();
    String key = deviceId + ":" + now.getYear() + "-" + now.getMonth() + "-" + now.getDayOfMonth();

    return pgPool
      .preparedQuery(SqlQueries.stepsCountForToday())
      .rxExecute(Tuple.of(deviceId))
      .map(rs -> rs.iterator().next())
      .map(row -> new JsonObject()
        .put("deviceId", deviceId)
        .put("timestamp", row.getTemporal(0).toString())
        .put("stepsCount", row.getLong(1)))
      .flatMap(json -> updateProducer.rxSend(KafkaProducerRecord.create("daily.step.updates", key, json)))
      .map(rs -> record)
      .toFlowable();
  }

  private Publisher<?> commitKafkaConsumerOffset(KafkaConsumerRecord<String, JsonObject> record) {
    return eventConsumer.rxCommit().toFlowable();
  }
}
