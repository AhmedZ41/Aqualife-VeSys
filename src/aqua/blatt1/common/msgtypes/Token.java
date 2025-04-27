package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

/**
 * Token message used to control which client has permission to send fish.
 */
public final class Token implements Serializable {

    private static final long serialVersionUID = 1L;

    // No fields needed — the existence of the Token is enough
    public Token() {
        // empty constructor
    }
}
