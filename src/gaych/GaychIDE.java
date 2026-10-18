package gaych;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;

/** A small dark-themed IDE: editor with highlighting, Run/Stop, and an interactive console. */
public final class GaychIDE extends JFrame {

    private static final Color BG = new Color(0x1e1e2e), FG = new Color(0xcdd6f4), GUTTER = new Color(0x181825),
            GUTTER_FG = new Color(0x6c7086), TYPE = new Color(0xf783ac),
            LITERAL = new Color(0xffe066), STRING = new Color(0x8ce99a), COMMENT = new Color(0x6c7086),
            NUMBER = new Color(0xffa94d), ERROR = new Color(0xff6b6b), ACCENT = new Color(0xda77f2);

    /** Pride-flag colors, used for bars and buttons. */
    private static final Color[] FLAG = {new Color(0xE40303), new Color(0xFF8C00), new Color(0xFFED00),
            new Color(0x008026), new Color(0x004DFF), new Color(0x750787)};
    /** Brighter versions of the same six colors so text stays readable on the dark background. */
    private static final Color[] BRIGHT = {new Color(0xFF6B6B), new Color(0xFFA94D), new Color(0xFFE066),
            new Color(0x69DB7C), new Color(0x4DABF7), new Color(0xDA77F2)};
    private static final java.util.List<String> KEYWORD_ORDER = java.util.List.of(
            "bongga", "gawa", "keri", "ewan", "gora", "subukan", "sagip", "uwi", "getch", "chika");

    private static final String[][] EXAMPLES = {
            {"Hello World", "hello"}, {"Input / Output", "greeter"}, {"Countdown (from the slides)", "countdown"},
            {"Functions & recursion", "functions"}, {"Calculator", "calculator"}, {"Error handling", "errors"},
            {"Full vocabulary", "bongga"}};

    private static final String STARTER = """
            // Welcome to GaychLang!
            // getch = input, chika = output, bongga = main
            bongga {
                eme name = getch("Ano pangalan mo? ");
                chika("Hello, " + name + "!");
            }
            """;

    private final JTextPane editor = new JTextPane();
    private final JTextPane console = new JTextPane();
    private final JTextField inputField = new JTextField();
    private final JLabel status = new JLabel(" Ready");
    private final JButton runBtn = new PillButton("▶ Run", FLAG);
    private final JButton stopBtn = new PillButton("■ Stop", new Color(0xe03131), new Color(0xc2255c));
    private final LinkedBlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private volatile Thread worker;
    private volatile boolean waitingForInput;
    private Path currentFile;
    private boolean dirty;

    private final SimpleAttributeSet defaultAttr = attr(FG, false);
    private final SimpleAttributeSet[] rainbowAttrs = rainbowAttrSets();
    private final SimpleAttributeSet typeAttr = attr(TYPE, true);
    private final SimpleAttributeSet literalAttr = attr(LITERAL, false);
    private final SimpleAttributeSet stringAttr = attr(STRING, false);
    private final SimpleAttributeSet commentAttr = attr(COMMENT, false);
    private final SimpleAttributeSet numberAttr = attr(NUMBER, false);

    private static final Pattern TOKENS = Pattern.compile(
            "(?<comment>//[^\\n]*|/\\*[\\s\\S]*?\\*/)"
                    + "|(?<string>\"(?:\\\\.|[^\"\\\\\\n])*\")"
                    + "|(?<keyword>\\b(?:bongga|gawa|keri|ewan|gora|subukan|sagip|uwi|getch|chika|pili|kaso|iba)\\b)"
                    + "|(?<type>\\b(?:chuchu|eme|charot)\\b)"
                    + "|(?<literal>\\b(?:winner|chaka)\\b)"
                    + "|(?<number>\\b\\d+(?:\\.\\d+)?\\b)");

    private static SimpleAttributeSet[] rainbowAttrSets() {
        SimpleAttributeSet[] a = new SimpleAttributeSet[BRIGHT.length];
        for (int i = 0; i < a.length; i++) a[i] = attr(BRIGHT[i], true);
        return a;
    }

    /** Every keyword gets its own rainbow color. */
    private SimpleAttributeSet keywordAttr(String word) {
        int i = Math.max(0, KEYWORD_ORDER.indexOf(word));
        return rainbowAttrs[i % rainbowAttrs.length];
    }

