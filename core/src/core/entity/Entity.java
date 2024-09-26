package core.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.google.common.base.Objects;
import com.sun.tools.javac.util.Pair;
import core.app.screen.assets.BaseAssetManager;
import core.app.screen.assets.animations.AnimationManager;
import core.app.screen.assets.animations.AnimationState;
import core.chunk.Chunk;
import core.chunk.world.CreateBodyCallable;
import core.chunk.world.EntityBodyBuilder;
import core.chunk.world.exceptions.BodyNotFound;
import core.common.Clock;
import core.common.CommonFactory;
import core.common.Coordinates;
import core.common.Direction;
import core.common.GameSettings;
import core.common.exceptions.ChunkNotFound;
import core.entity.attributes.Attribute;
import core.entity.attributes.AttributeType;
import core.entity.attributes.inventory.Equipped;
import core.entity.attributes.inventory.InventoryBag;
import core.entity.attributes.inventory.item.AbstractInventoryItem;
import core.entity.attributes.msc.AnimationStateWrapper;
import core.entity.attributes.msc.CoordinatesWrapper;
import core.entity.attributes.msc.DirectionWrapper;
import core.entity.attributes.msc.Health;
import core.entity.controllers.EntityController;
import core.entity.controllers.events.types.AbstractEntityEventType;
import core.entity.controllers.events.types.EntityEventTypeFactory;
import core.entity.statemachine.EntityStateMachine;
import core.networking.events.interfaces.SerializeNetworkData;
import core.networking.translation.NetworkDataSerializer;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import networking.NetworkObjects;

public class Entity implements SerializeNetworkData {

  public static float staticHeight = 0.8f;
  public static float staticWidth = 0.8f;
  private final Clock clock;
  private final InventoryBag bag;
  //  public Coordinates coordinates;
  public int zindex = 3;
  public EntityBodyBuilder entityBodyBuilder;
  public Sprite sprite;
  public Health health;
  protected BaseAssetManager baseAssetManager;
  float stateTime;
  private EntityStateMachine entityStateMachine =
      new EntityStateMachine(this, new HashMap<>(), new HashMap<>());
  private DirectionWrapper directionWrapper = new DirectionWrapper(Direction.RIGHT);
  private CoordinatesWrapper coordinatesWrapper;
  private UUID uuid;
  private Chunk chunk;
  private EntityController entityController;
  private int width;
  private int height;
  private AnimationStateWrapper animationStateWrapper =
      new AnimationStateWrapper(AnimationState.DEFAULT);

  public Entity(
      Clock clock,
      BaseAssetManager baseAssetManager,
      EntityBodyBuilder entityBodyBuilder,
      Coordinates coordinates) {
    this.setHeight((int) (staticHeight * GameSettings.PIXEL_SCALE));
    this.setWidth((int) (staticWidth * GameSettings.PIXEL_SCALE));
    this.clock = clock;
    this.baseAssetManager = baseAssetManager;
    this.entityBodyBuilder = entityBodyBuilder;
    //    this.coordinates = coordinates;
    this.coordinatesWrapper = new CoordinatesWrapper(coordinates);
    this.uuid = UUID.randomUUID();
    this.health = new Health(100);
    this.bag = new InventoryBag();
    stateTime = (float) ThreadLocalRandom.current().nextDouble(0, 20);
  }

  public EntityStateMachine getEntityStateMachine() {
    return entityStateMachine;
  }

  public void setEntityStateMachine(EntityStateMachine entityStateMachine) {
    this.entityStateMachine = entityStateMachine;
  }

  public AnimationStateWrapper getAnimationStateWrapper() {
    return animationStateWrapper;
  }

  public void setAnimationStateWrapper(AnimationStateWrapper animationStateWrapper) {
    this.animationStateWrapper = animationStateWrapper;
  }

  public DirectionWrapper getDirectionWrapper() {
    return directionWrapper;
  }

  public void setDirectionWrapper(DirectionWrapper directionWrapper) {
    this.directionWrapper = directionWrapper;
  }

  public Health getHealth() {
    return health;
  }

  public InventoryBag getBag() {
    return bag;
  }

  public AbstractEntityEventType updateAttribute(Attribute attr) {
    if (attr.getType().equals(AttributeType.COORDINATES)) {
      this.setCoordinatesWrapper((CoordinatesWrapper) attr);
    } else if (attr.getType().equals(AttributeType.HEALTH)) {
      this.health = (Health) attr;
      return EntityEventTypeFactory.createChangeHealthEventType(this);
    } else if (attr.getType().equals(AttributeType.ITEM)) {
      this.getBag().updateItem((AbstractInventoryItem) attr);
    } else if (attr.getType().equals(AttributeType.EQUIPPED)) {
      this.getBag().setEquipped((Equipped) attr);
    } else if (attr.getType().equals(AttributeType.DIRECTION)) {
      this.setDirectionWrapper((DirectionWrapper) attr);
    } else if (attr.getType().equals(AttributeType.ANIMATION_STATE)) {
      this.setAnimationStateWrapper((AnimationStateWrapper) attr);
      return null;
    }
    return null;
  }

