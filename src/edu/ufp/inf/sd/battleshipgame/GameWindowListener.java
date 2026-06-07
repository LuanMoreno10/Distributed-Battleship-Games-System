package edu.ufp.inf.sd.battleshipgame;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GameWindowListener extends WindowAdapter {

    private final GameUI ui;

    public GameWindowListener(GameUI ui) {
        this.ui = ui;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        try { ui.game.detach(ui.token, ui.username); } catch (Exception ignored) {}
        if (ui.consumer != null) { try { ui.consumer.close(); } catch (Exception ignored) {} }
        if (ui.actionPublisher != null) { try { ui.actionPublisher.close(); } catch (Exception ignored) {} }
        ui.dispose();
    }
}
