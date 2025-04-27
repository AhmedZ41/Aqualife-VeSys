package aqua.blatt1.client;

import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private final TankModel tankModel;

		private ClientForwarder(TankModel tankModel) {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
			this.tankModel = tankModel;
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish) {
			InetSocketAddress neighbor;

			if (fish.getDirection() == Direction.RIGHT) {
				neighbor = tankModel.getRightNeighbor();
			} else {
				neighbor = tankModel.getLeftNeighbor();
			}

			if (neighbor != null) {
				endpoint.send(neighbor, new HandoffRequest(fish));
				System.out.println("Sent fish to " + (fish.getDirection() == Direction.RIGHT ? "right" : "left") + " neighbor: " + neighbor);
			} else {
				System.out.println("No neighbor available to send fish.");
			}
		}

		public void sendToken(InetSocketAddress neighbor) {
			endpoint.send(neighbor, new Token());
		}

	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();
				Object payload = msg.getPayload();

				if (payload instanceof RegisterResponse) {
					tankModel.onRegistration(((RegisterResponse) payload).getId());
				} else if (payload instanceof HandoffRequest) {
					tankModel.receiveFish(((HandoffRequest) payload).getFish());
				} else if (payload instanceof NeighborUpdate) {
					NeighborUpdate neighborUpdate = (NeighborUpdate) payload;
					if (neighborUpdate.isLeft()) {
						tankModel.setLeftNeighbor(neighborUpdate.getNeighborAddress());
						System.out.println("Updated left neighbor to: " + neighborUpdate.getNeighborAddress());
					} else {
						tankModel.setRightNeighbor(neighborUpdate.getNeighborAddress());
						System.out.println("Updated right neighbor to: " + neighborUpdate.getNeighborAddress());
					}
				}
				else if (payload instanceof Token) {
					tankModel.receiveToken();
				}

			}
			System.out.println("Receiver stopped.");
		}

	}

	public ClientForwarder newClientForwarder(TankModel tankModel) {
		return new ClientForwarder(tankModel);
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
