package ua.ihromant.mathutils.nauty;

import java.util.List;

public interface Graph {
    List<NautyNode> nodes();

    /**
     * @return The graph's size in nodes.
     */
    int size();
}
