package gaych;

import gaych.Ast.*;
import gaych.Token.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage 2: tokens -> AST (recursive descent).
 *
 * program   -> funcDecl* 'bongga' block
 * funcDecl  -> 'gawa' IDENT '(' (IDENT (',' IDENT)*)? ')' block
 * stmt      -> decl | assign | print | callStmt | ifStmt | whileStmt | tryStmt | return | block
 * decl      -> TYPE IDENT ('=' expr)? ';'
 * assign    -> IDENT '=' expr ';'
 * print     -> 'chika' '(' expr ')' ';'
 * ifStmt    -> 'keri' '(' expr ')' block ('ewan' (ifStmt | block))?
 * whileStmt -> 'gora' '(' expr ')' block
 * tryStmt   -> 'subukan' block 'sagip' '(' IDENT ')' block
 * return    -> 'uwi' expr? ';'
 * expr      -> or ; or -> and ('||' and)* ; and -> equality ('&&' equality)*
 * equality  -> comparison (('=='|'!=') comparison)*
 * comparison-> term (('<'|'<='|'>'|'>=') term)*
 * term      -> factor (('+'|'-') factor)* ; factor -> unary (('*'|'/'|'%') unary)*
 * unary     -> ('-'|'!') unary | primary
 * primary   -> NUMBER | STRING | 'winner' | 'chaka' | 'getch' '(' expr? ')' | IDENT ('(' args ')')? | '(' expr ')'
 */
public final class Parser {
    private final List<Token> toks;
    private int p = 0;

    public Parser(List<Token> tokens) { this.toks = tokens; }

    public Program parseProgram() {
        List<Func> funcs = new ArrayList<>();
        while (check(Type.FUNC)) funcs.add(funcDecl());
        if (!check(Type.MAIN))
            throw err("Expected 'bongga' (the main entry point), found " + describe(cur()));
        advance();
        Block main = block();
        if (!check(Type.EOF))
            throw err("Unexpected " + describe(cur()) + " after the end of 'bongga'");
        return new Program(funcs, main);
    }

    private Func funcDecl() {
        int line = advance().line(); // gawa
        String name = expect(Type.IDENT, "function name").text();
        expect(Type.LPAREN, "'('");
        List<String> params = new ArrayList<>();
        if (!check(Type.RPAREN)) {
            do { params.add(expect(Type.IDENT, "parameter name").text()); } while (match(Type.COMMA));
        }
        expect(Type.RPAREN, "')'");
        return new Func(name, params, block(), line);
    }

    private Block block() {
        expect(Type.LBRACE, "'{'");
        List<Stmt> list = new ArrayList<>();
        while (!check(Type.RBRACE)) {
            if (check(Type.EOF)) throw err("Missing '}' before end of file");
            list.add(statement());
        }
        advance();
        return new Block(list);
    }

    private Stmt statement() {
        Token t = cur();
        switch (t.type()) {
            case TYPE: {
                advance();
                String name = expect(Type.IDENT, "variable name").text();
                Expr init = match(Type.ASSIGN) ? expr() : null;
                expect(Type.SEMI, "';'");
                return new Decl(t.text(), name, init, t.line());
            }
            case PRINT: {
                advance();
                expect(Type.LPAREN, "'(' after chika");
                Expr e = expr();
                expect(Type.RPAREN, "')'");
                expect(Type.SEMI, "';'");
                return new Print(e, t.line());
            }
            case IF: return ifStmt();
            case WHILE: {
                advance();
                expect(Type.LPAREN, "'(' after gora");
                Expr c = expr();
                expect(Type.RPAREN, "')'");
                return new While(c, block(), t.line());
            }
            case TRY: {
                advance();
                Block body = block();
                expect(Type.CATCH, "'sagip' after the subukan block");
                expect(Type.LPAREN, "'(' after sagip");
                String en = expect(Type.IDENT, "error variable name").text();
                expect(Type.RPAREN, "')'");
                return new Try(body, en, block(), t.line());
            }
            case RETURN: {
                advance();
                Expr v = check(Type.SEMI) ? null : expr();
                expect(Type.SEMI, "';'");
                return new Return(v, t.line());
            }
            case LBRACE: return block();
            case IDENT: {
                if (peekType(1) == Type.ASSIGN) {
                    advance(); advance();
                    Expr v = expr();
                    expect(Type.SEMI, "';'");
                    return new Assign(t.text(), v, t.line());
                }
                Expr e = expr();
                expect(Type.SEMI, "';'");
                if (!(e instanceof Call) && !(e instanceof Input))
                    throw new GaychException("This expression does nothing on its own", t.line());
                return new ExprStmt(e, t.line());
            }
            case INPUT: {
                Expr e = expr();
                expect(Type.SEMI, "';'");
                return new ExprStmt(e, t.line());
            }
            default:
                throw err("Unexpected " + describe(t));
        }
    }

