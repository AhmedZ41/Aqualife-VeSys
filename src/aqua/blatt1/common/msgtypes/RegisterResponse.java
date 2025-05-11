package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Map;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private final InetSocketAddress clientAddress;
	private final Map<String, InetSocketAddress> knownClients;



	public RegisterResponse(String id, InetSocketAddress clientAddress, Map<String, InetSocketAddress> knownClients) {
		this.id = id;
		this.clientAddress = clientAddress;
		this.knownClients = knownClients;
	}
	public Map<String, InetSocketAddress> getKnownClients() {
		return knownClients;
	}



	public InetSocketAddress getClientAddress() {
		return clientAddress;
	}


	public String getId() {
		return id;
	}

}
