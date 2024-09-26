package core.networking.events.consumer.server.incoming;

import com.google.inject.Inject;
import core.app.user.UserID;
import core.chunk.ActiveChunkManager;
import core.common.GameStore;
import core.common.events.EventService;
import core.common.events.types.EventType;
import core.common.exceptions.EntityNotFound;
import core.entity.Entity;
import core.entity.attributes.Attribute;
import core.networking.events.EventTypeFactory;
import core.networking.events.types.incoming.UpdateEntityIncomingEventType;
import core.networking.events.types.outgoing.UpdateEntityOutgoingEventType;
import core.networking.server.ServerNetworkHandle;
import core.networking.translation.NetworkDataDeserializer;

public class UpdateEntityIncomingConsumerServer implements MyConsumer<EventType> {

  @Inject NetworkDataDeserializer entitySerializationConverter;
  @Inject ServerNetworkHandle serverNetworkHandle;
  @Inject ActiveChunkManager activeChunkManager;
  @Inject GameStore gameStore;
  @Inject EventService eventService;

  @Override
  public void accept(EventType eventType) {
    UpdateEntityIncomingEventType incoming = (UpdateEntityIncomingEventType) eventType;
    Entity entity;
    try {
      entity = gameStore.getEntity(incoming.getUuid());
    } catch (EntityNotFound e) {
      e.printStackTrace();
      // TODO test this
      serverNetworkHandle.initHandshake(incoming.getUserID(), incoming.getChunkRange());
      return;
    }

    for (Attribute attr : incoming.getAttributeList()) {
      eventService.fireEvent(entity.updateAttribute(attr));
    }

    UpdateEntityOutgoingEventType outgoing =
        EventTypeFactory.createUpdateEntityOutgoingEvent(
            incoming.getAttributeList(), incoming.getChunkRange(), entity.getUuid());

    for (UserID userID : activeChunkManager.getChunkRangeUsers(outgoing.getChunkRange())) {
      if (userID.equals(incoming.getUserID())) {
        continue;
      }
      serverNetworkHandle.send(userID, outgoing.toNetworkEvent());
    }
  }
}
