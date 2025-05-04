package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

/**
 * Special marker used to start a distributed snapshot.
 */
public final class SnapshotMarker implements Serializable {

    private static final long serialVersionUID = 1L;

    // No fields needed. The presence of this message is the trigger.
    public SnapshotMarker() {
        // empty constructor
    }
}
