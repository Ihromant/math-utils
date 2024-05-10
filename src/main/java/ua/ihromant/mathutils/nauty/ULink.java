package ua.ihromant.mathutils.nauty;

public interface ULink<L> extends Link<L> {

    /**
     * Returns the first node, after one occurrence of the given
     * node is ignored. If this link link the same node, that node is returned.
     *
     * @param current
     * @return
     */
    public UNode<L> other(Node<L> current);
}
