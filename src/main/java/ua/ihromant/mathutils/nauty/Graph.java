package ua.ihromant.mathutils.nauty;

import java.util.List;

public interface Graph {
    List<? extends Node> nodes();

    /**
     * @return The graph's size in nodes.
     */
    int size();
}
