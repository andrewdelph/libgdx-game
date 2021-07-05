package infra.entity.pathfinding.template;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import infra.entity.Entity;

public class GreedyEdge extends AbstractEdge {
  RelativeCoordinates currentRelativeCoordinates;

  public GreedyEdge(BlockStructure blockStructure, RelativeVertex from, RelativeVertex to) {
    super(blockStructure, from, to);
    this.currentRelativeCoordinates = from.relativeCoordinates;
  }

  @Override
  public void follow(Body body, Entity entity) {
    String actionKey;

    // if difference is too little, just set.
    RelativeCoordinates difference = currentRelativeCoordinates.sub(this.to.relativeCoordinates);
    if (Math.abs(difference.relativeX) < 0.2) {
      Vector2 setBodyPosition = body.getPosition().cpy().add(difference.toVector2());
      body.setTransform(setBodyPosition, 0);
      this.finish();
      return;
    }

    if (currentRelativeCoordinates.relativeX < to.relativeCoordinates.relativeX) {
      actionKey = "right";
    } else {
      actionKey = "left";
    }

    Vector2 startingPosition = body.getPosition().cpy();
    entity.entityController.applyAction(actionKey, body);
    Vector2 endingPosition = body.getPosition().cpy();

    Vector2 differencePosition = endingPosition.sub(startingPosition);

    currentRelativeCoordinates =
        currentRelativeCoordinates.add(new RelativeCoordinates(differencePosition));
  }
}
