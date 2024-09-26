package core.entity.controllers.actions;

import com.badlogic.gdx.physics.box2d.Body;
import core.chunk.world.exceptions.BodyNotFound;
import core.common.exceptions.ChunkNotFound;
import core.entity.Entity;

public interface EntityAction {

  void apply(Entity entity) throws ChunkNotFound, BodyNotFound;

  MyConsumer<Body> applyBodyConsumer();

  Boolean isValid(Entity entity) throws ChunkNotFound;
}
