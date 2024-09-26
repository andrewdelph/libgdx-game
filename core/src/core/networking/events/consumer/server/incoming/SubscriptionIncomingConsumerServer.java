package core.networking.events.consumer.server.incoming;

import com.google.inject.Inject;
import core.chunk.ActiveChunkManager;
import core.common.ChunkRange;
import core.common.events.types.EventType;
import core.generation.ChunkGenerationService;
import core.networking.events.types.incoming.SubscriptionIncomingEventType;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SubscriptionIncomingConsumerServer implements MyConsumer<EventType> {

  @Inject ActiveChunkManager activeChunkManager;
  @Inject ChunkGenerationService chunkGenerationService;

  @Override
  public void accept(EventType eventType) {
    SubscriptionIncomingEventType realEvent = (SubscriptionIncomingEventType) eventType;
    Set<ChunkRange> userChunkRangeList =
        activeChunkManager.getUserChunkRanges(realEvent.getUserID());
    Predicate<ChunkRange> doesNotContain = (userChunkRangeList::contains);
    doesNotContain = doesNotContain.negate();
    List<ChunkRange> newChunkRangeList =
        realEvent.getChunkRangeList().stream()
            .distinct()
            .filter(doesNotContain)
            .collect(Collectors.toList());

    activeChunkManager.setUserChunkSubscriptions(
        realEvent.getUserID(), realEvent.getChunkRangeList());

    chunkGenerationService.queueChunkRangeToGenerate(realEvent.getChunkRangeList());
  }
}
