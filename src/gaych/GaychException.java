package gaych;

/** Any error raised by GaychLang itself (syntax, semantic or runtime). */
public class GaychException extends RuntimeException {
    private final int line;
    private final String raw;

    public GaychException(String message, int line) {
        super(line > 0 ? "Line " + line + ": " + message : message);
        this.raw = message;
        this.line = line;
    }

    /** Message without the "Line N:" prefix (this is what a sagip(e) block receives). */
    public String rawMessage() { return raw; }

    public int line() { return line; }
}
