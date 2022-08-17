package core.app.game;

import com.badlogic.gdx.math.Vector2;
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
import core.entity.block.EmptyBlock;
import core.entity.block.SkyBlock;
import core.entity.controllers.events.types.AbstractEntityEventType;
import core.entity.controllers.factories.EntityControllerFactory;
import core.entity.groups.GroupService;
import core.entity.misc.Ladder;
import core.entity.misc.Orb;
import core.entity.misc.Projectile;
import core.entity.misc.Turret;
import core.entity.misc.water.Water;
import core.entity.misc.water.WaterPosition;
import core.networking.events.EventTypeFactory;
import core.networking.events.types.outgoing.CreateEntityOutgoingEventType;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameController {

  final Logger LOGGER = LogManager.getLogger();
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

  public Entity createEntity(Coordinates coordinates, Consumer<Entity> consumer)
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

  public Block createSkyBlock(Coordinates coordinates) throws ChunkNotFound {
    Block entity = blockFactory.createSky(coordinates);
    this.gameStore.addEntity(entity);
    CreateEntityOutgoingEventType createEntityOutgoingEvent =
        EventTypeFactory.createCreateEntityOutgoingEvent(
            entity.toNetworkData(), CommonFactory.createChunkRange(coordinates));
    this.eventService.fireEvent(createEntityOutgoingEvent);
    return entity;
  }

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
    entity.setEntityController(entityControllerFactory.createWaterController(entity));
    eventService.queuePostUpdateEvent(EventTypeFactory.createCreateEntityEventType(entity));
    return entity;
  }

  public Entity createLadder(Coordinates coordinates) throws ChunkNotFound {
    try {
      if (!(this.gameStore.getBlock(coordinates) instanceof EmptyBlock)) {
        LOGGER.debug("Did not find EmptyBlock");
        return null;
      }
    } catch (EntityNotFound e) {
      LOGGER.error("Could not create Ladder");
      return null;
    }
    if (this.gameStore.getLadder(coordinates) != null) return this.gameStore.getLadder(coordinates);
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
      if (!(this.gameStore.getBlock(coordinates) instanceof EmptyBlock)) {
        LOGGER.debug("Did not find EmptyBlock");
        return null;
      }
    } catch (EntityNotFound e) {
      LOGGER.error(e);
      return null;
    }

    if (this.gameStore.getTurret(coordinates) != null) return this.gameStore.getTurret(coordinates);

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

  public void placeBlock(Entity entity, Direction direction, Class blockClass)
      throws EntityNotFound {
    Block removeBlock = null;
    if (direction == Direction.LEFT) {
      removeBlock = this.gameStore.getBlock(entity.getCenter().getLeft());
    } else if (direction == Direction.RIGHT) {
      removeBlock = this.gameStore.getBlock(entity.getCenter().getRight());
    } else if (direction == Direction.UP) {
      removeBlock = this.gameStore.getBlock(entity.getCenter().getUp());
    } else if (direction == Direction.DOWN) {
      removeBlock = this.gameStore.getBlock(entity.getCenter().getDown());
    }
    if (removeBlock == null) throw new EntityNotFound("Block to remove not found in direction.");
    if (removeBlock.getClass() == blockClass) return;

    Block replacementBlock;
    if (blockClass == SkyBlock.class) {
      replacementBlock =
          blockFactory.createSky(removeBlock.getCoordinatesWrapper().getCoordinates());
    } else if (blockClass == DirtBlock.class) {
      replacementBlock =
          blockFactory.createDirt(removeBlock.getCoordinatesWrapper().getCoordinates());
    } else {
      return;
    }
    this.replaceBlock(removeBlock, replacementBlock);
  }

  public void replaceBlock(Block target, Block replacementBlock) {
    Ladder removeLadder = this.gameStore.getLadder(target.getCoordinatesWrapper().getCoordinates());
    if (removeLadder != null) {
      this.removeEntity(removeLadder.getUuid());
    }
    Turret removeTurret = this.gameStore.getTurret(target.getCoordinatesWrapper().getCoordinates());
    if (removeTurret != null) {
      this.removeEntity(removeTurret.getUuid());
    }
    // put this into a post update event
    this.eventService.queuePostUpdateEvent(
        EventTypeFactory.createReplaceEntityEvent(
            target.getUuid(),
            replacementBlock,
            false,
            CommonFactory.createChunkRange(target.getCoordinatesWrapper().getCoordinates())));
    this.eventService.fireEvent(
        EventTypeFactory.createReplaceBlockOutgoingEvent(
            target.getUuid(),
            replacementBlock,
            CommonFactory.createChunkRange(target.getCoordinatesWrapper().getCoordinates())));
  }

  public Entity triggerReplaceEntity(UUID target, Entity replacementEntity)
      throws EntityNotFound, ChunkNotFound, BodyNotFound, DestroyBodyException {
    return triggerReplaceEntity(target, replacementEntity, false);
  }

  public Entity triggerReplaceEntity(UUID target, Entity replacementEntity, Boolean swapVelocity)
      throws EntityNotFound, ChunkNotFound, BodyNotFound, DestroyBodyException {
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
