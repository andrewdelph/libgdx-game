package infra.app.server;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import configuration.ServerConfig;
import infra.app.Game;
import infra.chunk.ChunkFactory;
import infra.common.GameStore;
import infra.generation.ChunkGenerationManager;
import infra.networking.consumer.NetworkConsumer;
import infra.networking.server.ServerNetworkHandle;

import java.io.IOException;

public class ServerGame extends Game {

  @Inject ServerNetworkHandle serverNetworkHandle;

  @Inject
  public ServerGame(
      GameStore gameStore,
      ChunkFactory chunkFactory,
      ChunkGenerationManager chunkGenerationManager,
      NetworkConsumer networkConsumer)
      throws Exception {
    super(gameStore, chunkFactory, chunkGenerationManager, networkConsumer);
  }

  public static void main(String[] args) throws InterruptedException, IOException {
    Injector injector = Guice.createInjector(new ServerConfig());
    Game game = injector.getInstance(Game.class);
    game.start();

    while (true) {
      Thread.sleep(Long.MAX_VALUE);
    }
  }

  @Override
  public void start() throws IOException {
    super.start();
    serverNetworkHandle.start();
  }

  @Override
  public void stop() {
    super.stop();
    this.serverNetworkHandle.close();
  }
}
