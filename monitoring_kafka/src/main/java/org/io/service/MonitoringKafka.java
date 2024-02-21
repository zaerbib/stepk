package org.io.service;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Hello world!
 */
public class MonitoringKafka {

  private static final Logger log = LoggerFactory.getLogger(MonitoringKafka.class.getName());
  public static void main(String[] args) throws InterruptedException {
    final var topic = "incoming.steps";

    final Map<String, Object> config = Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
      ConsumerConfig.GROUP_ID_CONFIG, "first-consumer",
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    try (var consumer = new KafkaConsumer<String, Object>(config)) {
      consumer.subscribe(Set.of(topic));
      while(true) {
        final var records = consumer.poll(Duration.ofMillis(500));
        if(records.isEmpty()) {
          Thread.sleep(5000);
        } else {
          for(var record: records) {
            System.out.format("Got record with value %s%n", record.value());
          }
        }

      }
    }
  }
}
