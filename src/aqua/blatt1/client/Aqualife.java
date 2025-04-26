package aqua.blatt1.client;

import javax.swing.SwingUtilities;

public class Aqualife {

	public static void main(String[] args) {
		ClientCommunicator communicator = new ClientCommunicator();

		// Step 1: Create TankModel first, forwarder not yet ready
		TankModel tankModel = new TankModel(null);

		// Step 2: Create ClientForwarder with reference to TankModel
		ClientCommunicator.ClientForwarder forwarder = communicator.newClientForwarder(tankModel);

		// Step 3: Set the forwarder into TankModel
		tankModel.setForwarder(forwarder);

		// Start receiver
		communicator.newClientReceiver(tankModel).start();

		// Start GUI
		SwingUtilities.invokeLater(new AquaGui(tankModel));

		// Run tank logic
		tankModel.run();
	}

}
