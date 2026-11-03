package asmquest;

import asmquest.ui.GameFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class AsmQuestApp {
    private AsmQuestApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // The game also works with Swing's default appearance.
            }
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}
