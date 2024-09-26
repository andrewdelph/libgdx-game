package core.networking.events.consumer.client.incoming;

import com.badlogic.gdx.Gdx;
import com.google.inject.Inject;
import core.common.GameSettings;
import core.common.GameStore;
import core.common.events.types.EventType;
import core.common.exceptions.EntityNotFound;
import core.entity.Entity;
import core.entity.attributes.Attribute;
import core.networking.client.ClientNetworkHandle;
import core.networking.events.types.incoming.UpdateEntityIncomingEventType;

public class UpdateEntityIncomingConsumerClient implements MyConsumer<EventType> {

  @Inject ClientNetworkHandle clientNetworkHandle;
  @Inject GameStore gameStore;

  @Override
  public void accept(EventType eventType) {
    UpdateEntityIncomingEventType realEvent = (UpdateEntityIncomingEventType) eventType;

    Entity entity;
    try {
      entity = gameStore.getEntity(realEvent.getUuid());
    } catch (EntityNotFound e) {
      Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
      clientNetworkHandle.initHandshake(realEvent.getChunkRange());
      return;
    }

    for (Attribute attr : realEvent.getAttributeList()) {
      entity.updateAttribute(attr);
    }
  }
}
