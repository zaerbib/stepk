package org.io.service.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.ext.web.api.service.WebApiServiceGen;
import io.vertx.rxjava3.ext.auth.mongo.MongoAuthentication;
import io.vertx.rxjava3.ext.auth.mongo.MongoUserUtil;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import org.io.service.model.User;
import org.io.service.service.impl.UserProfileServiceApiImpl;


@WebApiServiceGen
public interface UserProfileServiceApi {

  String WEBSERVICE_ADDRESS_USERPROFILESERVICEAPIAPI = "user.profile.service.api";

  static UserProfileServiceApi create(MongoAuthentication authProvider,
                                      MongoUserUtil userutil,
                                      MongoClient mongoClient) {
    return new UserProfileServiceApiImpl(authProvider, userutil, mongoClient);
  }

  void authenticate(User body,
                    ServiceRequest request,
                    Handler<AsyncResult<ServiceResponse>> resultHandler);
  void getDeviceIdFromOwns(String deviceId,
                           ServiceRequest request,
                           Handler<AsyncResult<ServiceResponse>> resultHandler);
  void getUserFromUsername(User body,
                           ServiceRequest request,
                           Handler<AsyncResult<ServiceResponse>> resultHandler);
  void registerUser(User body,
                    ServiceRequest request,
                    Handler<AsyncResult<ServiceResponse>> resultHandler);
  void updateUserFromUsername(User body,
                              ServiceRequest request,
                              Handler<AsyncResult<ServiceResponse>> resultHandler);
}
