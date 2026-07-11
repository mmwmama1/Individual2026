package student;

import game.Cavern;
import game.ExplorationState;
import game.Node;
import game.NodeStatus;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A minimal, self-contained implementation of {@link ExplorationState} backed by a
 * real {@link Cavern}, used only so that {@link Explorer#explore} can be exercised
 * directly in tests without needing the full GUI/GameState machinery (which is not
 * reachable from the student package).
 * <p>
 * Distances are computed the same way {@code GameState} computes them: grid
 * (Manhattan) distance to the target, ignoring walls.
 */
class TestExplorationState implements ExplorationState {
    private final Cavern cavern;
    private Node position;
    private int stepsTaken;

    TestExplorationState(Cavern cavern) {
        this.cavern = cavern;
        this.position = cavern.getEntrance();
        this.stepsTaken = 0;
    }

    /** Return how many calls to {@code moveTo} have been made so far. */
    int getStepsTaken() {
        return stepsTaken;
    }

    /** Return the node currently occupied. */
    Node getPosition() {
        return position;
    }

    @Override
    public long getCurrentLocation() {
        return position.getId();
    }

    @Override
    public Collection<NodeStatus> getNeighbours() {
        Collection<NodeStatus> result = new ArrayList<>();
        for (Node neighbour : position.getNeighbours()) {
            result.add(new NodeStatus(neighbour.getId(), gridDistanceToTarget(neighbour)));
        }
        return result;
    }

    @Override
    public int getDistanceToTarget() {
        return gridDistanceToTarget(position);
    }

    @Override
    public void moveTo(long id) {
        for (Node neighbour : position.getNeighbours()) {
            if (neighbour.getId() == id) {
                position = neighbour;
                stepsTaken++;
                return;
            }
        }
        throw new IllegalArgumentException("moveTo: node " + id + " is not adjacent to the current position");
    }

    private int gridDistanceToTarget(Node node) {
        Node target = cavern.getTarget();
        return Math.abs(node.getTile().getRow() - target.getTile().getRow())
                + Math.abs(node.getTile().getColumn() - target.getTile().getColumn());
    }
}