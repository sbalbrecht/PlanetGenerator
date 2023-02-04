package dev.urth.planetgen.util;

public class Log {
    private long dt;
    private String label;
    private boolean logging;

    public Log() {
        dt = 0;
    }

    public static void log(String s) {
        System.out.printf("%s : %s%n", (System.currentTimeMillis() + "").substring(8), s);
    }

    public void start(String label) {
        if (logging) {
            end();
        }
        logging = true;
        this.label = label;
        dt = System.currentTimeMillis();
    }

    public void end() {
        logging = false;
        dt = System.currentTimeMillis() - dt;
        if (label != null) {
            log(String.format("%s: %d ms", label, dt));
        }
    }
}
