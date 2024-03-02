package org.io.service;

import io.reactivex.rxjava3.core.Maybe;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.io.service.core.UserProfileVerticle;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.Arrays;

/**
 * Unit test for simple App.
 */

@ExtendWith(VertxExtension.class)
@DisplayName("User profile API integration tests")
@Testcontainers
public class UserProfileServiceAppTest {


  @Container
  private static final DockerComposeContainer CONTAINERS =
    new DockerComposeContainer(new File("../docker-compose-test.yml"))
      .withExposedService("mongo_1", 27017)
      .withLocalCompose(true);

  private static RequestSpecification requestSpecification;
  private MongoClient mongoClient;

  @BeforeEach
  void setUp(Vertx vertx, VertxTestContext testContext) {
    JsonObject mongoConfig = new JsonObject()
      .put("host", "host.docker.internal")
      .put("port", 27017)
      .put("maxIdleTimeMS", 30000)
      .put("maxLifeTimeMS",3600000)
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
  void allHttpTest(Vertx vertx, VertxTestContext testContext) {
    JsonObject user = basicUser();
    JsonObject request = new JsonObject()
      .put("username", user.getString("username"))
      .put("password", user.getString("password"));

    vertx.deployVerticle(new UserProfileVerticle(), testContext.succeeding(id -> {
      HttpClient client = vertx.createHttpClient();
      client.request(HttpMethod.POST, 9095, "localhost", "/register")
        .onSuccess(requestH -> {
          requestH.putHeader("content-type", "application/json");
          requestH.write(user.encode());
          requestH.end();
          testContext.completeNow();
        })
        .onFailure(testContext::failNow);
    }));
  }
}
