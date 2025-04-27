package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.Observable;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;

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
		}
	}

	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);
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





}