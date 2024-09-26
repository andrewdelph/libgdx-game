package core.common;

import com.badlogic.gdx.Gdx;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import networking.NetworkObjects;
import org.javatuples.Pair;

public class CommonFactory {

  static LoadingCache<Pair<Float, Float>, Coordinates> coordinatesCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(10, TimeUnit.SECONDS)
          .build(
              new CacheLoader<Pair<Float, Float>, Coordinates>() {
                public Coordinates load(Pair<Float, Float> pair) {
                  return new Coordinates(pair.getValue0(), pair.getValue1());
                }
              });

  static LoadingCache<Coordinates, ChunkRange> chunkRangeCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(10, TimeUnit.MINUTES)
          .build(
              new CacheLoader<Coordinates, ChunkRange>() {
                public ChunkRange load(Coordinates coordinates) {
                  return new ChunkRange(coordinates);
                }
              });

  public static Coordinates createCoordinates(float x, float y) {
    try {
      return coordinatesCache.get(new Pair<>(x, y));
    } catch (ExecutionException e) {
      Gdx.app.error(GameSettings.LOG_TAG, ("Failed to load Coordinates cache."), e);
      return new Coordinates(x, y);
    }
  }

  public static ChunkRange createChunkRange(Coordinates coordinates) {
    try {
      return chunkRangeCache.get(coordinates);
    } catch (ExecutionException e) {
      Gdx.app.error(GameSettings.LOG_TAG, ("Failed to load ChunkRange cache."), e);
      return new ChunkRange(coordinates);
    }
  }

  public static ChunkRange createChunkRange(NetworkObjects.NetworkData networkData) {
    float x = 0, y = 0;
    for (NetworkObjects.NetworkData child : networkData.getChildrenList()) {
      if (child.getKey().equals("x")) {
        x = Float.parseFloat(child.getValue());
      } else if (child.getKey().equals("y")) {
        y = Float.parseFloat(child.getValue());
      }
    }
    return createChunkRange(CommonFactory.createCoordinates(x, y));
  }
}
