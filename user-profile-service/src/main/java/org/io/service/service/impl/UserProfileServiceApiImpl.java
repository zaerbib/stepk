package org.io.service.service.impl;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeSource;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.rxjava3.ext.auth.mongo.MongoAuthentication;
import io.vertx.rxjava3.ext.auth.mongo.MongoUserUtil;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import lombok.extern.slf4j.Slf4j;
import org.io.service.model.User;
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
  public void authenticate(User body,
                           ServiceRequest request,
                           Handler<AsyncResult<ServiceResponse>> resultHandler) {
    authProvider.rxAuthenticate(body.toJson())
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
  public void getUserFromUsername(User body,
                                  ServiceRequest request,
                                  Handler<AsyncResult<ServiceResponse>> resultHandler) {

  }

  @Override
  public void registerUser(User body,
                           ServiceRequest request,
                           Handler<AsyncResult<ServiceResponse>> resultHandler) {
    String username = body.getUsername();
    String password = body.getPassword();

    JsonObject extraInfo = new JsonObject()
      .put("$set", new JsonObject())
      .put("email", body.getEmail())
      .put("city", body.getCity())
      .put("deviceId", body.getDeviceId())
      .put("makePublic", body.getMakePublic());

    userUtil.rxCreateUser(username, password)
      .flatMapMaybe(docId -> insertExtraInfo(extraInfo, docId))
      .ignoreElement()
      .subscribe(
        () -> completeRegistration(resultHandler),
        err -> handleRegistrationError(err, resultHandler)
      );
  }

  @Override
  public void updateUserFromUsername(User body,
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

  private MaybeSource<? extends JsonObject> insertExtraInfo(JsonObject extraInfo, String docId) {
    JsonObject query = new JsonObject().put("_id", docId);
    return mongoClient
      .rxFindOneAndUpdate("user", query, extraInfo)
      .onErrorResumeNext(err -> deleteInCompleteUser(query, err));
  }

  private MaybeSource<? extends JsonObject> deleteInCompleteUser(JsonObject query, Throwable err) {
    if(isIndexViolated(err)) {
      return mongoClient
        .rxRemoveDocument("user", query)
        .flatMap(del -> Maybe.error(err));
    } else {
      return Maybe.error(err);
    }
  }

  private boolean isIndexViolated(Throwable err) {
    return err.getMessage().contains("E11000");
  }

  private void completeRegistration(Handler<AsyncResult<ServiceResponse>> resultHandler) {
    resultHandler.handle(Future.succeededFuture(
      new ServiceResponse()
        .setStatusCode(200)
    ));
  }

  private void handleRegistrationError(Throwable err,
                                       Handler<AsyncResult<ServiceResponse>> resultHandler) {
    if(isIndexViolated(err)) {
      log.error("Registration failure: {}", err.getMessage());
      resultHandler.handle(Future.succeededFuture(
        new ServiceResponse()
          .setStatusCode(409)
      ));
    } else {
      fail500(err, resultHandler);
    }
  }
}
