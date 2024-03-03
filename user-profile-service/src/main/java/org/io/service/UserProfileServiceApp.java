package org.io.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;
import org.io.service.core.UserProfileVerticle;

/**
 * Hello world!
 *
 */

@Slf4j
public class UserProfileServiceApp extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) {
    vertx.deployVerticle(UserProfileVerticle.class.getName())
      .onFailure(error -> {
        log.error("UserProfileServiceVerticle : Deployment failed");
        startPromise.fail(error);
      })
      .onSuccess(server -> {
        log.info("UserProfileServiceVerticle : Deployed");
        startPromise.complete();
      });
  }
}
