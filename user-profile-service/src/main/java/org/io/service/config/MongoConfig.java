package org.io.service.config;

import io.vertx.core.json.JsonObject;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MongoConfig {

  public JsonObject mongoConfig() {
    return new JsonObject()
      .put("host", "localhost")
      .put("port", 27018)
      .put("db_name", "proffiles");
  }
}
