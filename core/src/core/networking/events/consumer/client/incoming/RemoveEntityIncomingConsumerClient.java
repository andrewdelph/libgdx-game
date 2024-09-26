package core.networking.events.consumer.client.incoming;

import com.badlogic.gdx.Gdx;
import com.google.inject.Inject;
import core.app.game.GameController;
import core.chunk.world.exceptions.DestroyBodyException;
import core.common.GameSettings;
import core.common.events.types.EventType;
import core.common.exceptions.EntityNotFound;
import core.networking.events.types.incoming.RemoveEntityIncomingEventType;

public class RemoveEntityIncomingConsumerClient implements MyConsumer<EventType> {

  @Inject GameController gameController;

  @Override
  public void accept(EventType eventType) {
    RemoveEntityIncomingEventType incoming = (RemoveEntityIncomingEventType) eventType;
    try {
      gameController.triggerRemoveEntity(incoming.getTarget());
    } catch (EntityNotFound | DestroyBodyException e) {
      Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
    }
  }
}