  public CoordinatesWrapper getCoordinatesWrapper() {
    return coordinatesWrapper;
  }

  public void setCoordinatesWrapper(CoordinatesWrapper coordinatesWrapper) {
    this.coordinatesWrapper = coordinatesWrapper;
  }

  public Chunk getChunk() throws ChunkNotFound {
    Chunk toReturn = chunk;
    if (toReturn == null) {
      throw new ChunkNotFound(this.toString());
    }
    return toReturn;
  }

  public void setChunk(Chunk chunk) {
    this.chunk = chunk;
  }

  public EntityController getEntityController() {
    return entityController;
  }

  public synchronized void setEntityController(EntityController entityController) {
    this.entityController = entityController;
  }

  public String getTextureName() {
    return "frog.png";
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public CreateBodyCallable addWorld(Chunk chunk) {
    return new CreateBodyCallable() {
      @Override
      protected Pair<UUID, Body> addWorld(World world) {
        return EntityBodyBuilder.createEntityBody(world, chunk.chunkRange, Entity.this);
      }
    };
  }

  public void render(AnimationManager animationManager, SpriteBatch batch) {
    //    entityStateMachine.callAnimation();
    stateTime += Gdx.graphics.getDeltaTime();

    Vector2 v2 = this.getCoordinatesWrapper().getCoordinates().toRenderVector2();
    if (animationManager.getGameAnimation(this.getClass()) != null) {
      batch.draw(
          animationManager
              .getGameAnimation(this.getClass())
              .getAnimation(this.getAnimationStateWrapper().getAnimationState())
              .getKeyFrame(stateTime, true),
          v2.x,
          v2.y,
          this.getWidth(),
          this.getHeight());
    } else {
      this.renderSync();
      this.sprite.draw(batch);
    }
  }

  public synchronized void renderSync() {
    if (this.sprite == null) {
      this.sprite = new Sprite((Texture) baseAssetManager.get(this.getTextureName()));
      this.sprite.setSize(this.getWidth(), this.getHeight());
    }
    this.sprite.setPosition(
        this.getCoordinatesWrapper().getCoordinates().getXReal() * GameSettings.PIXEL_SCALE,
        this.getCoordinatesWrapper().getCoordinates().getYReal() * GameSettings.PIXEL_SCALE);
  }

  public synchronized void setZindex(int zindex) {
    this.zindex = zindex;
  }

  public int getUpdateTimeout() {
    return 1;
  }

  public NetworkObjects.NetworkData toNetworkData() {
    return NetworkDataSerializer.createEntity(this);
  }

  public Vector2 getBodyVelocity() throws BodyNotFound, ChunkNotFound {
    return getChunk().getWorldWrapper().getVelocity(this);
  }

  public void setBodyVelocity(Vector2 velocity) throws ChunkNotFound, BodyNotFound {
    getChunk().getWorldWrapper().setVelocity(this, velocity);
  }

  public Vector2 getBodyPosition() throws BodyNotFound, ChunkNotFound {
    return getChunk().getWorldWrapper().getPosition(this);
  }

  public void setBodyPosition(Vector2 position) throws ChunkNotFound, BodyNotFound {
    getChunk().getWorldWrapper().setPosition(this, position);
  }

  public void applyBody(MyConsumer<Body> applyFunction) throws ChunkNotFound, BodyNotFound {
    getChunk().getWorldWrapper().applyBody(this, applyFunction);
  }

  public boolean hasBody() throws ChunkNotFound {
    return getChunk().getWorldWrapper().hasBody(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Entity entity = (Entity) o;
    return Objects.equal(
            this.getCoordinatesWrapper().getCoordinates(),
            entity.getCoordinatesWrapper().getCoordinates())
        && Objects.equal(health, entity.health)
        && Objects.equal(uuid, entity.uuid)
        && Objects.equal(bag, entity.bag)
        && Objects.equal(animationStateWrapper, entity.animationStateWrapper);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(uuid);
  }

  public Coordinates getCenter() {
    return CommonFactory.createCoordinates(
        this.getCoordinatesWrapper().getCoordinates().getXReal()
            + ((float) this.getWidth() / GameSettings.PIXEL_SCALE / 2),
        this.getCoordinatesWrapper().getCoordinates().getYReal()
            + ((float) this.getHeight() / GameSettings.PIXEL_SCALE / 2));
  }

  @Override
  public String toString() {
    return this.getClass().getName()
        + "{"
        + "uuid="
        + uuid
        + ", coordinates="
        + this.getCoordinatesWrapper().getCoordinates()
        + '}';
  }

  public UUID getUuid() {
    return this.uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }
}
