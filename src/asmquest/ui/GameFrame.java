package asmquest.ui;

import asmquest.model.Level;
import asmquest.model.LevelCatalog;
import asmquest.vm.AsmException;
import asmquest.vm.MachineState;
import asmquest.vm.VirtualMachine;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GameFrame extends JFrame {
    private static final Color BACKGROUND = new Color(9, 16, 29);
    private static final Color PANEL = new Color(14, 23, 38);
    private static final Color BORDER = new Color(36, 51, 68);
    private static final Color TEXT = new Color(222, 234, 240);
    private static final Color MUTED = new Color(126, 148, 165);
    private static final Color GREEN = new Color(118, 230, 165);

    private final List<Level> levels = LevelCatalog.all();
    private final Set<Integer> completed = new HashSet<>();
    private final CodeEditor codeEditor = new CodeEditor();
    private final VisualizerPanel visualizer = new VisualizerPanel();
    private final JEditorPane taskViewer = new JEditorPane();
    private final JTextArea statusArea = new JTextArea();
    private final JComboBox<Level> levelCombo = new JComboBox<>();
    private final JLabel progressLabel = new JLabel();
    private final JButton runButton = button("▶  Запустить", new Color(55, 151, 105), TEXT);
    private final JButton stepButton = button("→  Шаг", new Color(29, 46, 63), TEXT);
    private final JButton resetButton = button("↺  Сброс", new Color(29, 46, 63), TEXT);
    private final JButton previousButton = button("←", new Color(23, 36, 51), TEXT);
    private final JButton nextButton = button("→", new Color(23, 36, 51), TEXT);
    private final Timer runTimer;

    private int currentLevel;
    private VirtualMachine.Session session;
    private boolean programDirty = true;
    private boolean synchronizing;
    private boolean selectingLevel;

    public GameFrame() {
        super("ASM Pond — игра об ассемблере");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1360, 860);
        setMinimumSize(new Dimension(1080, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);

        configureLevelCombo();
        configureTaskViewer();
        configureStatusArea();
        setLayout(new BorderLayout());
        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);

        runTimer = new Timer(380, event -> executeOne());
        runTimer.setCoalesce(true);

        runButton.addActionListener(event -> toggleRun());
        stepButton.addActionListener(event -> {
            stopRun();
            executeOne();
        });
        resetButton.addActionListener(event -> resetProgram());
        previousButton.addActionListener(event -> loadLevel(currentLevel - 1));
        nextButton.addActionListener(event -> loadLevel(currentLevel + 1));

        codeEditor.setChangeListener(this::onCodeChanged);
        installShortcuts();
        loadLevel(0);
        SwingUtilities.invokeLater(() -> ((JSplitPane) ((JSplitPane) getContentPane().getComponent(1))
                .getLeftComponent()).setDividerLocation(330));
    }

    private JComponent createHeader() {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(new Color(11, 19, 32));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(12, 22, 12, 22)));
        header.setPreferredSize(new Dimension(100, 76));

        JPanel brand = new JPanel(new GridLayout(2, 1));
        brand.setOpaque(false);
        JLabel logo = new JLabel("<html><font color='#e7f2f5'>ASM</font>"
                + "<font color='#76e6a5'>//</font><font color='#e7f2f5'>POND</font></html>");
        logo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 23));
        brand.add(logo);
        JLabel subtitle = new JLabel("игра об ассемблере");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        subtitle.setForeground(MUTED);
        brand.add(subtitle);
        header.add(brand, BorderLayout.WEST);

        JPanel levelControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        levelControls.setOpaque(false);
        levelCombo.setPreferredSize(new Dimension(280, 36));
        levelControls.add(levelCombo);
        progressLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        progressLabel.setForeground(MUTED);
        levelControls.add(progressLabel);
        header.add(levelControls, BorderLayout.CENTER);

        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 8));
        navigation.setOpaque(false);
        previousButton.setPreferredSize(new Dimension(42, 36));
        nextButton.setPreferredSize(new Dimension(42, 36));
        previousButton.setToolTipText("Предыдущий уровень");
        nextButton.setToolTipText("Следующий уровень");
        navigation.add(previousButton);
        navigation.add(nextButton);
        header.add(navigation, BorderLayout.EAST);
        return header;
    }

    private JComponent createMainContent() {
        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createTaskPanel(), createEditorPanel());
        vertical.setResizeWeight(0.45);
        vertical.setDividerSize(5);
        vertical.setBorder(null);
        vertical.setBackground(BORDER);
        vertical.setContinuousLayout(true);

        JSplitPane horizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, vertical, visualizer);
        horizontal.setResizeWeight(0.39);
        horizontal.setDividerLocation(500);
        horizontal.setDividerSize(6);
        horizontal.setBorder(null);
        horizontal.setBackground(BORDER);
        horizontal.setContinuousLayout(true);
        return horizontal;
    }

    private JComponent createTaskPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL);
        panel.setMinimumSize(new Dimension(360, 230));
        JScrollPane scroll = new JScrollPane(taskViewer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(PANEL);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL);
        panel.setMinimumSize(new Dimension(360, 300));

        JPanel editorHeader = new JPanel(new BorderLayout());
        editorHeader.setBackground(new Color(15, 25, 40));
        editorHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(8, 16, 8, 12)));
        JLabel file = new JLabel("program.asm");
        file.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        file.setForeground(new Color(148, 178, 193));
        editorHeader.add(file, BorderLayout.WEST);
        JButton hintButton = button("?  Подсказка", new Color(28, 43, 58), new Color(244, 194, 105));
        hintButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        hintButton.addActionListener(event -> JOptionPane.showMessageDialog(this,
                levels.get(currentLevel).hint(), "Подсказка · уровень " + (currentLevel + 1),
                JOptionPane.INFORMATION_MESSAGE));
        editorHeader.add(hintButton, BorderLayout.EAST);
        panel.add(editorHeader, BorderLayout.NORTH);
        panel.add(codeEditor, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(new Color(12, 21, 35));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        controls.setOpaque(false);
        runButton.setToolTipText("F5 — выполнить всю программу");
        stepButton.setToolTipText("F6 — выполнить одну команду");
        resetButton.setToolTipText("Ctrl+R — вернуть процессор в исходное состояние");
        controls.add(runButton);
        controls.add(stepButton);
        controls.add(resetButton);
        bottom.add(controls, BorderLayout.NORTH);
        bottom.add(statusArea, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void configureLevelCombo() {
        for (Level level : levels) {
            levelCombo.addItem(level);
        }
        levelCombo.setBackground(new Color(20, 34, 49));
        levelCombo.setForeground(TEXT);
        levelCombo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        levelCombo.setFocusable(false);
        levelCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Level) {
                    setText(String.format("%02d  ·  %s%s", index + 1, ((Level) value).title(),
                            completed.contains(index) ? "   ✓" : ""));
                }
                setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                return this;
            }
        });
        levelCombo.addActionListener(event -> {
            if (!selectingLevel && levelCombo.getSelectedIndex() != currentLevel) {
                loadLevel(levelCombo.getSelectedIndex());
            }
        });
    }

    private void configureTaskViewer() {
        taskViewer.setContentType("text/html");
        taskViewer.setEditable(false);
        taskViewer.setBackground(PANEL);
        taskViewer.setForeground(TEXT);
        taskViewer.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        taskViewer.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    }

    private void configureStatusArea() {
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setRows(2);
        statusArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        statusArea.setBackground(new Color(12, 21, 35));
        statusArea.setForeground(MUTED);
        statusArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(8, 16, 9, 16)));
    }

    private void loadLevel(int index) {
        if (index < 0 || index >= levels.size()) {
            return;
        }
        stopRun();
        currentLevel = index;
        Level level = levels.get(index);
        selectingLevel = true;
        levelCombo.setSelectedIndex(index);
        selectingLevel = false;
        taskViewer.setText(taskHtml(level, index));
        taskViewer.setCaretPosition(0);
        synchronizing = true;
        codeEditor.setText(level.starterCode());
        synchronizing = false;
        session = null;
        programDirty = true;
        visualizer.reset(level, initialState());
        codeEditor.highlightLine(-1);
        setStatus("Измени программу и нажми «Запустить» или F6 для пошагового выполнения.", MUTED);
        previousButton.setEnabled(index > 0);
        nextButton.setEnabled(index < levels.size() - 1);
    }

    private String taskHtml(Level level, int index) {
        return "<html><body style='font-family:sans-serif; background:#0e1726; color:#deeaf0; "
                + "padding:18px 22px 20px 22px; margin:0;'>"
                + "<div style='font-size:10px; color:#76e6a5; letter-spacing:1px;'>УРОВЕНЬ "
                + String.format("%02d", index + 1) + "  /  " + String.format("%02d", levels.size()) + "</div>"
                + "<h1 style='font-size:24px; margin:9px 0 4px 0; color:#eef6f7;'>" + level.title() + "</h1>"
                + "<div style='font-size:11px; color:#8fb3c2; padding-bottom:14px;'>" + level.concept() + "</div>"
                + "<div style='border-top:1px solid #243344; padding-top:13px; font-size:13px; line-height:150%; color:#adc1cd;'>"
                + level.theory() + "</div>"
                + "<div style='margin-top:16px; padding:12px; background:#142b35; color:#e1f3ea; "
                + "border-left:3px solid #76e6a5; font-size:13px; line-height:150%;'><b>ЗАДАЧА</b><br/>"
                + level.task() + "</div>"
                + "<div style='margin-top:14px; font-size:11px; color:#6f899c;'>Комментарий начинается с ; &nbsp;·&nbsp; "
                + "Регистры R0–R3 &nbsp;·&nbsp; Память [0]–[7]</div>"
                + "</body></html>";
    }

    private void onCodeChanged() {
        if (synchronizing) {
            return;
        }
        stopRun();
        session = null;
        programDirty = true;
        visualizer.reset(levels.get(currentLevel), initialState());
        codeEditor.highlightLine(-1);
        setStatus("Код изменён. Процессор готов к новому запуску.", MUTED);
    }

    private void toggleRun() {
        if (runTimer.isRunning()) {
            stopRun();
            setStatus("Пауза. Можно продолжить запуск или сделать один шаг.", new Color(244, 194, 105));
            return;
        }
        try {
            if (session != null && session.isFinished() && !levels.get(currentLevel).isSolved(session.state())) {
                session = null;
                programDirty = true;
            }
            ensureSession();
            if (session.isFinished()) {
                setStatus("Программа уже завершена. Нажми «Сброс», чтобы начать заново.", MUTED);
                return;
            }
            runButton.setText("Ⅱ  Пауза");
            runTimer.start();
        } catch (AsmException exception) {
            showError(exception);
        }
    }

    private void executeOne() {
        try {
            ensureSession();
            if (session.isFinished()) {
                stopRun();
                return;
            }
            if (session.state().steps() >= 500) {
                stopRun();
                setStatus("Остановлено после 500 шагов. Проверь, не зациклилась ли программа.",
                        new Color(255, 151, 128));
                return;
            }
            MachineState state = session.step();
            visualizer.setState(state);
            codeEditor.highlightLine(state.lastLine());
            Level level = levels.get(currentLevel);
            if (level.isSolved(state)) {
                stopRun();
                completed.add(currentLevel);
                visualizer.setCompleted(true);
                String quality = state.steps() <= level.par() ? "Отличное решение!" : "Цель достигнута!";
                setStatus(quality + " Выполнено шагов: " + state.steps() + ". "
                        + (currentLevel == levels.size() - 1 ? "Все уровни пройдены — ты хранитель пруда!"
                        : "Можно переходить к следующему уровню."), GREEN);
                updateProgress();
            } else if (session.isFinished()) {
                stopRun();
                setStatus("Программа завершена, но цель ещё не достигнута. Проверь регистры и память.",
                        new Color(244, 194, 105));
            }
        } catch (AsmException exception) {
            showError(exception);
        }
    }

    private void ensureSession() throws AsmException {
        if (session == null || programDirty) {
            session = VirtualMachine.compile(codeEditor.getText(), initialState());
            programDirty = false;
            visualizer.reset(levels.get(currentLevel), session.state());
            codeEditor.highlightLine(-1);
        }
    }

    private void resetProgram() {
        stopRun();
        session = null;
        programDirty = true;
        visualizer.reset(levels.get(currentLevel), initialState());
        codeEditor.highlightLine(-1);
        setStatus("Процессор сброшен. Код остался в редакторе.", MUTED);
    }

    private void stopRun() {
        if (runTimer != null) {
            runTimer.stop();
        }
        runButton.setText("▶  Запустить");
    }

    private void showError(AsmException exception) {
        stopRun();
        if (exception.line() > 0) {
            codeEditor.highlightLine(exception.line());
        }
        setStatus("Строка " + exception.line() + ": " + exception.getMessage(), new Color(255, 151, 128));
    }

    private void setStatus(String message, Color color) {
        statusArea.setText(message);
        statusArea.setForeground(color);
        statusArea.setCaretPosition(0);
    }

    private void updateProgress() {
        progressLabel.setText(completed.size() + " / " + levels.size() + " пройдено");
        levelCombo.repaint();
    }

    private MachineState initialState() {
        Level level = levels.get(currentLevel);
        return new MachineState(level.initialRegisters(), level.initialMemory());
    }

    private void installShortcuts() {
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "run", this::toggleRun);
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "step", this::executeOne);
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK), "reset", this::resetProgram);
    }

    private void bind(KeyStroke keyStroke, String name, Runnable action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, name);
        getRootPane().getActionMap().put(name, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                action.run();
            }
        });
    }

    private static JButton button(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.brighter()),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
