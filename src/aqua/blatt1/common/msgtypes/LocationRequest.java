package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * A message sent to locate and toggle a specific fish in the ring.
 */
public final class LocationRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String fishId;
    private final InetSocketAddress origin;

    public LocationRequest(String fishId, InetSocketAddress origin) {
        this.fishId = fishId;
        this.origin = origin;
    }

    public String getFishId() {
        return fishId;
    }

    public InetSocketAddress getOrigin() {
        return origin;
    }
}
