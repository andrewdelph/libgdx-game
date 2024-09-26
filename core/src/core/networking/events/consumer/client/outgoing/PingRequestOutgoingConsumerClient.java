package core.networking.events.consumer.client.outgoing;

import com.google.inject.Inject;
import core.common.events.types.EventType;
import core.networking.client.ClientNetworkHandle;
import core.networking.events.types.outgoing.PingRequestOutgoingEventType;
import core.networking.translation.NetworkDataSerializer;

public class PingRequestOutgoingConsumerClient implements MyConsumer<EventType> {

  @Inject ClientNetworkHandle clientNetworkHandle;

  @Override
  public void accept(EventType eventType) {
    PingRequestOutgoingEventType realEvent = (PingRequestOutgoingEventType) eventType;

    clientNetworkHandle.send(NetworkDataSerializer.createPingRequestOutgoingEventType(realEvent));
  }
}
