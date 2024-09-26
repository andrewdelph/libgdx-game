package core.networking.events.consumer.server.incoming;

import com.badlogic.gdx.Gdx;
import com.google.inject.Inject;
import core.app.game.GameController;
import core.app.user.User;
import core.common.GameSettings;
import core.common.GameStore;
import core.common.events.types.CreateTurretEventType;
import core.common.events.types.EventType;
import core.common.exceptions.ChunkNotFound;
import core.common.exceptions.EntityNotFound;
import core.entity.ActiveEntityManager;
import core.entity.Entity;
import core.entity.misc.Turret;

public class CreateTurretIncomingConsumerServer implements MyConsumer<EventType> {

  @Inject GameController gameController;
  @Inject ActiveEntityManager activeEntityManager;
  @Inject User user;
  @Inject GameStore gameStore;

  @Override
  public void accept(EventType eventType) {
    CreateTurretEventType realEvent = (CreateTurretEventType) eventType;

    try {
      Entity entity = gameStore.getEntity(realEvent.getEntityUUID());
      Turret turret = gameController.createTurret(entity, realEvent.getCoordinates());
      if (turret != null) {
        activeEntityManager.registerActiveEntity(user.getUserID(), turret.getUuid());
      }
    } catch (ChunkNotFound | EntityNotFound e) {
      Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
    }
  }
}
