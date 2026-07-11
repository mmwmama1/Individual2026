package student;

import game.Edge;
import game.EscapeState;
import game.ExplorationState;
import game.Node;
import game.NodeStatus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

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
     * <p>
     * <b>Strategy used here.</b> Every tile that has been physically visited has its
     * neighbours recorded, so we always know a subgraph of the cavern that has actually
     * been walked (the "known graph"). Every neighbour that has been seen but not yet
     * visited is a candidate destination on the "frontier". At each step we greedily pick
     * the frontier tile with the smallest {@code getDistanceToTarget()} (the tile that,
     * ignoring walls, is closest to the Orb) and walk to it, backtracking through the
     * known graph if it is not directly adjacent to our current position. Because
     * {@code getDistanceToTarget()} is an admissible heuristic (real graph distance can
     * only ever be greater than or equal to grid distance, since walls only add detours),
     * this greedy best-first strategy consistently steers the explorer towards the Orb
     * and only backtracks when a dead end has been fully exhausted.
     *
     * @param state the information available at the current state
     */
    public void explore(ExplorationState state) {
        // known.get(id) is non-null exactly for tiles we have physically stood on;
        // it stores the neighbour information we saw while standing there.
        Map<Long, Collection<NodeStatus>> known = new HashMap<>();

        // For every tile that has been discovered (as a neighbour of a visited tile)
        // but not yet visited, remember one already-visited tile adjacent to it, so
        // that we know how to walk there when it is chosen.
        Map<Long, Long> discoveredVia = new HashMap<>();

        // Tiles that have been discovered but not yet visited, ordered by their
        // (admissible) grid distance to the Orb - NodeStatus is Comparable on this value.
        PriorityQueue<NodeStatus> frontier = new PriorityQueue<>();

        long current = state.getCurrentLocation();
        known.put(current, List.of()); // placeholder; filled in on the first iteration below

        while (state.getDistanceToTarget() != 0) {
            recordNeighbours(state, current, known, discoveredVia, frontier);

            NodeStatus next = frontier.poll();
            while (next != null && known.containsKey(next.nodeID())) {
                // Stale entry: this tile turned out to be reachable from more than one
                // direction and was already visited via a different route. Skip it.
                next = frontier.poll();
            }
            if (next == null) {
                // Should never happen: the cavern is always fully connected, so the
                // Orb is always reachable and the frontier cannot run dry first.
                throw new IllegalStateException("Explorer ran out of places to go before finding the Orb.");
            }

            long via = discoveredVia.get(next.nodeID());
            walk(state, current, via, known);
            state.moveTo(next.nodeID());
            current = next.nodeID();
        }
    }

    /**
     * Record the neighbours of the tile we are currently standing on ({@code current})
     * into {@code known}, and add any newly-discovered neighbour to {@code frontier}
     * (remembering, in {@code discoveredVia}, that it can be reached via {@code current}).
     */
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

    /**
     * Walk from {@code from} to {@code to} using only tiles already recorded in
     * {@code known} (i.e. without discovering anything new), by finding the shortest
     * such route with a breadth-first search and then following it one step at a time.
     * Does nothing if {@code from} equals {@code to}.
     */
    private void walk(ExplorationState state, long from, long to,
                       Map<Long, Collection<NodeStatus>> known) {
        for (long step : shortestKnownPath(from, to, known)) {
            state.moveTo(step);
        }
    }

    /**
     * Return the sequence of tile IDs to move through (in order, not including
     * {@code from} itself) to travel from {@code from} to {@code to}, using only
     * edges between tiles already present in {@code known}. Returns an empty list
     * if {@code from} equals {@code to}.
     */
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
                // Should never happen: "to" is always a tile we have already visited,
                // so it must be reachable from "from" through the known graph.
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
     * <p>
     * <b>Strategy used here.</b> Pre-compute weighted shortest-path distances from the
     * exit to every node (Dijkstra). Each step: pick up gold on the current tile; then
     * either detour toward the highest-value gold pile that can still be reached and
     * escaped from within the time limit, or otherwise take a safe neighbour on a shortest
     * path to the exit.
     *
     * @param state the information available at the current state
     */
    public void escape(EscapeState state) {
        Node exit = state.getExit();
        Collection<Node> vertices = state.getVertices();
        Map<Long, Integer> distToExit = dijkstraDistances(exit);

        Node current = state.getCurrentNode();
        while (!current.equals(exit)) {
            collectGoldIfPresent(state);

            Node next = chooseEscapeMove(state, current, distToExit, vertices);
            state.moveTo(next);
            current = state.getCurrentNode();
        }

        collectGoldIfPresent(state);
    }

    private void collectGoldIfPresent(EscapeState state) {
        if (state.getCurrentNode().getTile().getGold() > 0) {
            state.pickUpGold();
        }
    }

    /**
     * Choose the next node to move to: prefer a profitable gold detour when safe,
     * otherwise follow a shortest weighted path toward the exit.
     */
    private Node chooseEscapeMove(EscapeState state, Node current,
                                  Map<Long, Integer> distToExit, Collection<Node> vertices) {
        int timeLeft = state.getTimeRemaining();
        Map<Long, Integer> distFromCurrent = dijkstraDistances(current);

        Node bestGoldTarget = null;
        int bestGold = 0;
        for (Node vertex : vertices) {
            int gold = vertex.getTile().getGold();
            if (gold <= 0) {
                continue;
            }
            Integer toGold = distFromCurrent.get(vertex.getId());
            Integer fromGold = distToExit.get(vertex.getId());
            if (toGold == null || fromGold == null) {
                continue;
            }
            if (toGold + fromGold <= timeLeft && gold > bestGold) {
                bestGold = gold;
                bestGoldTarget = vertex;
            }
        }

        if (bestGoldTarget != null) {
            Node step = firstStepOnShortestPath(current, bestGoldTarget);
            if (step != null && isSafeMove(state, current, step, distToExit)) {
                return step;
            }
        }

        Node bestNeighbour = null;
        int bestDist = Integer.MAX_VALUE;
        for (Node neighbour : current.getNeighbours()) {
            if (!isSafeMove(state, current, neighbour, distToExit)) {
                continue;
            }
            int dist = distToExit.getOrDefault(neighbour.getId(), Integer.MAX_VALUE);
            if (dist < bestDist) {
                bestDist = dist;
                bestNeighbour = neighbour;
            }
        }

        if (bestNeighbour == null) {
            throw new IllegalStateException("No safe move toward the exit with time remaining.");
        }
        return bestNeighbour;
    }

    /**
     * Return true if moving from {@code from} to {@code to} leaves enough time to reach the exit.
     */
    private boolean isSafeMove(EscapeState state, Node from, Node to, Map<Long, Integer> distToExit) {
        int edgeCost = from.getEdge(to).length();
        int timeAfterMove = state.getTimeRemaining() - edgeCost;
        Integer remainingPath = distToExit.get(to.getId());
        return remainingPath != null && timeAfterMove >= remainingPath;
    }

    /**
     * Return the first step on a shortest weighted path from {@code from} to {@code to}.
     */
    private Node firstStepOnShortestPath(Node from, Node to) {
        if (from.equals(to)) {
            return null;
        }

        Map<Long, Node> predecessor = new HashMap<>();
        Map<Long, Integer> dist = new HashMap<>();
        PriorityQueue<Node> frontier = new PriorityQueue<>(
                Comparator.comparingInt(n -> dist.getOrDefault(n.getId(), Integer.MAX_VALUE)));

        dist.put(from.getId(), 0);
        frontier.add(from);
        Set<Long> settled = new HashSet<>();

        while (!frontier.isEmpty()) {
            Node node = frontier.poll();
            if (!settled.add(node.getId())) {
                continue;
            }
            if (node.equals(to)) {
                break;
            }

            int nodeDist = dist.get(node.getId());
            for (Edge edge : node.getExits()) {
                Node neighbour = edge.getOther(node);
                int newDist = nodeDist + edge.length();
                if (newDist < dist.getOrDefault(neighbour.getId(), Integer.MAX_VALUE)) {
                    dist.put(neighbour.getId(), newDist);
                    predecessor.put(neighbour.getId(), node);
                    frontier.add(neighbour);
                }
            }
        }

        if (!predecessor.containsKey(to.getId()) && !from.equals(to)) {
            return null;
        }

        Node step = to;
        while (predecessor.get(step.getId()) != null && !predecessor.get(step.getId()).equals(from)) {
            step = predecessor.get(step.getId());
        }
        return step;
    }

    /**
     * Run Dijkstra's algorithm from {@code source} and return minimum weighted distances
     * to every reachable node (keyed by node id).
     */
    private Map<Long, Integer> dijkstraDistances(Node source) {
        Map<Long, Integer> dist = new HashMap<>();
        PriorityQueue<Node> frontier = new PriorityQueue<>(
                Comparator.comparingInt(n -> dist.getOrDefault(n.getId(), Integer.MAX_VALUE)));
        Set<Long> settled = new HashSet<>();

        dist.put(source.getId(), 0);
        frontier.add(source);

        while (!frontier.isEmpty()) {
            Node node = frontier.poll();
            if (!settled.add(node.getId())) {
                continue;
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

        return dist;
    }
}