package org.io.service;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.amqp.AmqpReceiverOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.amqp.AmqpClient;
import io.vertx.rxjava3.amqp.AmqpMessage;
import io.vertx.rxjava3.amqp.AmqpReceiver;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.RxHelper;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.RoutingContext;
import io.vertx.rxjava3.ext.web.handler.BodyHandler;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Hello world!
 */
public class IngestService extends AbstractVerticle {

  private static final int HTTP_PORT = 3002;
  private static final Logger logger = LoggerFactory.getLogger(IngestService.class);

  private KafkaProducer<String, JsonObject> updateProducer;

  @Override
  public Completable rxStart() {
    updateProducer = KafkaProducer.create(vertx, kafkaConfig());

    AmqpClientOptions amqpClientOptions = amqpConfig();
    AmqpReceiverOptions receiverOptions = new AmqpReceiverOptions().setAutoAcknowledgement(false).setDurable(true);

    AmqpClient.create(vertx, amqpClientOptions).rxConnect().flatMap(conn -> conn.rxCreateReceiver("step-events", receiverOptions)).flatMapPublisher(AmqpReceiver::toFlowable).doOnError(this::logAmqpError).retryWhen(this::retryLater).subscribe(this::handleAmqpMessage);

    Router router = Router.router(vertx);
    router.post().handler(BodyHandler.create());
    router.post("/ingest").handler(this::httpIngest);

    return vertx.createHttpServer().requestHandler(router).rxListen(HTTP_PORT).ignoreElement();
  }

  private Flowable<Throwable> retryLater(Flowable<Throwable> errs) {
    return errs.delay(10, TimeUnit.SECONDS, RxHelper.scheduler(vertx));
  }

  private AmqpClientOptions amqpConfig() {
    return new AmqpClientOptions().setHost("localhost").setPort(5672).setUsername("artemis").setPassword("simetraehcapa");
  }

  private Map<String, String> kafkaConfig() {
    Map<String, String> config = new HashMap<>();
    config.put("boostrap.servers", "stepk_kafka_1:9092");
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");
    config.put("acks", "1");

    return config;
  }

  private void logAmqpError(Throwable err) {
    logger.error("Woops AMQP", err);
  }

  private void handleAmqpMessage(AmqpMessage message) {
    if (!"application/json".equals(message.contentType()) || invalidIngestedJson(message.bodyAsJsonObject())) {
      logger.error("Invalid AMQP message (discarded): {}", message.bodyAsBinary());
      message.accepted();
      return;
    }

    JsonObject payload = message.bodyAsJsonObject();
    KafkaProducerRecord<String, JsonObject> record = makeKafkaRecord(payload);
    updateProducer.rxSend(record).subscribe(ok -> message.accepted(), err -> {
      logger.error("AMQP ingestion faled", err);
    });
  }

  private void httpIngest(RoutingContext ctx) {
    JsonObject payload = ctx.body().asJsonObject();
    if (invalidIngestedJson(payload)) {
      logger.error("Invalid HTTP JSON (discarded): {}", payload.encodePrettily());
      ctx.fail(400);
      return;
    }

    KafkaProducerRecord<String, JsonObject> record = makeKafkaRecord(payload);
    updateProducer.rxSend(record).doOnSuccess(item -> ctx.response().end()).doOnError(err -> {
      logger.error("HTTP ingestion failed", err);
      ctx.fail(500);
    }).subscribe();
  }

  private boolean invalidIngestedJson(JsonObject payload) {
    return !payload.containsKey("deviceId")
      || !payload.containsKey("deviceSync")
      || !payload.containsKey("stepsCount");
  }

  private KafkaProducerRecord<String, JsonObject> makeKafkaRecord(JsonObject payload) {
    String deviceId = payload.getString("deviceId");
    JsonObject recordData = new JsonObject().put("deviceId", payload.getString("deviceId")).put("deviceSync", payload.getString("deviceSync")).put("stepsCount", payload.getString("stepsCount"));

    return KafkaProducerRecord.create("incoming.steps", deviceId, recordData);
  }

  public static void main(String[] args) {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
    Vertx vertx = Vertx.vertx();
    vertx.rxDeployVerticle(new IngestService())
      .doOnSuccess(ok -> logger.info("HTTP server started on port {}", HTTP_PORT))
      .doOnError(err -> logger.error("Woops", err)).subscribe();
  }
}
