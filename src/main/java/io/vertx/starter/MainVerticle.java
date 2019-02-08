package io.vertx.starter;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class MainVerticle extends AbstractVerticle {

  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

  private final static HashMap<String, CacheController> cacheControllers = new HashMap<String, CacheController>();

  /* maps the caches to their servers */
  private final static JsonObject caches = new JsonObject();

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

//    Completable retrieveCache = CacheController.<String, String>create("localhost", vertx)
//      .doOnSuccess(c -> this.cacheControllers.put(c.toString(), c))
//      .toCompletable();

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
      .end(new JsonObject().put("cacheControllers", cacheControllers).encodePrettily());
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

    /*
    Single.just(routingContext.getBodyAsJson())
      .doOnSuccess(c -> {
        String host = c.getString("host");
        String cache = c.getString("cache");
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
          CacheController.create(host, vertx).doOnSuccess(n -> {
            LOGGER.finest("cache controller does not contains host " + host);
            this.cacheControllers.put(host, n);
            LOGGER.finest("added CacheController for host " + host);
            this.caches.put(host, new JsonArray().add(cache));
            LOGGER.finest(this.caches.getJsonArray(host).toString());
            routingContext.response()
              .setStatusCode(200)
              .putHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
              .end(this.caches.encodePrettily());
          }).doOnError(e -> {
            routingContext.response()
              .setStatusCode(500)
              .putHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
              .end(new JsonObject().put("error", e.getMessage()).encodePrettily());
          });
        }
      }).doOnError(e -> {
      routingContext.response()
        .setStatusCode(500)
        .putHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
        .end(new JsonObject().put("error", e.getMessage()).encodePrettily());
    }).subscribe();
    Single.just(routingContext.getBodyAsJson().getJsonObject("cache"))
      .doOnSuccess(c -> {
        if(this.caches.containsKey(c.getString("host"))){
          this.caches.getJsonArray(c.getString("host")).add(c.getString("cache"));
        }else{
          CacheController.create(c.getString("host"), vertx).doOnSuccess(n -> {
            this.cacheControllers.put(c.getString("host"), n);
            this.caches.put(c.getString("host"), new JsonArray().add(c.getString("cache")));
          });
        }
        routingContext.response()
          .setStatusCode(200)
          .putHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
          .end(this.caches.encodePrettily());
      }).doOnError(e -> {
      routingContext.response()
        .setStatusCode(500)
        .putHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
        .end(new JsonObject().put("error", e.getMessage()).encodePrettily());
    }).subscribe();
*/
  }

}


