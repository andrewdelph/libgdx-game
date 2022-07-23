package core.entity.collision.ground;

import core.common.ChunkRange;
import core.entity.Entity;
import core.entity.collision.CollisionPoint;

public class EntityFeetSensor extends CollisionPoint {

  public EntityFeetSensor(Entity entity, ChunkRange chunkRange) {
    super(entity, chunkRange);
  }
}
