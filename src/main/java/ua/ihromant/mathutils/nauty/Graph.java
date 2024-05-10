package ua.ihromant.mathutils.nauty;

import java.util.List;

/**
 *
 *
 * Design choices:
 * <ul>
 * <li>A Graph is _not_ a Collection of Nodes, because the generic type cannot
 * be further restricted in the subclasses of graph. Eg. we cannot make DTGraph
 * a collection of DTnodes. thus the client must call nodes() to get a
 * collection of the graphs nodes. </li>
 * <li>To maintain the LSP, the subclasses of Node can be modified by all types
 * of node (if we restricted this to subclasses of nodes we would be restricting
 * preconditions in a subclass). In practice, all implementations have a single
 * type of node and only allow that type, but that rule is not enforced at the
 * API level.</li>
 * </ul>
 *
 * <h2>A note on equality and inheritance</h2>
 * <p>
 * Our choice to use inheritance in the definition of graphs presents a problem
 * with the implementation of equals(). Consider the following: a DTGraph
 * implementation considers another graph equal to itself if its nodes, links,
 * labels and tags match. For a DGraph implementation, there are no tags, so
 * only the nodes, links and labels are checked. The second considers itself
 * equal to the first, but not vice versa. The DGraph has no way of knowing that
 * the DTGraph has extended functionality, since it isn't aware that such
 * functionality exists.
 * </p><p>
 * The solution is that equality is only defined over a single level of the
 * hierarchy. Each graph reports not only which graphtypes it inherits from
 * (through interfaces), but also which level of the graph hierarchy it
 * identifies with. This can be either an interface, or a class, but for two
 * graphs to be equal, their level must match.
 * </p>
 *
 * TODO: Make numLinks() return a long, and make {links()} return an iterable.
 *
 *
 * @param <L>
 */
public interface Graph<L> {
    public List<? extends Node<L>> nodes();

    /**
     * @return The graph's size in nodes.
     */
    public int size();

    public long numLinks();
}
