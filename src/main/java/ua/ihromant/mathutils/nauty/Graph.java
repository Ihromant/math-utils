package ua.ihromant.mathutils.nauty;

import java.util.List;

public interface Graph {
    /**
     * @return The graph's size in nodes.
     */
    int size();

    List<List<NautyNode>> partition();
}
