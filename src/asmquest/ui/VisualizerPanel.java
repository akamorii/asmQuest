package asmquest.ui;

import asmquest.model.Level;
import asmquest.vm.MachineState;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

public final class VisualizerPanel extends JPanel {
    private static final Color BACKGROUND = new Color(9, 16, 29);
    private static final Color GREEN = new Color(118, 230, 165);
    private static final Color MUTED = new Color(108, 130, 151);
    private static final int GRID_SIZE = 5;

    private final List<Point> trail = new ArrayList<>();
    private final Timer animationTimer;
    private Level level;
    private MachineState state;
    private boolean completed;
    private double fromX;
    private double fromY;
    private long animationStarted;

    public VisualizerPanel() {
        setBackground(BACKGROUND);
        setDoubleBuffered(true);
        animationTimer = new Timer(16, event -> repaint());
    }

    public void reset(Level level, MachineState state) {
        this.level = level;
        this.state = state;
        this.completed = false;
        this.fromX = clamp(state.register(0));
        this.fromY = clamp(state.register(1));
        this.trail.clear();
        this.trail.add(new Point(state.register(0), state.register(1)));
        animationTimer.stop();
        repaint();
    }

    public void setState(MachineState state) {
        if (this.state == null) {
            this.state = state;
            return;
        }
        this.fromX = clamp(this.state.register(0));
        this.fromY = clamp(this.state.register(1));
        this.state = state;
        this.trail.add(new Point(state.register(0), state.register(1)));
        this.animationStarted = System.currentTimeMillis();
        this.animationTimer.restart();
        repaint();
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (level == null || state == null) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        g.setPaint(new GradientPaint(0, 0, new Color(14, 28, 43), width, height, BACKGROUND));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(29, 70, 75, 45));
        g.fillOval(width - 210, -120, 330, 300);

        drawHeader(g, width);

        int top = 88;
        int cell = Math.max(38, Math.min((width - 84) / GRID_SIZE,
                (height - top - 190) / GRID_SIZE));
        int gridWidth = cell * GRID_SIZE;
        int gridX = (width - gridWidth) / 2;
        int gridY = top;

        g.setColor(new Color(10, 26, 40, 220));
        g.fillRoundRect(gridX - 16, gridY - 16, gridWidth + 32, gridWidth + 32, 24, 24);
        g.setColor(new Color(39, 74, 87, 150));
        g.drawRoundRect(gridX - 16, gridY - 16, gridWidth + 32, gridWidth + 32, 24, 24);

        drawGrid(g, gridX, gridY, cell);
        drawTrail(g, gridX, gridY, cell);
        drawTarget(g, gridX + level.targetX() * cell, gridY + level.targetY() * cell, cell);
        drawFrog(g, gridX, gridY, cell);

        int registersY = gridY + gridWidth + 24;
        drawRegisters(g, gridX, registersY, gridWidth);
        drawMemory(g, gridX, registersY + 72, gridWidth);
        drawStats(g, gridX, registersY + 137, gridWidth);

