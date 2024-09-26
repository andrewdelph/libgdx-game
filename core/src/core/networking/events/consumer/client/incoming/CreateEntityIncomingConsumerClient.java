package core.networking.events.consumer.client.incoming;

import com.badlogic.gdx.Gdx;
import com.google.inject.Inject;
import core.app.game.GameController;
import core.common.GameSettings;
import core.common.GameStore;
import core.common.events.types.EventType;
import core.common.exceptions.ChunkNotFound;
import core.common.exceptions.SerializationDataMissing;
import core.entity.Entity;
import core.entity.controllers.factories.EntityControllerFactory;
import core.networking.events.types.incoming.CreateEntityIncomingEventType;
import core.networking.translation.NetworkDataDeserializer;

public class CreateEntityIncomingConsumerClient implements MyConsumer<EventType> {

  @Inject GameController gameController;
  @Inject NetworkDataDeserializer entitySerializationConverter;
  @Inject GameStore gameStore;
  @Inject EntityControllerFactory entityControllerFactory;

  @Override
  public void accept(EventType eventType) {
    CreateEntityIncomingEventType realEvent = (CreateEntityIncomingEventType) eventType;
    Entity entity;
    try {
      entity = entitySerializationConverter.createEntity(realEvent.getData());
      entity.setEntityController(entityControllerFactory.createRemoteBodyController(entity));
    } catch (SerializationDataMissing e) {
      Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
      // TODO disconnect the client
      return;
    }

    if (gameStore.doesEntityExist(entity.getUuid())) {
      Gdx.app.debug(GameSettings.LOG_TAG, "Entity already exists: " + entity.getUuid());
      return;
    }
    try {
      gameController.triggerAddEntity(entity);
    } catch (ChunkNotFound e) {
      Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
    }
  }
}
