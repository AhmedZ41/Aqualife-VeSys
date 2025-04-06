package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.PoisonPill;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.msgtypes.*;


import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Broker {
    private static final int PORT = 4711;
    private final Endpoint endpoint;
    private final ClientCollection<InetSocketAddress> clients;
    private final AtomicInteger idCounter;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile boolean stopRequested = false; //volatile = it's visible to all threads



    public Broker() {
        this.endpoint = new Endpoint(PORT); // Bind to port 4711
        this.clients = new ClientCollection<>();
        this.idCounter = new AtomicInteger(1);

        new Thread(() -> {
            javax.swing.JOptionPane.showMessageDialog(null, "Press OK button to stop server");
            stopRequested = true;
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
        String clientId = "tank" + idCounter.getAndIncrement();
        InetSocketAddress clientAddress = message.getSender();

        clients.add(clientId, clientAddress);
        endpoint.send(clientAddress, new RegisterResponse(clientId));

        System.out.println("Registered: " + clientId + " at " + clientAddress);
    }

    private void deregister(Message message) {
        DeregisterRequest request = (DeregisterRequest) message.getPayload();
        String clientId = request.getId();

        int index = clients.indexOf(clientId);
        if (index != -1) {
            clients.remove(index);
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
