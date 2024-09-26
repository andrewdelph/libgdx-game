package core.networking.events.consumer.client.incoming;

import com.google.inject.Inject;
import core.common.events.types.EventType;
import core.networking.client.ClientNetworkHandle;
import core.networking.events.EventTypeFactory;
import core.networking.events.types.incoming.PingRequestIncomingEventType;
import core.networking.events.types.outgoing.PingResponseOutgoingEventType;
import core.networking.translation.NetworkDataSerializer;

public class PingRequestIncomingConsumerClient implements MyConsumer<EventType> {

  @Inject ClientNetworkHandle clientNetworkHandle;

  @Override
  public void accept(EventType eventType) {
    PingRequestIncomingEventType realEvent = (PingRequestIncomingEventType) eventType;

    PingResponseOutgoingEventType pingResponseOutgoingEventType =
        EventTypeFactory.createPingResponseOutgoingEventType(realEvent.getPingID());

    clientNetworkHandle.send(
        NetworkDataSerializer.createPingResponseOutgoingEventType(pingResponseOutgoingEventType));
  }
}
