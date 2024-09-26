package core.networking.events.consumer.client.incoming;

import com.badlogic.gdx.Gdx;
import com.google.inject.Inject;
import core.app.game.GameController;
import core.chunk.world.exceptions.BodyNotFound;
import core.chunk.world.exceptions.DestroyBodyException;
import core.common.GameSettings;
import core.common.events.types.EventType;
import core.common.exceptions.ChunkNotFound;
import core.common.exceptions.EntityNotFound;
import core.networking.client.ClientNetworkHandle;
import core.networking.events.types.incoming.ReplaceBlockIncomingEventType;

public class ReplaceBlockIncomingConsumerClient implements MyConsumer<EventType> {

  @Inject GameController gameController;
  @Inject ClientNetworkHandle clientNetworkHandle;

  @Override
  public void accept(EventType eventType) {
    ReplaceBlockIncomingEventType incoming = (ReplaceBlockIncomingEventType) eventType;
    try {
      gameController.triggerReplaceEntity(incoming.getTarget(), incoming.getReplacementBlock());
    } catch (EntityNotFound e) {
      Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
      clientNetworkHandle.initHandshake(incoming.getChunkRange());
    } catch (ChunkNotFound | BodyNotFound | DestroyBodyException e) {
      Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
    }
  }
}
