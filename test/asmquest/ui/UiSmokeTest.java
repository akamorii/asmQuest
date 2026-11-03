package asmquest.ui;

import asmquest.model.Level;
import asmquest.model.LevelCatalog;
import asmquest.vm.MachineState;
import asmquest.vm.VirtualMachine;

import javax.swing.JPanel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class UiSmokeTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Level level = LevelCatalog.all().get(7);
        MachineState initial = new MachineState(level.initialRegisters(), level.initialMemory());
        VirtualMachine.Session session = VirtualMachine.compile(level.solution(), initial);
        while (!session.isFinished()) {
            session.step();
        }

        VisualizerPanel visualizer = new VisualizerPanel();
        visualizer.setSize(760, 780);
        visualizer.reset(level, initial);
        visualizer.setState(session.state());
        visualizer.setCompleted(true);
        BufferedImage image = new BufferedImage(760, 780, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        visualizer.paint(graphics);
        graphics.dispose();
        if (image.getRGB(400, 400) == 0) {
            throw new AssertionError("Визуализатор не нарисовал сцену");
        }

        CodeEditor editor = new CodeEditor();
        editor.setText(level.starterCode());
        editor.highlightLine(3);
        if (!editor.getText().equals(level.starterCode())) {
            throw new AssertionError("Редактор изменил код уровня");
        }
        JPanel panel = new JPanel();
        panel.add(editor);
        panel.add(visualizer);
        System.out.println("OK: редактор и визуализатор прошли off-screen проверку.");
    }
}
