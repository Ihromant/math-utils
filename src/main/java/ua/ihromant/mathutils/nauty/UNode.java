package ua.ihromant.mathutils.nauty;

import java.util.Collection;

public interface UNode<L> extends Node<L>
{
    @Override
    public Collection<? extends UNode<L>> neighbors();

    public Collection<? extends ULink<L>> links(Node<L> other);

    /**
     * Returns the graph object to which these nodes belong. Nodes always belong
     * to a single graph and cannot be exchanged between them. This is a very
     * important property for the correctness of the API.
     * @return
     */
    public UGraph<L> graph();

}
