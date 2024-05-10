package ua.ihromant.mathutils.nauty;

import java.util.Collection;

public interface Node {
    /**
     * The neighbors of this node. Each node occurs once, even if there are
     * multiple links.
     * Will include the node itself if there are multiple links.
     */
    Collection<? extends Node> neighbors();

    int label();

    boolean connected(Node other);

    /**
     * The index of the node in the graph to which it belongs
     */
    int index();
}
