package aqua.blatt1.broker;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * This class is not thread-safe and hence must be used in a thread-safe way, e.g. thread confined or 
 * externally synchronized. 
 */

public class ClientCollection<T> {
	private class Client {
		final String id;
		final ClientInfo clientInfo;

		Client(String id, T client) {
			this.id = id;
			this.clientInfo = new ClientInfo(id, (InetSocketAddress) client);
		}
	}

	private final List<Client> clients;

	public ClientCollection() {
		clients = new ArrayList<Client>();
	}

	public ClientCollection<T> add(String id, T client) {
		// Check if client already exists
		if (client instanceof InetSocketAddress address) {
			for (int i = 0; i < clients.size(); i++) {
				if (clients.get(i).clientInfo.getAddress().equals(address)) {
					// Update timestamp of existing client
					clients.get(i).clientInfo.updateTimestamp();
					return this;
				}
			}
		}
		// Add new client
		clients.add(new Client(id, client));
		return this;
	}

	public ClientCollection<T> remove(int index) {
		clients.remove(index);
		return this;
	}

	public int indexOf(String id) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).id.equals(id))
				return i;
		return -1;
	}

	public int indexOf(T client) {
		if (client instanceof InetSocketAddress address) {
			for (int i = 0; i < clients.size(); i++) {
				if (clients.get(i).clientInfo.getAddress().equals(address)) {
					return i;
				}
			}
		}
		return -1;
	}

	public String getClientId(int index) {
		return clients.get(index).id;
	}

	public T getClient(int index) {
		return (T) clients.get(index).clientInfo.getAddress();
	}

	public int size() {
		return clients.size();
	}

	public T getLeftNeighorOf(int index) {
		return index == 0 ? 
			(T) clients.get(clients.size() - 1).clientInfo.getAddress() : 
			(T) clients.get(index - 1).clientInfo.getAddress();
	}

	public T getRightNeighorOf(int index) {
		return index < clients.size() - 1 ? 
			(T) clients.get(index + 1).clientInfo.getAddress() : 
			(T) clients.get(0).clientInfo.getAddress();
	}

	public Map<String, InetSocketAddress> toMap() {
		Map<String, InetSocketAddress> result = new HashMap<>();
		for (Client client : clients) {
			result.put(client.id, client.clientInfo.getAddress());
		}
		return result;
	}

	public void removeExpiredClients(long leaseTimeMillis) {
		Instant now = Instant.now();
		List<Client> expiredClients = new ArrayList<>();

		// Collect expired clients
		for (Client client : clients) {
			long timeSinceLastUpdate = ChronoUnit.MILLIS.between(client.clientInfo.getLastUpdated(), now);
			if (timeSinceLastUpdate > leaseTimeMillis) {
				expiredClients.add(client);
			}
		}

		// Remove expired clients
		for (Client expired : expiredClients) {
			int index = indexOf(expired.id);
			if (index != -1) {
				remove(index);
				System.out.println("Client " + expired.id + " expired and was removed");
			}
		}
	}
}
