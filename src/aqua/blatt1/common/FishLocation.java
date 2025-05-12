package aqua.blatt1.common;

/**
 * Used to track the known location of a fish.
 */
public enum FishLocation {
    HERE,   // The fish is in the local tank
    LEFT,   // The fish was sent to the left neighbor
    RIGHT   // The fish was sent to the right neighbor
}
