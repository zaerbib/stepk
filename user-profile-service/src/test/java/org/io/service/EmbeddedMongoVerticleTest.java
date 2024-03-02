package org.io.service;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.test.core.VertxTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class EmbeddedMongoVerticleTest extends VertxTestBase {

  public static final long MAX_WORKER_EXECUTE_TIME = 30 * 60 * 1000L;
  public static final int TEST_PORT = 7533;

  @Override
  protected VertxOptions getOptions() {
    return new VertxOptions().setMaxWorkerExecuteTime(MAX_WORKER_EXECUTE_TIME);
  }

  @Test
  public void testEmbeddedMongo(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle("service:org.io.service.user-profile-service",
      onSuccess(deploymentID -> {
        assertNotNull(deploymentID);
        undeploy(vertx, deploymentID);
        testContext.completeNow();
      }));
  }

  private void undeploy(Vertx vertx, String deploymentID) {
    vertx.undeploy(deploymentID, onSuccess(v -> {
      assertNull(v);
      testComplete();
    }));
  }
}
