package de.lexej.VNFP.model.log;

/**
 * Epic class for epic things.
 * (This generic message was probably left here because
 * this file's name is self-explanatory.)
 *
 * @author alex
 */
public class Debugger {
    private static boolean print = false;

    public static void print(String s) {
        if (print) System.out.print(s);
    }

    public static void println(String s) {
        if (print) System.out.println(s);
    }
}
