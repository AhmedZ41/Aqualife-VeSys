package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishLocation;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.SnapshotToken;

import javax.swing.*;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected ClientCommunicator.ClientForwarder forwarder;
	private InetSocketAddress leftNeighbor;
	private InetSocketAddress rightNeighbor;
	private boolean hasToken = false;
	private java.util.Timer tokenTimer;
	// Enum for tracking input channels during snapshot recording
	private enum RecordingState {
		IDLE, LEFT, RIGHT, BOTH
	}
	// --- Snapshot-related state ---
	private RecordingState recordingState = RecordingState.IDLE; // Which channels are being recorded
	private int snapshotLocalCount = 0;         // How many fish we had when snapshot started
	private boolean snapshotInitiator = false;  // True if we initiated the global snapshot
	// Tracks where each known fish is located: HERE, LEFT, or RIGHT
	private final Map<String, FishLocation> fishLocations = new ConcurrentHashMap<>();






	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
			fishLocations.put(fish.getId(), FishLocation.HERE); // Mark as locally present

		}
	}

	public synchronized void receiveFish(FishModel fish) {
		// Handle snapshot logic
		String senderTankId = fish.getTankId(); // ID of the sender client (e.g. "tank2")

		if (recordingState != RecordingState.IDLE) {
			if (leftNeighbor != null && senderTankId.equals(getIdByAddress(leftNeighbor))) {
				// Fish came from left neighbor
				if (recordingState == RecordingState.BOTH) {
					recordingState = RecordingState.RIGHT; // done with left
					snapshotLocalCount++; // count this in-transit fish
					System.out.println("Snapshot: Counted fish from LEFT");
				} else if (recordingState == RecordingState.LEFT) {
					recordingState = RecordingState.IDLE;
					snapshotLocalCount++;
					System.out.println("Snapshot: Counted last fish from LEFT — done");
					maybeSendSnapshotToken(); // check if we’re done
				}
			} else if (rightNeighbor != null && senderTankId.equals(getIdByAddress(rightNeighbor))) {
				// Fish came from right neighbor
				if (recordingState == RecordingState.BOTH) {
					recordingState = RecordingState.LEFT; // done with right
					snapshotLocalCount++;
					System.out.println("Snapshot: Counted fish from RIGHT");
				} else if (recordingState == RecordingState.RIGHT) {
					recordingState = RecordingState.IDLE;
					snapshotLocalCount++;
					System.out.println("Snapshot: Counted last fish from RIGHT — done");
					maybeSendSnapshotToken(); // check if we’re done
				}
			}
		}

		// Normal receive behavior: reset fish position and add to tank
		fish.setToStart();
		fishies.add(fish);
		fishLocations.put(fish.getId(), FishLocation.HERE); // Mark fish as HERE
	}

	private void maybeSendSnapshotToken() {
		if (recordingState == RecordingState.IDLE && snapshotInitiator) {
			forwarder.sendSnapshotToken(leftNeighbor, snapshotLocalCount);
			System.out.println("Initiator: Sending SnapshotToken with count = " + snapshotLocalCount);
			snapshotInitiator = false; // reset
		}
	}

	private String getIdByAddress(InetSocketAddress address) {
		return address.toString(); // or a cleaner mapping if needed
	}

	public synchronized void receiveSnapshotToken(SnapshotToken token) {
		token.addCount(snapshotLocalCount); // Add local count

		if (snapshotInitiator) {
			// I'm the initiator — show the result
			snapshotInitiator = false; // Reset flag
			recordingState = RecordingState.IDLE; // Reset state
			JOptionPane.showMessageDialog(null, "Global Snapshot: " + token.getTotalCount() + " fish");
		} else {
			// Forward to left neighbor
			forwarder.sendSnapshotToken(leftNeighbor, token.getTotalCount());
			System.out.println("Forwarded SnapshotToken with total = " + token.getTotalCount());
		}
	}





	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge()) {
				// If we have the token, send the fish to the neighbor
				if (hasToken()) {
					forwarder.handOff(fish);
					if (fish.getDirection() == Direction.LEFT) {
						fishLocations.put(fish.getId(), FishLocation.LEFT); // Mark as sent left
					} else {
						fishLocations.put(fish.getId(), FishLocation.RIGHT); // Mark as sent right
					}

				} else {
					fish.reverse(); // Reverse direction if no token
				}
			}

			if (fish.disappears())
				it.remove();
		}
	}


	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}

	public synchronized void setLeftNeighbor(InetSocketAddress neighbor) {
		this.leftNeighbor = neighbor;
	}

	public synchronized void setRightNeighbor(InetSocketAddress neighbor) {
		this.rightNeighbor = neighbor;
	}

	public synchronized InetSocketAddress getLeftNeighbor() {
		return leftNeighbor;
	}

	public synchronized InetSocketAddress getRightNeighbor() {
		return rightNeighbor;
	}

	public synchronized void setForwarder(ClientCommunicator.ClientForwarder forwarder) {
		this.forwarder = forwarder;
	}

	public synchronized void receiveToken() {
		hasToken = true; // Now we have the token
		System.out.println("Token received.");

		tokenTimer = new java.util.Timer();
		tokenTimer.schedule(new java.util.TimerTask() {
			@Override
			public void run() {
				synchronized (TankModel.this) {
					hasToken = false; // After holding token, release it
					sendToken(); // Pass token to left neighbor
				}
			}
		}, 2000); // Wait for 2 seconds (2000 milliseconds)
	}
	public synchronized void sendToken() {
		if (leftNeighbor != null) {
			forwarder.sendToken(leftNeighbor);
			System.out.println("Token sent to left neighbor: " + leftNeighbor);
		} else {
			System.out.println("No left neighbor to send token!");
		}
	}
	public synchronized boolean hasToken() {
		return hasToken;
	}

	public synchronized void initiateSnapshot() {
		System.out.println("Snapshot initiated.");

		// Save current local state (number of fish)
		snapshotLocalCount = fishies.size();
		snapshotInitiator = true;

		// Start recording both input channels
		recordingState = RecordingState.BOTH;

		// Send SnapshotMarker to both neighbors
		if (leftNeighbor != null) {
			forwarder.sendSnapshotMarker(leftNeighbor);
			System.out.println("Sent SnapshotMarker to LEFT neighbor: " + leftNeighbor);
		}

		if (rightNeighbor != null) {
			forwarder.sendSnapshotMarker(rightNeighbor);
			System.out.println("Sent SnapshotMarker to RIGHT neighbor: " + rightNeighbor);
		}
	}

	public synchronized void receiveSnapshotMarker(InetSocketAddress sender) {
		String senderId = getIdByAddress(sender);

		if (recordingState == RecordingState.IDLE) {
			// First marker received → start snapshot
			System.out.println("First SnapshotMarker received from: " + senderId);

			snapshotLocalCount = fishies.size();  // Save local state
			recordingState = isLeftNeighbor(sender) ? RecordingState.RIGHT : RecordingState.LEFT;

			// Forward SnapshotMarker to both neighbors
			if (leftNeighbor != null) forwarder.sendSnapshotMarker(leftNeighbor);
			if (rightNeighbor != null) forwarder.sendSnapshotMarker(rightNeighbor);

		} else if (recordingState == RecordingState.LEFT && isRightNeighbor(sender)) {
			recordingState = RecordingState.IDLE;
			System.out.println("Second marker from RIGHT — done recording");
			maybeSendSnapshotToken();

		} else if (recordingState == RecordingState.RIGHT && isLeftNeighbor(sender)) {
			recordingState = RecordingState.IDLE;
			System.out.println("Second marker from LEFT — done recording");
			maybeSendSnapshotToken();

		} else if (recordingState == RecordingState.BOTH) {
			// First channel already marked, still one open
			if (isLeftNeighbor(sender)) {
				recordingState = RecordingState.RIGHT;
				System.out.println("Snapshot: Left channel marked");
			} else if (isRightNeighbor(sender)) {
				recordingState = RecordingState.LEFT;
				System.out.println("Snapshot: Right channel marked");
			}
		}
	}

	private boolean isLeftNeighbor(InetSocketAddress addr) {
		return leftNeighbor != null && getIdByAddress(leftNeighbor).equals(getIdByAddress(addr));
	}

	private boolean isRightNeighbor(InetSocketAddress addr) {
		return rightNeighbor != null && getIdByAddress(rightNeighbor).equals(getIdByAddress(addr));
	}







}