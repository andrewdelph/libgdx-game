package core.networking.events.consumer.server.incoming;

import com.google.inject.Inject;
import core.app.user.UserID;
import core.common.ChunkRange;
import core.common.GameStore;
import core.common.events.types.EventType;
import core.entity.Entity;
import core.networking.events.EventTypeFactory;
import core.networking.events.types.incoming.HandshakeIncomingEventType;
import core.networking.events.types.outgoing.CreateEntityOutgoingEventType;
import core.networking.server.ServerNetworkHandle;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class HandshakeIncomingConsumerServer implements MyConsumer<EventType> {

  @Inject GameStore gameStore;
  @Inject ServerNetworkHandle serverNetworkHandle;

  @Override
  public void accept(EventType eventType) {
    HandshakeIncomingEventType handshakeIncoming = (HandshakeIncomingEventType) eventType;

    ChunkRange chunkRange = handshakeIncoming.getChunkRange();
    UserID clientUserID = handshakeIncoming.getRequestUserID();

    List<UUID> missingUUIDList = new LinkedList<>(handshakeIncoming.getListUUID());

    if (missingUUIDList.size() > 0) {
      // need to get the uuid of the client
      // send each entity to the client
      List<Entity> missingEntityList = this.gameStore.getEntityListFromList(missingUUIDList);

      for (Entity missingEntity : missingEntityList) {
        CreateEntityOutgoingEventType createEntityOutgoing =
            EventTypeFactory.createCreateEntityOutgoingEvent(
                missingEntity.toNetworkData(), chunkRange);
        this.serverNetworkHandle.send(clientUserID, createEntityOutgoing.toNetworkEvent());
      }
    } else {
      this.serverNetworkHandle.initHandshake(clientUserID, chunkRange);
    }
  }
}
