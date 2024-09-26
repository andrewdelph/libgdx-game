package core.chunk;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.World;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import core.app.game.GameController;
import core.chunk.world.WorldWrapper;
import core.chunk.world.exceptions.BodyNotFound;
import core.chunk.world.exceptions.DestroyBodyException;
import core.common.ChunkRange;
import core.common.Clock;
import core.common.CommonFactory;
import core.common.Coordinates;
import core.common.GameSettings;
import core.common.GameStore;
import core.common.Tick;
import core.common.exceptions.ChunkNotFound;
import core.common.exceptions.EntityNotFound;
import core.common.javautil.MyConsumer;
import core.entity.Entity;
import core.entity.block.SolidBlock;
import core.entity.collision.EntityContactListenerFactory;
import core.entity.misc.Ladder;
import core.entity.misc.Turret;
import core.networking.events.interfaces.SerializeNetworkData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import networking.NetworkObjects;
import networking.NetworkObjects.NetworkData;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class Chunk implements Callable<Chunk>, SerializeNetworkData {

  final ConcurrentHashMap<UUID, Entity> chunkMap = new ConcurrentHashMap<>();
  private final WorldWrapper worldWrapper;
  private final Set<Entity> neighborEntitySet = new HashSet<>();
  public ChunkRange chunkRange;
  public Tick updateTick;
  GameStore gameStore;
  GameController gameController;
  Clock clock;

  public Chunk(
      Clock clock,
      GameStore gameStore,
      GameController gameController,
      final EntityContactListenerFactory entityContactListenerFactory,
      ChunkRange chunkRange) {
    this.gameStore = gameStore;
    this.gameController = gameController;
    this.clock = clock;
    this.chunkRange = chunkRange;
    this.nextTick(1);
    this.worldWrapper = new WorldWrapper(chunkRange);
    this.worldWrapper.applyWorld(
        new MyConsumer<World>() {
          @Override
          public void accept(World world) {
            world.setContactListener(entityContactListenerFactory.createEntityContactListener());
          }
        });
  }

  public WorldWrapper getWorldWrapper() {
    return worldWrapper;
  }

  public void nextTick(int timeout) {
    this.updateTick = new Tick(clock.getCurrentTick().time + timeout);
  }

  @Override
  public Chunk call() throws Exception {
    try {
      this.update();
    } catch (Exception e) {
      Gdx.app.error(GameSettings.LOG_TAG, ("CHUNK UPDATE FAILED: " + this), e);
    }
    return this;
  }

  public synchronized Entity removeEntity(UUID uuid) throws EntityNotFound, DestroyBodyException {
    Entity entity = this.getEntity(uuid);
    this.chunkMap.remove(uuid);
    // updateNeighbors should clean this up

    //    synchronized (worldWrapper) {
    //      if (worldWrapper.hasBody(entity) && !neighborEntitySet.contains(entity))
    //        worldWrapper.destroyEntity(entity);
    //    }
    neighborEntitySet.add(entity);
    this.nextTick(1);
    return entity;
  }

  public synchronized void addEntity(Entity entity) {
    this.chunkMap.put(entity.getUuid(), entity);
    this.addBody(entity);
    neighborEntitySet.remove(entity);
    this.nextTick(1);
  }

  public synchronized boolean addBody(Entity entity) {
    synchronized (worldWrapper) {
      if (worldWrapper.hasBody(entity)) {
        return false;
      }
      worldWrapper.addEntity(entity.addWorld(this));
    }
    return true;
  }

  public void addAllEntity(List<Entity> entityList) {
    for (Entity entity : entityList) {
      this.addEntity(entity);
    }
  }

  public Entity getEntity(UUID uuid) throws EntityNotFound {
    Entity toReturn = this.chunkMap.get(uuid);
    if (toReturn == null) {
      throw new EntityNotFound("Entity not found in chunk #UUID " + uuid);
    }
    return this.chunkMap.get(uuid);
  }

  public Set<UUID> getEntityUUIDSet() {
    return this.chunkMap.keySet();
  }

  public List<Entity> getEntityList() {
    return new LinkedList<Entity>(this.chunkMap.values());
  }

  public List<Chunk> getNeighborChunks() {
    List<Chunk> neighborChunkList = new LinkedList<>();
    neighborChunkList.add(this.gameStore.getChunk(this.chunkRange.getUp()));
    neighborChunkList.add(this.gameStore.getChunk(this.chunkRange.getDown()));
    neighborChunkList.add(this.gameStore.getChunk(this.chunkRange.getLeft()));
    neighborChunkList.add(this.gameStore.getChunk(this.chunkRange.getRight()));
    neighborChunkList.add(this.gameStore.getChunk(this.chunkRange.getLeft().getUp()));
    neighborChunkList.add(this.gameStore.getChunk(this.chunkRange.getLeft().getDown()));
    neighborChunkList.add(this.gameStore.getChunk(this.chunkRange.getRight().getUp()));
    neighborChunkList.add(this.gameStore.getChunk(this.chunkRange.getRight().getDown()));
    neighborChunkList =
        new ArrayList<>(
            Collections2.filter(
                neighborChunkList,
                new Predicate<Chunk>() {
                  @Override
                  public boolean apply(@NullableDecl Chunk input) {
                    return input != null;
                  }
                }));
    return neighborChunkList;
  }

  private void updateNeighbors() throws BodyNotFound, ChunkNotFound {
    Set<Entity> currentNeighborEntitySet = new HashSet<>();

    Coordinates neighborBottomLeft =
        (CommonFactory.createCoordinates(this.chunkRange.bottom_x, this.chunkRange.bottom_y))
            .getLeft()
            .getLeft()
            .getDown()
            .getDown();
    Coordinates neighborTopRight =
        (CommonFactory.createCoordinates(this.chunkRange.top_x, this.chunkRange.top_y))
            .getRight()
            .getRight()
            .getUp()
            .getUp();

    for (Chunk neighbor : getNeighborChunks()) {
      if (neighbor == null) {
        continue;
      }
      currentNeighborEntitySet.addAll(
          neighbor.getEntityInRange(neighborBottomLeft, neighborTopRight));
    }

    // check the difference
    Set<Entity> entityToAddSet = new HashSet<>(currentNeighborEntitySet);
    entityToAddSet.removeAll(neighborEntitySet);
    Set<Entity> entityToRemoveSet = new HashSet<>(neighborEntitySet);
    entityToRemoveSet.removeAll(currentNeighborEntitySet);

    // add temp entity to set
    for (Entity entity : entityToAddSet) {
      //      if (entity.getClass().equals(WaterPosition.class))
      // System.out.println("waterposition");
      this.addBody(entity);
      neighborEntitySet.add(entity);
    }

    // remove temp entity from set
    for (Entity entity : entityToRemoveSet) {
      neighborEntitySet.remove(entity);
      if (!worldWrapper.hasBody(entity)) {
        continue;
      }
      try {
        worldWrapper.destroyEntity(entity);
      } catch (DestroyBodyException e) {
        Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
      }
    }

    // update the neighbor body position and velocity
    for (Entity entity : currentNeighborEntitySet) {
      if (!worldWrapper.hasBody(entity)) {
        //        System.out.println(entity.getClass());
        continue;
      }
      worldWrapper.setPosition(entity, entity.getBodyPosition());
      worldWrapper.setVelocity(entity, entity.getBodyVelocity());
    }
  }

  synchronized void update() throws Exception { // TODO this shouldnt throw all Exceptions
    this.updateNeighbors();

    int tickTimeout = Integer.MAX_VALUE;

    for (Entity entity : this.chunkMap.values()) {
      if (entity.getEntityController() == null) {
        continue;
      }
      entity.getEntityController().beforeWorldUpdate();
      try {
        this.gameStore.syncEntity(entity);
      } catch (EntityNotFound e) {
        Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
      }
    }

    worldWrapper.tick();

    for (Entity entity : this.chunkMap.values()) {
      if (entity.getEntityController() != null) {
        entity.getEntityController().afterWorldUpdate();
      }

      int entityTick = entity.getUpdateTimeout();
      if (entityTick < tickTimeout) {
        tickTimeout = entityTick;
      }
    }
    this.nextTick(tickTimeout);
  }

  public List<Entity> getEntityInRange(
      Coordinates bottomLeftCoordinates, Coordinates topRightCoordinates) {

    List<Entity> entityList = new LinkedList<>();

    for (Entity entity : this.getEntityList()) {
      if (Coordinates.isInRange(
          bottomLeftCoordinates,
          topRightCoordinates,
          entity.getCoordinatesWrapper().getCoordinates())) {
        entityList.add(entity);
      }
    }

    return entityList;
  }

  public SolidBlock getBlock(Coordinates coordinates) throws EntityNotFound {
    List<Entity> entityList = this.getEntityInRange(coordinates, coordinates);
    for (Entity entity : entityList) {
      if (entity instanceof SolidBlock) {
        return (SolidBlock) entity;
      }
    }
    throw new EntityNotFound("could not find block at " + coordinates.toString());
  }

  @Override
  public String toString() {
    return "Chunk{" + "chunkRange=" + chunkRange + '}';
  }

  public Ladder getLadder(Coordinates coordinates) {
    List<Entity> entityList = this.getEntityInRange(coordinates, coordinates);
    for (Entity entity : entityList) {
      if (entity instanceof Ladder) {

        return (Ladder) entity;
      }
    }
    return null;
  }

  public Turret getTurret(Coordinates coordinates) {
    List<Entity> entityList = this.getEntityInRange(coordinates, coordinates);
    for (Entity entity : entityList) {
      if (entity instanceof Turret
          && Coordinates.isInRange(
              coordinates, coordinates, entity.getCoordinatesWrapper().getCoordinates())) {

        return (Turret) entity;
      }
    }
    return null;
  }

  public List<Entity> getEntityListBaseCoordinates(Coordinates coordinates) {
    coordinates = coordinates.getBase();
    return this.getEntityInRange(coordinates, coordinates);
  }

  @Override
  public NetworkObjects.NetworkData toNetworkData() {
    NetworkObjects.NetworkData.Builder networkDataBuilder = NetworkObjects.NetworkData.newBuilder();
    for (Entity entity : this.getEntityList()) {
      NetworkData networkData = entity.toNetworkData();
      if (networkData == null) {
        continue;
      }
      networkDataBuilder.addChildren(entity.toNetworkData());
    }
    networkDataBuilder.addChildren(this.chunkRange.toNetworkData());
    return networkDataBuilder.build();
  }

  public boolean equals(Object anObject) {
    if (this == anObject) {
      return true;
    }
    if (!(anObject instanceof Chunk)) {
      return false;
    }
    Chunk other = (Chunk) anObject;
    if (!this.chunkRange.equals(other.chunkRange)) {
      return false;
    }

    return this.getEntityUUIDSet().equals(other.getEntityUUIDSet());
  }
}
