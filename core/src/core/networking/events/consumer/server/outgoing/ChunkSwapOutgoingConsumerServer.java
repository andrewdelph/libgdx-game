package core.networking.events.consumer.server.outgoing;

import com.badlogic.gdx.Gdx;
import com.google.inject.Inject;
import core.app.user.UserID;
import core.chunk.ActiveChunkManager;
import core.common.CommonFactory;
import core.common.GameSettings;
import core.common.GameStore;
import core.common.events.types.EventType;
import core.common.exceptions.EntityNotFound;
import core.entity.Entity;
import core.networking.events.EventTypeFactory;
import core.networking.events.types.outgoing.ChunkSwapOutgoingEventType;
import core.networking.events.types.outgoing.CreateEntityOutgoingEventType;
import core.networking.server.ServerNetworkHandle;
import java.util.HashSet;
import java.util.Set;
import networking.NetworkObjects;

public class ChunkSwapOutgoingConsumerServer implements MyConsumer<EventType> {

  @Inject ActiveChunkManager activeChunkManager;
  @Inject ServerNetworkHandle serverNetworkHandle;
  @Inject GameStore gameStore;

  @Override
  public void accept(EventType eventType) {
    /*
     Happens when an entity is chunk swapped on the server.
     The client needs to remove the entity if they don't have the chunk that it is swapped to.
     If the client doesn't have the chunk the entity is swapped into. It needs to create that entity.

     userIDSFrom are the users which don't have the chunks moved to
     userIDSTo are the users which don't have the chunk moved from

    */
    ChunkSwapOutgoingEventType chunkSwapOutgoingEventType = (ChunkSwapOutgoingEventType) eventType;
    NetworkObjects.NetworkEvent chunkSwapOutgoingNetworkEvent =
        chunkSwapOutgoingEventType.toNetworkEvent();

    Set<UserID> userIDSFromOriginal =
        activeChunkManager.getChunkRangeUsers(chunkSwapOutgoingEventType.getFrom());
    Set<UserID> userIDSToOriginal =
        activeChunkManager.getChunkRangeUsers(chunkSwapOutgoingEventType.getTo());

    Set<UserID> userIDSFrom = (new HashSet<>(userIDSFromOriginal));
    userIDSFrom.removeAll(userIDSToOriginal);

    Set<UserID> userIDSTo = (new HashSet<>(userIDSToOriginal));
    userIDSTo.removeAll(userIDSFromOriginal);

    for (UserID userID : userIDSFrom) {
      serverNetworkHandle.send(userID, chunkSwapOutgoingNetworkEvent);
    }

    Entity entityToCreate;
    try {
      entityToCreate = gameStore.getEntity(chunkSwapOutgoingEventType.getTarget());
    } catch (EntityNotFound e) {
      Gdx.app.error(
          GameSettings.LOG_TAG, ("Unable to fetch entity for creation in chunk swap outgoing"));
      return;
    }

    CreateEntityOutgoingEventType createEntityOutgoingEventType =
        EventTypeFactory.createCreateEntityOutgoingEvent(
            entityToCreate.toNetworkData(),
            CommonFactory.createChunkRange(
                entityToCreate.getCoordinatesWrapper().getCoordinates()));

    NetworkObjects.NetworkEvent createEntityOutgoingNetworkEvent =
        createEntityOutgoingEventType.toNetworkEvent();

    for (UserID userID : userIDSTo) {
      serverNetworkHandle.send(userID, createEntityOutgoingNetworkEvent);
    }
  }
}
