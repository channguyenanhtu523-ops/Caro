package com.dmx.caro.client;

import com.dmx.caro.client.controller.GameClientController;
import com.dmx.caro.client.model.ClientGameModel;
import com.dmx.caro.client.view.MainWindow;
import com.dmx.caro.common.config.AppConfig;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class GameClientLauncher {
    private GameClientLauncher() {
    }

    public static void main(String[] args) {
        try {
            AppConfig config = AppConfig.load(Path.of("config", "app-config.xml"));
            SwingUtilities.invokeLater(() -> launch(config));
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
        }
    }

    private static void launch(AppConfig config) {
        installLookAndFeel();
        ClientGameModel model = new ClientGameModel();
        MainWindow window = new MainWindow(model);
        GameClientController controller = new GameClientController(config, model, window);
        controller.initialize();
        window.setVisible(true);
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Use default Swing look and feel if the system theme is unavailable.
        }
    }
}
