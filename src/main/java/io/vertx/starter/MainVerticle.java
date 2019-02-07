package io.vertx.starter;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

import java.util.HashMap;

public class MainVerticle extends AbstractVerticle {

  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";
  private final static HashMap<String, Cache> caches = new HashMap<String, Cache>();

  private final Logger LOGGER = LoggerFactory.getLogger("Cache-Verticle");

  @Override
  public void start(Future<Void> startFuture) {

    // HTTP API
    Router baseRouter = Router.router(vertx);
    baseRouter.route().handler(BodyHandler.create());
    baseRouter.get("/health").handler(rc -> rc.response().end("OK"));
    baseRouter.get("/*").handler(StaticHandler.create());

    Router apiRouter = Router.router(vertx);
    apiRouter.route("/*").handler(BodyHandler.create());
    apiRouter.get("/caches").handler(this::getCachesHandler);

    baseRouter.mountSubRouter("/api", apiRouter);

    Completable retrieveCache = Cache.<String, String>create("localhost", vertx)
      .doOnSuccess(c -> this.caches.put(c.toString(), c))
      .toCompletable();

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


