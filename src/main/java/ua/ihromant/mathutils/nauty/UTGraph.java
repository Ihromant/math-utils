package ua.ihromant.mathutils.nauty;

import java.util.List;
import java.util.Set;

/**
 * A directed graph with both nodes and links labeled. The labels of graph links are
 * called tags. TGraph is short for Tagged Graph
 *
 * @author Peter
 *
 * @param <L>
 * @param <T>
 */
public interface UTGraph<L, T> extends TGraph<L, T>, UGraph<L>
{
    /**
     * Returns the first node in the Graph which has the given label
     *
     * @param label
     * @return
     */
    public UTNode<L, T> node(L label);

    public Set<? extends UTNode<L, T>> nodes(L label);

    public List<? extends UTNode<L, T>> nodes();

    @Override
    public UTNode<L, T> get(int i);

    public Iterable<? extends UTLink<L, T>> links();

    /**
     * Adds a new node with the given label
     */
    public UTNode<L, T> add(L label);

}
