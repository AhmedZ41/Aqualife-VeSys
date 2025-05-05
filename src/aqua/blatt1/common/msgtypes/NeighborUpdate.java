package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * NeighborUpdate is a message sent by the broker to clients,
 * informing them of their new left or right neighbor in the ring.
 */
public final class NeighborUpdate implements Serializable {

    private static final long serialVersionUID = 1L;

    private final InetSocketAddress neighborAddress; // The address of the neighbor
    private final boolean isLeft; // true if this is the left neighbor, false if right

    // Constructor
    public NeighborUpdate(InetSocketAddress neighborAddress, boolean isLeft) {
        this.neighborAddress = neighborAddress;
        this.isLeft = isLeft;
    }

    // Getter: Get the neighbor's address
    public InetSocketAddress getNeighborAddress() {
        return neighborAddress;
    }

    // Getter: Find out if this is the left neighbor
    public boolean isLeft() {
        return isLeft;
    }
}
