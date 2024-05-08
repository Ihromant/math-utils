package ua.ihromant.mathutils.vf2;

public interface IsomorphismTester {
    /**
     * Returns {@code true} if the graphs are isomorphism of each other.
     */
    boolean areIsomorphic(Graph g1, Graph g2);
}
