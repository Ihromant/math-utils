package ua.ihromant.mathutils.nauty;

import java.util.List;

public interface UGraph<L> extends Graph<L>
{
    @Override
    public List<? extends UNode<L>> nodes();
}