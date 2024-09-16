package com.mygdx.game;

import android.os.Bundle;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.inject.Guice;
import com.google.inject.Injector;
import core.app.screen.GameScreen;
import core.configuration.StandAloneConfig;

public class AndroidLauncher extends AndroidApplication {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    //    PhysicsTest2 physicsTest = new PhysicsTest2();

    Injector injector = Guice.createInjector(new StandAloneConfig());

    GameScreen gameScreen = injector.getInstance(GameScreen.class);

    //    final Logger LOGGER = LogManager.getLogger();

    ApplicationAdapter applicationAdapter =
        new ApplicationAdapter() {
          @Override
          public void render() {
            super.render();
            System.out.println("render");
          }
        };

    System.out.println("lalalala");

    initialize(gameScreen, new AndroidApplicationConfiguration());
  }
}
