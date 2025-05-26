package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.PoisonPill;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.msgtypes.*;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {
    private static final int PORT = 4711;
    private static final long LEASE_TIME_MILLIS = 2000; // Lease time constant
    private static final long CLEANUP_INTERVAL = 1000; // Check every second
    
    private final Endpoint endpoint;
    private final ClientCollection<InetSocketAddress> clients;
    private final AtomicInteger idCounter;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile boolean stopRequested = false; //volatile = it's visible to all threads
    private final Timer cleanupTimer;

    public Broker() {
        this.endpoint = new Endpoint(PORT); // Bind to port 4711
        this.clients = new ClientCollection<>();
        this.idCounter = new AtomicInteger(1);
        this.cleanupTimer = new Timer(true); // Create as daemon timer

        // Start the cleanup timer task
        cleanupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                lock.writeLock().lock();
                try {
                    clients.removeExpiredClients(LEASE_TIME_MILLIS);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }, CLEANUP_INTERVAL, CLEANUP_INTERVAL);

        new Thread(() -> {
            javax.swing.JOptionPane.showMessageDialog(null, "Press OK button to stop server");
            stopRequested = true;
            cleanupTimer.cancel(); // Stop the cleanup timer when broker stops
        }).start();
    }

    private class BrokerTask implements Runnable {
        private final Message message;

        BrokerTask(Message message) {
            this.message = message;
        }

        @Override
        public void run() {
            Object payload = message.getPayload();

            if (payload instanceof RegisterRequest){
                lock.writeLock().lock();
                try {
                    register(message);
                } finally {
                    lock.writeLock().unlock();
                }
            } else if (payload instanceof DeregisterRequest){
                lock.writeLock().lock();
                try {
                    deregister(message);
                } finally {
                    lock.writeLock().unlock();
                }
                
            } else if (payload instanceof HandoffRequest){
                lock.readLock().lock();
                try {
                    handoffFish(message);
                } finally {
                    lock.readLock().unlock();
                }
            } else if (payload instanceof PoisonPill) {
                System.out.println("Poison pill received. Shutting down broker...");
                stopRequested = true;
                return;
            }

        }
    }

    public void broker(){
        while (!stopRequested) {
            Message message = endpoint.blockingReceive();
            executor.execute(new BrokerTask(message));
        }
        executor.shutdown();
    }

    private void register(Message message) {
        InetSocketAddress clientAddress = message.getSender();

        // Check if this client already exists
        int existingIndex = clients.indexOf(clientAddress);
        if (existingIndex != -1) {
            // Client exists, just update its timestamp
            // The update is handled in ClientCollection.add()
            String existingId = clients.getClientId(existingIndex);
            clients.add(existingId, clientAddress);

            // Send RegisterResponse with existing ID
            Map<String, InetSocketAddress> snapshot = clients.toMap();
            endpoint.send(clientAddress, new RegisterResponse(existingId, clientAddress, snapshot, LEASE_TIME_MILLIS));
            System.out.println("Re-registered: " + existingId + " at " + clientAddress);
            return;
        }

        // New client registration
        String clientId = "tank" + idCounter.getAndIncrement();
        clients.add(clientId, clientAddress);

        // Find neighbors
        int newIndex = clients.indexOf(clientAddress);
        InetSocketAddress leftNeighbor = clients.getLeftNeighorOf(newIndex);
        InetSocketAddress rightNeighbor = clients.getRightNeighorOf(newIndex);

        // Inform the new client about its left and right neighbors
        endpoint.send(clientAddress, new NeighborUpdate(leftNeighbor, true));
        endpoint.send(clientAddress, new NeighborUpdate(rightNeighbor, false));

        // Inform the left neighbor about its new right neighbor
        endpoint.send(leftNeighbor, new NeighborUpdate(clientAddress, false));

        // Inform the right neighbor about its new left neighbor
        endpoint.send(rightNeighbor, new NeighborUpdate(clientAddress, true));

        // Send RegisterResponse to the newly registered client
        Map<String, InetSocketAddress> snapshot = clients.toMap();
        endpoint.send(clientAddress, new RegisterResponse(clientId, clientAddress, snapshot, LEASE_TIME_MILLIS));

        System.out.println("Registered new: " + clientId + " at " + clientAddress);

        if (clients.size() == 1) {
            endpoint.send(clientAddress, new Token());
            System.out.println("First Token sent to: " + clientAddress);
        }
    }


    private void deregister(Message message) {
        DeregisterRequest request = (DeregisterRequest) message.getPayload();
        String clientId = request.getId();

        int index = clients.indexOf(clientId);
        if (index != -1) {
            // Find the leaving client's neighbors BEFORE removal
            InetSocketAddress leavingClient = clients.getClient(index);
            InetSocketAddress leftNeighbor = clients.getLeftNeighorOf(index);
            InetSocketAddress rightNeighbor = clients.getRightNeighorOf(index);

            clients.remove(index);

            // Inform left neighbor: your new right neighbor is rightNeighbor
            endpoint.send(leftNeighbor, new NeighborUpdate(rightNeighbor, false)); // false = right
            // Inform right neighbor: your new left neighbor is leftNeighbor
            endpoint.send(rightNeighbor, new NeighborUpdate(leftNeighbor, true)); // true = left

            System.out.println("Deregistered: " + clientId);
        } else {
            System.out.println("Deregister request received, but client not found!");
        }
    }

    private void handoffFish(Message message) {
        HandoffRequest request = (HandoffRequest) message.getPayload();
        InetSocketAddress senderAddress = message.getSender();

        int senderIndex = clients.indexOf(senderAddress);
        if (senderIndex != -1) {
            FishModel fish = request.getFish(); // Extract fish data
            InetSocketAddress recipientAddress;

            if (fish.getDirection() == Direction.RIGHT) {
                recipientAddress = clients.getRightNeighorOf(senderIndex); // Move Right
            } else {
                recipientAddress = clients.getLeftNeighorOf(senderIndex); // Move Left
            }

            endpoint.send(recipientAddress, request); // Forward fish to the correct tank
            System.out.println("Handoff fish from " + senderAddress + " to " + recipientAddress);
        } else {
            System.out.println("Handoff failed: Sender not found!");
        }
    }



    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }
}
