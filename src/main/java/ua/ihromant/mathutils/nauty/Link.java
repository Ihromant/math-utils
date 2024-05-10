package ua.ihromant.mathutils.nauty;

/**
 * Represents a link in a graph.
 *
 * @author Peter
 *
 * @param <L>
 */
public interface Link<L> {
    /**
     * Returns the first node, after one occurrence of the given
     * node is ignored. If this link link the same node, that node is returned.
     *
     * @param current
     * @return
     */
    public Node<L> other(Node<L> current);
}
