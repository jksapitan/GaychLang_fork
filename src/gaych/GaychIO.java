package gaych;

/** How the interpreter talks to the outside world (terminal, GUI, tests ...). */
public interface GaychIO {
    /** Called by chika(...). Implementations should append a newline. */
    void println(String text);

    /** Called by getch(prompt). prompt may be empty. Return null on end-of-input. */
    String readLine(String prompt) throws InterruptedException;
}
