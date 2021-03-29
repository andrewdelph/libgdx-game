package render;

import com.google.inject.Guice;
import com.google.inject.Injector;
import configure.CoreApp;
import infra.entity.Entity;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class TestRenderManager {

    @Test
    public void testTestRenderManager() {
        Injector injector = Guice.createInjector(new CoreApp());
        RenderManager renderManager = injector.getInstance(RenderManager.class);
        assertNotNull(renderManager);
        assertNotNull(renderManager.get(Entity.class));
    }
}
