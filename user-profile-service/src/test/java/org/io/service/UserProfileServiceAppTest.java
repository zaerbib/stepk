package org.io.service;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.slf4j.Slf4j;
import org.io.service.core.EmbeddedMongoVerticle;
import org.io.service.core.UserProfileVerticle;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Unit test for simple App.
 */

@ExtendWith(VertxExtension.class)
@DisplayName("User profile API integration tests")
@Testcontainers
@Slf4j
public class UserProfileServiceAppTest {
  public static final int TEST_PORT = 27018;


  private MongoClient mongoClient;

  @BeforeAll
  static void prepareSpec(Vertx vertx) {
    EmbeddedMongoVerticle mongo = new EmbeddedMongoVerticle();
    vertx.deployVerticle(mongo, createOptions());
  }

  @BeforeEach
  void setUp(Vertx vertx, VertxTestContext testContext) {
    JsonObject mongoConfig = new JsonObject()
      .put("host", "localhost")
      .put("port", 27018)
      .put("maxIdleTimeMS", 30000)
      .put("maxLifeTimeMS", 3600000)
      .put("db_name", "profiles");

    mongoClient = MongoClient.createShared(vertx, mongoConfig);

    mongoClient
      .createIndexWithOptions("user", new JsonObject().put("username", 1), new IndexOptions().unique(true))
      .andThen(result -> mongoClient.createIndexWithOptions("user", new JsonObject().put("deviceId", 1), new IndexOptions().unique(true)))
      .andThen(drop -> dropAllUsers())
      .onSuccess(ok -> testContext.completeNow())
      .onFailure(testContext::failNow);
  }

  @AfterEach
  void cleanUp(VertxTestContext testContext) {
    dropAllUsers()
      .onSuccess(res -> testContext.completeNow())
      .onFailure(testContext::failNow);
  }

  private Future<MongoClientDeleteResult> dropAllUsers() {
    return mongoClient.removeDocuments("user", new JsonObject());
  }

  private JsonObject basicUser() {
    return new JsonObject()
      .put("username", "abc")
      .put("password", "123")
      .put("email", "abc@email.me")
      .put("city", "Lyon")
      .put("deviceId", "a1b2c32")
      .put("makePublic", true);
  }

  @Test
  @DisplayName("Authenticate an existing user")
  void registerTest(Vertx vertx, VertxTestContext testContext) {
    JsonObject user = basicUser();

    vertx.deployVerticle(new UserProfileVerticle(), testContext.succeeding(id -> {
      HttpClient client = vertx.createHttpClient();

      client.request(HttpMethod.POST, 9095, "localhost", "/register")
        .compose(req -> {
          req.putHeader("content-type", "application/json")
            .setChunked(true)
            .write(user.encode());
          return req.send().compose(HttpClientResponse::body);
        })
        .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
          JsonObject tmp = buffer.toJsonObject();
          Assertions.assertEquals(user.getString("username"), tmp.getString("username"));
          testContext.completeNow();
        })))
        .onFailure(testContext::failNow);
    }));
    testContext.completeNow();
  }

  @Test
  @DisplayName("Register and fetch data")
  void registerAndFetch(Vertx vertx, VertxTestContext testContext) {
    JsonObject user = basicUser();
    mongoClient.save("user", basicUser())
      .onSuccess(ok -> testContext.completeNow())
      .onFailure(testContext::failNow);

    vertx.deployVerticle(new UserProfileVerticle(), testContext.succeeding(id -> {
      HttpClient client = vertx.createHttpClient();
      client.request(HttpMethod.GET, 9095, "localhost", "/user/" + user.getString("username"))
        .compose(req -> req.send().compose(HttpClientResponse::body))
        .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
          JsonObject test = buffer.toJsonObject();
          Assertions.assertAll(
            "user service test",
            () -> Assertions.assertNotNull(buffer),
            () -> Assertions.assertEquals(test.getString("username"), user.getString("username")),
            () -> Assertions.assertEquals(test.getString("email"), user.getString("email")),
            () -> Assertions.assertEquals(test.getString("deviceId"), user.getString("deviceId"))
          );
          testContext.completeNow();
        })))
        .onFailure(testContext::failNow);
    }));
  }

  @Test
  @DisplayName("Register then update user")
  void updateUser(Vertx vertx, VertxTestContext testContext) {
    JsonObject original = basicUser();
    mongoClient.save("user", original)
      .onSuccess(user -> testContext.completeNow())
      .onFailure(testContext::failNow);

    JsonObject update = original.put("deviceId", "vertx-in-action-123")
      .put("email", "vertx@email.me")
      .put("city", "Nevers")
      .put("makePublic", false)
      .put("username", "Bean");

    vertx.deployVerticle(new UserProfileVerticle(), testContext.succeeding(id -> {
      HttpClient client = vertx.createHttpClient();
      client.request(HttpMethod.PUT, 9095, "localhot", "/user/" + original.getString("username"))
        .compose(req -> {
          req.putHeader("content-type", "application/json")
            .setChunked(true)
            .write(original.encode());
          return req.send().compose(HttpClientResponse::body);
        })
        .onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
          JsonObject tmp = buffer.toJsonObject();
          Assertions.assertEquals(update.getString("username"), tmp.getString("username"));
          Assertions.assertEquals(update.getString("deviceId"), tmp.getString("deviceId"));
          Assertions.assertEquals(update.getString("city"), tmp.getString("city"));
          Assertions.assertEquals(update.getString("email"), tmp.getString("email"));
          Assertions.assertEquals(update.getString("makePublic"), tmp.getString("makePublic"));

          testContext.completeNow();
        })));
    }));
  }

  private static DeploymentOptions createOptions() {
    return createOptions("3.4.3");
  }

  private static DeploymentOptions createOptions(String version) {
    return createEmptyOptions().setConfig(new JsonObject()
      .put("port", UserProfileServiceAppTest.TEST_PORT)
      .put("version", version));
  }

  private static DeploymentOptions createEmptyOptions() {
    return new DeploymentOptions().setWorker(true);
  }
}
