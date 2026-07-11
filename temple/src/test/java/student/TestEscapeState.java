package student;

import game.Cavern;
import game.EscapeState;
import game.Node;

import java.util.Collection;

/**
 * A minimal {@link EscapeState} backed by a real {@link Cavern}, for testing
 * {@link Explorer#escape} without the full GUI/GameState machinery.
 */
class TestEscapeState implements EscapeState {
    private final Cavern cavern;
    private Node position;
    private int timeRemaining;
    private int goldCollected;

    TestEscapeState(Cavern cavern, Node start, int timeRemaining) {
        this.cavern = cavern;
        this.position = start;
        this.timeRemaining = timeRemaining;
    }

    int getGoldCollected() {
        return goldCollected;
    }

    Node getPosition() {
        return position;
    }

    @Override
    public Node getCurrentNode() {
        return position;
    }

    @Override
    public Node getExit() {
        return cavern.getTarget();
    }

    @Override
    public Collection<Node> getVertices() {
        return cavern.getGraph();
    }

    @Override
    public void moveTo(Node n) {
        int cost = position.getEdge(n).length();
        if (timeRemaining - cost < 0) {
            throw new OutOfTimeException();
        }
        if (!position.getNeighbours().contains(n)) {
            throw new IllegalArgumentException("moveTo: Node must be adjacent to position");
        }
        position = n;
        timeRemaining -= cost;
    }

    @Override
    public void pickUpGold() {
        if (position.getTile().getGold() <= 0) {
            throw new IllegalStateException("pickUpGold: Error, no gold on this tile");
        }
        goldCollected += position.getTile().takeGold();
    }

    @Override
    public int getTimeRemaining() {
        return timeRemaining;
    }

    private static class OutOfTimeException extends RuntimeException {
    }
}
