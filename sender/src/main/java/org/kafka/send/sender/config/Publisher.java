package org.kafka.send.sender.config;

import lombok.extern.slf4j.Slf4j;
import org.kafka.send.sender.data.Step;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Publisher {
  private final JmsTemplate jmsTemplate;
  private final String topic = "step-events";

  public Publisher(JmsTemplate jmsTemplate) {
    this.jmsTemplate = jmsTemplate;
  }

  public void send(Step step) {
    jmsTemplate.convertAndSend(topic, step);
    log.info("Step::"+step.getDeviceId()+" Published successfully");
  }
}
