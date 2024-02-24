package org.io.service.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.rxjava3.pgclient.PgPool;
import io.vertx.rxjava3.sqlclient.Tuple;
import org.io.service.config.SqlQueries;
import org.io.service.service.StepAccountActivityService;

public class StepAccountActivityServiceImpl implements StepAccountActivityService {
  private final PgPool pgPool;

  public StepAccountActivityServiceImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public void getStepCountTotal(String deviceId,
                                ServiceRequest request,
                                Handler<AsyncResult<ServiceResponse>> resultHandler) {

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
