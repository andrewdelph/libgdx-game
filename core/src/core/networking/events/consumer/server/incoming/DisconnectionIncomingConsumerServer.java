package core.networking.events.consumer.server.incoming;

import com.google.inject.Inject;
import core.app.user.UserID;
import core.chunk.ActiveChunkManager;
import core.common.CommonFactory;
import core.common.GameStore;
import core.common.events.EventService;
import core.common.events.types.EventType;
import core.common.exceptions.EntityNotFound;
import core.entity.ActiveEntityManager;
import core.entity.Entity;
import core.networking.ConnectionStore;
import core.networking.events.EventTypeFactory;
import core.networking.events.types.incoming.DisconnectionIncomingEventType;
import core.networking.events.types.outgoing.RemoveEntityOutgoingEventType;
import core.networking.server.ServerNetworkHandle;
import java.util.UUID;
import java.util.logging.Logger;

public class DisconnectionIncomingConsumerServer implements MyConsumer<EventType> {

  private static final Logger LOGGER = Logger.getLogger(GameStore.class.getName());
  @Inject EventService eventService;
  @Inject ServerNetworkHandle serverNetworkHandle;
  @Inject GameStore gameStore;
  @Inject EventTypeFactory eventTypeFactory;
  @Inject ConnectionStore connectionStore;
  @Inject ActiveChunkManager activeChunkManager;
  @Inject ActiveEntityManager activeEntityManager;

  @Override
  public void accept(EventType eventType) {
    DisconnectionIncomingEventType realEvent = (DisconnectionIncomingEventType) eventType;
    connectionStore.removeConnection(realEvent.getUserID());
    activeChunkManager.removeUser(realEvent.getUserID());
    for (UUID ownersEntityUuid :
        activeEntityManager.getUserActiveEntitySet(realEvent.getUserID())) {
      Entity entity;
      try {
        entity = this.gameStore.getEntity(ownersEntityUuid);
      } catch (EntityNotFound e) {
        LOGGER.severe("DISCONNECT COULD NOT FIND AN ENTITY IT OWNS");
        e.printStackTrace();
        continue;
      }
      this.eventService.queuePostUpdateEvent(
          eventTypeFactory.createRemoveEntityEvent(ownersEntityUuid));

      RemoveEntityOutgoingEventType removeEntityOutgoingEvent =
          EventTypeFactory.createRemoveEntityOutgoingEvent(
              entity.getUuid(),
              CommonFactory.createChunkRange(entity.getCoordinatesWrapper().getCoordinates()));

      for (UserID subscriptionUserID :
          activeChunkManager.getChunkRangeUsers(
              CommonFactory.createChunkRange(entity.getCoordinatesWrapper().getCoordinates()))) {
        serverNetworkHandle.send(subscriptionUserID, removeEntityOutgoingEvent.toNetworkEvent());
      }
      activeEntityManager.deregisterUser(realEvent.getUserID());
    }
  }
}
