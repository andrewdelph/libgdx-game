package core.networking.events.consumer.server.outgoing;

import com.google.inject.Inject;
import core.app.user.UserID;
import core.chunk.ActiveChunkManager;
import core.common.events.types.EventType;
import core.networking.events.types.outgoing.RemoveEntityOutgoingEventType;
import core.networking.server.ServerNetworkHandle;

public class RemoveEntityOutgoingConsumerServer implements MyConsumer<EventType> {

  @Inject ActiveChunkManager activeChunkManager;
  @Inject ServerNetworkHandle serverNetworkHandle;

  @Override
  public void accept(EventType eventType) {
    RemoveEntityOutgoingEventType outgoing = (RemoveEntityOutgoingEventType) eventType;
    for (UserID userID : activeChunkManager.getChunkRangeUsers(outgoing.getChunkRange())) {
      serverNetworkHandle.send(userID, outgoing.toNetworkEvent());
    }
  }
}
