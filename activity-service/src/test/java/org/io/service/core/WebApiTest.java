package org.io.service.core;

import io.reactivex.rxjava3.core.Completable;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.rxjava3.sqlclient.Tuple;
import io.vertx.sqlclient.PoolOptions;
import org.io.service.config.PgConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
@DisplayName("Http API tests")
@Testcontainers
public class WebApiTest {

  @Container
  public static final DockerComposeContainer CONTAINERS =
    new DockerComposeContainer(new File("../docker-compose-test.yml"))
      .withExposedService("postgres_1", 5432)
      .withExposedService("mongo_1", 27017)
      .withLocalCompose(true);

  private static RequestSpecification requestSpecification;

  @BeforeAll
  static void prepareSpec() {
    requestSpecification = new RequestSpecBuilder()
      .addFilters(Arrays.asList(new ResponseLoggingFilter(), new RequestLoggingFilter()))
      .setBaseUri("http://localhost:9097")
      .build();
  }

  @BeforeEach
  void prepareDb(Vertx vertx, VertxTestContext vertxTestContext) {
    String inserQuery = "INSERT INTO stepevent VALUES($1, $2, $3::timestamp, $4)";
    LocalDateTime now = LocalDateTime.now();
    List<Tuple> data = Arrays.asList(
      Tuple.of("123", 1, LocalDateTime.of(2019, 4, 1, 23, 0), 6541),
      Tuple.of("123", 2, LocalDateTime.of(2019, 5, 20, 10, 0), 200),
      Tuple.of("123", 3, LocalDateTime.of(2019, 5, 21, 10, 10), 100),
      Tuple.of("456", 1, LocalDateTime.of(2019, 5, 21, 10, 15), 123),
      Tuple.of("123", 4, LocalDateTime.of(2019, 5, 21, 11, 0), 320),
      Tuple.of("abc", 1, now.minus(1, ChronoUnit.HOURS), 1000),
      Tuple.of("def", 1, now.minus(2, ChronoUnit.HOURS), 100),
      Tuple.of("def", 2, now.minus(30, ChronoUnit.MINUTES), 900),
      Tuple.of("abc", 2, now, 1500)
    );

    PgPool pgPool = PgPool.pool(vertx, PgConfig.pgConnectOptions(), new PoolOptions());
    pgPool.query("DELETE FROM stepevent")
      .rxExecute()
      .flatMap(rows -> pgPool.preparedQuery(inserQuery).rxExecuteBatch(data))
      .ignoreElement()
      .andThen(vertx.rxDeployVerticle(new EventsVerticle()))
      .ignoreElement()
      .andThen(Completable.fromAction(pgPool::close))
      .subscribe(vertxTestContext::completeNow, vertxTestContext::failNow);
  }


  @Test
  @DisplayName("Operation a few succesful steps count queries over the dataset")
  void stepsCountQueries() {
    JsonPath jsonPath = given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .get("/456/total")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(123);

    jsonPath = given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .get("/123/total")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(7161);

  }
}
