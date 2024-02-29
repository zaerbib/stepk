package org.io.service.core;

import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.core.http.HttpServer;
import io.vertx.rxjava3.ext.auth.mongo.MongoAuthentication;
import io.vertx.rxjava3.ext.auth.mongo.MongoUserUtil;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.ext.web.openapi.RouterBuilder;
import io.vertx.serviceproxy.ServiceBinder;
import lombok.extern.slf4j.Slf4j;
import org.io.service.config.MongoConfig;
import org.io.service.service.UserProfileServiceApi;

@Slf4j
public class UserProfileVerticle extends AbstractVerticle {
  private static final int HTTP_PORT = 9098;
  private MongoClient mongoClient;
  private MongoAuthentication authProvider;
  private MongoUserUtil userUtil;
  private HttpServer httpServer;

  @Override
  public Completable rxStart() {
    this.initMongFriendly();
    this.serviceBinder();
    this.startingHttpServer();
    return super.rxStart();
  }

  private void initMongFriendly() {
    mongoClient = MongoClient.createShared(vertx, MongoConfig.mongoConfig());
    authProvider = MongoAuthentication.create(mongoClient, new MongoAuthenticationOptions());
    userUtil = MongoUserUtil.create(mongoClient, new MongoAuthenticationOptions(), new MongoAuthorizationOptions());
  }

  private void serviceBinder() {
    ServiceBinder serviceBinder = new ServiceBinder(vertx.getDelegate());
    UserProfileServiceApi userServiceApi = UserProfileServiceApi.create(authProvider, userUtil, mongoClient);
    serviceBinder
      .setAddress(UserProfileServiceApi.WEBSERVICE_ADDRESS_USERPROFILESERVICEAPIAPI)
      .register(UserProfileServiceApi.class, userServiceApi);
  }

  private void startingHttpServer() {
    RouterBuilder.create(vertx, "openapi.json")
      .doOnError(Throwable::printStackTrace)
      .subscribe(
        routerBuilder -> {
          routerBuilder.mountServicesFromExtensions();
          Router router = Router.newInstance(routerBuilder.createRouter().getDelegate());
          router.errorHandler(400, ctx -> log.debug("Bad request : " + ctx.failure()));

          httpServer = vertx.createHttpServer(new HttpServerOptions()
            .setPort(HTTP_PORT)
            .setHost("localhost"));
          httpServer.requestHandler(router);
          httpServer.getDelegate().listen().mapEmpty();
        },
        err -> log.debug("HttpServer Failed to start")
      );
  }
}
