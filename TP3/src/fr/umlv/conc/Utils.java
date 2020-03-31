package fr.umlv.conc;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.Path;

public class Utils {
  private static Path HOME;
  private final static Object lock = new Object();
  private static final VarHandle HANDLE;
  static {
    var lookup = MethodHandles.lookup();
    try {
      HANDLE = lookup.findVarHandle(Utils.class, "HOME", Path.class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  public static Path getHome() {
    var home = HANDLE.getAcquire(HOME); 
    if (home == null) {
      synchronized(lock) {
        home = HANDLE.getAcquire(HOME);
        if (home == null) {
          return HOME = (Path) HANDLE.getAndSetRelease(Path.of(System.getenv("HOME")));
     
        }
      }
    }
    return (Path) home;
  }
}