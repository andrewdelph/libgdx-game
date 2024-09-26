package core.networking.events.consumer.server.incoming;

import static core.networking.translation.DataTranslationEnum.AUTH;

import com.google.inject.Inject;
import core.common.events.types.EventType;
import core.networking.ConnectionStore;
import core.networking.events.types.incoming.AuthenticationIncomingEventType;
import core.networking.server.ServerNetworkHandle;
import networking.NetworkObjects;

public class AuthenticationIncomingConsumerServer implements MyConsumer<EventType> {

  @Inject ServerNetworkHandle serverNetworkHandle;

  @Inject ConnectionStore connectionStore;

  @Override
  public void accept(EventType eventType) {
    AuthenticationIncomingEventType incoming = (AuthenticationIncomingEventType) eventType;

    connectionStore.addConnection(incoming.getUser(), incoming.getRequestNetworkEventObserver());

    NetworkObjects.NetworkEvent authenticationEvent =
        NetworkObjects.NetworkEvent.newBuilder().setEvent(AUTH).build();

    serverNetworkHandle.send(incoming.getUser(), authenticationEvent);
  }
}
