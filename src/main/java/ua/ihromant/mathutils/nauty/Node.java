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

    /**
     * Whether the current node is connected to the given node.
     *
     * If the graph is directed, a connection in either direction will cause
     * true to be returned.
     *
     * @param other
     * @return
     */
    public boolean connected(Node<L> other);

    /**
     * Returns all links between this node and the given other node.
     *
     * For directed graphs, this will return links in both directions.
     *
     * @param other
     * @return
     */
    public Collection<? extends Link<L>> links(Node<L> other);

    /**
     * Returns the graph object to which these nodes belong. Nodes always belong
     * to a single graph and cannot be exchanged between them. This is a very
     * important property for the correctness of the API.
     * @return
     */
    public Graph<L> graph();

    /**
     * The index of the node in the graph to which it belongs
     * @return
     */
    public int index();

    /**
     * Returns the degree of the node, ie. the number of connections to other
     * nodes. Note that this value will differ from neighbors.size() if there
     * are multiple links between this node and one of its neighbors.
     * @return
     */
    public int degree();
}
