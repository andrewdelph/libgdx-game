package core.app.screen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.google.inject.Inject;
import core.app.game.Game;
import core.app.game.GameController;
import core.app.screen.assets.BaseAssetManager;
import core.app.screen.assets.animations.AnimationManager;
import core.app.user.User;
import core.chunk.Chunk;
import core.chunk.world.exceptions.BodyNotFound;
import core.common.CommonFactory;
import core.common.GameSettings;
import core.common.GameStore;
import core.common.exceptions.ChunkNotFound;
import core.common.exceptions.SerializationDataMissing;
import core.common.exceptions.WrongVersion;
import core.common.javautil.MyConsumer;
import core.entity.ActiveEntityManager;
import core.entity.Entity;
import core.entity.EntityFactory;
import core.entity.controllers.factories.EntityControllerFactory;
import core.entity.groups.Group;
import core.entity.groups.GroupService;
import core.entity.statemachine.EntityStateMachineFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GameScreen extends ApplicationAdapter {

  public static ShapeRenderer pathDebugRender;

  @Inject Game game;
  @Inject GameStore gameStore;
  @Inject EntityFactory entityFactory;
  @Inject BaseAssetManager baseAssetManager;
  @Inject BaseCamera baseCamera;
  @Inject GameController gameController;
  @Inject EntityControllerFactory entityControllerFactory;
  @Inject ActiveEntityManager activeEntityManager;
  @Inject User user;
  @Inject GameSettings gameSettings;
  @Inject GroupService groupService;
  @Inject AnimationManager animationManager;
  @Inject EntityStateMachineFactory entityStateMachineFactory;
  Box2DDebugRenderer debugRenderer;
  Matrix4 debugMatrix;
  Entity myEntity;
  SpriteBatch batch;

  // A variable for tracking elapsed time for the animation
  float stateTime;

  @Inject
  public GameScreen() {}

  @Override
  public void create() {
    baseAssetManager.init();
    animationManager.init();
    baseCamera.init();
    try {
      game.start();
    } catch (IOException
        | InterruptedException
        | SerializationDataMissing
        | BodyNotFound
        | WrongVersion e) {
      Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
      this.dispose();
    }
    batch = new SpriteBatch();
    batch.enableBlending();

    createMyEntity();

    groupService.registerEntityGroup(myEntity.getUuid(), Group.PLAYER_GROUP);
    activeEntityManager.registerActiveEntity(user.getUserID(), myEntity.getUuid());
    debugRenderer = new Box2DDebugRenderer();
    pathDebugRender = new ShapeRenderer();
    pathDebugRender.setColor(Color.RED);
    Gdx.graphics.setTitle("" + gameSettings.getVersion());
  }

  protected void createMyEntity() {
    myEntity = entityFactory.createEntity(CommonFactory.createCoordinates(0, 2));
    myEntity.setEntityController(entityControllerFactory.createEntityUserController(myEntity));
    try {
      myEntity = gameController.addEntity(myEntity);
    } catch (ChunkNotFound e) {
      Gdx.app.error(GameSettings.LOG_TAG, e.getMessage(), e);
      this.dispose();
    }
    myEntity.setEntityStateMachine(entityStateMachineFactory.createEntityStateMachine(myEntity));

    Gdx.app.log(GameSettings.LOG_TAG, "my entity " + myEntity.getUuid());
  }

  @Override
  public void resize(int width, int height) {
    Gdx.app.log(GameSettings.LOG_TAG, "resize:" + width + "," + height);
    baseCamera.setToOrtho(false, width, height);
  }

  @Override
  public void render() {

    stateTime += Gdx.graphics.getDeltaTime(); // Accumulate elapsed animation time

    if (!myEntity.getHealth().isAlive()) {
      createMyEntity();
    }

    debugMatrix =
        batch
            .getProjectionMatrix()
            .cpy()
            .scale(
                ((float) GameSettings.PIXEL_SCALE / GameSettings.PHYSICS_SCALE),
                ((float) GameSettings.PIXEL_SCALE / GameSettings.PHYSICS_SCALE),
                0);
    baseCamera.position.set(
        myEntity.getCoordinatesWrapper().getCoordinates().getXReal() * GameSettings.PIXEL_SCALE,
        myEntity.getCoordinatesWrapper().getCoordinates().getYReal() * GameSettings.PIXEL_SCALE,
        0);
    baseCamera.update();
    // focus camera
    batch.setProjectionMatrix(baseCamera.combined);
    // clear screen
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    batch.begin();

    if (GameSettings.RENDER_DEBUG) {
      pathDebugRender.begin(ShapeRenderer.ShapeType.Line);
      pathDebugRender.setProjectionMatrix(debugMatrix);
    }

    List<Entity> renderList =
        gameStore.getEntityInRange(
            baseCamera.getBottomLeftCoordinates(), baseCamera.getTopRightCoordinates());

    Collections.sort(
        renderList,
        new Comparator<Entity>() {
          @Override
          public int compare(Entity entity, Entity t1) {
            return entity.zindex - t1.zindex;
          }
        });

    for (Entity entity : renderList) {
      // render entity
      try {
        entity.render(animationManager, batch);
        if (entity.getEntityController() != null) {
          entity.getEntityController().render();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    batch.end();

    if (GameSettings.RENDER_DEBUG) {

      Chunk mainChunk =
          this.gameStore.getChunk(
              (CommonFactory.createChunkRange(myEntity.getCoordinatesWrapper().getCoordinates())));

      mainChunk
          .getWorldWrapper()
          .applyWorld(
              new MyConsumer<World>() {
                @Override
                public void accept(World world) {
                  debugRenderer.render(world, debugMatrix);
                }
              });
      pathDebugRender.end();

      Chunk lowerChunk =
          this.gameStore.getChunk(
              (CommonFactory.createChunkRange(myEntity.getCoordinatesWrapper().getCoordinates()))
                  .getDown());
      Chunk leftChunk =
          this.gameStore.getChunk(
              (CommonFactory.createChunkRange(myEntity.getCoordinatesWrapper().getCoordinates()))
                  .getLeft());
      Chunk rightChunk =
          this.gameStore.getChunk(
              (CommonFactory.createChunkRange(myEntity.getCoordinatesWrapper().getCoordinates()))
                  .getRight());

      if (lowerChunk == null) {
        return;
      }
      debugMatrix =
          batch
              .getProjectionMatrix()
              .cpy()
              .scale(
                  ((float) GameSettings.PIXEL_SCALE / GameSettings.PHYSICS_SCALE),
                  ((float) GameSettings.PIXEL_SCALE / GameSettings.PHYSICS_SCALE),
                  0)
              .translate(
                  0,
                  -10
                      - GameSettings.PIXEL_SCALE
                          * GameSettings.CHUNK_SIZE
                          * ((float) GameSettings.PHYSICS_SCALE / GameSettings.PIXEL_SCALE),
                  0);

      lowerChunk
          .getWorldWrapper()
          .applyWorld(
              new MyConsumer<World>() {
                @Override
                public void accept(World world) {
                  debugRenderer.render(world, debugMatrix);
                }
              });

      if (leftChunk == null) {
        return;
      }
      debugMatrix =
          batch
              .getProjectionMatrix()
              .cpy()
              .scale(
                  ((float) GameSettings.PIXEL_SCALE / GameSettings.PHYSICS_SCALE),
                  ((float) GameSettings.PIXEL_SCALE / GameSettings.PHYSICS_SCALE),
                  0)
              .translate(
                  -10
                      - GameSettings.PIXEL_SCALE
                          * GameSettings.CHUNK_SIZE
                          * ((float) GameSettings.PHYSICS_SCALE / GameSettings.PIXEL_SCALE),
                  0,
                  0);

      leftChunk
          .getWorldWrapper()
          .applyWorld(
              new MyConsumer<World>() {
                @Override
                public void accept(World world) {
                  debugRenderer.render(world, debugMatrix);
                }
              });

      if (rightChunk == null) {
        return;
      }
      debugMatrix =
          batch
              .getProjectionMatrix()
              .cpy()
              .scale(
                  ((float) GameSettings.PIXEL_SCALE / GameSettings.PHYSICS_SCALE),
                  ((float) GameSettings.PIXEL_SCALE / GameSettings.PHYSICS_SCALE),
                  0)
              .translate(
                  10
                      + GameSettings.PIXEL_SCALE
                          * GameSettings.CHUNK_SIZE
                          * ((float) GameSettings.PHYSICS_SCALE / GameSettings.PIXEL_SCALE),
                  0,
                  0);

      rightChunk
          .getWorldWrapper()
          .applyWorld(
              new MyConsumer<World>() {
                @Override
                public void accept(World world) {
                  debugRenderer.render(world, debugMatrix);
                }
              });
    }
  }

  @Override
  public void dispose() {
    this.game.stop();
  }
}
