package org.io.service;

import io.vertx.rxjava3.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.io.service.core.UserProfileVerticle;

/**
 * Hello world!
 *
 */

@Slf4j
public class UserProfileServiceApp
{
    public static void main( String[] args ) {
      System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
      Vertx vertx = Vertx.vertx();

      vertx
        .rxDeployVerticle(new UserProfileVerticle())
        .subscribe(
          ok -> log.info("Deploys succesfull !!!"),
          err -> log.info("Woops", err)
        );
    }
}
