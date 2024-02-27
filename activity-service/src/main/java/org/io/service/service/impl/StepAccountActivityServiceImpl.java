package org.io.service.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.rxjava3.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.io.service.config.SqlQueries;
import org.io.service.service.StepAccountActivityService;

import java.time.DateTimeException;
import java.time.LocalDateTime;

@Slf4j
public class StepAccountActivityServiceImpl implements StepAccountActivityService {
  private final PgPool pgPool;

  public StepAccountActivityServiceImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public void getStepCountTotal(String deviceId,
                                ServiceRequest request,
                                Handler<AsyncResult<ServiceResponse>> resultHandler) {
    Tuple params = Tuple.of(deviceId);

    pgPool
      .preparedQuery(SqlQueries.totalStepsCount())
      .rxExecute(params)
      .map(rs -> rs.iterator().next())
      .subscribe(
        row -> sendCount(resultHandler, row),
        err -> handleError(resultHandler, err));
  }

  @Override
  public void getStepCountForParticularMonth(String deviceId,
                                             String year,
                                             String month,
                                             ServiceRequest request,
                                             Handler<AsyncResult<ServiceResponse>> resultHandler) {
    try {
      LocalDateTime dateTime = LocalDateTime.of(
        Integer.parseInt(year),
        Integer.parseInt(month),
        1, 0, 0
      );

      Tuple params = Tuple.of(deviceId, dateTime);
      pgPool.preparedQuery(SqlQueries.monthlyStepCount())
        .rxExecute(params)
        .map(rs -> rs.iterator().next())
        .subscribe(
          row -> sendCount(resultHandler, row),
          err -> handleError(resultHandler, err)
        );
    } catch (DateTimeException | NumberFormatException e) {
      sendBadRequest(resultHandler);
    }
  }

  @Override
  public void getStepCountParticularDay(String deviceId,
                                        String year,
                                        String month,
                                        String day,
                                        ServiceRequest request,
                                        Handler<AsyncResult<ServiceResponse>> resultHandler) {
    try {
      LocalDateTime dateTime = LocalDateTime.of(
        Integer.parseInt(year),
        Integer.parseInt(month),
        Integer.parseInt(day), 0, 0);

      Tuple params = Tuple.of(deviceId, dateTime);
      pgPool.preparedQuery(SqlQueries.dailyStepsCount())
        .rxExecute(params)
        .map(rs -> rs.iterator().next())
        .subscribe(
          row -> sendCount(resultHandler, row),
          err -> handleError(resultHandler, err)
        );
    } catch (DateTimeException | NumberFormatException e) {
      sendBadRequest(resultHandler);
    }
  }

  @Override
  public void getRankingLast24Hours(ServiceRequest request, Handler<AsyncResult<ServiceResponse>> resultHandler) {
    pgPool
      .preparedQuery(SqlQueries.rankingLast24Hours())
      .rxExecute()
      .subscribe(
        rows -> sendRanking(resultHandler, rows),
        err -> handleError(resultHandler, err)
      );
  }

  private void sendCount(Handler<AsyncResult<ServiceResponse>> resultHandler, Row row) {
    Integer count = row.getInteger(0);
    if (count != null) {
      JsonObject payload = new JsonObject();
      payload.put("count", count);

      resultHandler.handle(Future.succeededFuture(
        ServiceResponse.completedWithJson(payload)
          .putHeader("Content-Type", "application/json")
          .setStatusCode(200)
      ));
    } else {
      resultHandler.handle(Future.succeededFuture(
        new ServiceResponse()
          .setStatusCode(404)
          .setStatusMessage("Device not found")
      ));
    }
  }

  private void handleError(Handler<AsyncResult<ServiceResponse>> resultHandler, Throwable err) {
    log.error("Woops !!!", err);
    resultHandler.handle(
      Future.succeededFuture(
        new ServiceResponse().setStatusCode(500)
          .setStatusMessage("Technical Error")
      )
    );
  }

  private void sendBadRequest(Handler<AsyncResult<ServiceResponse>> resultHandler) {
    log.error("Woops !!! Bad Request");
    resultHandler.handle(
      Future.succeededFuture(
        new ServiceResponse().setStatusCode(400)
          .setStatusMessage("Bad Request !!!")
      )
    );
  }

  private void sendRanking(Handler<AsyncResult<ServiceResponse>> resultHandler, RowSet<Row> rows) {
    JsonArray data = new JsonArray();
    for(Row row : rows) {
      data.add(new JsonObject()
        .put("deviceId", row.getValue("device_id"))
        .put("stepsCount", row.getValue("steps")));
    }
    resultHandler.handle(Future.succeededFuture(
      ServiceResponse.completedWithJson(data)
        .putHeader("Content-Type", "application/json")
        .setStatusCode(200)));
  }
}
