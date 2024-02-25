package org.monitoring.consumer.monitor.listner;

import lombok.extern.slf4j.Slf4j;
import org.monitoring.consumer.monitor.model.Step;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaConsumer {

  @KafkaListener(topics = "incoming.steps", containerFactory = "kafkaListner")
  public void consumerJson(Step step) {
    log.info("Got from kafka: "+step);
  }
}
