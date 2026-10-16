package gaych;

import gaych.Ast.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Stage 4: a tree-walking evaluator. It dispatches on node TYPE, never on keyword text. */
public final class Interpreter {
    private static final int MAX_DEPTH = 800;

    /** Thrown when the user presses Stop (or the thread is interrupted). Not catchable by sagip. */
    public static final class Stopped extends RuntimeException {
        public Stopped() { super("Stopped", null, false, false); }
    }

    private static final class ReturnSignal extends RuntimeException {
        final Object value;
        ReturnSignal(Object v) { super(null, null, false, false); value = v; }
    }

    private static final class Slot {
        final String type; // chuchu | eme | charot | null (untyped parameter)
        Object value;
        Slot(String type, Object value) { this.type = type; this.value = value; }
    }

    private static final class Env {
        final Env parent;
        final Map<String, Slot> vars = new HashMap<>();
        Env(Env parent) { this.parent = parent; }
        Slot find(String n) {
            for (Env e = this; e != null; e = e.parent) { Slot s = e.vars.get(n); if (s != null) return s; }
            return null;
        }
    }

    private final GaychIO io;
    private final Map<String, Func> funcs = new HashMap<>();
    private int depth = 0;

    public Interpreter(GaychIO io) { this.io = io; }

    public void run(Program prog) {
        for (Func f : prog.funcs()) funcs.put(f.name(), f);
        try {
            execBlock(prog.main(), new Env(null));
        } catch (ReturnSignal r) {
            // uwi inside bongga simply ends the program
        }
    }

    // ---------- statements ----------
    private void execBlock(Block b, Env parent) {
        Env env = new Env(parent);
        for (Stmt s : b.stmts()) exec(s, env);
    }

    private void tick() {
        if (Thread.currentThread().isInterrupted()) throw new Stopped();
    }

    private void exec(Stmt s, Env env) {
        tick();
        if (s instanceof Decl d) {
            Object v = d.init() == null ? defaultFor(d.type()) : coerce(d.type(), eval(d.init(), env), d.name(), d.line());
            env.vars.put(d.name(), new Slot(d.type(), v));
        } else if (s instanceof Assign a) {
            Slot slot = env.find(a.name());
            if (slot == null) throw new GaychException("Variable '" + a.name() + "' is not declared", a.line());
            Object v = eval(a.value(), env);
            slot.value = slot.type == null ? v : coerce(slot.type, v, a.name(), a.line());
        } else if (s instanceof Print p) {
            io.println(fmt(eval(p.value(), env)));
        } else if (s instanceof ExprStmt e) {
            eval(e.expr(), env);
        } else if (s instanceof If i) {
            if (truthy(eval(i.cond(), env), i.line())) execBlock(i.then(), env);
            else if (i.otherwise() != null) execBlock(i.otherwise(), env);
        } else if (s instanceof While w) {
            while (truthy(eval(w.cond(), env), w.line())) { tick(); execBlock(w.body(), env); }
        } else if (s instanceof Try t) {
            try {
                execBlock(t.body(), env);
            } catch (GaychException ex) {
                Env h = new Env(env);
                h.vars.put(t.errName(), new Slot("eme", ex.rawMessage()));
                for (Stmt hs : t.handler().stmts()) exec(hs, h);
            }
        } else if (s instanceof Return r) {
            throw new ReturnSignal(r.value() == null ? null : eval(r.value(), env));
        } else if (s instanceof Block b) {
            execBlock(b, env);
        }
    }

    // ---------- expressions ----------
    private Object eval(Expr e, Env env) {
        if (e instanceof Num n) return n.value();
        if (e instanceof Str s) return s.value();
        if (e instanceof Bool b) return b.value();
        if (e instanceof Var v) {
            Slot s = env.find(v.name());
            if (s == null) throw new GaychException("Variable '" + v.name() + "' is not declared", v.line());
            return s.value;
        }
        if (e instanceof Unary u) {
            Object o = eval(u.operand(), env);
            if (u.op().equals("-")) return num(o, u.line(), "-") * -1;
            if (!(o instanceof Boolean bo)) throw new GaychException("'!' needs a charot (winner/chaka) value", u.line());
            return !bo;
        }
        if (e instanceof Binary b) return binary(b, env);
        if (e instanceof Input in) {
            String prompt = in.prompt() == null ? "" : fmt(eval(in.prompt(), env));
            try {
                String line = io.readLine(prompt);
                return line == null ? "" : line;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new Stopped();
            }
        }
        if (e instanceof Call c) return call(c, env);
        throw new IllegalStateException("Unknown expression " + e);
    }

