package org.kafka.send.sender;

import org.kafka.send.sender.config.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import static org.kafka.send.sender.util.StepGenerate.generatListStep;

@SpringBootApplication
@EnableScheduling
@EnableJms
public class SenderObjectApplication {

  private final Publisher publisher;

  public SenderObjectApplication(Publisher publisher) {
    this.publisher = publisher;
  }

  public static void main(String[] args) {
		SpringApplication.run(SenderObjectApplication.class, args);
	}

  @Scheduled(fixedRate = 5000, initialDelay = 10000)
  public void sendListToAmqp() {
    generatListStep(1000)
      .forEach(publisher::send);
  }
}