        if (completed) {
            drawSuccess(g, gridX, gridY, gridWidth);
        }
        g.dispose();
    }

    private void drawHeader(Graphics2D g, int width) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.setColor(GREEN);
        g.drawString("СИМУЛЯТОР ПРУДА", 28, 31);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
        g.setColor(new Color(231, 241, 245));
        g.drawString("Код оживает в движении", 28, 58);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.setColor(MUTED);
        String legend = "R0 = X    R1 = Y";
        g.drawString(legend, width - g.getFontMetrics().stringWidth(legend) - 28, 33);
    }

    private void drawGrid(Graphics2D g, int gridX, int gridY, int cell) {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int px = gridX + x * cell;
                int py = gridY + y * cell;
                g.setColor((x + y) % 2 == 0 ? new Color(16, 48, 62) : new Color(18, 53, 67));
                g.fillRect(px, py, cell, cell);
                g.setColor(new Color(41, 83, 94, 130));
                g.drawRect(px, py, cell, cell);
                if ((x * 3 + y * 5) % 7 == 0) {
                    g.setColor(new Color(66, 128, 130, 45));
                    g.fillOval(px + cell / 5, py + cell * 2 / 3, cell / 3, Math.max(2, cell / 16));
                }
            }
        }
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        g.setColor(new Color(86, 125, 137));
        for (int i = 0; i < GRID_SIZE; i++) {
            String value = Integer.toString(i);
            g.drawString(value, gridX + i * cell + cell / 2 - 3, gridY - 5);
            g.drawString(value, gridX - 11, gridY + i * cell + cell / 2 + 4);
        }
    }

    private void drawTrail(Graphics2D g, int gridX, int gridY, int cell) {
        g.setColor(new Color(118, 230, 165, 65));
        int radius = Math.max(3, cell / 18);
        for (int i = 0; i < trail.size() - 1; i++) {
            Point point = trail.get(i);
            if (point.x >= 0 && point.x < GRID_SIZE && point.y >= 0 && point.y < GRID_SIZE) {
                int x = gridX + point.x * cell + cell / 2;
                int y = gridY + point.y * cell + cell / 2;
                g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            }
        }
    }

    private void drawTarget(Graphics2D g, int x, int y, int cell) {
        int centerX = x + cell / 2;
        int centerY = y + cell / 2;
        int size = (int) (cell * 0.72);
        g.setColor(new Color(104, 235, 176, 25));
        g.fillOval(centerX - size / 2 - 8, centerY - size / 2 - 8, size + 16, size + 16);
        g.setColor(new Color(79, 186, 137));
        g.fillOval(centerX - size / 2, centerY - size / 2, size, size);
        g.setColor(new Color(112, 216, 155));
        g.fillArc(centerX - size / 2 + 4, centerY - size / 2 + 4, size - 8, size - 8, 25, 295);
        g.setColor(new Color(16, 48, 62));
        g.fillPolygon(new int[]{centerX, centerX + size / 2, centerX + size / 3},
                new int[]{centerY, centerY - size / 4, centerY + 2}, 3);
        g.setColor(new Color(180, 255, 203, 170));
        g.setStroke(new BasicStroke(2f));
        g.drawOval(centerX - size / 2 - 5, centerY - size / 2 - 5, size + 10, size + 10);
    }

    private void drawFrog(Graphics2D g, int gridX, int gridY, int cell) {
        double progress = animationTimer.isRunning()
                ? Math.min(1.0, (System.currentTimeMillis() - animationStarted) / 280.0) : 1.0;
        if (progress >= 1.0) {
            animationTimer.stop();
        }
        double eased = 1 - Math.pow(1 - progress, 3);
        double currentX = fromX + (clamp(state.register(0)) - fromX) * eased;
        double currentY = fromY + (clamp(state.register(1)) - fromY) * eased;
        double jump = Math.sin(progress * Math.PI) * cell * 0.15;
        int centerX = gridX + (int) (currentX * cell) + cell / 2;
        int centerY = gridY + (int) (currentY * cell) + cell / 2 - (int) jump;
        int body = (int) (cell * 0.48);

        g.setColor(new Color(2, 12, 18, 90));
        g.fillOval(centerX - body / 2, gridY + (int) (currentY * cell) + cell * 2 / 3,
                body, Math.max(5, body / 4));

        g.setColor(new Color(71, 177, 105));
        g.fillOval(centerX - body * 2 / 3, centerY + body / 8, body / 2, body / 3);
        g.fillOval(centerX + body / 6, centerY + body / 8, body / 2, body / 3);
        g.setColor(new Color(106, 222, 132));
        g.fillOval(centerX - body / 2, centerY - body / 2, body, body);
        g.setColor(new Color(136, 239, 153));
        g.fillOval(centerX - body / 2, centerY - body * 3 / 4, body, body * 3 / 4);

        int eye = Math.max(7, body / 4);
        int eyeY = centerY - body * 3 / 4;
        g.fillOval(centerX - body / 3, eyeY, eye, eye);
        g.fillOval(centerX + body / 3 - eye, eyeY, eye, eye);
        g.setColor(new Color(241, 255, 231));
        g.fillOval(centerX - body / 3 + 2, eyeY + 2, eye - 4, eye - 4);
        g.fillOval(centerX + body / 3 - eye + 2, eyeY + 2, eye - 4, eye - 4);
        g.setColor(new Color(13, 38, 29));
        int pupil = Math.max(3, eye / 3);
        g.fillOval(centerX - body / 3 + eye / 2 - pupil / 2, eyeY + eye / 2 - pupil / 2, pupil, pupil);
        g.fillOval(centerX + body / 3 - eye / 2 - pupil / 2, eyeY + eye / 2 - pupil / 2, pupil, pupil);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(centerX - body / 5, centerY - body / 3, body * 2 / 5, body / 4, 200, 140);

        if (state.register(0) < 0 || state.register(0) >= GRID_SIZE
                || state.register(1) < 0 || state.register(1) >= GRID_SIZE) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g.setColor(new Color(255, 151, 128));
            centered(g, "ВНЕ ПРУДА", centerX, centerY - body, getWidth());
        }
    }

    private void drawRegisters(Graphics2D g, int x, int y, int width) {
        int gap = 8;
        int cardWidth = (width - gap * 3) / 4;
        String[] names = {"R0 · X", "R1 · Y", "R2", "R3"};
        Color[] accents = {GREEN, GREEN, new Color(244, 194, 105), new Color(173, 155, 255)};
        for (int i = 0; i < 4; i++) {
            int cardX = x + i * (cardWidth + gap);
            g.setColor(new Color(19, 31, 47));
            g.fillRoundRect(cardX, y, cardWidth, 58, 12, 12);
            g.setColor(new Color(43, 59, 76));
            g.drawRoundRect(cardX, y, cardWidth, 58, 12, 12);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            g.setColor(accents[i]);
            g.drawString(names[i], cardX + 11, y + 18);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 23));
            g.setColor(new Color(229, 239, 244));
            g.drawString(Integer.toString(state.register(i)), cardX + 11, y + 46);
        }
    }

    private void drawMemory(Graphics2D g, int x, int y, int width) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        for (int i = 0; i < MachineState.MEMORY_SIZE; i++) {
            int cellWidth = width / MachineState.MEMORY_SIZE;
            int cellX = x + i * cellWidth;
            g.setColor(new Color(14, 26, 40));
            g.fillRoundRect(cellX + 2, y, cellWidth - 4, 48, 8, 8);
            g.setColor(MUTED);
            centered(g, "[" + i + "]", cellX + cellWidth / 2, y + 15, cellWidth);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
            g.setColor(new Color(194, 213, 222));
            centered(g, Integer.toString(state.memory(i)), cellX + cellWidth / 2, y + 36, cellWidth);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        }
    }

    private void drawStats(Graphics2D g, int x, int y, int width) {
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        g.setColor(MUTED);
        String stats = "PC " + String.format("%02d", state.programCounter())
                + "     ZF " + (state.zeroFlag() ? "1" : "0")
                + "     ШАГ " + state.steps()
                + (state.halted() ? "     HLT" : "");
        g.drawString(stats, x + 3, y);
        String target = "ЦЕЛЬ  (" + level.targetX() + ", " + level.targetY() + ")";
        g.setColor(new Color(104, 183, 150));
        g.drawString(target, x + width - g.getFontMetrics().stringWidth(target), y);
    }

    private void drawSuccess(Graphics2D g, int x, int y, int width) {
        g.setColor(new Color(7, 19, 24, 215));
        g.fillRoundRect(x + width / 6, y + 12, width * 2 / 3, 48, 16, 16);
        g.setColor(GREEN);
        g.setStroke(new BasicStroke(1.4f));
        g.drawRoundRect(x + width / 6, y + 12, width * 2 / 3, 48, 16, 16);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        centered(g, "✓  УРОВЕНЬ ПРОЙДЕН!", x + width / 2, y + 42, width);
        for (int i = 0; i < 18; i++) {
            int px = x + 12 + (i * 71) % Math.max(20, width - 24);
            int py = y + 72 + (i * 37) % 90;
            g.setColor(i % 3 == 0 ? new Color(244, 194, 105)
                    : i % 3 == 1 ? GREEN : new Color(125, 183, 255));
            g.fillRoundRect(px, py, 4, 9, 2, 2);
        }
    }

    private static void centered(Graphics2D g, String text, int centerX, int baseline, int maxWidth) {
        FontMetrics metrics = g.getFontMetrics();
        int x = centerX - Math.min(maxWidth, metrics.stringWidth(text)) / 2;
        g.drawString(text, x, baseline);
    }

    private static double clamp(int value) {
        return Math.max(0, Math.min(GRID_SIZE - 1, value));
    }
}
