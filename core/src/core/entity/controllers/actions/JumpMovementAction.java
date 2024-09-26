package core.entity.controllers.actions;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import core.app.screen.assets.animations.AnimationState;
import core.chunk.world.exceptions.BodyNotFound;
import core.common.exceptions.ChunkNotFound;
import core.entity.Entity;
import core.entity.collision.ground.EntityGroundContact;

public class JumpMovementAction implements EntityAction {

  EntityGroundContact entityGroundContact;

  JumpMovementAction(EntityGroundContact entityGroundContact) {
    this.entityGroundContact = entityGroundContact;
  }

  @Override
  public void apply(Entity entity) throws ChunkNotFound, BodyNotFound {
    entity.getEntityStateMachine().attemptTransition(AnimationState.JUMPING);
    entity.applyBody(this.applyBodyConsumer());
  }

  @Override
  public MyConsumer<Body> applyBodyConsumer() {
    return (Body body) -> {
      body.setLinearVelocity(new Vector2(0, 9));
    };
  }

  @Override
  public Boolean isValid(Entity entity) throws ChunkNotFound {
    return this.entityGroundContact.isOnGround(entity);
  }
}
