package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private final InetSocketAddress clientAddress;


	public RegisterResponse(String id, InetSocketAddress clientAddress) {
		this.id = id;
		this.clientAddress = clientAddress;
	}

	public InetSocketAddress getClientAddress() {
		return clientAddress;
	}


	public String getId() {
		return id;
	}

}
