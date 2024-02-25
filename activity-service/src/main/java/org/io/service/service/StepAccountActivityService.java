package org.io.service.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.ext.web.api.service.WebApiServiceGen;
import io.vertx.rxjava3.pgclient.PgPool;
import org.io.service.service.impl.StepAccountActivityServiceImpl;

@WebApiServiceGen
public interface StepAccountActivityService {

  static StepAccountActivityService create(PgPool pgPool) {
    return new StepAccountActivityServiceImpl(pgPool);
  }
  void getStepCountTotal(String deviceId,
                         ServiceRequest request,
                         Handler<AsyncResult<ServiceResponse>> resultHandler);
  void getStepCountForParticularMonth(String deviceId,
                                      String year,
                                      String month,
                                      ServiceRequest request,
                                      Handler<AsyncResult<ServiceResponse>> resultHandler);
  void getStepCountParticularDay(String deviceId,
                                 String year,
                                 String month,
                                 String day,
                                 ServiceRequest request,
                                 Handler<AsyncResult<ServiceResponse>> resultHandler);
  void getRankingLast24Hours(ServiceRequest request,
                             Handler<AsyncResult<ServiceResponse>> resultHandler);
}
