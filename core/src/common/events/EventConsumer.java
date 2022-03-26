package common.events;

import app.GameController;
import com.google.inject.Inject;
import common.GameStore;
import common.events.types.RemoveEntityEventType;
import common.events.types.ReplaceEntityEventType;
import common.exceptions.EntityNotFound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventConsumer {

  final Logger LOGGER = LogManager.getLogger();

  @Inject EventService eventService;
  @Inject GameController gameController;
  @Inject GameStore gameStore;

  @Inject
  public EventConsumer() {}

  public void init() {
    this.eventService.addPostUpdateListener(
        ReplaceEntityEventType.type,
        event -> {
          ReplaceEntityEventType realEvent = (ReplaceEntityEventType) event;
          try {
            this.gameController.triggerReplaceEntity(
                realEvent.getTargetUUID(),
                realEvent.getReplacementEntity(),
                realEvent.getSwapVelocity());
          } catch (EntityNotFound e) {
            LOGGER.error(e);
          }
        });
    this.eventService.addPostUpdateListener(
        common.events.types.RemoveEntityEventType.type,
        event -> {
          common.events.types.RemoveEntityEventType realEvent = (RemoveEntityEventType) event;
          try {
            this.gameStore.removeEntity(realEvent.getEntityUUID());
          } catch (EntityNotFound e) {
            e.printStackTrace();
          }
        });
  }
}
