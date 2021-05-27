package infra.networking.consumer;

import com.google.inject.Inject;
import infra.app.GameController;
import infra.common.events.Event;
import infra.common.events.EventService;
import infra.entity.EntitySerializationConverter;
import infra.networking.client.ClientNetworkHandle;
import infra.networking.events.CreateEntityIncomingEvent;
import infra.networking.events.CreateEntityOutgoingEvent;
import infra.networking.events.UpdateEntityIncomingEvent;

import java.util.function.Consumer;

public class ClientEventConsumer extends NetworkConsumer {
  @Inject EventService eventService;
  @Inject GameController gameController;
  @Inject EntitySerializationConverter entitySerializationConverter;

  @Inject ClientNetworkHandle clientNetworkHandle;

  @Inject
  public ClientEventConsumer() {}

  public void init() {
    this.eventService.addListener(
        CreateEntityIncomingEvent.type,
        new Consumer<Event>() {
          @Override
          public void accept(Event event) {
            CreateEntityIncomingEvent realEvent = (CreateEntityIncomingEvent) event;
            gameController.triggerCreateEntity(
                entitySerializationConverter.createEntity(realEvent.getData()));
          }
        });
    this.eventService.addListener(
        UpdateEntityIncomingEvent.type,
        new Consumer<Event>() {
          @Override
          public void accept(Event event) {
            UpdateEntityIncomingEvent realEvent = (UpdateEntityIncomingEvent) event;
            entitySerializationConverter.updateEntity(realEvent.getData());
          }
        });
    this.eventService.addListener(
        CreateEntityOutgoingEvent.type,
        new Consumer<Event>() {
          @Override
          public void accept(Event event) {
            CreateEntityOutgoingEvent realEvent = (CreateEntityOutgoingEvent) event;
            clientNetworkHandle.send(realEvent.getNetworkEvent());
          }
        });
  }
}
