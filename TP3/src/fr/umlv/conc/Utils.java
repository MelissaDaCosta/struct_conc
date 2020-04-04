package fr.umlv.conc;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.Path;

/**
 * Exercice 3 - Question 4 et 5
 * 
 * @return
 */

public class Utils {
    private static Path HOME;
    private Path HOME2;
    private final static VarHandle HANDLE;
    static {
        try {
            HANDLE = MethodHandles.lookup().findStaticVarHandle(Utils.class, "HOME", Path.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Should not happen");
        }
    }

    public static Path getHome() {
        var home = (Path) HANDLE.getAcquire();
        if (home == null) {
            synchronized (Utils.class) {
                home = (Path) HANDLE.getAcquire();
                if (home == null) {
                    HANDLE.setRelease(Path.of(System.getenv("HOME")));
                    return (Path) HANDLE.getAcquire();
                }
            }
        }
        return home;
    }

    private static class LazyHolder {
        static final Path HOME2 = Path.of(System.getenv("HOME"));
    }

    public static Path getHome2() {
        return LazyHolder.HOME2;
    }

    public static void main(String[] args) {
        System.out.println(Utils.getHome2());
    }
}