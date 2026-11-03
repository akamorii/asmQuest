package asmquest.ui;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

public final class CodeEditor extends JPanel {
    private static final Color BACKGROUND = new Color(12, 20, 34);
    private static final Color CURRENT_LINE = new Color(24, 43, 57);

    private final CodeTextArea textArea = new CodeTextArea();
    private final LineNumberGutter gutter = new LineNumberGutter(textArea);
    private Runnable changeListener = () -> { };

    public CodeEditor() {
        super(new BorderLayout());
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        textArea.setBackground(BACKGROUND);
        textArea.setForeground(new Color(213, 227, 238));
        textArea.setCaretColor(new Color(118, 230, 165));
        textArea.setSelectionColor(new Color(43, 89, 100));
        textArea.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 12, 14, 12));
        textArea.setTabSize(2);
        textArea.setLineWrap(false);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null);
        scrollPane.setRowHeaderView(gutter);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        });
    }

    private void changed() {
        gutter.refresh();
        SwingUtilities.invokeLater(gutter::repaint);
        changeListener.run();
    }

    public void setChangeListener(Runnable listener) {
        this.changeListener = listener;
    }

    public String getText() {
        return textArea.getText();
    }

    public void setText(String text) {
        textArea.setText(text);
        textArea.setCaretPosition(0);
        gutter.refresh();
    }

    public void highlightLine(int line) {
        textArea.currentLine = line;
        textArea.repaint();
        gutter.repaint();
        if (line > 0) {
            try {
                int offset = textArea.getLineStartOffset(line - 1);
                Rectangle2D rectangle = textArea.modelToView2D(offset);
                if (rectangle != null) {
                    textArea.scrollRectToVisible(rectangle.getBounds());
                }
            } catch (Exception ignored) {
                // A stale highlight must never interrupt execution.
            }
        }
    }

    private static final class CodeTextArea extends JTextArea {
        private int currentLine = -1;

        @Override
        protected void paintComponent(Graphics graphics) {
            if (currentLine > 0 && currentLine <= getLineCount()) {
                try {
                    int offset = getLineStartOffset(currentLine - 1);
                    Rectangle2D rectangle = modelToView2D(offset);
                    if (rectangle != null) {
                        graphics.setColor(CURRENT_LINE);
                        graphics.fillRect(0, (int) rectangle.getY(), getWidth(),
                                (int) Math.ceil(rectangle.getHeight()));
                    }
                } catch (Exception ignored) {
                    // Skip painting the highlight for an invalid line.
                }
            }
            super.paintComponent(graphics);
        }
    }

    private static final class LineNumberGutter extends JComponent {
        private final JTextArea textArea;

        private LineNumberGutter(JTextArea textArea) {
            this.textArea = textArea;
            setOpaque(true);
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            refresh();
        }

        private void refresh() {
            int height = Math.max(textArea.getPreferredSize().height, 300);
            setPreferredSize(new Dimension(48, height));
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(10, 17, 29));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(33, 48, 64));
            g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
            g.setFont(getFont());
            FontMetrics metrics = g.getFontMetrics();
            int lineHeight = textArea.getFontMetrics(textArea.getFont()).getHeight();
            int top = textArea.getInsets().top;
            int current = ((CodeTextArea) textArea).currentLine;
            for (int line = 1; line <= textArea.getLineCount(); line++) {
                g.setColor(line == current ? new Color(118, 230, 165) : new Color(78, 96, 114));
                String number = String.format("%02d", line);
                int y = top + (line - 1) * lineHeight + (lineHeight + metrics.getAscent() - metrics.getDescent()) / 2;
                g.drawString(number, getWidth() - metrics.stringWidth(number) - 12, y);
            }
            g.dispose();
        }
    }
}
