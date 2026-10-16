# GaychLang reference

## Program shape
Functions (optional) come first, then the entry point:

```gaych
gawa greet(who) { chika("Hi " + who); }

bongga {
    greet("world");
}
```

## Keywords
| GaychLang | Meaning |
|---|---|
| `bongga` | main entry point |
| `getch(prompt)` | read a line of input (returns `eme`; prompt optional) |
| `chika(x)` | print `x` followed by a newline |
| `gawa` | define a function |
| `chuchu` / `eme` / `charot` | number / text / boolean variable |
| `winner` / `chaka` | true / false |
| `keri` / `ewan` | if / else (`ewan keri` chains) |
| `gora` | while loop |
| `subukan` / `sagip(e)` | try / catch |
| `uwi` | return (inside `bongga` it ends the program) |

## Types and conversion
- `chuchu x = getch("n? ");` converts the typed text to a number (error if invalid).
- `eme` converts any value to text. `charot` accepts `winner`/`chaka`.
- Function parameters are untyped.
- `+` joins text if either side is text; otherwise it adds numbers.
- Conditions must be `charot` values.

## Operators (low → high precedence)
`||`, `&&`, `== !=`, `< <= > >=`, `+ -`, `* / %`, unary `- !`

## Errors
Syntax errors, undeclared names and wrong argument counts are found before the
program runs. Runtime errors (division by zero, bad conversion, stack overflow)
can be caught with `subukan { } sagip(e) { }`; `e` holds the message.

## Grammar
```
program   → funcDecl* 'bongga' block
funcDecl  → 'gawa' IDENT '(' params? ')' block
stmt      → decl | assign | print | call ';' | ifStmt | whileStmt | tryStmt | return | block
decl      → TYPE IDENT ('=' expr)? ';'
print     → 'chika' '(' expr ')' ';'
ifStmt    → 'keri' '(' expr ')' block ('ewan' (ifStmt | block))?
whileStmt → 'gora' '(' expr ')' block
tryStmt   → 'subukan' block 'sagip' '(' IDENT ')' block
return    → 'uwi' expr? ';'
```
