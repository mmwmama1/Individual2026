package student;

import game.Cavern;
import game.Node;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Explorer#explore}. Since a real GameState/GUI cannot be driven
 * from outside the game package, these tests run explore() against
 * {@link TestExplorationState}, a small wrapper around a real {@link Cavern}.
 */
class ExplorerTest {

    /**
     * explore() must always finish standing exactly on the Orb, regardless of the
     * (randomly generated) shape of the cavern.
     */
    @RepeatedTest(15)
    void alwaysFindsTheOrb(RepetitionInfo info) {
        Cavern cavern = randomCavern(info.getCurrentRepetition());
        TestExplorationState state = new TestExplorationState(cavern);

        new Explorer().explore(state);

        assertEquals(cavern.getTarget(), state.getPosition(),
                "explore() must return while standing on the Orb");
        assertEquals(0, state.getDistanceToTarget());
    }

    /**
     * A small, fixed cavern used as a fast, fully reproducible regression check
     * independent of the randomised tests above.
     */
    @Test
    void findsTheOrbInAFixedCavern() {
        Cavern cavern = Cavern.digExploreCavern(8, 12, new Random(42));
        TestExplorationState state = new TestExplorationState(cavern);

        new Explorer().explore(state);

        assertEquals(cavern.getTarget(), state.getPosition());
    }

    /**
     * The explorer should not wander hopelessly: the number of steps it actually
     * takes should stay within a generous multiple of the true shortest path
     * through the fully-known graph (computed here via a plain BFS). The bound
     * is deliberately loose - greedy best-first search on an admissible-but-not-
     * always-tight heuristic can require some backtracking - but it is tight
     * enough to catch a fundamentally broken (e.g. near-random-walk) strategy.
     */
    @RepeatedTest(10)
    void isReasonablyEfficient(RepetitionInfo info) {
        Cavern cavern = randomCavern(info.getCurrentRepetition());
        int shortest = shortestGraphDistance(cavern);
        TestExplorationState state = new TestExplorationState(cavern);

        new Explorer().explore(state);

        int stepsTaken = state.getStepsTaken();
        int allowance = shortest * 5 + 30;
        assertTrue(stepsTaken <= allowance,
                "took " + stepsTaken + " steps; true shortest path is " + shortest
                        + ", allowance was " + allowance);
    }

    /** Build a randomly shaped, but reproducibly seeded, exploration-only cavern. */
    private Cavern randomCavern(int seed) {
        Random rand = new Random(seed);
        int rows = 8 + rand.nextInt(15);
        int cols = 12 + rand.nextInt(20);
        return Cavern.digExploreCavern(rows, cols, rand);
    }

    /**
     * Compute the true shortest number of steps from the entrance to the Orb via
     * breadth-first search over the actual (fully known) graph. Valid here because
     * every edge in an explore-only cavern has weight 1 (see {@code GameState}).
     */
    private int shortestGraphDistance(Cavern cavern) {
        Map<Long, Integer> distance = new HashMap<>();
        Queue<Node> queue = new ArrayDeque<>();
        Node start = cavern.getEntrance();
        Node target = cavern.getTarget();

        distance.put(start.getId(), 0);
        queue.add(start);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (node.equals(target)) {
                return distance.get(node.getId());
            }
            for (Node neighbour : node.getNeighbours()) {
                if (!distance.containsKey(neighbour.getId())) {
                    distance.put(neighbour.getId(), distance.get(node.getId()) + 1);
                    queue.add(neighbour);
                }
            }
        }
        throw new IllegalStateException("Orb unreachable in test cavern - this should never happen");
    }
}
