package core.chunk;

import com.google.inject.Guice;
import com.google.inject.Injector;
import core.app.user.UserID;
import core.common.CommonFactory;
import core.configuration.BaseServerConfig;
import core.entity.attributes.msc.Coordinates;
import org.junit.Before;
import org.junit.Test;

public class testActiveChunkManager {
  ActiveChunkManager activeChunkManager;
  Injector serverInjector;

  @Before
  public void setup() {
    serverInjector = Guice.createInjector(new BaseServerConfig());
    activeChunkManager = serverInjector.getInstance(ActiveChunkManager.class);
  }

  @Test
  public void testActiveEntityManager() {
    UserID testUserID = UserID.createUserID();
    Coordinates testCoordinates = CommonFactory.createCoordinates(0, 0);
    ChunkRange testChunkRange = new ChunkRange(testCoordinates);

    activeChunkManager.addUserChunkSubscriptions(testUserID, testChunkRange);

    assert activeChunkManager.getChunkRangeUsers(testChunkRange).contains(testUserID);
    assert activeChunkManager.getUserChunkRanges(testUserID).contains(testChunkRange);

    activeChunkManager.removeUser(testUserID);

    assert !activeChunkManager.getChunkRangeUsers(testChunkRange).contains(testUserID);
    assert !activeChunkManager.getUserChunkRanges(testUserID).contains(testChunkRange);
  }
}
