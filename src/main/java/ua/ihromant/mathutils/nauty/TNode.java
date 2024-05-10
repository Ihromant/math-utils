package ua.ihromant.mathutils.nauty;

import java.util.Collection;

/**
 *
 * @author Peter
 *
 * @param <L>
 * @param <T>
 */
public interface TNode<L, T> extends Node<L>
{
    public Collection<? extends TNode<L, T>> neighbors();

    public Collection<? extends TLink<L, T>> links(Node<L> other);

    /**
     * The index of the node in the graph to which it belongs
     * @return
     */
    public int index();
}
