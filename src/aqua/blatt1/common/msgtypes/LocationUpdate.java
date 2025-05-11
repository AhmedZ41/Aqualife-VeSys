package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public final class LocationUpdate implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String fishId;
    private final InetSocketAddress newLocation;

    public LocationUpdate(String fishId, InetSocketAddress newLocation) {
        this.fishId = fishId;
        this.newLocation = newLocation;
    }

    public String getFishId() {
        return fishId;
    }

    public InetSocketAddress getNewLocation() {
        return newLocation;
    }
}
