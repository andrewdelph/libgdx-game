package networking.events.consumer.server;

import com.google.inject.Inject;
import common.events.EventConsumer;
import common.events.EventService;
import common.events.types.CreateAIEntityEventType;
import networking.events.consumer.server.incoming.*;
import networking.events.consumer.server.outgoing.CreateEntityOutgoingConsumerServer;
import networking.events.consumer.server.outgoing.ReplaceBlockOutgoingConsumerServer;
import networking.events.consumer.server.outgoing.UpdateEntityOutgoingConsumerServer;
import networking.events.types.NetworkEventTypeEnum;
import networking.events.types.incoming.*;
import networking.events.types.outgoing.CreateEntityOutgoingEventType;
import networking.events.types.outgoing.ReplaceBlockOutgoingEventType;
import networking.events.types.outgoing.UpdateEntityOutgoingEventType;


public class ServerEventConsumer extends EventConsumer {
    @Inject
    EventService eventService;

    @Inject
    SubscriptionIncomingConsumerServer subscriptionIncomingConsumerServer;
    @Inject
    DisconnectionIncomingConsumerServer disconnectionIncomingConsumerServer;
    @Inject
    CreateEntityIncomingConsumerServer createEntityIncomingConsumerServer;
    @Inject
    UpdateEntityIncomingConsumerServer updateEntityIncomingConsumerServer;
    @Inject
    ReplaceBlockIncomingConsumerServer replaceBlockIncomingConsumerServer;
    @Inject
    CreateEntityOutgoingConsumerServer createEntityOutgoingConsumerServer;
    @Inject
    UpdateEntityOutgoingConsumerServer updateEntityOutgoingConsumerServer;
    @Inject
    ReplaceBlockOutgoingConsumerServer replaceBlockOutgoingConsumerServer;
    @Inject
    CreateAIEntityConsumerServer createAIEntityConsumerServer;
    @Inject
    HandshakeIncomingConsumerServer handshakeIncomingConsumerServer;

    public void init() {
        super.init();
        this.eventService.addListener(
                SubscriptionIncomingEventType.type, subscriptionIncomingConsumerServer);
        this.eventService.addListener(
                DisconnectionIncomingEventType.type, disconnectionIncomingConsumerServer);
        this.eventService.addListener(
                CreateEntityIncomingEventType.type, createEntityIncomingConsumerServer);
        this.eventService.addListener(
                UpdateEntityIncomingEventType.type, updateEntityIncomingConsumerServer);
        this.eventService.addListener(
                ReplaceBlockIncomingEventType.type, replaceBlockIncomingConsumerServer);
        this.eventService.addListener(
                CreateEntityOutgoingEventType.type, createEntityOutgoingConsumerServer);
        this.eventService.addListener(
                UpdateEntityOutgoingEventType.type, updateEntityOutgoingConsumerServer);
        this.eventService.addListener(
                ReplaceBlockOutgoingEventType.type, replaceBlockOutgoingConsumerServer);
        this.eventService.addPostUpdateListener(
                CreateAIEntityEventType.type, createAIEntityConsumerServer);
        this.eventService.addListener(NetworkEventTypeEnum.HANDSHAKE_INCOMING, handshakeIncomingConsumerServer);
    }
}
