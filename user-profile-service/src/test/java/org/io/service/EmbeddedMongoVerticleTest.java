package org.io.service;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.test.core.VertxTestBase;
import org.io.service.core.EmbeddedMongoVerticle;
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
        testContext.completeNow();
        undeploy(vertx, deploymentID);
      }));
  }

  @Test
  public void testConfiguredPort(Vertx vertx, VertxTestContext testContext) {
    EmbeddedMongoVerticle mongo = new EmbeddedMongoVerticle();
    vertx.deployVerticle(mongo, createOptions(), onSuccess(deploymentID -> {
      assertNotNull(deploymentID);
      assertEquals(TEST_PORT, mongo.actualPort());
      testContext.completeNow();

      undeploy(vertx, deploymentID);
    }));
  }

  private void undeploy(Vertx vertx, String deploymentID) {
    vertx.undeploy(deploymentID, onSuccess(v -> {
      assertNull(v);
      testComplete();
    }));
  }

  private DeploymentOptions createOptions() {
    return createOptions("3.4.3");
  }

  private DeploymentOptions createOptions(String version) {
    return createEmptyOptions().setConfig(new JsonObject().put("port", EmbeddedMongoVerticleTest.TEST_PORT).put("version", version));
  }

  private DeploymentOptions createEmptyOptions() {
    return new DeploymentOptions().setWorker(true);
  }
}
