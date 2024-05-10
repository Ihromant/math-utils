package ua.ihromant.mathutils.nauty;

public interface UTLink<L, T> extends TLink<L, T>, ULink<L> {
    /**
     * Returns the first node, after one occurrence of the given
     * node is ignored. If this link link the same node, that node is returned.
     *
     * @param current
     * @return
     */
    public UTNode<L, T> other(Node<L> current);
}
