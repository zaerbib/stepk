package org.io.service;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducer;
import io.vertx.rxjava3.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 */
public class IngestService {

  private static final int HTTP_PORT = 3002;
  private static final Logger logger = LoggerFactory.getLogger(IngestService.class);

  private KafkaProducer<String, JsonObject> updateProducer;

  private boolean invalidIngestedJson(JsonObject payload) {
    return !payload.containsKey("deviceId")
      || !payload.containsKey("deviceSync")
      || !payload.containsKey("stepsCount");
  }

  private KafkaProducerRecord<String, JsonObject> makeKafkaRecord(JsonObject payload) {
    String deviceId = payload.getString("deviceId");
    JsonObject recordData = new JsonObject()
      .put("deviceId", payload.getString("deviceId"))
      .put("deviceSync", payload.getString("deviceSync"))
      .put("stepsCount", payload.getString("stepsCount"));

    return KafkaProducerRecord.create("incoming.steps", deviceId, recordData);
  }

  public static void main(String[] args) {
    System.out.println("Hello World!");
  }
}
