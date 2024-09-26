package core.networking.events.consumer.client.incoming;

import com.badlogic.gdx.Gdx;
import com.google.inject.Inject;
import core.chunk.Chunk;
import core.chunk.world.exceptions.DestroyBodyException;
import core.common.ChunkRange;
import core.common.GameSettings;
import core.common.GameStore;
import core.common.events.EventService;
import core.common.events.types.EventType;
import core.common.exceptions.EntityNotFound;
import core.entity.ActiveEntityManager;
import core.networking.events.EventTypeFactory;
import core.networking.events.types.incoming.HandshakeIncomingEventType;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class HandshakeIncomingConsumerClient implements MyConsumer<EventType> {

  @Inject GameStore gameStore;
  @Inject EventService eventService;
  @Inject ActiveEntityManager activeEntityManager;

  @Override
  public void accept(EventType eventType) {
    HandshakeIncomingEventType handshakeIncoming = (HandshakeIncomingEventType) eventType;

    ChunkRange chunkRange = handshakeIncoming.getChunkRange();
    Chunk chunk = gameStore.getChunk(chunkRange);

    if (chunk == null) {
      Gdx.app.debug(GameSettings.LOG_TAG, "HandshakeIncomingConsumerClient on null chunk.");
      return;
    }

    // get the missing uuid on the client
    List<UUID> missing = new LinkedList<>(handshakeIncoming.getListUUID());
    missing.removeAll(chunk.getEntityUUIDSet());

    // get the extra uuid on the client
    List<UUID> extra = new ArrayList<>(chunk.getEntityUUIDSet());
    extra.removeAll(handshakeIncoming.getListUUID());
    extra.removeAll(activeEntityManager.getActiveEntities());

    // remove the extra
    for (UUID toRemove : extra) {
      try {
        chunk.removeEntity(toRemove);
      } catch (EntityNotFound | DestroyBodyException e) {
        Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
      }
    }

    // request the missing
    if (missing.size() > 0) {
      eventService.fireEvent(
          EventTypeFactory.createHandshakeOutgoingEventType(chunkRange, missing));
    }
  }
}
