package gaych;

import java.util.List;

/** Stage 2 output: the generic syntax tree. It has no idea what the keywords were spelled like. */
public final class Ast {
    private Ast() {}

    // ---- expressions ----
    public interface Expr {}
    public record Num(double value) implements Expr {}
    public record Str(String value) implements Expr {}
    public record Bool(boolean value) implements Expr {}
    public record Var(String name, int line) implements Expr {}
    public record Unary(String op, Expr operand, int line) implements Expr {}
    public record Binary(String op, Expr left, Expr right, int line) implements Expr {}
    public record Call(String name, List<Expr> args, int line) implements Expr {}
    /** getch("prompt") */
    public record Input(Expr prompt, int line) implements Expr {}

    // ---- statements ----
    public interface Stmt {}
    public record Block(List<Stmt> stmts) implements Stmt {}
    public record Decl(String type, String name, Expr init, int line) implements Stmt {}
    public record Assign(String name, Expr value, int line) implements Stmt {}
    /** chika(expr); */
    public record Print(Expr value, int line) implements Stmt {}
    public record ExprStmt(Expr expr, int line) implements Stmt {}
    public record If(Expr cond, Block then, Block otherwise, int line) implements Stmt {}
    public record While(Expr cond, Block body, int line) implements Stmt {}
    public record Try(Block body, String errName, Block handler, int line) implements Stmt {}
    public record Return(Expr value, int line) implements Stmt {}

    // ---- top level ----
    public record Func(String name, List<String> params, Block body, int line) {}
    public record Program(List<Func> funcs, Block main) {}
}
