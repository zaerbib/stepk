package org.io.service.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.rxjava3.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.io.service.config.SqlQueries;
import org.io.service.service.StepAccountActivityService;

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
        row -> {
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
        },
        err -> {
          log.error("Woops !!!", err);
          resultHandler.handle(
            Future.succeededFuture(
              new ServiceResponse().setStatusCode(500)
                .setStatusMessage("Technical Error")
            )
          );
        }
      );
  }

  @Override
  public void getStepCountForParticularMonth(String deviceId,
                                             String year,
                                             String month,
                                             ServiceRequest request,
                                             Handler<AsyncResult<ServiceResponse>> resultHandler) {

  }

  @Override
  public void getStepCountParticularDay(String deviceId, String year, String month, String day, ServiceRequest request, Handler<AsyncResult<ServiceResponse>> resultHandler) {

  }

  @Override
  public void getRankingLast24Hours(ServiceRequest request, Handler<AsyncResult<ServiceResponse>> resultHandler) {

  }
}
