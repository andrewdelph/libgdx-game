package core.common;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.inject.Inject;
import core.chunk.Chunk;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class ChunkClockMap {

  Map<ChunkRange, Chunk> map;

  @Inject
  ChunkClockMap() {
    this.map = new ConcurrentHashMap<>();
  }

  void add(Chunk chunk) {
    this.map.put(chunk.chunkRange, chunk);
  }

  Chunk get(ChunkRange chunkRange) {
    return this.map.get(chunkRange);
  }

  Chunk remove(ChunkRange chunkRange) {
    Chunk toRemove = this.map.get(chunkRange);
    this.map.remove(chunkRange);
    return toRemove;
  }

  Boolean doesChunkExist(ChunkRange chunkRange) {
    return this.map.containsKey(chunkRange);
  }

  public Set<ChunkRange> getChunkRangeSet() {
    return this.map.keySet();
  }

  Set<Chunk> getChunksOnTick(final Tick tick) {

    Set<Chunk> onMyTick =
        new HashSet<Chunk>(
            Collections2.filter(
                map.values(),
                new Predicate<Chunk>() {
                  @Override
                  public boolean apply(@NullableDecl Chunk input) {
                    return input != null && input.updateTick.time == tick.time;
                  }
                }));

    Set<Chunk> surroundingChunks = new HashSet<>();

    for (Chunk onTick : onMyTick) {
      surroundingChunks.addAll(onTick.getNeighborChunks());
    }

    onMyTick.addAll(surroundingChunks);

    return onMyTick;
  }
}
