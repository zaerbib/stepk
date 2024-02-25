package org.kafka.send.sender.util;

import com.github.javafaker.Faker;
import lombok.experimental.UtilityClass;
import org.kafka.send.sender.data.Step;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@UtilityClass
public class StepGenerate {
  private final Faker faker = new Faker();

  public List<Step> generatListStep(Integer number) {
    return IntStream.range(0, number)
      .mapToObj(item -> genereateOne())
      .collect(Collectors.toList());
  }

  private Step genereateOne() {
    return Step.builder()
      .deviceId(String.valueOf(faker.number().numberBetween(100, 1000)))
      .deviceSync(faker.number().numberBetween(1L, 1000L))
      .stepsCount(faker.number().numberBetween(10L, 15000L))
      .build();
  }
}
