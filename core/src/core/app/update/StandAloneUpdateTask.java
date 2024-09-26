package core.app.update;

import com.badlogic.gdx.Gdx;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.inject.Inject;
import core.app.screen.BaseCamera;
import core.chunk.Chunk;
import core.common.ChunkRange;
import core.common.Clock;
import core.common.GameSettings;
import core.common.GameStore;
import core.common.events.EventService;
import core.common.exceptions.ChunkNotFound;
import core.entity.ActiveEntityManager;
import core.entity.misc.water.WaterService;
import core.generation.ChunkGenerationService;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class StandAloneUpdateTask extends UpdateTask {

  private static final ExecutorService executor = Executors.newCachedThreadPool();

  @Inject public Clock clock;
  @Inject public GameStore gameStore;
  @Inject EventService eventService;
  @Inject BaseCamera baseCamera;
  @Inject ActiveEntityManager activeEntityManager;
  @Inject ChunkGenerationService chunkGenerationService;
  @Inject WaterService waterService;

  public StandAloneUpdateTask() {}

  @Override
  public void run() {
    /*
    - create requested chunks from screen. send them to the generation manager
    - generate around active entities
    - don't delete chunks
     */
    this.clock.tick();

    Set<ChunkRange> requiredChunkRanges = new HashSet<>();

    // get the set of onscreen chunks
    requiredChunkRanges.addAll(baseCamera.getChunkRangeOnScreen());

    // get the set of active entities. get their chunks
    requiredChunkRanges.addAll(activeEntityManager.getActiveChunkRanges());
    // generate them all
    chunkGenerationService.queueChunkRangeToGenerate(requiredChunkRanges);

    Set<Chunk> chunksOnTick = new HashSet<>();
    try {
      chunksOnTick = this.gameStore.getChunkOnClock(this.clock.getCurrentTick());
      Gdx.app.debug(GameSettings.LOG_TAG, "Updating " + chunksOnTick.size() + " chunks.");

      if (chunksOnTick.size() > 100) {
        Gdx.app.debug(
            GameSettings.LOG_TAG,
            "Updating "
                + chunksOnTick.size()
                + " chunks."
                + baseCamera.getChunkRangeOnScreen().size()
                + " chunks on screen.");
      }
      executor.invokeAll(this.gameStore.getChunkOnClock(this.clock.getCurrentTick()));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    this.eventService.firePostUpdateEvents();

    try {
      Collection<ChunkRange> updatedWatchChunkRanges =
          Collections2.transform(
              chunksOnTick,
              new Function<Chunk, ChunkRange>() {
                @NullableDecl
                @Override
                public ChunkRange apply(@NullableDecl Chunk input) {
                  return input.chunkRange;
                }
              });

      waterService.update(updatedWatchChunkRanges);
    } catch (ChunkNotFound e) {
      e.printStackTrace();
    }
  }
}
