package org.monitoring.consumer.monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Step {
  private String deviceId;
  private Long deviceSync;
  private Long stepsCount;
}
