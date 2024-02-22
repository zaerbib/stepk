package org.io.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaReadStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class MonitoringKafkaVertx extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(MonitoringKafkaVertx.class.getName());

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MonitoringKafkaVertx())
      .onSuccess(ok -> log.info("Success !!!"))
      .onFailure(err -> log.info("Failure !!!"));
  }

  @Override
  public void start() {
    KafkaReadStream<String, JsonObject> consumer = KafkaReadStream.create(vertx, configKafka(), String.class, JsonObject.class);
    consumer.subscribe(Collections.singleton("incoming.steps"))
      .onSuccess(handler -> {
        log.info("Consumer subcribed");

        vertx.setPeriodic(1000, timerId -> {
          consumer
            .poll(Duration.ofMillis(100))
            .onSuccess(records -> {
              for (var record : records) {
                System.out.format("Got record with value %s%n", record.value());
              }
            }).onFailure(cause -> {
              System.out.println("Something went wrong when polling " + cause.toString());
              log.error("Error : " + cause.getMessage());

              vertx.cancelTimer(timerId);
            });
        });
      });
  }

  private Properties configKafka() {
    Properties properties = new Properties();
    properties.put("bootstrap.servers", "localhost:9092");
    properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    properties.put("value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");
    properties.put("acks", "1");

    return properties;
  }
}
