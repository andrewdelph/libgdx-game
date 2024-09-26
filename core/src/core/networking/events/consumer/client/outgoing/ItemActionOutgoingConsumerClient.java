package core.networking.events.consumer.client.outgoing;

import com.google.inject.Inject;
import core.common.events.types.EventType;
import core.common.events.types.ItemActionEventType;
import core.entity.attributes.inventory.item.comsumers.ItemActionService;
import core.networking.client.ClientNetworkHandle;

public class ItemActionOutgoingConsumerClient implements MyConsumer<EventType> {

  @Inject ClientNetworkHandle clientNetworkHandle;
  @Inject ItemActionService itemActionService;

  @Override
  public void accept(EventType eventType) {
    ItemActionEventType realEvent = (ItemActionEventType) eventType;

    Boolean gcd = itemActionService.checkTriggerGCD(realEvent.getControleeUUID());
    if (!gcd) {
      return;
    }

    clientNetworkHandle.send(realEvent.toNetworkEvent());
  }
}
