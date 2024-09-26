package core.networking.events.consumer.server.incoming;

import com.google.inject.Inject;
import core.app.game.GameController;
import core.app.user.UserID;
import core.chunk.ActiveChunkManager;
import core.common.events.types.EventType;
import core.common.exceptions.ChunkNotFound;
import core.common.exceptions.SerializationDataMissing;
import core.entity.ActiveEntityManager;
import core.entity.Entity;
import core.entity.controllers.factories.EntityControllerFactory;
import core.entity.groups.Group;
import core.entity.groups.GroupService;
import core.entity.misc.Ladder;
import core.networking.events.EventTypeFactory;
import core.networking.events.types.incoming.CreateEntityIncomingEventType;
import core.networking.events.types.outgoing.CreateEntityOutgoingEventType;
import core.networking.server.ServerNetworkHandle;
import core.networking.translation.NetworkDataDeserializer;

public class CreateEntityIncomingConsumerServer implements MyConsumer<EventType> {

  @Inject GameController gameController;
  @Inject NetworkDataDeserializer entitySerializationConverter;
  @Inject ServerNetworkHandle serverNetworkHandle;
  @Inject ActiveEntityManager activeEntityManager;
  @Inject ActiveChunkManager activeChunkManager;
  @Inject EntityControllerFactory entityControllerFactory;
  @Inject GroupService groupService;

  @Override
  public void accept(EventType eventType) {
    CreateEntityIncomingEventType incoming = (CreateEntityIncomingEventType) eventType;
    Entity entity;
    try {
      entity =
          gameController.triggerAddEntity(
              entitySerializationConverter.createEntity(incoming.getData()));
      entity.setEntityController(entityControllerFactory.createRemoteBodyController(entity));
      groupService.registerEntityGroup(entity.getUuid(), Group.PLAYER_GROUP);

      if (entity.getClass().equals(Ladder.class)) {
        entity.setEntityController(entityControllerFactory.createLadderController(entity));
      }

    } catch (SerializationDataMissing | ChunkNotFound e) {
      e.printStackTrace();
      return;
    }

    if (entity.getClass() == Entity.class) {
      activeEntityManager.registerActiveEntity(incoming.getUserID(), entity.getUuid());
    }

    CreateEntityOutgoingEventType outgoing =
        EventTypeFactory.createCreateEntityOutgoingEvent(
            incoming.getData(), incoming.getChunkRange());

    for (UserID userID : activeChunkManager.getChunkRangeUsers(incoming.getChunkRange())) {
      if (userID.equals(incoming.getUserID())) {
        continue;
      }
      serverNetworkHandle.send(userID, outgoing.toNetworkEvent());
    }
  }
}
