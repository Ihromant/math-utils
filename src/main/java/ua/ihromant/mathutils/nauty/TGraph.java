package ua.ihromant.mathutils.nauty;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A graph with both nodes and links labeled. The labels of graph links are
 * called tags. TGraph is short for Tagged Graph
 *
 * @author Peter
 *
 * @param <L> The label type
 * @param <T> The tag type
 */
public interface TGraph<L, T> extends Graph<L>
{

    public Collection<? extends TNode<L, T>> nodes(L label);

    public List<? extends TNode<L, T>> nodes();

    public Iterable<? extends TLink<L, T>> links();

    /**
     * Adds a new node with the given label
     */
    public TNode<L, T> add(L label);

    public long numLinks();
}
