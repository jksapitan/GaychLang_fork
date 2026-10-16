package gaych;

import gaych.Token.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stage 1: source text -> tokens.
 * Only this class knows the GaychLang words. Everything after it works with
 * plain token types (IF, WHILE, PRINT ...).
 */
public final class Lexer {

    /** The keyword table. Change this map and the whole language gets a new skin. */
    static final Map<String, Type> KEYWORDS = Map.ofEntries(
            Map.entry("bongga", Type.MAIN),
            Map.entry("gawa", Type.FUNC),
            Map.entry("chuchu", Type.TYPE),
            Map.entry("eme", Type.TYPE),
            Map.entry("charot", Type.TYPE),
            Map.entry("keri", Type.IF),
            Map.entry("ewan", Type.ELSE),
            Map.entry("gora", Type.WHILE),
            Map.entry("subukan", Type.TRY),
            Map.entry("sagip", Type.CATCH),
            Map.entry("uwi", Type.RETURN),
            Map.entry("chika", Type.PRINT),
            Map.entry("getch", Type.INPUT),
            Map.entry("winner", Type.TRUE),
            Map.entry("chaka", Type.FALSE));

    private final String src;
    private int pos = 0;
    private int line = 1;
    private final List<Token> tokens = new ArrayList<>();

    public Lexer(String src) { this.src = src; }

    public List<Token> tokenize() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '\n') { line++; pos++; }
            else if (Character.isWhitespace(c)) { pos++; }
            else if (c == '/' && peek(1) == '/') { while (pos < src.length() && src.charAt(pos) != '\n') pos++; }
            else if (c == '/' && peek(1) == '*') { blockComment(); }
            else if (Character.isDigit(c)) { number(); }
            else if (Character.isLetter(c) || c == '_') { word(); }
            else if (c == '"') { string(); }
            else { symbol(c); }
        }
        tokens.add(new Token(Type.EOF, "", line));
        return tokens;
    }

    private char peek(int n) { return pos + n < src.length() ? src.charAt(pos + n) : '\0'; }

    private void blockComment() {
        int startLine = line;
        pos += 2;
        while (pos < src.length() && !(src.charAt(pos) == '*' && peek(1) == '/')) {
            if (src.charAt(pos) == '\n') line++;
            pos++;
        }
        if (pos >= src.length()) throw new GaychException("Unterminated comment", startLine);
        pos += 2;
    }

    private void number() {
        int start = pos;
        while (Character.isDigit(peek(0))) pos++;
        if (peek(0) == '.' && Character.isDigit(peek(1))) {
            pos++;
            while (Character.isDigit(peek(0))) pos++;
        }
        tokens.add(new Token(Type.NUMBER, src.substring(start, pos), line));
    }

    private void word() {
        int start = pos;
        while (Character.isLetterOrDigit(peek(0)) || peek(0) == '_') pos++;
        String w = src.substring(start, pos);

        tokens.add(new Token(KEYWORDS.getOrDefault(w, Type.IDENT), w, line));
    }

    private void string() {
        int startLine = line;
        pos++; // opening quote
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= src.length() || src.charAt(pos) == '\n')
                throw new GaychException("Unterminated string", startLine);
            char c = src.charAt(pos++);
            if (c == '"') break;
            if (c == '\\') {
                char n = pos < src.length() ? src.charAt(pos++) : '\0';
                switch (n) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> throw new GaychException("Unknown escape \\" + n, line);
                }
            } else sb.append(c);
        }
        tokens.add(new Token(Type.STRING, sb.toString(), startLine));
    }

    private void symbol(char c) {
        char n = peek(1);
        Type t;
        int len = 1;
        switch (c) {
            case '(' -> t = Type.LPAREN;
            case ')' -> t = Type.RPAREN;
            case '{' -> t = Type.LBRACE;
            case '}' -> t = Type.RBRACE;
            case ',' -> t = Type.COMMA;
            case ';' -> t = Type.SEMI;
            case '+' -> t = Type.PLUS;
            case '-' -> t = Type.MINUS;
            case '*' -> t = Type.STAR;
            case '/' -> t = Type.SLASH;
            case '%' -> t = Type.PERCENT;
            case '=' -> { if (n == '=') { t = Type.EQ; len = 2; } else t = Type.ASSIGN; }
            case '!' -> { if (n == '=') { t = Type.NEQ; len = 2; } else t = Type.NOT; }
            case '<' -> { if (n == '=') { t = Type.LE; len = 2; } else t = Type.LT; }
            case '>' -> { if (n == '=') { t = Type.GE; len = 2; } else t = Type.GT; }
            case '&' -> { if (n == '&') { t = Type.AND; len = 2; } else throw new GaychException("Did you mean '&&'?", line); }
            case '|' -> { if (n == '|') { t = Type.OR; len = 2; } else throw new GaychException("Did you mean '||'?", line); }
            default -> throw new GaychException("Unexpected character '" + c + "'", line);
        }
        tokens.add(new Token(t, src.substring(pos, pos + len), line));
        pos += len;
    }
}
