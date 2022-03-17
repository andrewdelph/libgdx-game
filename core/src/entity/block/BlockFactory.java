package entity.block;

import app.screen.BaseAssetManager;
import com.google.inject.Inject;
import common.Clock;
import entity.EntityBodyBuilder;

public class BlockFactory {
  @Inject Clock clock;
  @Inject BaseAssetManager baseAssetManager;
  @Inject EntityBodyBuilder entityBodyBuilder;

  BlockFactory() {}

  public DirtBlock createDirt() {
    return new DirtBlock(clock, baseAssetManager, entityBodyBuilder);
  }

  public StoneBlock createStone() {
    return new StoneBlock(clock, baseAssetManager, entityBodyBuilder);
  }

  public SkyBlock createSky() {
    return new SkyBlock(clock, baseAssetManager, entityBodyBuilder);
  }
}