    private Object binary(Binary b, Env env) {
        String op = b.op();
        if (op.equals("&&") || op.equals("||")) {
            Object l = eval(b.left(), env);
            boolean lv = truthy(l, b.line());
            if (op.equals("&&") && !lv) return false;
            if (op.equals("||") && lv) return true;
            return truthy(eval(b.right(), env), b.line());
        }
        Object l = eval(b.left(), env);
        Object r = eval(b.right(), env);
        switch (op) {
            case "+":
                if (l instanceof String || r instanceof String) return fmt(l) + fmt(r);
                return num(l, b.line(), op) + num(r, b.line(), op);
            case "-": return num(l, b.line(), op) - num(r, b.line(), op);
            case "*": return num(l, b.line(), op) * num(r, b.line(), op);
            case "/": {
                double d = num(r, b.line(), op);
                if (d == 0) throw new GaychException("Division by zero", b.line());
                return num(l, b.line(), op) / d;
            }
            case "%": {
                double d = num(r, b.line(), op);
                if (d == 0) throw new GaychException("Modulo by zero", b.line());
                return num(l, b.line(), op) % d;
            }
            case "<": return num(l, b.line(), op) < num(r, b.line(), op);
            case "<=": return num(l, b.line(), op) <= num(r, b.line(), op);
            case ">": return num(l, b.line(), op) > num(r, b.line(), op);
            case ">=": return num(l, b.line(), op) >= num(r, b.line(), op);
            case "==": return equal(l, r);
            case "!=": return !equal(l, r);
            default: throw new IllegalStateException("Unknown operator " + op);
        }
    }

    private Object call(Call c, Env env) {
        Func f = funcs.get(c.name());
        if (f == null) throw new GaychException("Function '" + c.name() + "' is not defined", c.line());
        List<Object> args = new ArrayList<>();
        for (Expr a : c.args()) args.add(eval(a, env));
        if (++depth > MAX_DEPTH) { depth = 0; throw new GaychException("Stack overflow (too many nested calls in '" + c.name() + "')", c.line()); }
        Env local = new Env(null);
        for (int i = 0; i < f.params().size(); i++) local.vars.put(f.params().get(i), new Slot(null, args.get(i)));
        try {
            for (Stmt s : f.body().stmts()) exec(s, local);
            return null;
        } catch (ReturnSignal r) {
            return r.value;
        } finally {
            if (depth > 0) depth--;
        }
    }

    // ---------- helpers ----------
    private static boolean truthy(Object o, int line) {
        if (o instanceof Boolean b) return b;
        throw new GaychException("Expected a charot (winner/chaka) condition but got " + describe(o), line);
    }

    private static double num(Object o, int line, String op) {
        if (o instanceof Double d) return d;
        throw new GaychException("Operator '" + op + "' needs chuchu (number) values but got " + describe(o), line);
    }

    private static boolean equal(Object a, Object b) {
        if (a == null || b == null) return a == b;
        return a.equals(b);
    }

    private static Object defaultFor(String type) {
        return switch (type) { case "chuchu" -> 0.0; case "eme" -> ""; default -> false; };
    }

    private static Object coerce(String type, Object v, String name, int line) {
        if (v == null) throw new GaychException("Cannot store 'walang' (a function with no uwi value) in '" + name + "'", line);
        switch (type) {
            case "chuchu":
                if (v instanceof Double) return v;
                if (v instanceof String s) {
                    try { return Double.parseDouble(s.trim()); }
                    catch (NumberFormatException ex) { throw new GaychException("Cannot convert \"" + s + "\" to a chuchu (number)", line); }
                }
                break;
            case "eme":
                return fmt(v);
            case "charot":
                if (v instanceof Boolean) return v;
                if (v instanceof String s && (s.equals("winner") || s.equals("chaka"))) return s.equals("winner");
                break;
            default:
        }
        throw new GaychException("'" + name + "' is a " + type + " but got " + describe(v), line);
    }

    private static String describe(Object o) {
        if (o == null) return "walang (no value)";
        if (o instanceof Double) return "a chuchu (" + fmt(o) + ")";
        if (o instanceof String) return "a eme (\"" + o + "\")";
        return "a charot (" + fmt(o) + ")";
    }

    /** How values are shown to the user. */
    public static String fmt(Object o) {
        if (o == null) return "walang";
        if (o instanceof Boolean b) return b ? "winner" : "chaka";
        if (o instanceof Double d) {
            if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) return Long.toString(d.longValue());
            return Double.toString(d);
        }
        return o.toString();
    }
}
