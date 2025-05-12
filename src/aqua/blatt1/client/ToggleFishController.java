package aqua.blatt1.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToggleFishController implements ActionListener {
    private final TankModel tankModel;
    private final String fishId;

    public ToggleFishController(TankModel tankModel, String fishId) {
        this.tankModel = tankModel;
        this.fishId = fishId;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tankModel.locateFishGlobally(fishId);
    }
}
