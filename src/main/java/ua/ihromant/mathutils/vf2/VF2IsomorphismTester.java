package ua.ihromant.mathutils.vf2;

/**
 * An implementation of the VF2 algorithm for detecting isomorphic graphs.  This
 * algorithm may be found in:
 * <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> Luigi P. Cordella,
 *      Pasquale Foggia, Carlo Sansone, and Mario Vento.  A (Sub)Graph
 *      Isomorphism Algorithm for Matching Large Graphs.  <i>IEEE Transactions
 *      on Pattern Analysis and Machine Intelligence,</i> <b>26:10</b>.  2004.
 *      Available <a
 *      href="http://ieeexplore.ieee.org/xpls/abs_all.jsp?arnumber=1323804">here</a>
 *
 * </ul> This implementation will test that the number of edges between two
 * vertices are equivalent.  However, the implementation does not test that the
 * types are equivalent.  In essence, <b>this class checks only for structural
 * equivalence</b>, independent of any addition properties on the nodes or
 * edges.
 *
 * <p>This implementation is an adaptation of the VFLib implementation.
 *
 * <p>This class is thread-safe.
 *
 * @author David Jurgens
 */
public class VF2IsomorphismTester extends AbstractIsomorphismTester {
    /**
     * Creates a new {@code VF2IsomorphismTester} instance
     */
    public VF2IsomorphismTester() { }

    /**
     * Returns a new {@code State} for running the VF2 algorithm.
     */
    protected State makeInitialState(Graph g1, Graph g2) {
        return new VF2State(g1, g2);
    }
}
