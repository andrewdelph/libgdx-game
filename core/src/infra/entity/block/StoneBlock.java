package infra.entity.block;

import infra.app.render.BaseAssetManager;
import infra.common.Clock;
import infra.entity.EntityBodyBuilder;

public class StoneBlock extends SolidBlock {
  public StoneBlock(
      Clock clock, BaseAssetManager baseAssetManager, EntityBodyBuilder entityBodyBuilder) {
    super(clock, baseAssetManager, entityBodyBuilder);
    this.textureName = "stone.png";
  }
}
