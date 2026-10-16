package gaych;

/** The whole pipeline in one place: Lexer -> Parser -> SemanticChecker -> Interpreter. */
public final class Gaych {
    public static final String VERSION = "1.0.0";
    private Gaych() {}

    public static Ast.Program compile(String source) {
        Ast.Program prog = new Parser(new Lexer(source).tokenize()).parseProgram();
        new SemanticChecker().check(prog);
        return prog;
    }

    public static void run(String source, GaychIO io) {
        new Interpreter(io).run(compile(source));
    }
}
