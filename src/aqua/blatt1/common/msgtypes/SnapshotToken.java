package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

/**
 * SnapshotToken: Collects total fish count during global snapshot.
 */
public final class SnapshotToken implements Serializable {

    private static final long serialVersionUID = 1L;

    private int totalCount;

    public SnapshotToken(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void addCount(int count) {
        this.totalCount += count;
    }
}
