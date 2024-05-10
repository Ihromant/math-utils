package ua.ihromant.mathutils.nauty;

import java.util.Collection;
import java.util.List;

public interface UGraph<L> extends Graph<L>
{

    @Override
    public Collection<? extends UNode<L>> nodes(L label);

    @Override

    public List<? extends UNode<L>> nodes();

    @Override
    public UNode<L> get(int i);
}