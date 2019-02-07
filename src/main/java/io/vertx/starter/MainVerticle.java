package io.vertx.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.HashMap;

public class MainVerticle extends AbstractVerticle {

  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";
  private final static HashMap<String, String> caches = new HashMap<String, String>();

  private final Logger LOGGER = LoggerFactory.getLogger("Cache-Verticle");

  @Override
  public void start(Future<Void> startFuture) {

    caches.put("localhost", "CARTCACHE");

    // HTTP API
    Router baseRouter = Router.router(vertx);
    baseRouter.route().handler(BodyHandler.create());
    baseRouter.get("/health").handler(rc -> rc.response().end("OK"));
    baseRouter.get("/*").handler(StaticHandler.create());

    Router apiRouter = Router.router(vertx);
    apiRouter.route("/*").handler(BodyHandler.create());
    apiRouter.get("/caches").handler(this::getCachesHandler);

    baseRouter.mountSubRouter("/api", apiRouter);

    vertx
      .createHttpServer()
      .requestHandler(baseRouter::accept)
      .listen(8080, result -> {
        if (result.succeeded()) {
          startFuture.complete();
        } else {
          startFuture.fail(result.cause());
        }
      });

  }

  private void getCachesHandler(RoutingContext routingContext) {
    routingContext.response()
      .setStatusCode(200)
      .putHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
      .end(new JsonObject().put("caches", caches).encodePrettily());
  }
}


