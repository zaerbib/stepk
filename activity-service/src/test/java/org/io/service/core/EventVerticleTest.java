package org.io.service.core;

import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.kafka.admin.KafkaAdminClient;
import io.vertx.rxjava3.kafka.client.consumer.KafkaConsumer;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducerRecord;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.io.service.config.KafkaConfig;
import org.io.service.config.PgConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
@DisplayName("Kafka event processing tests")
@Testcontainers
public class EventVerticleTest {

  @Container
  private static final DockerComposeContainer CONTAINERS = new DockerComposeContainer(new File("../docker-compose-test.yml"))
    .withExposedService("postgres_1", 5432)
    .withExposedService("mongo_1", 27017)
    .withExposedService("broker_1", 9092)
    .withLocalCompose(true);

  private KafkaConsumer<String, JsonObject> consumer;
  private KafkaProducer<String, JsonObject> producer;

  @BeforeEach
  public void resetPgAndKafka(Vertx vertx, VertxTestContext testContext) {
    consumer = KafkaConsumer.create(vertx, KafkaConfig.consumer("activity-service-test-" + System.currentTimeMillis()));
    producer = KafkaProducer.create(vertx, KafkaConfig.producer());
    KafkaAdminClient adminClient = KafkaAdminClient.create(vertx, KafkaConfig.producer());

    PgPool pgPool = PgPool.pool(vertx, PgConfig.pgConnectOptions(), new PoolOptions());
    pgPool.query("DELETE FROM stepevent")
      .rxExecute()
      .flatMapCompletable(rs -> adminClient.rxDeleteTopics(Arrays.asList("incoming.steps", "daily.step.updates")))
      .andThen(Completable.fromAction(pgPool::close))
      .onErrorComplete()
      .subscribe(
        testContext::completeNow,
        testContext::failNow
      );
  }

  @Test
  @DisplayName("Send events from the same device, and observe that a correct daily steps count event is being produced")
  void observeDailyStepsCountEvent(Vertx vertx, VertxTestContext testContext) {
    vertx
      .rxDeployVerticle(new EventsVerticle())
      .flatMap(id -> {
        JsonObject steps = new JsonObject()
          .put("deviceId", "123")
          .put("deviceSync", 1L)
          .put("stepsCount", 200);
        return producer.rxSend(KafkaProducerRecord.create("incoming.steps", "123", steps));
      })
      .flatMap(id -> {
        JsonObject steps = new JsonObject()
          .put("deviceId", "123")
          .put("deviceSync", 2L)
          .put("stepsCount", 50);
        return producer.rxSend(KafkaProducerRecord.create("incoming.steps", "123", steps));
      })
      .subscribe(ok -> {
      }, testContext::failNow);

    consumer.subscribe("daily.step.updates");
    consumer.handler(record -> {
      JsonObject json = record.value();
      testContext.verify(() -> {
        assertThat(json.getString("deviceId")).isEqualTo("123");
        assertThat(json.containsKey("timestamp")).isTrue();
        assertThat(json.getInteger("stepsCount")).isEqualTo(250);

        testContext.completeNow();
      });
    });
  }
}
