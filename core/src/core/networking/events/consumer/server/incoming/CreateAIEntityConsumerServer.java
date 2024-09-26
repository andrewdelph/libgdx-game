package core.networking.events.consumer.server.incoming;

import com.google.inject.Inject;
import core.app.user.User;
import core.common.events.types.CreateAIEntityEventType;
import core.common.events.types.EventType;
import core.common.exceptions.ChunkNotFound;
import core.common.exceptions.EntityNotFound;
import core.entity.AIManager;

public class CreateAIEntityConsumerServer implements MyConsumer<EventType> {

  @Inject User user;
  @Inject AIManager aiManager;

  @Override
  public void accept(EventType eventType) {
    try {
      CreateAIEntityEventType realEvent = (CreateAIEntityEventType) eventType;
      aiManager.requestCreateAI(
          user.getUserID(), realEvent.getCoordinates(), realEvent.getTarget());
    } catch (EntityNotFound | ChunkNotFound e) {
      e.printStackTrace();
    }
  }
}
