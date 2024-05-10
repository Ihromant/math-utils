package ua.ihromant.mathutils.nauty;

import nl.peterbloem.kit.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Nauty {
    /**
     * Find the canonical ordering for the given graph
     */
    private static Order order(Graph graph) {
        // * Start with the unit partition
        List<List<NautyNode>> partition = partition(graph);

        // * The equitable refinement procedure.
        partition = refine(partition);

        // * Start the search for the maximal isomorph
        Search search = new Search(partition);
        search.search();

        List<Integer> order = new ArrayList<>(graph.size());
        List<List<NautyNode>> max = search.max();

        for(List<NautyNode> cell : max) {
            assert(cell.size() == 1);

            NautyNode node = cell.getFirst();
            order.add(node.index());
        }

        assert(order.size() == graph.size());

        return new Order(order).inverse();
    }

    /**
     * This object encapsulates the information in a single search.
     */
    private static class Search {
        private final Deque<SNode> buffer = new LinkedList<>();

        private SNode max = null;
        private String maxString;

        public Search(List<List<NautyNode>> initial)
        {
            // ** Set up the search stack
            buffer.add(new SNode(initial));
        }

        public void search()
        {
            while(! buffer.isEmpty())
            {
                SNode current = buffer.poll();

                List<SNode> children = current.children();
                if(children.isEmpty())
                    observe(current);

                for(SNode child : children)
                    buffer.addFirst(child);
            }
        }

        private void observe(SNode node)
        {
            String nodeString = Nauty.toString(node.partition());

            if(max == null || nodeString.compareTo(maxString) > 0)
            {
                max = node;
                maxString = nodeString;
            }
        }

        public List<List<NautyNode>> max()
        {
            return max.partition();
        }
    }

    public static List<List<NautyNode>> partition(Graph graph) {
        Map<Integer, List<NautyNode>> byLabel = new LinkedHashMap<>();
        for(NautyNode node : graph.nodes())
        {
            if(!byLabel.containsKey(node.label()))
                byLabel.put(node.label(), new ArrayList<>());
            byLabel.get(node.label()).add(node);
        }

        List<Integer> keys = new ArrayList<>(byLabel.keySet());
        Collections.sort(keys);

        List<List<NautyNode>> result = new ArrayList<>();

        for(Integer key : keys)
            result.add(byLabel.get(key));
        return result;
    }

    /**
     * Refine the given partition to the coarsest equitable refinement.
     */
    public static List<List<NautyNode>> refine(List<List<NautyNode>> partition)
    {
        List<List<NautyNode>> result = new ArrayList<>();
        for(List<NautyNode> cell : partition)
            result.add(new ArrayList<>(cell));

        while(searchShattering(result));

        return result;
    }

    private static boolean searchShattering(List<List<NautyNode>> partition)
    {
        // * Loop through every pair of partition cells
        for(int i = 0; i < partition.size(); i++)
        {
            for(int j = 0; j < partition.size(); j++)
            {
                if(shatters(partition.get(i), partition.get(j)))
                {
                    List<List<NautyNode>> shattering = shattering(partition.get(i), partition.get(j));

                    // * This edit to the list we're looping over is safe,
                    //   because we return right after
                    partition.remove(i);

                    partition.addAll(i, shattering);

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Re-orders the nodes in 'from' by their degree relative to 'to'
     */
    public static List<List<NautyNode>> shattering(List<NautyNode> from, List<NautyNode> to)
    {
        Map<Integer, List<NautyNode>> byDegree = new LinkedHashMap<>();

        for(NautyNode node : from)
        {
            int degree = degree(node, to);

            if(!byDegree.containsKey(degree))
                byDegree.put(degree, new ArrayList<>());

            byDegree.get(degree).add(node);
        }

        List<Integer> keys = new ArrayList<>(byDegree.keySet());
        Collections.sort(keys);

        List<List<NautyNode>> result = new ArrayList<>();
        for(int key : keys)
            result.add(byDegree.get(key));

        return result;
    }

    /**
     * A set of nodes shatters another set of nodes, if the outdegree
     * relative to the second set differs between members of the first.
     */
    public static boolean shatters(List<NautyNode> from, List<NautyNode> to) {
        int num = -1;

        for(NautyNode node : from)
            if(num == -1)
                num = degree(node, to);
            else
            if(num != degree(node, to))
                return true;

        return false;
    }

    public static int degree(NautyNode from, List<NautyNode> to)
    {
        int sum = 0;

        for(NautyNode node : to) // * this should automatically work right for directed/undirected
            if (from.connected(node)) {
                sum++;
            }

        return sum;
    }

    private record SNode(List<List<NautyNode>> partition) {
        public List<SNode> children() {
                List<SNode> children = new ArrayList<>(partition.size() + 1);

                for (int cellIndex = 0; cellIndex < partition.size(); cellIndex++) {
                    List<NautyNode> cell = partition.get(cellIndex);
                    if (cell.size() > 1)
                        for (int nodeIndex = 0; nodeIndex < cell.size(); nodeIndex++) {
                            List<NautyNode> rest = new ArrayList<>(cell);
                            List<NautyNode> single = Collections.singletonList(rest.remove(nodeIndex));

                            // * Careful... We're shallow copying the cells. We must
                            //   make sure never to modify a cell.
                            List<List<NautyNode>> newPartition = new ArrayList<>(partition);

                            newPartition.remove(cellIndex);
                            newPartition.add(cellIndex, single);
                            newPartition.add(cellIndex + 1, rest);

                            children.add(new SNode(newPartition));
                        }
                }

                return children;
            }
        }

    /**
     * Converts a trivial partition to a string representing the graph's
     * structure (without labels) in a particular format.
     */
    private static String toString(List<List<NautyNode>> partition)
    {
        StringBuilder buffer = new StringBuilder();

        int[] order = new int[partition.size()];
        int i = 0;
        for(List<NautyNode> cell : partition) {
            order[cell.getFirst().index()] = i;
            i++;
        }

        for(List<NautyNode> cell : partition) {
            assert(cell.size() == 1);
            NautyNode current = cell.getFirst();

            List<Integer> neighbors = new ArrayList<>(current.neighbors().size());
            for(NautyNode neighbor : current.neighbors())
            {
                int rawIndex = neighbor.index(); // index in the original graph
                int neighborIndex = order[rawIndex]; // index in the re-ordered graph

                neighbors.add(neighborIndex);
            }

            Collections.sort(neighbors);
            for(int neighborIndex : neighbors)
                buffer.append(neighborIndex).append(' ');

            buffer.append(',');
        }

        return buffer.toString();
    }

    public static Order canonicalOrdering(Graph graph) {
        return Nauty.order(graph);
    }
}
