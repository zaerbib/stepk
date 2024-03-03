package org.io.service.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.MongoAuthentication;
import io.vertx.ext.auth.mongo.MongoUserUtil;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.io.service.model.User;
import org.io.service.service.UserProfileServiceApi;

import java.util.NoSuchElementException;
import java.util.regex.Pattern;

@Slf4j
public class UserProfileServiceApiImpl implements UserProfileServiceApi {

  private final MongoAuthentication authProvider;
  private final MongoUserUtil userUtil;
  private final MongoClient mongoClient;

  private final Pattern validUsername = Pattern.compile("\\w[\\w+|-]*");
  private final Pattern validDeviceId = Pattern.compile("\\w[\\w+|-]*");
  private final Pattern validEmail = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

  public UserProfileServiceApiImpl(MongoAuthentication authProvider, MongoUserUtil userUtil, MongoClient mongoClient) {
    this.authProvider = authProvider;
    this.userUtil = userUtil;
    this.mongoClient = mongoClient;
  }

  @Override
  public void authenticate(User body, ServiceRequest request, Handler<AsyncResult<ServiceResponse>> resultHandler) {
    authProvider.authenticate(fromPojoToJson(body))
      .onSuccess(user ->
        completeEmptySuccess(user.principal(), resultHandler)
      ).onFailure(err ->
        handleAuthenticationError(err, resultHandler)
      );
  }

  @Override
  public void getDeviceIdFromOwns(String deviceId, ServiceRequest request, Handler<AsyncResult<ServiceResponse>> resultHandler) {
    JsonObject query = new JsonObject().put("deviceId", deviceId);
    JsonObject fields = new JsonObject().put("_id", 0).put("username", 1).put("deviceId", 1);

    mongoClient.findOne("user", query, fields).onSuccess(user ->
      completeFetchRequest(user, resultHandler)
    ).onFailure(err ->
      handleFetchError(err, resultHandler)
    );
  }

  @Override
  public void getUserFromUsername(User body, ServiceRequest request, Handler<AsyncResult<ServiceResponse>> resultHandler) {

  }

  @Override
  public void registerUser(User body, ServiceRequest request, Handler<AsyncResult<ServiceResponse>> resultHandler) {

    this.validationRegistration(body, resultHandler);
    JsonObject extraInfo = new JsonObject().put("$set", new JsonObject()
      .put("email", body.getEmail())
      .put("city", body.getCity())
      .put("deviceId", body.getDeviceId())
      .put("makePublic", body.getMakePublic()));

    userUtil.createUser(body.getUsername(), body.getPassword())
      .map(docId -> {
        insertExtraInfo(extraInfo, docId);
        return docId;
      }).onSuccess(data -> completeRegistration(resultHandler))
      .onFailure(err -> handleRegistrationError(err, resultHandler));
  }

  @Override
  public void updateUserFromUsername(User body, ServiceRequest request, Handler<AsyncResult<ServiceResponse>> resultHandler) {

  }

  private void completeEmptySuccess(JsonObject user, Handler<AsyncResult<ServiceResponse>> resultHandler) {
    resultHandler.handle(Future.succeededFuture(ServiceResponse.completedWithJson(user)
      .putHeader("Content-Type", "application/json")
      .setStatusCode(200)));
  }

  private void handleAuthenticationError(Throwable err, Handler<AsyncResult<ServiceResponse>> resultHandler) {
    log.error("Authentication problem {}", err.getMessage());
    resultHandler.handle(Future.succeededFuture(
      new ServiceResponse()
        .setStatusCode(401)
        .setStatusMessage("Authentication failed")));
  }

  private void completeFetchRequest(JsonObject user, Handler<AsyncResult<ServiceResponse>> resultHandler) {
    resultHandler.handle(Future.succeededFuture(ServiceResponse.completedWithJson(user).putHeader("Content-Type", "application/json").setStatusCode(200)));
  }

  private void handleFetchError(Throwable err, Handler<AsyncResult<ServiceResponse>> resultHandler) {
    if (err instanceof NoSuchElementException) {
      resultHandler.handle(Future.succeededFuture(new ServiceResponse().setStatusCode(404)));
    } else {
      fail500(err, resultHandler);
    }
  }

  private void fail500(Throwable err, Handler<AsyncResult<ServiceResponse>> resultHandler) {
    log.error("Woops", err);
    resultHandler.handle(Future.succeededFuture(new ServiceResponse().setStatusCode(500)));
  }

  private void insertExtraInfo(JsonObject extraInfo, String docId) {
    JsonObject query = new JsonObject().put("_id", docId);
    mongoClient.findOneAndUpdate("user", query, extraInfo).onFailure(err -> deleteInCompleteUser(query, err));
  }

  private void deleteInCompleteUser(JsonObject query, Throwable err) {
    if (isIndexViolated(err)) {
      mongoClient.removeDocument("user", query).onFailure(Future::failedFuture);
    }
  }

  private boolean isIndexViolated(Throwable err) {
    return err.getMessage().contains("E11000");
  }

  private void completeRegistration(Handler<AsyncResult<ServiceResponse>> resultHandler) {
    resultHandler.handle(Future.succeededFuture(new ServiceResponse().setStatusCode(200)));
  }

  private void handleRegistrationError(Throwable err,
                                       Handler<AsyncResult<ServiceResponse>> resultHandler) {
    if (isIndexViolated(err)) {
      log.error("Registration failure: {}", err.getMessage());
      resultHandler.handle(Future.succeededFuture(new ServiceResponse().setStatusCode(409)));
    } else {
      fail500(err, resultHandler);
    }
  }

  private void validationRegistration(User user,
                                      Handler<AsyncResult<ServiceResponse>> resultHandler) {
    if (anyRegistrationFieldIsMissing(user) || anyRegistrationFieldIsWrong(user)) {
      resultHandler.handle(Future.succeededFuture(
        new ServiceResponse()
          .setStatusCode(400)
      ));
    }
  }

  private boolean anyRegistrationFieldIsMissing(User user) {
    JsonObject userObject = fromPojoToJson(user);
    return !(userObject.containsKey("username") &&
      userObject.containsKey("password") &&
      userObject.containsKey("email") &&
      userObject.containsKey("city") &&
      userObject.containsKey("deviceId") &&
      userObject.containsKey("makePublic"));
  }

  private boolean anyRegistrationFieldIsWrong(User user) {
    return !validUsername.matcher(user.getUsername()).matches() ||
      !validEmail.matcher(user.getEmail()).matches() ||
      user.getPassword().trim().isEmpty() ||
      !validDeviceId.matcher(user.getDeviceId()).matches();
  }

  private JsonObject fromPojoToJson(User user) {
    return new JsonObject()
      .put("username", user.getUsername())
      .put("password", user.getPassword())
      .put("email", user.getEmail())
      .put("city", user.getCity())
      .put("deviceId", user.getDeviceId())
      .put("makePublic", user.getMakePublic());
  }
}
