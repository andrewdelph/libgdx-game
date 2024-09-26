package core.networking.events.consumer.server.incoming;

import com.badlogic.gdx.Gdx;
import com.google.inject.Inject;
import core.app.game.GameController;
import core.app.user.UserID;
import core.chunk.ActiveChunkManager;
import core.chunk.world.exceptions.BodyNotFound;
import core.chunk.world.exceptions.DestroyBodyException;
import core.common.GameSettings;
import core.common.events.types.EventType;
import core.common.exceptions.ChunkNotFound;
import core.common.exceptions.EntityNotFound;
import core.networking.events.EventTypeFactory;
import core.networking.events.types.incoming.ReplaceBlockIncomingEventType;
import core.networking.events.types.outgoing.ReplaceBlockOutgoingEventType;
import core.networking.server.ServerNetworkHandle;

public class ReplaceBlockIncomingConsumerServer implements MyConsumer<EventType> {

  @Inject ActiveChunkManager activeChunkManager;
  @Inject ServerNetworkHandle serverNetworkHandle;
  @Inject GameController gameController;

  @Override
  public void accept(EventType eventType) {
    ReplaceBlockIncomingEventType incoming = (ReplaceBlockIncomingEventType) eventType;
    try {
      gameController.triggerReplaceEntity(incoming.getTarget(), incoming.getReplacementBlock());
    } catch (EntityNotFound e) {
      Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
      serverNetworkHandle.initHandshake(incoming.getUserID(), incoming.getChunkRange());
    } catch (ChunkNotFound | BodyNotFound | DestroyBodyException e) {
      Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
      return;
    }
    ReplaceBlockOutgoingEventType outgoing =
        EventTypeFactory.createReplaceBlockOutgoingEvent(
            incoming.getTarget(), incoming.getReplacementBlock(), incoming.getChunkRange());
    for (UserID userID : activeChunkManager.getChunkRangeUsers(incoming.getChunkRange())) {
      if (incoming.getUserID().equals(userID)) {
        continue;
      }
      serverNetworkHandle.send(userID, outgoing.toNetworkEvent());
    }
  }
}
