package core.networking.events.consumer.client.outgoing;

import com.google.inject.Inject;
import core.common.events.types.CreateTurretEventType;
import core.common.events.types.EventType;
import core.networking.client.ClientNetworkHandle;

public class CreateTurretOutgoingConsumerClient implements MyConsumer<EventType> {

  @Inject ClientNetworkHandle clientNetworkHandle;

  @Override
  public void accept(EventType eventType) {
    CreateTurretEventType realEvent = (CreateTurretEventType) eventType;
    clientNetworkHandle.send(realEvent.toNetworkEvent());
  }
}
