package networking.events.types.outgoing;

import static networking.events.types.NetworkEventTypeEnum.REMOVE_ENTITY_OUTGOING;

import chunk.ChunkRange;
import com.google.inject.Inject;
import common.events.types.EventType;
import java.util.UUID;
import networking.NetworkObjects;
import networking.events.interfaces.SerializeNetworkEvent;
import networking.translation.NetworkDataSerializer;

public class RemoveEntityOutgoingEventType extends EventType implements SerializeNetworkEvent {
  public static String type = REMOVE_ENTITY_OUTGOING;

  UUID target;
  ChunkRange chunkRange;

  @Inject
  public RemoveEntityOutgoingEventType(UUID target, ChunkRange chunkRange) {
    this.target = target;
    this.chunkRange = chunkRange;
  }

  public ChunkRange getChunkRange() {
    return chunkRange;
  }

  public UUID getTarget() {
    return target;
  }

  @Override
  public String getEventType() {
    return type;
  }

  @Override
  public NetworkObjects.NetworkEvent toNetworkEvent() {
    return NetworkDataSerializer.createRemoveEntityOutgoingEventType(this);
  }
}
