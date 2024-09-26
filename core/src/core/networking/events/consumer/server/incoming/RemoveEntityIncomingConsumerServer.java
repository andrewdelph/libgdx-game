package core.networking.events.consumer.server.incoming;

import com.google.inject.Inject;
import core.app.game.GameController;
import core.app.user.User;
import core.app.user.UserID;
import core.chunk.ActiveChunkManager;
import core.chunk.world.exceptions.DestroyBodyException;
import core.common.events.types.EventType;
import core.common.exceptions.EntityNotFound;
import core.entity.ActiveEntityManager;
import core.networking.events.EventTypeFactory;
import core.networking.events.types.incoming.RemoveEntityIncomingEventType;
import core.networking.events.types.outgoing.RemoveEntityOutgoingEventType;
import core.networking.server.ServerNetworkHandle;

public class RemoveEntityIncomingConsumerServer implements MyConsumer<EventType> {

  @Inject GameController gameController;
  @Inject ServerNetworkHandle serverNetworkHandle;
  @Inject ActiveChunkManager activeChunkManager;
  @Inject ActiveEntityManager activeEntityManager;
  @Inject User user;

  @Override
  public void accept(EventType eventType) {
    RemoveEntityIncomingEventType incoming = (RemoveEntityIncomingEventType) eventType;

    try {
      gameController.triggerRemoveEntity(incoming.getTarget());
      activeEntityManager.removeActiveEntity(user.getUserID(), incoming.getTarget());
    } catch (EntityNotFound | DestroyBodyException e) {
      e.printStackTrace();
      this.serverNetworkHandle.initHandshake(incoming.getUserID(), incoming.getChunkRange());
    }

    RemoveEntityOutgoingEventType outgoing =
        EventTypeFactory.createRemoveEntityOutgoingEvent(
            incoming.getTarget(), incoming.getChunkRange());

    for (UserID userID : activeChunkManager.getChunkRangeUsers(incoming.getChunkRange())) {
      if (userID.equals(incoming.getUserID())) {
        continue;
      }
      serverNetworkHandle.send(userID, outgoing.toNetworkEvent());
    }
  }
}
