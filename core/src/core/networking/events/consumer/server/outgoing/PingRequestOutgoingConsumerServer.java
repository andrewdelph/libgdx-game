package core.networking.events.consumer.server.outgoing;

import com.google.inject.Inject;
import core.common.events.types.EventType;
import core.networking.events.types.outgoing.PingRequestOutgoingEventType;
import core.networking.server.ServerNetworkHandle;
import core.networking.translation.NetworkDataSerializer;

public class PingRequestOutgoingConsumerServer implements MyConsumer<EventType> {

  @Inject ServerNetworkHandle serverNetworkHandle;

  @Override
  public void accept(EventType eventType) {
    PingRequestOutgoingEventType realEvent = (PingRequestOutgoingEventType) eventType;

    serverNetworkHandle.send(
        realEvent.getUserID(), NetworkDataSerializer.createPingRequestOutgoingEventType(realEvent));
  }
}
