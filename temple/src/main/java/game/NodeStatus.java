package game;

import java.util.Objects;

/**
 * An instance maintains the status of a node -- it's id and its distance from the Orb.
 */
public record NodeStatus(long nodeID, int distanceToTarget) implements Comparable<NodeStatus> {
    /**
     * Return a negative number, 0, or a positive number depending on
     * whether this is closer to, at the same distance, or farther from the Orb.
     */
    @Override
    public int compareTo(NodeStatus other) {
        return Integer.compare(distanceToTarget(), other.distanceToTarget());
    }

    /**
     * Return true if ob is an instance of NodeStatus and has the same id as this one.
     */
    @Override
    public boolean equals(Object ob) {
        if (ob == this) {
            return true;
        }
        if (!(ob instanceof NodeStatus)) {
            return false;
        }
        return nodeID() == ((NodeStatus) ob).nodeID();
    }

    /**
     * Return a hashcode for this, based solely on its id.
     */
    @Override
    public int hashCode() {
        return Objects.hash(nodeID());
    }
}
