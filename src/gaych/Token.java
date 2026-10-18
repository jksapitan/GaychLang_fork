package gaych;

/** A single lexical token. The lexeme text is kept only for error messages and literals. */
public record Token(Type type, String text, int line) {

    public enum Type {
        // literals / names
        NUMBER, STRING, IDENT,
        // keywords (this is where the "GaychLang skin" lives)
        MAIN,      // bongga
        FUNC,      // gawa
        TYPE,      // chuchu | eme | charot
        IF,        // keri
        ELSE,      // ewan
        WHILE,     // gora
        TRY,       // subukan
        CATCH,     // sagip
        RETURN,    // uwi
        PRINT,     // chika
        INPUT,     // getch
        TRUE,      // winner
        FALSE,     // chaka
        SWITCH,    // pili
        CASE,      // kaso
        DEFAULT,   // iba
        // punctuation / operators
        LPAREN, RPAREN, LBRACE, RBRACE, COMMA, SEMI,
        ASSIGN, PLUS, MINUS, STAR, SLASH, PERCENT,
        EQ, NEQ, LT, LE, GT, GE, AND, OR, NOT,
        EOF
    }

    @Override
    public String toString() {
        return type + (text.isEmpty() ? "" : "(" + text + ")");
    }
}
