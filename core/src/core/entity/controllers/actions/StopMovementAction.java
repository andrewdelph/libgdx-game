package core.entity.controllers.actions;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.google.inject.Inject;
import core.app.screen.assets.animations.AnimationState;
import core.chunk.world.exceptions.BodyNotFound;
import core.common.exceptions.ChunkNotFound;
import core.entity.Entity;

public class StopMovementAction implements EntityAction {

  @Inject
  StopMovementAction() {}

  @Override
  public void apply(Entity entity) throws ChunkNotFound, BodyNotFound {
    entity.getEntityStateMachine().attemptTransition(AnimationState.DEFAULT);
    entity.applyBody(this.applyBodyConsumer());
  }

  @Override
  public MyConsumer<Body> applyBodyConsumer() {
    return body -> {
      body.setLinearVelocity(new Vector2(0, body.getLinearVelocity().y));
    };
  }

  @Override
  public Boolean isValid(Entity entity) {
    return true;
  }
}
