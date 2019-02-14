package io.vertx.starter;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Manages the interactions with the CacheController server.
 */
public class CacheController<K, V> {

  private final Vertx vertx;
  private final RemoteCache<K, V> cache;
  private static final String host = "localhost";

  private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("io.vertx.starter");

  public static <K, V> Single<CacheController<K, V>> create(String host, int port, Vertx vertx) {
    LOGGER.finest("creating cache for " + host);
    // default port is 11222
    Configuration config = new ConfigurationBuilder().addServer().host(host).port(port).build();
    return vertx.
      <RemoteCache<K, V>>rxExecuteBlocking(
        future -> {
          RemoteCache<K, V> cache = new RemoteCacheManager(config).getCache();
          future.complete(cache);
        }
      )
      .map(rc -> new CacheController<>(vertx, rc));
  }

  private CacheController(Vertx vertx, RemoteCache<K, V> rc) {
    this.vertx = vertx;
    this.cache = rc;
  }

  public Completable remove(K key) {
    return vertx.rxExecuteBlocking(
      future -> {
        cache.remove(key);
        future.complete();
      }
    ).toCompletable();
  }

  public Single<Optional<V>> get(K key) {
    // While this method can use the Maybe type, I found easier to use an Optional.
    return vertx.rxExecuteBlocking(future -> {
      V value = cache.get(key);
      future.complete(Optional.ofNullable(value));
    });
  }

  public Completable put(K key, V value, long ttl) {
    return vertx.rxExecuteBlocking(future -> {
      cache.put(key, value, ttl, TimeUnit.SECONDS);
      future.complete();
    }).toCompletable();
  }

  public JsonObject toJsonObject(){
    return new JsonObject().put("host", this.host).put("size", this.cache.size());
  }

}
