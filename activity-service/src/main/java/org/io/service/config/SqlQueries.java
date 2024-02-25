package org.io.service.config;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SqlQueries {

  public String insertStepEvent() {
    return "INSERT INTO stepevent VALUES($1, $2, current_timestamp, $3)";
  }

  public String stepsCountForToday() {
    return "SELECT current_timestamp, coalesce(sum(steps_count), 0) FROM stepevent WHERE " +
      "(device_id = $1) AND " +
      "(date_trunc('day', sync_timestamp) = date_trunc('day', current_timestamp))";
  }

  public String totalStepsCount() {
    return "SELECT sum(steps_count) FROM stepevent WHERE" +
      "(device_id = $1)";
  }

  public String monthlyStepCount() {
    return "SELECT sum(steps_count) FROM stepevent WHERE" +
      "(device_id = $1) AND" +
      "(date_trunc('month', sync_timestamp) = $2::timestamp)";
  }

  public String dailyStepsCount() {
    return "SELECT sum(steps_count) FROM stepevent WHERE" +
      "(device_id = $1) AND" +
      "(date_trunc('day', sync_timestamp) = $2::timestamp)";
  }

  public String rankingLast24Hours() {
    return "SELECT device_id, SUM(steps_count) as steps FROM stepevent WHERE" +
      "(now() - sync_timestamp <= (interval '24 hours'))" +
      "GROUP BY device_id ORDER BY steps DESC";
  }
}
