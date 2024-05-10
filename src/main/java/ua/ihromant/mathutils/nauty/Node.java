package ua.ihromant.mathutils.nauty;

import java.util.Collection;

public interface Node<L>
{
    /**
     * The neighbors of this node. Each node occurs once, even if there are
     * multiple links.
     *
     * Will include the node itself if there are multiple links.
     *
     * @return
     */
    public Collection<? extends Node<L>> neighbors();

    public L label();

    public boolean connected(Node<L> other);

    /**
     * The index of the node in the graph to which it belongs
     * @return
     */
    public int index();
}
