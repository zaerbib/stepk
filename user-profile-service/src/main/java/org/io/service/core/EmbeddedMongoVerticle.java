package org.io.service.core;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import org.slf4j.Logger;

public class EmbeddedMongoVerticle extends AbstractVerticle {

  private MongodExecutable exe;
  private int actualPort;

  @Override
  public void start() throws Exception {
    if(vertx != null && !context.isWorkerContext()) {
      throw new IllegalStateException("Must be started as worker verticle !");
    }

    JsonObject config = context.config();
    int port = config.getInteger("port", 0);

    final Version versionEnum = Version.valueOf("LATEST_NIGHTLY");

    IMongodConfig embeddedConfig = new MongodConfigBuilder()
      .version(versionEnum)
      .net(port == 0 ? new Net(): new Net(port, Network.localhostIsIPv6()))
      .build();

    Logger logger = (Logger) new SLF4JLogDelegateFactory()
      .createDelegate(EmbeddedMongoVerticle.class.getCanonicalName())
      .unwrap();

    IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
      .defaultsWithLogger(Command.MongoD, logger)
      .build();

    exe = MongodStarter.getInstance(runtimeConfig).prepare(embeddedConfig);
    exe.start();

    actualPort = embeddedConfig.net().getPort();
  }

  public int actualPort() {
    return actualPort;
  }

  @Override
  public void stop() throws Exception {
    exe.stop();
  }
}
