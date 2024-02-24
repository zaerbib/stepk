package org.io.service;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.amqp.AmqpClient;
import io.vertx.rxjava3.amqp.AmqpMessage;
import io.vertx.rxjava3.core.RxHelper;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.kafka.admin.KafkaAdminClient;
import io.vertx.rxjava3.kafka.client.consumer.KafkaConsumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static io.restassured.RestAssured.given;

/**
 * Unit test for simple App.
 */

@ExtendWith(VertxExtension.class)
@Testcontainers
public class IngestServiceTest {

  private static final Logger logger = LoggerFactory.getLogger(IngestServiceTest.class);

  @Container
  private static final DockerComposeContainer CONTAINERS =
    new DockerComposeContainer(new File("../docker-compose-test.yml"))
      .withExposedService("artemis_1", 5672)
      .withExposedService("broker_1", 9092)
      .withLocalCompose(true);

  private static RequestSpecification requestSpecification;
  private static KafkaConsumer<String, JsonObject> kafkaConsumer;
  private AmqpClient amqpClient;

  @BeforeAll
  public static void prepareSpec() {
    requestSpecification = new RequestSpecBuilder()
      .addFilters(asList(new ResponseLoggingFilter(), new ResponseLoggingFilter()))
      .setBaseUri("http://localhost:3002")
      .build();
  }

  public static Map<String, String> kafkaConfig() {
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("value.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", "false");
    config.put("group.id", "ingester-test-" + System.currentTimeMillis());
    return config;
  }

  public static AmqpClientOptions amqpClientOptions() {
    return new AmqpClientOptions()
      .setHost("localhost")
      .setPort(5672)
      .setUsername("artemis")
      .setPassword("simetraehcapa");
  }

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    kafkaConsumer = KafkaConsumer.create(vertx, kafkaConfig());
    amqpClient = AmqpClient.create(vertx, amqpClientOptions());
    KafkaAdminClient adminClient = KafkaAdminClient.create(vertx, kafkaConfig());

    vertx.rxDeployVerticle(new IngestService())
      .delay(1000, TimeUnit.MILLISECONDS, RxHelper.scheduler(vertx))
      .flatMapCompletable(id -> adminClient.rxDeleteTopics(Collections.singletonList("incoming.steps")))
      .onErrorComplete()
      .doOnComplete(testContext::completeNow)
      .doOnError(testContext::failNow)
      .subscribe();
  }

  @Test
  @DisplayName("Ingest well-formed AMQP Message")
  void amqIngest(VertxTestContext testContext) {
    JsonObject body = new JsonObject()
      .put("deviceId", "123")
      .put("deviceSync", 1L)
      .put("stepsCount", 500);

    amqpClient.rxConnect()
      .flatMap(con -> con.rxCreateSender("step-events"))
      .doOnSuccess(sender -> {
        AmqpMessage message = AmqpMessage.create()
          .durable(true)
          .ttl(5000)
          .withJsonObjectAsBody(body).build();
        sender.send(message);
        testContext.completeNow();
      })
      .doOnError(testContext::failNow)
      .subscribe();

    kafkaConsumer.rxSubscribe("incoming.steps");
    kafkaConsumer.handler(record -> testContext.verify(() -> {
      assertThat(record.key()).isEqualTo("123");
      JsonObject json = record.value();
      assertThat(json.getString("deviceId")).isEqualTo("123");
      assertThat(json.getLong("deviceSync")).isEqualTo(1L);
      assertThat(json.getInteger("stepsCount")).isEqualTo(500);
      testContext.completeNow();
    }));
  }

  @Test
  @DisplayName("Ingest a badly-formed AMQP message and observe no kafka record")
  void amqIngestWrong(Vertx vertx, VertxTestContext testContext) {
    JsonObject body = new JsonObject();

    amqpClient.rxConnect()
      .flatMap(connection -> connection.rxCreateSender("step-events"))
      .doOnSuccess(sender -> {
        AmqpMessage message = AmqpMessage.create()
          .durable(true)
          .ttl(5000)
          .withJsonObjectAsBody(body).build();
        sender.send(message);
        testContext.completeNow();
      })
      .doOnError(testContext::failNow)
      .subscribe();

    kafkaConsumer.subscribe("incoming.steps")
      .toFlowable()
      .timeout(3, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .subscribe(
        record -> testContext.failNow(new IllegalStateException("We must not get a record")),
        err -> {
          if (err instanceof TimeoutException) {
            testContext.completeNow();
          } else {
            testContext.failNow(err);
          }
        });
  }

  @Test
  @DisplayName("Ingest a well-formed JSON data over HTTP")
  void httpIngest(VertxTestContext testContext) {
    JsonObject body = new JsonObject()
      .put("deviceId", "456")
      .put("deviceSync", 3L)
      .put("stepsCount", 125);

    given(requestSpecification)
      .contentType(ContentType.JSON)
      .body(body.encode())
      .post("/ingest")
      .then()
      .assertThat()
      .statusCode(200);

    kafkaConsumer.subscribe("incoming.steps");
    kafkaConsumer.handler(record -> testContext.verify(() -> {
      assertThat(record.key()).isEqualTo("456");
      JsonObject json = record.value();
      assertThat(json.getString("deviceId")).isEqualTo("456");
      assertThat(json.getLong("deviceSync")).isEqualTo(3);
      assertThat(json.getInteger("stepsCount")).isEqualTo(125);
      testContext.completeNow();
    }));
  }
}
