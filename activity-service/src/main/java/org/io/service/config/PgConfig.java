package org.io.service.config;

import io.vertx.pgclient.PgConnectOptions;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PgConfig {
  public PgConnectOptions pgConnectOptions() {
    return new PgConnectOptions()
      .setHost("localhost")
      .setDatabase("postgres")
      .setUser("postgres")
      .setPassword("vertx")
      .setPort(5432)
      .setCachePreparedStatements(true)
      .setLogActivity(true);
  }
}
