package gaych;

import gaych.Ast.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Stage 3: catches undeclared names, duplicates and wrong argument counts BEFORE anything runs. */
public final class SemanticChecker {
    private final Map<String, Integer> funcs = new HashMap<>();
    private final Deque<Set<String>> scopes = new ArrayDeque<>();

    public void check(Program prog) {
        for (Func f : prog.funcs()) {
            if (funcs.put(f.name(), f.params().size()) != null)
                throw new GaychException("Function '" + f.name() + "' is defined twice", f.line());
        }
        for (Func f : prog.funcs()) {
            push();
            for (String param : f.params()) declare(param, f.line());
            stmts(f.body());
            pop();
        }
        block(prog.main());
    }

    private void push() { scopes.push(new HashSet<>()); }
    private void pop() { scopes.pop(); }

    private void declare(String name, int line) {
        if (!scopes.peek().add(name))
            throw new GaychException("'" + name + "' is already declared in this scope", line);
    }

    private boolean isDeclared(String name) {
        for (Set<String> s : scopes) if (s.contains(name)) return true;
        return false;
    }

    private void block(Block b) { push(); stmts(b); pop(); }
    private void stmts(Block b) { for (Stmt s : b.stmts()) stmt(s); }

    private void stmt(Stmt s) {
        if (s instanceof Decl d) {
            if (d.init() != null) expr(d.init());
            declare(d.name(), d.line());
        } else if (s instanceof Assign a) {
            if (!isDeclared(a.name())) throw new GaychException("Variable '" + a.name() + "' is not declared", a.line());
            expr(a.value());
        } else if (s instanceof Print p) {
            expr(p.value());
        } else if (s instanceof ExprStmt e) {
            expr(e.expr());
        } else if (s instanceof If i) {
            expr(i.cond());
            block(i.then());
            if (i.otherwise() != null) block(i.otherwise());
        } else if (s instanceof While w) {
            expr(w.cond());
            block(w.body());
        } else if (s instanceof Try t) {
            block(t.body());
            push();
            declare(t.errName(), t.line());
            stmts(t.handler());
            pop();
        } else if (s instanceof Return r) {
            if (r.value() != null) expr(r.value());
        } else if (s instanceof Block b) {
            block(b);
        }
    }

    private void expr(Expr e) {
        if (e instanceof Var v) {
            if (!isDeclared(v.name())) throw new GaychException("Variable '" + v.name() + "' is not declared", v.line());
        } else if (e instanceof Unary u) {
            expr(u.operand());
        } else if (e instanceof Binary b) {
            expr(b.left());
            expr(b.right());
        } else if (e instanceof Input in) {
            if (in.prompt() != null) expr(in.prompt());
        } else if (e instanceof Call c) {
            Integer arity = funcs.get(c.name());
            if (arity == null) throw new GaychException("Function '" + c.name() + "' is not defined (use gawa to create it)", c.line());
            if (arity != c.args().size())
                throw new GaychException("Function '" + c.name() + "' expects " + arity + " argument(s) but got " + c.args().size(), c.line());
            for (Expr a : c.args()) expr(a);
        }
    }
}
