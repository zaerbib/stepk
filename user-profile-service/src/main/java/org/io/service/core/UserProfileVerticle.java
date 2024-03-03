package org.io.service.core;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.MongoAuthentication;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;
import io.vertx.ext.auth.mongo.MongoUserUtil;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import io.vertx.serviceproxy.ServiceBinder;
import lombok.extern.slf4j.Slf4j;
import org.io.service.config.MongoConfig;
import org.io.service.service.UserProfileServiceApi;

@Slf4j
public class UserProfileVerticle extends AbstractVerticle {
  private static final int HTTP_PORT = 9095;
  private MongoClient mongoClient;
  private MongoAuthentication authProvider;
  private MongoUserUtil userUtil;
  private HttpServer httpServer;

  @Override
  public void start(Promise<Void> startPromise) {
    this.initMongoFriendly();
    this.serviceBinder();
    this.startingHttpServer(startPromise);
  }

  private void initMongoFriendly() {
    mongoClient = MongoClient.createShared(vertx, MongoConfig.mongoConfig());
    authProvider = MongoAuthentication.create(mongoClient, new MongoAuthenticationOptions());
    userUtil = MongoUserUtil.create(mongoClient, new MongoAuthenticationOptions(), new MongoAuthorizationOptions());
  }

  private void serviceBinder() {
    ServiceBinder serviceBinder = new ServiceBinder(vertx);
    UserProfileServiceApi userServiceApi = UserProfileServiceApi.create(authProvider, userUtil, mongoClient);
    serviceBinder
      .setAddress(UserProfileServiceApi.WEBSERVICE_ADDRESS_USERPROFILESERVICEAPIAPI)
      .register(UserProfileServiceApi.class, userServiceApi);
  }

  private void startingHttpServer(Promise<Void> startPromise) {
    RouterBuilder.create(vertx, "openapi-user-profile.json")
      .flatMap(routerBuilder -> {
        RouterBuilderOptions factoryOptions = new RouterBuilderOptions()
          .setRequireSecurityHandlers(false)
          .setMountResponseContentTypeHandler(true);
        routerBuilder.setOptions(factoryOptions);

        routerBuilder.mountServicesFromExtensions();

        return Future.succeededFuture(routerBuilder.createRouter());
      }).flatMap(openapiRouter -> {
        httpServer = vertx.createHttpServer(new HttpServerOptions()
          .setPort(HTTP_PORT)
          .setHost("localhost"));

        Router router = Router.router(vertx);
        router.route("/*").subRouter(openapiRouter);

        router.route().last().handler(context ->
          context.response()
            .setStatusCode(404)
            .end(new JsonObject()
              .put("message", "Resource not found")
              .encode()));

        return httpServer.requestHandler(router).listen()
          .onSuccess(server -> log.info("UserProfileVerticle started on port " + server.actualPort()));
      }).onSuccess(server -> startPromise.complete())
      .onFailure(startPromise::fail);
  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    this.httpServer.close()
      .onSuccess(server -> stopPromise.complete())
      .onFailure(stopPromise::fail);
  }
}
