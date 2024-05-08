package ua.ihromant.mathutils.vf2;

import java.util.Map;

import static ua.ihromant.mathutils.vf2.State.NULL_NODE;

/**
 * An abstraction of an {@link IsomorphismTester} that relies on
 * algorithm-specific {@link State} implementations to check for edge and vertex
 * contraints when performing isomorphism testing.
 *
 * <p> This class is a an adaptation of parts of the VFLib library.
 */
public abstract class AbstractIsomorphismTester implements IsomorphismTester {

    /**
     * {@inheritDoc}
     */
    public boolean areIsomorphic(Graph g1, Graph g2) {
        State state = makeInitialState(g1, g2);
        return match(state);
    }

    /**
     * Creates an empty {@link State} for mapping the vertices of {@code g1} to
     * {@code g2}.
     */
    protected abstract State makeInitialState(Graph g1, Graph g2);

    /**
     * Returns {@code true} if the graphs being matched by this state are
     * isomorphic.
     */
    private boolean match(State s) {
        if (s.isGoal())
            return true;

        if (s.isDead())
            return false;

        int n1 = NULL_NODE, n2 = NULL_NODE;
        IntPair next = null;
        boolean found = false;
        while (!found && (next = s.nextPair(n1, n2)) != null) {
            n1 = next.fst();
            n2 = next.snd();
            if (s.isFeasiblePair(n1, n2)) {
                State copy = s.copy();
                copy.addPair(n1, n2);
                found = match(copy);
                // If we found a match, then don't bother backtracking as it
                // would be wasted effort.
                if (!found)
                    copy.backTrack();
            }
        }
        return found;
    }

    /**
     * Returns {@code true} if the graphs being matched by this state are
     * isomorphic.
     */
    private boolean match(State s, Map<Integer,Integer> isoMap) {
        if (s.isGoal())
            return true;

        if (s.isDead())
            return false;

        int n1 = NULL_NODE, n2 = NULL_NODE;
        IntPair next = null;
        boolean found = false;
        while (!found && (next = s.nextPair(n1, n2)) != null) {
            n1 = next.fst();
            n2 = next.snd();
            if (s.isFeasiblePair(n1, n2)) {
                State copy = s.copy();
                copy.addPair(n1, n2);
                found = match(copy, isoMap);
                // If we found a mapping, fill the vertex mapping state
                if (found)
                    isoMap.putAll(copy.getVertexMapping());
                    // Otherwise, back track and try again
                else
                    copy.backTrack();
            }
        }
        return found;
    }
}
