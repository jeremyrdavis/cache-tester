package io.vertx.starter;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

import java.util.HashMap;

public class MainVerticle extends AbstractVerticle {

  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

  private HashMap<String, CacheController> cacheControllers = new HashMap<String, CacheController>();

/* maps the caches to their servers */

  private JsonObject caches = new JsonObject();

  private final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("io.vertx.starter");

  @Override
  public void start(Future<Void> startFuture) {

    // HTTP API
    Router baseRouter = Router.router(vertx);
    baseRouter.route().handler(BodyHandler.create());
    baseRouter.get("/health").handler(rc -> rc.response().end("OK"));
    baseRouter.get("/*").handler(StaticHandler.create());

    Router apiRouter = Router.router(vertx);
    apiRouter.route("/*").handler(BodyHandler.create());
    apiRouter.post("/cache").handler(this::addCacheHandler);
    apiRouter.get("/cacheControllers").handler(this::getCachesHandler);

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
      .end(new JsonObject().put("cacheControllers", "foo").encodePrettily());
  }

  /**
   * {
   *     "host": "host_name",
   *     "cache":"cache_name"
   * }
   * @param routingContext
   */
  private void addCacheHandler(RoutingContext routingContext) {

    LOGGER.finest(routingContext.getBody().toString());

    JsonObject body = routingContext.getBodyAsJson();
    String host = body.getString("host");
    String cache = body.getString("cache");
    LOGGER.finest(host);
    LOGGER.finest(cache);
    if(this.cacheControllers.containsKey(host)){
      LOGGER.finest("cache controller contains host " + host);
      this.caches.getJsonArray(host).add(cache);
      LOGGER.finest(this.caches.getJsonArray(host).toString());
      routingContext.response()
        .setStatusCode(200)
        .putHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
        .end(this.caches.encodePrettily());
    }else{
      CacheController.create(host, vertx).doOnSuccess(c -> {
        this.cacheControllers.put(host, c);
        this.caches.put(host, new JsonArray().add(cache));
        routingContext.response()
          .setStatusCode(200)
          .putHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
          .end(this.caches.encodePrettily());
      }).subscribe();
    }
  }

}


