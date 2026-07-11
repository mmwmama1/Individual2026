package student;

import game.EscapeState;
import game.ExplorationState;
import game.NodeStatus;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Contains Jeremy's strategies for exploring the cavern to find the Orb,
 * and (for the group assignment) escaping the cavern afterwards.
 */
public class Explorer {

    /**
     * Explore the cavern, trying to find the orb in as few steps as possible.
     * Once you find the orb, you must return from the function in order to pick
     * it up. If you continue to move after finding the orb rather
     * than returning, it will not count.
     * If you return from this function while not standing on top of the orb,
     * it will count as a failure.
     * <p>
     * There is no limit to how many steps you can take, but you will receive
     * a score bonus multiplier for finding the orb in fewer steps.
     * <p>
     * At every step, you only know your current tile's ID and the ID of all
     * open neighbor tiles, as well as the distance to the orb at each of these tiles
     * (ignoring walls and obstacles).
     * <p>
     * To get information about the current state, use functions
     * getCurrentLocation(),
     * getNeighbours(), and
     * getDistanceToTarget()
     * in ExplorationState.
     * You know you are standing on the orb when getDistanceToTarget() is 0.
     * <p>
     * Use function moveTo(long id) in ExplorationState to move to a neighboring
     * tile by its ID. Doing this will change state to reflect your new position.
     * <p>
     * A suggested first implementation that will always find the orb, but likely won't
     * receive a large bonus multiplier, is a depth-first search.
     *
     * @param state the information available at the current state
     */
    public void explore(ExplorationState state) {
        Map<Long, Collection<NodeStatus>> known = new HashMap<>();
        Map<Long, Long> discoveredVia = new HashMap<>();
        PriorityQueue<NodeStatus> frontier = new PriorityQueue<>();

        long current = state.getCurrentLocation();
        known.put(current, List.of());

        while (state.getDistanceToTarget() != 0) {
            recordNeighbours(state, current, known, discoveredVia, frontier);

            NodeStatus next = frontier.poll();
            while (next != null && known.containsKey(next.nodeID())) {
                next = frontier.poll();
            }
            if (next == null) {
                throw new IllegalStateException("Explorer ran out of places to go before finding the Orb.");
            }

            long via = discoveredVia.get(next.nodeID());
            walk(state, current, via, known);
            state.moveTo(next.nodeID());
            current = next.nodeID();
        }
    }

    private void recordNeighbours(ExplorationState state, long current,
                                   Map<Long, Collection<NodeStatus>> known,
                                   Map<Long, Long> discoveredVia,
                                   PriorityQueue<NodeStatus> frontier) {
        Collection<NodeStatus> neighbours = state.getNeighbours();
        known.put(current, neighbours);

        for (NodeStatus neighbour : neighbours) {
            long id = neighbour.nodeID();
            if (!known.containsKey(id) && !discoveredVia.containsKey(id)) {
                discoveredVia.put(id, current);
                frontier.add(neighbour);
            }
        }
    }

    private void walk(ExplorationState state, long from, long to,
                       Map<Long, Collection<NodeStatus>> known) {
        for (long step : shortestKnownPath(from, to, known)) {
            state.moveTo(step);
        }
    }

    private List<Long> shortestKnownPath(long from, long to, Map<Long, Collection<NodeStatus>> known) {
        if (from == to) {
            return List.of();
        }

        Map<Long, Long> cameFrom = new HashMap<>();
        cameFrom.put(from, from);
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(from);

        while (!queue.isEmpty()) {
            long node = queue.poll();
            if (node == to) {
                break;
            }
            for (NodeStatus neighbour : known.getOrDefault(node, List.of())) {
                long id = neighbour.nodeID();
                if (known.containsKey(id) && !cameFrom.containsKey(id)) {
                    cameFrom.put(id, node);
                    queue.add(id);
                }
            }
        }

        LinkedList<Long> path = new LinkedList<>();
        long node = to;
        while (node != from) {
            path.addFirst(node);
            Long previous = cameFrom.get(node);
            if (previous == null) {
                throw new IllegalStateException("No known route from " + from + " to " + to);
            }
            node = previous;
        }
        return path;
    }

    /**
     * Escape from the cavern before the ceiling collapses, trying to collect as much
     * gold as possible along the way. Your solution must ALWAYS escape before time runs
     * out, and this should be prioritized above collecting gold.
     * <p>
     * You now have access to the entire underlying graph, which can be accessed through EscapeState.
     * getCurrentNode() and getExit() will return you Node objects of interest, and getVertices()
     * will return a collection of all nodes on the graph.
     * <p>
     * Note that time is measured entirely in the number of steps taken, and for each step
     * the time remaining is decremented by the weight of the edge taken. You can use
     * getTimeRemaining() to get the time still remaining, pickUpGold() to pick up any gold
     * on your current tile (this will fail if no such gold exists), and moveTo() to move
     * to a destination node adjacent to your current node.
     * <p>
     * You must return from this function while standing at the exit. Failing to do so before time
     * runs out or returning from the wrong location will be considered a failed run.
     * <p>
     * You will always have enough time to escape using the shortest path from the starting
     * position to the exit, although this will not collect much gold.
     *
     * @param state the information available at the current state
     */
    public void escape(EscapeState state) {
        //TODO: Escape from the cavern before time runs out. This is not required for individual assignment
    }
}
