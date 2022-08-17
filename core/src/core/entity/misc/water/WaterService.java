package core.entity.misc.water;

import core.common.Coordinates;
import java.util.HashSet;
import java.util.Set;

public class WaterService {

  Set<Coordinates> waterSet = new HashSet<>();

  public void init() {}

  public void reset() {
    waterSet.clear();
  }

  public void registerPosition(Coordinates coordinates) {
    waterSet.add(coordinates.getBase());
  }

  public synchronized boolean hasPosition(Coordinates coordinates) {
    return waterSet.contains(coordinates.getBase());
  }

  public synchronized boolean hasPosition(Coordinates coordinates, Runnable runnable) {
    boolean hasWater = this.hasPosition(coordinates);
    if (!hasWater) runnable.run();
    return hasWater;
  }
}
