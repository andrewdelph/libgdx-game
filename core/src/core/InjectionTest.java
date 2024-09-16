package core;

import java.util.UUID;
import networking.NetworkObjects;

public class InjectionTest {

  public void test() {
    NetworkObjects.NetworkData.newBuilder().setKey(UUID.class.getName()).setValue("lala").build();
  }
}
