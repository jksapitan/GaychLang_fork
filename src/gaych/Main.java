package gaych;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entry point.
 *   java -jar GaychLang.jar                 -> opens the IDE
 *   java -jar GaychLang.jar program.gaych   -> runs in the terminal
 */
public final class Main {
    private Main() {}

    private static final String HELP = """
            GaychLang %s - a tiny language written in Java

            Usage:
              gaych <file.gaych>         run a program in the terminal
              gaych --check <file>       check syntax/semantics without running
              gaych --ide [file]         open the graphical IDE
              gaych --help | --version

            Running with no arguments opens the IDE (or this help if there is no display).
            """.formatted(Gaych.VERSION);

    public static void main(String[] args) {
        if (args.length == 0) {
            if (GraphicsEnvironment.isHeadless()) { System.out.print(HELP); return; }
            GaychIDE.launch(null);
            return;
        }
        switch (args[0]) {
            case "-h", "--help" -> System.out.print(HELP);
            case "-v", "--version" -> System.out.println("GaychLang " + Gaych.VERSION);
            case "--ide" -> GaychIDE.launch(args.length > 1 ? Path.of(args[1]) : null);
            case "--check" -> {
                if (args.length < 2) { System.err.println("Usage: gaych --check <file.gaych>"); System.exit(2); }
                System.exit(check(Path.of(args[1])));
            }
            default -> System.exit(runFile(Path.of(args[0])));
        }
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static int check(Path file) {
        try {
            Gaych.compile(read(file));
            System.out.println("OK - no errors found in " + file);
            return 0;
        } catch (GaychException e) {
            System.err.println("GaychLang error: " + e.getMessage());
            return 1;
        } catch (IOException e) {
            System.err.println("Cannot read " + file + ": " + e.getMessage());
            return 2;
        }
    }

    /** Runs on a big-stack thread so deep recursion in user programs is safe. */
    private static int runFile(Path file) {
        final String src;
        try { src = read(file); }
        catch (IOException e) { System.err.println("Cannot read " + file + ": " + e.getMessage()); return 2; }

        final int[] code = {0};
        Thread t = new Thread(null, () -> {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            GaychIO io = new GaychIO() {
                @Override public void println(String text) { System.out.println(text); }
                @Override public String readLine(String prompt) {
                    System.out.print(prompt);
                    System.out.flush();
                    try { return in.readLine(); } catch (IOException e) { return null; }
                }
            };
            try {
                Gaych.run(src, io);
            } catch (GaychException e) {
                System.out.flush();
                System.err.println("GaychLang error: " + e.getMessage());
                code[0] = 1;
            } catch (Interpreter.Stopped e) {
                code[0] = 130;
            }
        }, "gaych-main", 256L * 1024 * 1024);
        t.start();
        try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return code[0];
    }
}
