package ua.ihromant.mathutils.nauty;

import java.util.Collection;

/**
 *
 * @author Peter
 *
 * @param <L>
 * @param <T>
 */
public interface UTNode<L, T> extends UNode<L>, TNode<L, T> {
    public Collection<? extends UTNode<L, T>> neighbors();

    /**
     * <p>Connects this node to another node. </p>
     * <p>
     * The only prescription is that if this method succeeds, the other node
     * shows up in this nodes' {@link neighbours()}</p>
     * <p>
     * The particulars of the connection  are not prescribed by this interface,
     * nor does this interface prescribe what should happen when the connection
     * already exists. </p>
     *
     * @param other
     */
    public L label();

    /**
     * The index of the node in the graph to which it belongs
     * @return
     */
    public int index();
}