    private static SimpleAttributeSet attr(Color c, boolean bold) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, c);
        StyleConstants.setBold(a, bold);
        return a;
    }

    public static void launch(Path file) {
        SwingUtilities.invokeLater(() -> {
            applyTheme();
            GaychIDE ide = new GaychIDE();
            ide.setVisible(true);
            if (file != null) ide.openPath(file);
        });
    }

    private static void applyTheme() {
        for (String k : new String[]{"MenuBar.background", "Menu.background", "MenuItem.background", "PopupMenu.background"})
            UIManager.put(k, GUTTER);
        for (String k : new String[]{"MenuBar.foreground", "Menu.foreground", "MenuItem.foreground"})
            UIManager.put(k, FG);
        UIManager.put("MenuItem.acceleratorForeground", GUTTER_FG);
        UIManager.put("Menu.selectionBackground", new Color(0x45475a));
        UIManager.put("MenuItem.selectionBackground", new Color(0x45475a));
        UIManager.put("Menu.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(new Color(0x45475a)));
    }

    private static Image rainbowIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, 64, 64, 18, 18));
        for (int i = 0; i < FLAG.length; i++) {
            g.setColor(FLAG[i]);
            g.fillRect(0, i * 64 / FLAG.length, 64, 64 / FLAG.length + 1);
        }
        g.dispose();
        return img;
    }

    private GaychIDE() {
        super("GaychLang IDE");
        setIconImage(rainbowIcon());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1000, 720);
        setLocationRelativeTo(null);
        buildUi();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { if (confirmDiscard()) { stopProgram(); dispose(); System.exit(0); } }
        });
        setText(STARTER);
        dirty = false;
        updateTitle();
    }

    // ------------------------------------------------------------------ UI
    private void buildUi() {
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 15);

        editor.setFont(mono);
        editor.setBackground(BG);
        editor.setForeground(FG);
        editor.setCaretColor(Color.WHITE);
        editor.setSelectionColor(new Color(0x585b70));
        editor.setMargin(new Insets(4, 8, 4, 8));
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { /* attribute changes only */ }
        });
        // Tab key inserts 4 spaces
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "gaych-tab");
        editor.getActionMap().put("gaych-tab", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { editor.replaceSelection("    "); }
        });

        // editor without line wrapping + line numbers
        JPanel noWrap = new JPanel(new BorderLayout());
        noWrap.add(editor);
        JScrollPane editorScroll = new JScrollPane(noWrap);
        editorScroll.setRowHeaderView(new LineNumbers());
        editorScroll.getViewport().setBackground(BG);
        editorScroll.setBorder(BorderFactory.createEmptyBorder());
        editorScroll.getVerticalScrollBar().setUnitIncrement(16);

        console.setEditable(false);
        console.setFont(mono);
        console.setBackground(new Color(0x11111b));
        console.setForeground(FG);
        console.setMargin(new Insets(4, 8, 4, 8));
        console.setSelectionColor(new Color(0x585b70));
        JScrollPane consoleScroll = new JScrollPane(console);
        consoleScroll.setBorder(BorderFactory.createEmptyBorder());

        inputField.setFont(mono);
        inputField.setBackground(new Color(0x313244));
        inputField.setForeground(FG);
        inputField.setCaretColor(ACCENT);
        inputField.setEnabled(false);
        inputField.addActionListener(e -> submitInput());
        JLabel arrow = new JLabel(" input ▸ ");
        arrow.setForeground(ACCENT);
        arrow.setFont(mono);
        JPanel inputRow = new JPanel(new BorderLayout());
        inputRow.setBackground(new Color(0x313244));
        inputRow.add(arrow, BorderLayout.WEST);
        inputRow.add(inputField, BorderLayout.CENTER);

        JPanel consolePanel = new JPanel(new BorderLayout());
        JPanel consoleTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        consoleTitle.setBackground(GUTTER);
        consoleTitle.add(new RainbowLabel("CONSOLE", 12));
        consolePanel.add(consoleTitle, BorderLayout.NORTH);
        consolePanel.add(consoleScroll, BorderLayout.CENTER);
        consolePanel.add(inputRow, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScroll, consolePanel);
        split.setResizeWeight(0.65);
        split.setBorder(BorderFactory.createEmptyBorder());

        runBtn.addActionListener(e -> runProgram());
        stopBtn.addActionListener(e -> stopProgram());
        stopBtn.setEnabled(false);
        JButton clearBtn = new PillButton("Clear", new Color(0x585b70), new Color(0x45475a));
        clearBtn.addActionListener(e -> console.setText(""));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        row.setBackground(GUTTER);
        row.add(new RainbowLabel("GaychLang IDE", 22));
        row.add(Box.createHorizontalStrut(14));
        row.add(runBtn);
        row.add(stopBtn);
        row.add(clearBtn);
        JPanel header = new JPanel(new BorderLayout());
        header.add(new RainbowBar(6), BorderLayout.NORTH);
        header.add(row, BorderLayout.CENTER);

        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        status.setOpaque(true);
        status.setBackground(GUTTER);
        status.setForeground(FG);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(status, BorderLayout.CENTER);
        footer.add(new RainbowBar(4), BorderLayout.SOUTH);

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        setJMenuBar(buildMenu());
    }

    private JMenuBar buildMenu() {
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JMenuBar mb = new JMenuBar();

        JMenu file = new JMenu("File");
        file.add(item("New", KeyStroke.getKeyStroke(KeyEvent.VK_N, mask), this::newFile));
        file.add(item("Open...", KeyStroke.getKeyStroke(KeyEvent.VK_O, mask), this::openDialog));
        file.add(item("Save", KeyStroke.getKeyStroke(KeyEvent.VK_S, mask), () -> save(false)));
        file.add(item("Save As...", KeyStroke.getKeyStroke(KeyEvent.VK_S, mask | InputEvent.SHIFT_DOWN_MASK), () -> save(true)));
        file.addSeparator();
        file.add(item("Quit", KeyStroke.getKeyStroke(KeyEvent.VK_Q, mask),
                () -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))));

        JMenu run = new JMenu("Run");
        run.add(item("Run", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), this::runProgram));
        run.add(item("Stop", KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK), this::stopProgram));
        run.add(item("Check syntax", KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), this::checkOnly));

        JMenu ex = new JMenu("Examples");
        for (String[] e : EXAMPLES) ex.add(item(e[0], null, () -> loadExample(e[1])));

        JMenu help = new JMenu("Help");
        help.add(item("Language cheat sheet", null, this::showCheatSheet));
        help.add(item("About", null, () -> JOptionPane.showMessageDialog(this,
                "GaychLang " + Gaych.VERSION + "\ngetch = input · chika = output · bongga = main\nBuilt in Java.",
                "About GaychLang", JOptionPane.INFORMATION_MESSAGE)));

        mb.add(file); mb.add(run); mb.add(ex); mb.add(help);
        return mb;
    }

    private static JMenuItem item(String text, KeyStroke ks, Runnable r) {
        JMenuItem mi = new JMenuItem(text);
        if (ks != null) mi.setAccelerator(ks);
        mi.addActionListener(e -> r.run());
        return mi;
    }

    /** Smooth pride-flag gradient strip. */
    private static final class RainbowBar extends JComponent {
        private final int height;
        RainbowBar(int height) { this.height = height; }
        @Override public Dimension getPreferredSize() { return new Dimension(10, height); }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setPaint(gradient(getWidth()));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }

    private static Paint gradient(int width) {
        float[] f = new float[FLAG.length];
        for (int i = 0; i < f.length; i++) f[i] = i / (float) (f.length - 1);
        return new LinearGradientPaint(0, 0, Math.max(1, width), 0, f, FLAG);
    }

    /** Text with every letter in the next rainbow color. */
    private static final class RainbowLabel extends JComponent {
        private final String text;
        RainbowLabel(String text, float size) {
            this.text = text;
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.round(size)));
        }
        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + 4, fm.getHeight() + 2);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int x = 2, ci = 0;
            for (char ch : text.toCharArray()) {
                g.setColor(BRIGHT[ci % BRIGHT.length]);
                g.drawString(String.valueOf(ch), x, fm.getAscent() + 1);
                x += fm.charWidth(ch);
                if (!Character.isWhitespace(ch)) ci++;
            }
            g.dispose();
        }
    }

    /** Rounded, gradient-filled button. */
    private static final class PillButton extends JButton {
        private final Color[] colors;
        PillButton(String text, Color... colors) {
            super(text);
            this.colors = colors;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setFont(getFont().deriveFont(Font.BOLD, 14f));
            setBorder(BorderFactory.createEmptyBorder(7, 18, 7, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (!isEnabled()) g.setComposite(AlphaComposite.SrcOver.derive(0.3f));
            if (colors.length == 1) g.setColor(colors[0]);
            else {
                float[] f = new float[colors.length];
                for (int i = 0; i < f.length; i++) f[i] = i / (float) (f.length - 1);
                g.setPaint(new LinearGradientPaint(0, 0, Math.max(1, w), 0, f, colors));
            }
            g.fillRoundRect(0, 0, w, h, h, h);
            if (isEnabled() && getModel().isPressed()) { g.setColor(new Color(0, 0, 0, 80)); g.fillRoundRect(0, 0, w, h, h, h); }
            else if (isEnabled() && getModel().isRollover()) { g.setColor(new Color(255, 255, 255, 50)); g.fillRoundRect(0, 0, w, h, h, h); }
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int x = (w - fm.stringWidth(getText())) / 2, y = (h + fm.getAscent() - fm.getDescent()) / 2;
            g.setColor(new Color(0, 0, 0, 140));
            g.drawString(getText(), x + 1, y + 1);
            g.setColor(Color.WHITE);
            g.drawString(getText(), x, y);
            g.dispose();
        }
    }

    /** Line-number gutter. */
    private final class LineNumbers extends JComponent {
        LineNumbers() {
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            setBackground(GUTTER);
            setOpaque(true);
            editor.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { revalidate(); repaint(); }
                public void removeUpdate(DocumentEvent e) { revalidate(); repaint(); }
                public void changedUpdate(DocumentEvent e) { }
            });
        }
        @Override public Dimension getPreferredSize() {
            int lines = editor.getDocument().getDefaultRootElement().getElementCount();
            int digits = Math.max(3, String.valueOf(lines).length());
            return new Dimension(getFontMetrics(getFont()).charWidth('0') * digits + 16, editor.getHeight());
        }
        @Override protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(getFont());
            g.setColor(GUTTER_FG);
            Element root = editor.getDocument().getDefaultRootElement();
            Rectangle clip = g.getClipBounds();
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < root.getElementCount(); i++) {
                try {
                    Rectangle r = editor.modelToView2D(root.getElement(i).getStartOffset()).getBounds();
                    if (r.y + r.height < clip.y) continue;
                    if (r.y > clip.y + clip.height) break;
                    g.setColor(BRIGHT[i % BRIGHT.length]);
                    String n = String.valueOf(i + 1);
                    g.drawString(n, getWidth() - 8 - fm.stringWidth(n), r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
                } catch (BadLocationException ignored) { }
            }
        }
    }

    // ------------------------------------------------------------------ editing / highlighting
    private void changed() {
        if (!dirty) { dirty = true; updateTitle(); }
        SwingUtilities.invokeLater(this::highlight);
    }

    private void highlight() {
        StyledDocument doc = editor.getStyledDocument();
        try {
            String text = doc.getText(0, doc.getLength());
            doc.setCharacterAttributes(0, text.length(), defaultAttr, true);
            Matcher m = TOKENS.matcher(text);
            while (m.find()) {
                SimpleAttributeSet a = m.group("comment") != null ? commentAttr
                        : m.group("string") != null ? stringAttr
                        : m.group("keyword") != null ? keywordAttr(m.group("keyword"))
                        : m.group("type") != null ? typeAttr
                        : m.group("literal") != null ? literalAttr : numberAttr;
                doc.setCharacterAttributes(m.start(), m.end() - m.start(), a, false);
            }
        } catch (BadLocationException ignored) { }
    }

    private void setText(String text) {
        editor.setText(text);
        editor.setCaretPosition(0);
        highlight();
    }

    private void updateTitle() {
        setTitle((dirty ? "*" : "") + (currentFile == null ? "untitled.gaych" : currentFile.getFileName()) + " - GaychLang IDE");
    }

    // ------------------------------------------------------------------ files
    private boolean confirmDiscard() {
        if (!dirty) return true;
        int r = JOptionPane.showConfirmDialog(this, "You have unsaved changes. Save them first?", "Unsaved changes",
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (r == JOptionPane.CANCEL_OPTION || r == JOptionPane.CLOSED_OPTION) return false;
        if (r == JOptionPane.YES_OPTION) return save(false);
        return true;
    }

    private void newFile() {
        if (!confirmDiscard()) return;
        currentFile = null;
        setText("bongga {\n    \n}\n");
        dirty = false;
        updateTitle();
    }

    private void openDialog() {
        if (!confirmDiscard()) return;
        JFileChooser fc = chooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) openPath(fc.getSelectedFile().toPath());
    }

    private void openPath(Path p) {
        try {
            setText(Files.readString(p, StandardCharsets.UTF_8));
            currentFile = p;
            dirty = false;
            updateTitle();
            setStatus("Opened " + p);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not open file:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean save(boolean as) {
        if (currentFile == null || as) {
            JFileChooser fc = chooser();
            if (currentFile != null) fc.setSelectedFile(currentFile.toFile());
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return false;
            Path p = fc.getSelectedFile().toPath();
            if (!p.getFileName().toString().contains(".")) p = p.resolveSibling(p.getFileName() + ".gaych");
            currentFile = p;
        }
        try {
            Files.writeString(currentFile, editor.getText(), StandardCharsets.UTF_8);
            dirty = false;
            updateTitle();
            setStatus("Saved " + currentFile);
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save file:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private JFileChooser chooser() {
        JFileChooser fc = new JFileChooser(currentFile != null ? currentFile.getParent().toFile() : new java.io.File("."));
        fc.setFileFilter(new FileNameExtensionFilter("GaychLang programs (*.gaych)", "gaych"));
        return fc;
    }

    private void loadExample(String name) {
        if (!confirmDiscard()) return;
        String src = null;
        try (InputStream in = GaychIDE.class.getResourceAsStream("/examples/" + name + ".gaych")) {
            if (in != null) src = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ignored) { }
        if (src == null) {
            try { src = Files.readString(Path.of("examples", name + ".gaych")); } catch (IOException ignored) { }
        }
        if (src == null) { setStatus("Example '" + name + "' not found"); return; }
        currentFile = null;
        setText(src);
        dirty = false;
        updateTitle();
    }

    private void showCheatSheet() {
        String s = """
                bongga { ... }          main entry point
                gawa add(a, b) { uwi a+b; }   function (define before bongga)
                chuchu x = 5;               number      eme s = "hi";   text
                charot ok = winner;           boolean (winner = true, chaka = false)
                x = x + 1;                  assignment
                chika(x);                   output (print a line)
                eme n = getch("Name? ");  input (always returns eme)
                keri (c) { } ewan { }      if / else   (ewan keri ... also works)
                gora (c) { }              while loop
                subukan { } sagip(e) { }    try / catch
                uwi value;                return
                + - * / %   < <= > >= == !=   && || !
                // comment      /* block comment */
                """;
        JTextArea ta = new JTextArea(s);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        ta.setEditable(false);
        JOptionPane.showMessageDialog(this, ta, "GaychLang cheat sheet", JOptionPane.PLAIN_MESSAGE);
    }

    // ------------------------------------------------------------------ running
    private void setStatus(String s) { status.setText(" " + s); }

    private void print(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument d = console.getStyledDocument();
            try { d.insertString(d.getLength(), text, attr(color, false)); } catch (BadLocationException ignored) { }
            console.setCaretPosition(d.getLength());
        });
    }

    private void checkOnly() {
        try {
            Gaych.compile(editor.getText());
            setStatus("No errors found ✔");
        } catch (GaychException e) {
            print("GaychLang error: " + e.getMessage() + "\n", ERROR);
            setStatus("Error: " + e.getMessage());
        }
    }

    private void runProgram() {
        if (worker != null) return;
        console.setText("");
        final String src = editor.getText();
        runBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        setStatus("Running...");
        inputQueue.clear();

        GaychIO io = new GaychIO() {
            @Override public void println(String text) { print(text + "\n", FG); }
            @Override public String readLine(String prompt) throws InterruptedException {
                if (!prompt.isEmpty()) print(prompt, ACCENT);
                waitingForInput = true;
                SwingUtilities.invokeLater(() -> { inputField.setEnabled(true); inputField.requestFocusInWindow(); });
                try {
                    return inputQueue.take();
                } finally {
                    waitingForInput = false;
                    SwingUtilities.invokeLater(() -> inputField.setEnabled(false));
                }
            }
        };

        worker = new Thread(null, () -> {
            String result = "Finished";
            try {
                Gaych.run(src, io);
            } catch (GaychException e) {
                print("GaychLang error: " + e.getMessage() + "\n", ERROR);
                result = "Error: " + e.getMessage();
            } catch (Interpreter.Stopped e) {
                print("\n[stopped]\n", COMMENT);
                result = "Stopped";
            } catch (RuntimeException | StackOverflowError e) {
                print("Internal error: " + e + "\n", ERROR);
                result = "Internal error";
            }
            final String status = result;
            SwingUtilities.invokeLater(() -> {
                worker = null;
                runBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                inputField.setEnabled(false);
                setStatus(status);
            });
        }, "gaych-run", 128L * 1024 * 1024);
        worker.setDaemon(true);
        worker.start();
    }

    private void stopProgram() {
        Thread t = worker;
        if (t != null) t.interrupt();
    }

    private void submitInput() {
        if (!waitingForInput) return;
        String text = inputField.getText();
        inputField.setText("");
        print(text + "\n", STRING);
        inputQueue.offer(text);
    }
}
