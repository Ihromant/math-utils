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

    public Set<? extends UTNode<L, T>> nodes(L label);

    public List<? extends UTNode<L, T>> nodes();
}
