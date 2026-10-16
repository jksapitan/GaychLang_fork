# GaychLang

A tiny programming language written in Java, built with the same idea as the
*PokéLang* slides: **syntax is a skin, semantics is the engine.**

| Concept | GaychLang |
|---|---|
| input | `getch(...)` |
| output | `chika(...)` |
| main | `bongga { ... }` |

```gaych
bongga {
    eme name = getch("Ano pangalan mo? ");
    chika("Hello, " + name + "!");
}
```

Run it in the **terminal** or in the built-in **rainbow IDE** (pride-gradient header, a different color for every keyword, rainbow line numbers, and an interactive console).

## Download & run

Grab `GaychLang.jar` from the **Releases** page (you only need Java 17+ installed), then:

```bash
java -jar GaychLang.jar                    # opens the IDE
java -jar GaychLang.jar examples/hello.gaych   # runs in the terminal
java -jar GaychLang.jar --check file.gaych     # syntax/semantic check only
```

Files use the `.gaych` extension.

## Build from source

Needs a JDK 17+ (`javac` and `jar`).

```bash
./build.sh        # Linux / macOS   (Windows: build.bat)
./gaych examples/countdown.gaych      # launcher (Windows: gaych.bat)
```

### Using VS Code

1. Install the **Extension Pack for Java**.
2. Open this folder. `Ctrl+Shift+B` builds the jar.
3. `Terminal > Run Task...` → **Run current .gaych file** or **Open GaychLang IDE**.
4. Optional syntax highlighting for `.gaych` files: copy `vscode-extension/` to
   `~/.vscode/extensions/gaychlang` (Windows: `%USERPROFILE%\.vscode\extensions\gaychlang`) and restart VS Code.
5. `F5` runs/debugs the Java sources directly (see `.vscode/launch.json`).

## How it works

```
source → Lexer → Parser (AST) → SemanticChecker → Interpreter
```

Only `Lexer.java` knows the GaychLang words (see the `KEYWORDS` map). Everything
after it works with generic token types and AST nodes, so you can re-skin the
language by editing that one table. See [docs/LANGUAGE.md](docs/LANGUAGE.md) for the full reference.

## Publishing to GitHub

```bash
git init
git add .
git commit -m "Initial commit: GaychLang"
git branch -M main
git remote add origin https://github.com/<you>/GaychLang.git
git push -u origin main

# make a downloadable release (GitHub Actions builds and attaches the jar):
git tag v1.0.0
git push origin v1.0.0
```

## License

MIT
