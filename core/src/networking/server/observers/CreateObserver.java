package networking.server.observers;

import infra.entity.Entity;
import infra.entity.EntityData;
import infra.entity.EntityManager;
import infra.entity.factories.EntityDataFactory;
import infra.entity.factories.EntityFactory;
import infra.events.EventService;
import io.grpc.stub.StreamObserver;
import networking.NetworkObject;
import networking.connetion.ConnectionStore;
import networking.events.CreateEntityEvent;
import networking.events.DisconnectEvent;
import networking.events.RemoveEntityEvent;

import java.util.UUID;

public class CreateObserver implements StreamObserver<NetworkObject.CreateNetworkObject> {
    EntityManager entityManager;
    ConnectionStore connectionStore;
    EntityFactory entityFactory;
    EventService eventService;
    StreamObserver<NetworkObject.CreateNetworkObject> requestObserver;
    UUID ownerID;

    protected CreateObserver(EntityManager entityManager, ConnectionStore connectionStore, EntityFactory entityFactory, EventService eventService, StreamObserver<NetworkObject.CreateNetworkObject> requestObserver) {
        this.entityManager = entityManager;
        this.connectionStore = connectionStore;
        this.entityFactory = entityFactory;
        this.eventService = eventService;
        this.requestObserver = requestObserver;
        this.ownerID = UUID.randomUUID();
    }

    @Override
    public void onNext(NetworkObject.CreateNetworkObject update) {
        EntityData createData = EntityDataFactory.getInstance().createEntityData(update);
        createData.setOwner(this.ownerID.toString());
        CreateEntityEvent createEntityEvent = new CreateEntityEvent(createData, requestObserver);
        this.eventService.fireEvent(createEntityEvent);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("error " + throwable);
        DisconnectEvent disconnectEvent = new DisconnectEvent(this.requestObserver);
        this.eventService.fireEvent(disconnectEvent);
        this.removeOwned();
    }

    @Override
    public void onCompleted() {
        System.out.println("COMPLETE CreateObserver");
        DisconnectEvent disconnectEvent = new DisconnectEvent(this.requestObserver);
        this.eventService.fireEvent(disconnectEvent);
        this.removeOwned();
    }

    private void removeOwned() {
        for (Entity entity : this.entityManager.getAll()) {
            System.out.println(entity.getOwner().toString().compareTo(this.ownerID.toString()));
            if (entity.getOwner().toString().compareTo(this.ownerID.toString()) == 0) {
                RemoveEntityEvent removeEvent = new RemoveEntityEvent(entity.getEntityData(), null);
                this.eventService.fireEvent(removeEvent);
            }
        }
    }
}
