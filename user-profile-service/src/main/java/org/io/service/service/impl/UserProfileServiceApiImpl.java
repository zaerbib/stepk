package org.io.service.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.rxjava3.ext.auth.mongo.MongoAuthentication;
import io.vertx.rxjava3.ext.auth.mongo.MongoUserUtil;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import io.vertx.rxjava3.ext.web.validation.RequestParameter;
import lombok.extern.slf4j.Slf4j;
import org.io.service.service.UserProfileServiceApi;

import java.util.NoSuchElementException;

@Slf4j
public class UserProfileServiceApiImpl implements UserProfileServiceApi {

  private final MongoAuthentication authProvider;
  private final MongoUserUtil userUtil;
  private final MongoClient mongoClient;

  public UserProfileServiceApiImpl(MongoAuthentication authProvider, MongoUserUtil userUtil, MongoClient mongoClient) {
    this.authProvider = authProvider;
    this.userUtil = userUtil;
    this.mongoClient = mongoClient;
  }

  @Override
  public void authenticate(RequestParameter body,
                           ServiceRequest request,
                           Handler<AsyncResult<ServiceResponse>> resultHandler) {
    authProvider.rxAuthenticate(body.getJsonObject())
      .subscribe(
        user -> completeEmptySuccess(user.principal(), resultHandler),
        err -> handleAuthenticationError(err, resultHandler)
      );
  }

  @Override
  public void getDeviceIdFromOwns(String deviceId,
                                  ServiceRequest request,
                                  Handler<AsyncResult<ServiceResponse>> resultHandler) {
    JsonObject query = new JsonObject().put("deviceId", deviceId);
    JsonObject fields = new JsonObject()
      .put("_id", 0)
      .put("username", 1)
      .put("deviceId", 1);

    mongoClient.rxFindOne("user", query, fields)
      .toSingle()
      .subscribe(
        user -> completeFetchRequest(user, resultHandler),
        err -> handleFetchError(err, resultHandler)
      );
  }

  @Override
  public void getUserFromUsername(RequestParameter body,
                                  ServiceRequest request,
                                  Handler<AsyncResult<ServiceResponse>> resultHandler) {

  }

  @Override
  public void registerUser(RequestParameter body,
                           ServiceRequest request,
                           Handler<AsyncResult<ServiceResponse>> resultHandler) {

  }

  @Override
  public void updateUserFromUsername(RequestParameter body,
                                     ServiceRequest request,
                                     Handler<AsyncResult<ServiceResponse>> resultHandler) {

  }

  private void completeEmptySuccess(JsonObject user,
                                    Handler<AsyncResult<ServiceResponse>> resultHandler) {
    resultHandler.handle(Future.succeededFuture(
      ServiceResponse.completedWithJson(user)
        .putHeader("Content-Type", "application/json")
        .setStatusCode(200)
    ));
  }

  private void handleAuthenticationError(Throwable err,
                                         Handler<AsyncResult<ServiceResponse>> resultHandler) {
    log.error("Authentication problem {}", err.getMessage());
    resultHandler.handle(Future.succeededFuture(
      new ServiceResponse()
        .setStatusCode(401)
        .setStatusMessage("Authentication failed")
    ));
  }

  private void completeFetchRequest(JsonObject user,
                                    Handler<AsyncResult<ServiceResponse>> resultHandler) {
    resultHandler.handle(Future.succeededFuture(
      ServiceResponse.completedWithJson(user)
        .putHeader("Content-Type", "application/json")
        .setStatusCode(200)
    ));
  }

  private void handleFetchError(Throwable err,
                                Handler<AsyncResult<ServiceResponse>> resultHandler) {
    if(err instanceof NoSuchElementException) {
      resultHandler.handle(Future.succeededFuture(
        new ServiceResponse()
          .setStatusCode(404)
      ));
    } else {
      fail500(err, resultHandler);
    }
  }

  private void fail500(Throwable err,
                       Handler<AsyncResult<ServiceResponse>> resultHandler) {
    log.error("Woops", err);
    resultHandler.handle(Future.succeededFuture(
      new ServiceResponse()
        .setStatusCode(500)
    ));
  }
}
