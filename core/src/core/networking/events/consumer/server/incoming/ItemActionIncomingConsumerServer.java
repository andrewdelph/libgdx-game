package core.networking.events.consumer.server.incoming;

import com.google.inject.Inject;
import core.common.events.types.EventType;
import core.common.events.types.ItemActionEventType;
import core.common.exceptions.EntityNotFound;
import core.entity.attributes.inventory.item.comsumers.ItemActionService;

public class ItemActionIncomingConsumerServer implements MyConsumer<EventType> {

  @Inject ItemActionService itemActionService;

  @Override
  public void accept(EventType eventType) {
    ItemActionEventType realEvent = (ItemActionEventType) eventType;

    Boolean gcd = itemActionService.checkTriggerGCD(realEvent.getControleeUUID());
    if (!gcd) {
      // TODO trigger stop animation
      return;
    }

    try {
      itemActionService.use(realEvent.getItemActionType(), realEvent.getControleeUUID());
    } catch (EntityNotFound e) {
      e.printStackTrace();
    }
  }
}