    private Stmt ifStmt() {
        int line = advance().line(); // keri
        expect(Type.LPAREN, "'(' after keri");
        Expr c = expr();
        expect(Type.RPAREN, "')'");
        Block then = block();
        Block other = null;
        if (match(Type.ELSE)) {
            if (check(Type.IF)) other = new Block(List.of(ifStmt())); // else-if chain
            else other = block();
        }
        return new If(c, then, other, line);
    }

    // ---------- expressions ----------
    private Expr expr() { return or(); }

    private Expr or() {
        Expr l = and();
        while (check(Type.OR)) { int ln = advance().line(); l = new Binary("||", l, and(), ln); }
        return l;
    }
    private Expr and() {
        Expr l = equality();
        while (check(Type.AND)) { int ln = advance().line(); l = new Binary("&&", l, equality(), ln); }
        return l;
    }
    private Expr equality() {
        Expr l = comparison();
        while (check(Type.EQ) || check(Type.NEQ)) { Token o = advance(); l = new Binary(o.text(), l, comparison(), o.line()); }
        return l;
    }
    private Expr comparison() {
        Expr l = term();
        while (check(Type.LT) || check(Type.LE) || check(Type.GT) || check(Type.GE)) {
            Token o = advance(); l = new Binary(o.text(), l, term(), o.line());
        }
        return l;
    }
    private Expr term() {
        Expr l = factor();
        while (check(Type.PLUS) || check(Type.MINUS)) { Token o = advance(); l = new Binary(o.text(), l, factor(), o.line()); }
        return l;
    }
    private Expr factor() {
        Expr l = unary();
        while (check(Type.STAR) || check(Type.SLASH) || check(Type.PERCENT)) {
            Token o = advance(); l = new Binary(o.text(), l, unary(), o.line());
        }
        return l;
    }
    private Expr unary() {
        if (check(Type.MINUS) || check(Type.NOT)) { Token o = advance(); return new Unary(o.text(), unary(), o.line()); }
        return primary();
    }

    private Expr primary() {
        Token t = advance();
        switch (t.type()) {
            case NUMBER: return new Num(Double.parseDouble(t.text()));
            case STRING: return new Str(t.text());
            case TRUE: return new Bool(true);
            case FALSE: return new Bool(false);
            case INPUT: {
                expect(Type.LPAREN, "'(' after getch");
                Expr prompt = check(Type.RPAREN) ? null : expr();
                expect(Type.RPAREN, "')'");
                return new Input(prompt, t.line());
            }
            case IDENT: {
                if (match(Type.LPAREN)) {
                    List<Expr> args = new ArrayList<>();
                    if (!check(Type.RPAREN)) { do { args.add(expr()); } while (match(Type.COMMA)); }
                    expect(Type.RPAREN, "')' after arguments");
                    return new Call(t.text(), args, t.line());
                }
                return new Var(t.text(), t.line());
            }
            case LPAREN: {
                Expr e = expr();
                expect(Type.RPAREN, "')'");
                return e;
            }
            default:
                p--; // so the error points at the offending token
                throw err("Expected an expression, found " + describe(t));
        }
    }

    // ---------- helpers ----------
    private Token cur() { return toks.get(p); }
    private Type peekType(int n) { return toks.get(Math.min(p + n, toks.size() - 1)).type(); }
    private boolean check(Type t) { return cur().type() == t; }
    private Token advance() { Token t = toks.get(p); if (t.type() != Type.EOF) p++; return t; }
    private boolean match(Type t) { if (check(t)) { advance(); return true; } return false; }
    private Token expect(Type t, String what) {
        if (check(t)) return advance();
        // a missing ';' or ')' is best reported on the line where it should have been
        int line = (p > 0 && toks.get(p - 1).line() < cur().line()) ? toks.get(p - 1).line() : cur().line();
        throw new GaychException("Expected " + what + ", found " + describe(cur()), line);
    }
    private GaychException err(String msg) { return new GaychException(msg, cur().line()); }
    private static String describe(Token t) {
        return t.type() == Type.EOF ? "end of file" : "'" + t.text() + "'";
    }
}
