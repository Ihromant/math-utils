package ua.ihromant.mathutils.vf2;

public interface Graph {
    /**
     * Returns {@code true} if this graph contains an edge between {@code from}
     * and {@code to}.  Implementations are free to define whether the ordering
     * of the vertices matters.
     */
    boolean contains(int from, int to);

    /**
     * Returns the set of vertices that are connected to the specified vertex,
     * or an empty set if the vertex is not in this graph.
     */
    int[] getNeighbors(int vertex);

    /**
     * Returns the number of vertices in this graph.
     */
    int order();
}
