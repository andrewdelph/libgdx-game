package core.chunk.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.sun.tools.javac.util.Pair;
import core.chunk.world.exceptions.BodyNotFound;
import core.chunk.world.exceptions.DestroyBodyException;
import core.common.ChunkRange;
import core.common.GameSettings;
import core.common.javautil.MyConsumer;
import core.entity.Entity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldWrapper {

  private final World world = new World(new Vector2(0, -GameSettings.GRAVITY), false);
  private final Map<UUID, Body> uuidBodyMap = new HashMap<>();
  private final ChunkRange chunkRange;

  public WorldWrapper(ChunkRange chunkRange) {
    this.chunkRange = chunkRange;
  }

  public synchronized void tick() {
    world.step(
        GameSettings.WORLD_TIME_STEP,
        GameSettings.WORLD_VELOCITY_ITERATIONS,
        GameSettings.WORLD_POSITION_ITERATIONS);
  }

  public synchronized void addEntity(CreateBodyCallable createBodyCallable) {
    Pair<UUID, Body> pair = createBodyCallable.addWorld(world);
    if (pair == null) {
      return;
    }
    uuidBodyMap.put(pair.fst, pair.snd);
  }

  public synchronized void applyWorld(MyConsumer<World> worldConsumer) {
    worldConsumer.accept(this.world);
  }

  public synchronized void destroyEntity(Entity entity) throws DestroyBodyException {
    try {
      world.destroyBody(getBody(entity));

    } catch (Exception e) {
      throw new DestroyBodyException(e.toString());
    } finally {
      uuidBodyMap.remove(entity.getUuid());
    }
  }

  public synchronized void applyBody(Entity entity, MyConsumer<Body> consumer) throws BodyNotFound {
    consumer.accept(getBody(entity));
  }

  public synchronized Vector2 getPosition(Entity entity) throws BodyNotFound {
    return getBody(entity).getPosition();
  }

  public synchronized Vector2 getVelocity(Entity entity) throws BodyNotFound {
    return getBody(entity).getLinearVelocity();
  }

  public synchronized void setActive(Entity entity, Boolean flag) throws BodyNotFound {
    getBody(entity).setActive(flag);
  }

  public synchronized void setGravity(Entity entity, Float gravity) throws BodyNotFound {
    getBody(entity).setGravityScale(gravity);
  }

  public synchronized void setPosition(Entity entity, Vector2 position) throws BodyNotFound {
    getBody(entity).setTransform(position, 0);
  }

  public synchronized void setVelocity(Entity entity, Vector2 velocity) throws BodyNotFound {
    getBody(entity).setLinearVelocity(velocity);
  }

  public synchronized Boolean hasBody(Entity entity) {
    return uuidBodyMap.get(entity.getUuid()) != null;
  }

  private synchronized Body getBody(Entity entity) throws BodyNotFound {
    if (uuidBodyMap.get(entity.getUuid()) == null) {
      throw new BodyNotFound("Body not found for entity: " + entity.getUuid());
    }
    return uuidBodyMap.get(entity.getUuid());
  }
}
