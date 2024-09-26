package core.app.game;

import com.badlogic.gdx.math.Vector2;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.inject.Inject;
import core.app.user.User;
import core.chunk.ChunkFactory;
import core.chunk.world.exceptions.BodyNotFound;
import core.chunk.world.exceptions.DestroyBodyException;
import core.common.CommonFactory;
import core.common.Coordinates;
import core.common.Direction;
import core.common.GameStore;
import core.common.events.EventService;
import core.common.events.types.ItemActionEventType;
import core.common.exceptions.ChunkNotFound;
import core.common.exceptions.EntityNotFound;
import core.common.javautil.MyConsumer;
import core.entity.ActiveEntityManager;
import core.entity.Entity;
import core.entity.EntityFactory;
import core.entity.attributes.Attribute;
import core.entity.attributes.inventory.ItemNotFoundException;
import core.entity.attributes.inventory.item.ItemActionType;
import core.entity.attributes.inventory.item.OrbInventoryItem;
import core.entity.attributes.inventory.item.comsumers.ItemActionService;
import core.entity.attributes.msc.CoordinatesWrapper;
import core.entity.block.Block;
import core.entity.block.BlockFactory;
import core.entity.block.DirtBlock;
import core.entity.block.SolidBlock;
import core.entity.collision.RayCastService;
import core.entity.controllers.events.types.AbstractEntityEventType;
import core.entity.controllers.factories.EntityControllerFactory;
import core.entity.groups.GroupService;
import core.entity.misc.Ladder;
import core.entity.misc.Orb;
import core.entity.misc.Projectile;
import core.entity.misc.Sand;
import core.entity.misc.Turret;
import core.entity.misc.water.Water;
import core.entity.misc.water.WaterPosition;
import core.networking.events.EventTypeFactory;
import core.networking.events.types.outgoing.CreateEntityOutgoingEventType;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class GameController {

  @Inject GameStore gameStore;
  @Inject EntityFactory entityFactory;
  @Inject EventService eventService;
  @Inject EventTypeFactory eventTypeFactory;
  @Inject BlockFactory blockFactory;
  @Inject EntityControllerFactory entityControllerFactory;
  @Inject User user;
  @Inject ActiveEntityManager activeEntityManager;
  @Inject ChunkFactory chunkFactory;
  @Inject ItemActionService itemActionService;
  @Inject GroupService groupService;
  @Inject RayCastService rayCastService;

  public Entity addEntity(Entity entity) throws ChunkNotFound {
    triggerAddEntity(entity);
    CreateEntityOutgoingEventType createEntityOutgoingEvent =
        EventTypeFactory.createCreateEntityOutgoingEvent(
            entity.toNetworkData(),
            CommonFactory.createChunkRange(entity.getCoordinatesWrapper().getCoordinates()));
    this.eventService.fireEvent(createEntityOutgoingEvent);
    return entity;
  }

  public Entity triggerAddEntity(Entity entity) throws ChunkNotFound {
    this.gameStore.addEntity(entity);
    return entity;
  }

  public void removeEntity(UUID uuid) {
    Entity entity;
    try {
      entity = this.gameStore.getEntity(uuid);
    } catch (EntityNotFound e) {
      e.printStackTrace();
      return;
    }
    eventService.queuePostUpdateEvent(eventTypeFactory.createRemoveEntityEvent(uuid));
    eventService.fireEvent(
        EventTypeFactory.createRemoveEntityOutgoingEvent(
            entity.getUuid(),
            CommonFactory.createChunkRange(entity.getCoordinatesWrapper().getCoordinates())));
  }

  public Entity triggerRemoveEntity(UUID uuid) throws EntityNotFound, DestroyBodyException {
    groupService.removeEntity(uuid);
    activeEntityManager.removeActiveEntity(uuid);
    return this.gameStore.removeEntity(uuid);
  }

  public Entity createEntity(Coordinates coordinates) throws ChunkNotFound {
    return this.createEntity(coordinates, null);
  }

  public Entity createEntity(Coordinates coordinates, MyConsumer<Entity> consumer)
      throws ChunkNotFound {
    Entity entity = entityFactory.createEntity(coordinates);
    if (consumer != null) {
      consumer.accept(entity);
    }
    this.gameStore.addEntity(entity);
    CreateEntityOutgoingEventType createEntityOutgoingEvent =
        EventTypeFactory.createCreateEntityOutgoingEvent(
            entity.toNetworkData(), CommonFactory.createChunkRange(coordinates));
    this.eventService.fireEvent(createEntityOutgoingEvent);
    return entity;
  }

  //  public Block createSkyBlock(Coordinates coordinates) throws ChunkNotFound {
  //    Block entity = blockFactory.createSky(coordinates);
  //    this.gameStore.addEntity(entity);
  //    CreateEntityOutgoingEventType createEntityOutgoingEvent =
  //        EventTypeFactory.createCreateEntityOutgoingEvent(
  //            entity.toNetworkData(), CommonFactory.createChunkRange(coordinates));
  //    this.eventService.fireEvent(createEntityOutgoingEvent);
  //    return entity;
  //  }

  public Block createDirtBlock(Coordinates coordinates) throws ChunkNotFound {
    Block entity = blockFactory.createDirt(coordinates);
    this.gameStore.addEntity(entity);
    CreateEntityOutgoingEventType createEntityOutgoingEvent =
        EventTypeFactory.createCreateEntityOutgoingEvent(
            entity.toNetworkData(), CommonFactory.createChunkRange(coordinates));
    this.eventService.fireEvent(createEntityOutgoingEvent);
    return entity;
  }

  public Block createStoneBlock(Coordinates coordinates) throws ChunkNotFound {
    Block entity = blockFactory.createStone(coordinates);
    this.gameStore.addEntity(entity);
    CreateEntityOutgoingEventType createEntityOutgoingEvent =
        EventTypeFactory.createCreateEntityOutgoingEvent(
            entity.toNetworkData(), CommonFactory.createChunkRange(coordinates));
    this.eventService.fireEvent(createEntityOutgoingEvent);
    return entity;
  }

  public WaterPosition createWaterPosition(Coordinates coordinates) throws ChunkNotFound {
    WaterPosition entity = entityFactory.createWaterPosition(coordinates);
    entity.setEntityController(entityControllerFactory.createWaterPositionController(entity));
    eventService.queuePostUpdateEvent(EventTypeFactory.createCreateEntityEventType(entity));
    return entity;
  }

  public Water createWater(Coordinates coordinates) throws ChunkNotFound {
    Water entity = entityFactory.createWater(coordinates.getBase());

    // internal event
    eventService.queuePostUpdateEvent(EventTypeFactory.createCreateEntityEventType(entity));

    // outgoing event
    CreateEntityOutgoingEventType createEntityOutgoingEvent =
        EventTypeFactory.createCreateEntityOutgoingEvent(
            entity.toNetworkData(), CommonFactory.createChunkRange(coordinates));
    this.eventService.fireEvent(createEntityOutgoingEvent);
    return entity;
  }

  public Sand createSand(Coordinates coordinates) throws ChunkNotFound {
    Sand entity = entityFactory.createSand(coordinates.getBase());
    entity.setEntityController(entityControllerFactory.createSandController(entity));

    // internal event
    eventService.queuePostUpdateEvent(EventTypeFactory.createCreateEntityEventType(entity));

    // outgoing event
    CreateEntityOutgoingEventType createEntityOutgoingEvent =
        EventTypeFactory.createCreateEntityOutgoingEvent(
            entity.toNetworkData(), CommonFactory.createChunkRange(coordinates));
    this.eventService.fireEvent(createEntityOutgoingEvent);
    return entity;
  }

  public Entity createLadder(Coordinates coordinates) throws ChunkNotFound {
    coordinates = coordinates.getBase();
    try {
      this.gameStore.getBlock(coordinates);
    } catch (EntityNotFound e) {
      this.createDirtBlock(coordinates);
    }
    if (this.gameStore.getLadder(coordinates) != null) {
      return this.gameStore.getLadder(coordinates);
    }
    Entity entity = entityFactory.createLadder(coordinates);
    this.gameStore.addEntity(entity);
    CreateEntityOutgoingEventType createEntityOutgoingEvent =
        EventTypeFactory.createCreateEntityOutgoingEvent(
            entity.toNetworkData(), CommonFactory.createChunkRange(coordinates));
    this.eventService.fireEvent(createEntityOutgoingEvent);
    return entity;
  }

  public Projectile createProjectile(Coordinates coordinates, Vector2 velocity)
      throws ChunkNotFound, BodyNotFound {
    Projectile projectile = entityFactory.createProjectile(coordinates);
    this.gameStore.addEntity(projectile);
    projectile.setBodyVelocity(velocity);
    CreateEntityOutgoingEventType createEntityOutgoingEvent =
        EventTypeFactory.createCreateEntityOutgoingEvent(
            projectile.toNetworkData(), CommonFactory.createChunkRange(coordinates));
    this.eventService.fireEvent(createEntityOutgoingEvent);
    projectile.setEntityController(
        entityControllerFactory.createProjectileController(projectile, coordinates, 15));
    activeEntityManager.registerActiveEntity(user.getUserID(), projectile.getUuid());
    return projectile;
  }

  public Orb createOrb(Coordinates coordinates) throws ChunkNotFound {
    Orb orb = entityFactory.createOrb(coordinates);
    orb.setEntityController(entityControllerFactory.createOrbController(orb));
    this.gameStore.addEntity(orb);
    CreateEntityOutgoingEventType createEntityOutgoingEvent =
        EventTypeFactory.createCreateEntityOutgoingEvent(
            orb.toNetworkData(), CommonFactory.createChunkRange(coordinates));
    this.eventService.fireEvent(createEntityOutgoingEvent);
    return orb;
  }

  public void triggerCreateTurret(Entity entity, Coordinates coordinates) {
    this.eventService.queuePostUpdateEvent(
        EventTypeFactory.createTurretEventType(entity.getUuid(), coordinates));
  }

  public Turret createTurret(Entity entity, Coordinates coordinates) throws ChunkNotFound {

    try {
      this.gameStore.getBlock(coordinates);
      return null;
    } catch (EntityNotFound e) {
    }

    if (this.gameStore.getTurret(coordinates) != null) {
      return this.gameStore.getTurret(coordinates);
    }

    synchronized (entity.getBag()) {
      int orbIndex;
      try {
        orbIndex = entity.getBag().getClassIndex(OrbInventoryItem.class);
      } catch (ItemNotFoundException e) {
        return null;
      }
      entity.getBag().removeItem(orbIndex);
    }

    Turret turret = entityFactory.createTurret(coordinates);
    this.gameStore.addEntity(turret);
    CreateEntityOutgoingEventType createEntityOutgoingEvent =
        EventTypeFactory.createCreateEntityOutgoingEvent(
            turret.toNetworkData(), CommonFactory.createChunkRange(coordinates));
    this.eventService.fireEvent(createEntityOutgoingEvent);
    turret.setEntityController(entityControllerFactory.createTurretController(turret));
    activeEntityManager.registerActiveEntity(user.getUserID(), turret.getUuid());
    return turret;
  }

  public void moveEntity(UUID uuid, Coordinates coordinates) throws EntityNotFound {
    Entity entity = this.gameStore.getEntity(uuid);
    Coordinates preCoordinates = entity.getCoordinatesWrapper().getCoordinates();
    CoordinatesWrapper coordinatesWrapper = new CoordinatesWrapper(coordinates);
    entity.setCoordinatesWrapper(coordinatesWrapper);
    this.eventService.fireEvent(
        EventTypeFactory.createUpdateEntityOutgoingEvent(
            coordinatesWrapper, CommonFactory.createChunkRange(preCoordinates), uuid));
  }

  public void updateEntityAttribute(UUID uuid, Attribute attribute) throws EntityNotFound {
    Entity entity = this.gameStore.getEntity(uuid);
    Coordinates preCoordinates = entity.getCoordinatesWrapper().getCoordinates();
    AbstractEntityEventType entityAttributeEvent = entity.updateAttribute(attribute);
    eventService.fireEvent(entityAttributeEvent);
    this.eventService.fireEvent(
        EventTypeFactory.createUpdateEntityOutgoingEvent(
            attribute, CommonFactory.createChunkRange(preCoordinates), uuid));
  }

  public void placeBlock(
      Entity entity, Direction direction, Optional<Class<? extends SolidBlock>> blockClass) {
    Block removeBlock = null;

    Coordinates targetCoordinates = null;

    Set<Entity> rayCastSet = null;
    if (direction == Direction.LEFT) {
      targetCoordinates = entity.getCoordinatesWrapper().getCoordinates().add(-1f, 0);
      rayCastSet =
          rayCastService.rayCast(
              entity.getCoordinatesWrapper().getCoordinates(), targetCoordinates);
    } else if (direction == Direction.RIGHT) {
      targetCoordinates = entity.getCoordinatesWrapper().getCoordinates().add(1f, 0);
      rayCastSet =
          rayCastService.rayCast(
              entity.getCoordinatesWrapper().getCoordinates(), targetCoordinates);
    } else if (direction == Direction.UP) {
      targetCoordinates = entity.getCoordinatesWrapper().getCoordinates().add(0, 1f);
      rayCastSet =
          rayCastService.rayCast(
              entity.getCoordinatesWrapper().getCoordinates(), targetCoordinates);
    } else if (direction == Direction.DOWN) {
      targetCoordinates = entity.getCoordinatesWrapper().getCoordinates().add(0, -1f);
      rayCastSet =
          rayCastService.rayCast(
              entity.getCoordinatesWrapper().getCoordinates(), targetCoordinates);
    } else {
      return;
    }

    // from the set... get a block which is instanceof SolidBlock
    Collection<Entity> filteredRayCastSet =
        Collections2.filter(
            rayCastSet,
            new Predicate<Entity>() {
              @Override
              public boolean apply(Entity input) {
                return input instanceof SolidBlock;
              }
            });

    removeBlock = (Block) filteredRayCastSet.iterator().next();

    Block replacementBlock = null;
    targetCoordinates =
        ((removeBlock == null)
            ? targetCoordinates.getBase()
            : removeBlock.getCoordinatesWrapper().getCoordinates());

    if (targetCoordinates != null && blockClass.isPresent()) {

      Class<? extends SolidBlock> aClass = blockClass.get();
      if (DirtBlock.class.equals(aClass)) {
        replacementBlock = blockFactory.createDirt(targetCoordinates);
      } else if (Sand.class.equals(aClass)) {
        replacementBlock = entityFactory.createSand(targetCoordinates);
      } else {
      }
    }
    this.replaceBlock(Optional.fromNullable(removeBlock), Optional.fromNullable(replacementBlock));
  }

  public void replaceBlock(Optional<Block> toRemove, Optional<Block> toAdd) {
    // 1 both exist. replace
    // 2 only toRemove exists. remove it
    // 3 only toAdd exists. add it

    if (toRemove.isPresent()) {
      Ladder removeLadder =
          this.gameStore.getLadder(toRemove.get().getCoordinatesWrapper().getCoordinates());
      if (removeLadder != null) {
        this.removeEntity(removeLadder.getUuid());
      }
    }

    if (toAdd.isPresent()) {
      Turret removeTurret =
          this.gameStore.getTurret(toAdd.get().getCoordinatesWrapper().getCoordinates());
      if (removeTurret != null) {
        this.removeEntity(removeTurret.getUuid());
      }
    }

    if (toRemove.isPresent() && toAdd.isPresent()) {
      // put this into a post update event
      this.eventService.queuePostUpdateEvent(
          EventTypeFactory.createReplaceEntityEvent(
              toRemove.get().getUuid(),
              toAdd.get(),
              false,
              CommonFactory.createChunkRange(
                  toRemove.get().getCoordinatesWrapper().getCoordinates())));
      this.eventService.fireEvent(
          EventTypeFactory.createReplaceBlockOutgoingEvent(
              toRemove.get().getUuid(),
              toAdd.get(),
              CommonFactory.createChunkRange(
                  toRemove.get().getCoordinatesWrapper().getCoordinates())));
    } else if (toRemove.isPresent()) {
      this.removeEntity(toRemove.get().getUuid());
    } else if (toAdd.isPresent()) {
      try {
        this.addEntity(toAdd.get());
      } catch (ChunkNotFound e) {
        e.printStackTrace();
      }
    }
  }

  public Entity triggerReplaceEntity(UUID target, Entity replacementEntity)
      throws EntityNotFound, ChunkNotFound, BodyNotFound, DestroyBodyException {
    return triggerReplaceEntity(target, replacementEntity, false);
  }

  public Entity triggerReplaceEntity(UUID target, Entity replacementEntity, Boolean swapVelocity)
      throws EntityNotFound, ChunkNotFound, BodyNotFound, DestroyBodyException {

    if (replacementEntity == null) {
      return null;
    }

    Vector2 velocity = null;
    Entity removeEntity = this.gameStore.getEntity(target);
    if (swapVelocity) {
      velocity = removeEntity.getBodyVelocity();
    }
    this.gameStore.removeEntity(removeEntity.getUuid());
    this.gameStore.addEntity(replacementEntity);

    if (swapVelocity) {
      replacementEntity.setBodyVelocity(velocity);
    }
    return replacementEntity;
  }

  public void createAI(UUID target) {
    this.eventService.queuePostUpdateEvent(
        EventTypeFactory.createAIEntityEventType(CommonFactory.createCoordinates(0, 2), target));
  }

  public void useItem(Entity entity) {
    int index = entity.getBag().getEquipped().getIndex();
    ItemActionType type = entity.getBag().getItem(index).getItemActionType();
    ItemActionEventType itemActionEventType = new ItemActionEventType(type, entity.getUuid());

    eventService.queuePostUpdateEvent(itemActionEventType);
  }
}
