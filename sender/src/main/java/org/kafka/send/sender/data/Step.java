package org.kafka.send.sender.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Step {
  private String deviceId;
  private Long deviceSync;
  private Long stepsCount;
}
