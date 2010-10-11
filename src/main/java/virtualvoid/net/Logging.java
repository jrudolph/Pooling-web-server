package virtualvoid.net;

public abstract class Logging {
    public static void log(String format, Object... args) {
        System.out.printf(format+"\n", args);
    }
}
