package infra.common;

import com.google.inject.Inject;
import infra.chunk.Chunk;
import infra.chunk.ChunkRange;
import infra.entity.Entity;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class GameStore {

  Map<UUID, ChunkRange> entityMap;

  @Inject ChunkClockMap chunkClockMap;

  GameStore() {
    this.entityMap = new ConcurrentHashMap<>();
  }

  public void addEntity(Entity entity) {
    ChunkRange entityChunkRange = new ChunkRange(entity.coordinates);
    this.chunkClockMap.get(entityChunkRange).addEntity(entity);
    this.entityMap.put(entity.uuid, entityChunkRange);
  }

  public Entity getEntity(UUID uuid) {
    return this.chunkClockMap.get(this.entityMap.get(uuid)).getEntity(uuid);
  }

  public Chunk getEntityChunk(UUID uuid) {
    return this.chunkClockMap.get(this.entityMap.get(uuid));
  }

  public void addChunk(Chunk chunk) {
    this.chunkClockMap.add(chunk);
  }

  public Chunk getChunk(ChunkRange chunkRange) {
    return this.chunkClockMap.get(chunkRange);
  }

  public void syncEntity(Entity entity) {
    this.addEntity(entity);
  }

  public List<Entity> getEntityListInRange(int x1, int y1, int x2, int y2) {
    List<Entity> entityList = new ArrayList<>();
    for (UUID entityUUID : new ArrayList<>(this.entityMap.keySet())) {
      entityList.add(this.getEntity(entityUUID));
    }
    return entityList;
  }

  public Set<ChunkRange> getActiveChunkRangeSet() {
    return new HashSet<>(this.entityMap.values());
  }

  public List<Callable<Chunk>> getChunkOnClock(Tick tick) {
    return this.chunkClockMap.getChunksOnTick(tick);
  }
}
