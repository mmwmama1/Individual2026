package student;

import game.Cavern;
import game.Edge;
import game.Node;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Explorer#escape}.
 */
class ExplorerEscapeTest {

    @RepeatedTest(10)
    void alwaysEscapesBeforeTimeRunsOut(RepetitionInfo info) {
        Random rand = new Random(info.getCurrentRepetition());
        int rows = 8 + rand.nextInt(10);
        int cols = 12 + rand.nextInt(15);

        Cavern explore = Cavern.digExploreCavern(rows, cols, rand);
        var orbTile = explore.getTarget().getTile();
        Cavern escape = Cavern.digEscapeCavern(rows, cols, orbTile.getRow(), orbTile.getColumn(), rand);

        Node start = escape.getNodeAt(orbTile.getRow(), orbTile.getColumn());
        int shortest = shortestPathCost(escape, start, escape.getTarget());
        int timeBudget = shortest + (shortest / 2) + 20;

        TestEscapeState state = new TestEscapeState(escape, start, timeBudget);
        new Explorer().escape(state);

        assertEquals(escape.getTarget(), state.getPosition(),
                "escape() must return while standing on the exit");
        assertTrue(state.getTimeRemaining() >= 0, "escape() must not run out of time");
    }

    @Test
    void escapesInAFixedCavern() {
        Random rand = new Random(7);
        Cavern explore = Cavern.digExploreCavern(10, 14, rand);
        var orbTile = explore.getTarget().getTile();
        Cavern escape = Cavern.digEscapeCavern(10, 14, orbTile.getRow(), orbTile.getColumn(), rand);

        Node start = escape.getNodeAt(orbTile.getRow(), orbTile.getColumn());
        int timeBudget = shortestPathCost(escape, start, escape.getTarget()) * 2;

        TestEscapeState state = new TestEscapeState(escape, start, timeBudget);
        new Explorer().escape(state);

        assertEquals(escape.getTarget(), state.getPosition());
    }

    private int shortestPathCost(Cavern cavern, Node start, Node target) {
        Map<Long, Integer> dist = new HashMap<>();
        PriorityQueue<Node> frontier = new PriorityQueue<>(
                Comparator.comparingInt(n -> dist.getOrDefault(n.getId(), Integer.MAX_VALUE)));
        Set<Long> settled = new HashSet<>();

        dist.put(start.getId(), 0);
        frontier.add(start);

        while (!frontier.isEmpty()) {
            Node node = frontier.poll();
            if (!settled.add(node.getId())) {
                continue;
            }
            if (node.equals(target)) {
                return dist.get(node.getId());
            }
            int nodeDist = dist.get(node.getId());
            for (Edge edge : node.getExits()) {
                Node neighbour = edge.getOther(node);
                int newDist = nodeDist + edge.length();
                if (newDist < dist.getOrDefault(neighbour.getId(), Integer.MAX_VALUE)) {
                    dist.put(neighbour.getId(), newDist);
                    frontier.add(neighbour);
                }
            }
        }
        throw new IllegalStateException("Exit unreachable in test cavern");
    }
}
