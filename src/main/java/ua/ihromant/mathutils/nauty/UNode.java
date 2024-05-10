package ua.ihromant.mathutils.nauty;

import java.util.Collection;

public interface UNode<L> extends Node<L>
{
    @Override
    public Collection<? extends UNode<L>> neighbors();

    public Collection<? extends ULink<L>> links(Node<L> other);
}
