package core.networking.events.consumer.server.outgoing;

import com.google.inject.Inject;
import core.app.user.UserID;
import core.chunk.ActiveChunkManager;
import core.common.events.EventService;
import core.common.events.types.EventType;
import core.networking.events.types.outgoing.ReplaceBlockOutgoingEventType;
import core.networking.server.ServerNetworkHandle;

public class ReplaceBlockOutgoingConsumerServer implements MyConsumer<EventType> {

  @Inject EventService eventService;
  @Inject ActiveChunkManager activeChunkManager;
  @Inject ServerNetworkHandle serverNetworkHandle;

  @Override
  public void accept(EventType eventType) {
    ReplaceBlockOutgoingEventType realEvent = (ReplaceBlockOutgoingEventType) eventType;
    for (UserID userID : activeChunkManager.getChunkRangeUsers(realEvent.getChunkRange())) {
      serverNetworkHandle.send(userID, realEvent.toNetworkEvent());
    }
  }
}
