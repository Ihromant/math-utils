package ua.ihromant.mathutils.vf2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link State} implementation for testing isomorphism using the VF2
 * algorithm's logic.  Note that this implementation requires that the graphs
 * have contiguous vertex indices (beginning at 0 to {@code g.order()}-1.
 *
 * <p>This implementation is based on the vf2_state implemenation in VFLib.
 */
public class VF2State implements State {

    /**
     * The first graph being compared
     */
    private final Graph g1;

    /**
     * The second graph being compared
     */
    private final Graph g2;

    /**
     * The number of nodes currently being matched betwen g1 and g3
     */
    int coreLen;

    /**
     * The number of nodes that were matched prior to this current pair being
     * added, which is used in backtracking.
     */
    int origCoreLen;

    /**
     * The node in g1 that was most recently added.
     */
    int addedNode1;

    // State information
    int t1bothLen, t2bothLen, t1inLen, t1outLen,
            t2inLen, t2outLen; // Core nodes are also counted by these...

    int[] core1;
    int[] core2;
    int[] in1;
    int[] in2;
    int[] out1;
    int[] out2;

    int[] order;

    /**
     * The number of nodes in {@code g1}
     */
    private final int n1;

    /**
     * The number of nodes in {@code g2}
     */
    private final int n2;

    /**
     * Creates a new {@code VF2State} with an empty mapping between the two
     * graphs.
     */
    public VF2State(Graph g1, Graph g2) {
        this.g1 = g1;
        this.g2 = g2;

        n1 = g1.order();
        n2 = g2.order();

        order = null;

        coreLen = 0;
        origCoreLen = 0;
        t1bothLen = 0;
        t1inLen = 0;
        t1outLen = 0;
        t2bothLen = 0;
        t2inLen = 0;
        t2outLen = 0;

        addedNode1 = NULL_NODE;

        core1 = new int[n1];
        core2 = new int[n2];
        in1 = new int[n1];
        in2 = new int[n2];
        out1 = new int[n1];
        out2 = new int[n2];

        Arrays.fill(core1, NULL_NODE);
        Arrays.fill(core2, NULL_NODE);

    }

    protected VF2State(VF2State copy) {
        g1 = copy.g1;
        g2 = copy.g2;
        coreLen = copy.coreLen;
        origCoreLen = copy.origCoreLen;
        t1bothLen = copy.t1bothLen;
        t2bothLen = copy.t2bothLen;
        t1inLen = copy.t1inLen;
        t2inLen = copy.t2inLen;
        t1outLen = copy.t1outLen;
        t2outLen = copy.t2outLen;
        n1 = copy.n1;
        n2 = copy.n2;

        addedNode1 = NULL_NODE;

        // NOTE: we don't need to copy these arrays because their state restored
        // via the backTrack() function after processing on the cloned state
        // finishes
        core1 = copy.core1;
        core2 = copy.core2;
        in1 = copy.in1;
        in2 = copy.in2;
        out1 = copy.out1;
        out2 = copy.out2;
        order = copy.order;
    }

    public IntPair nextPair(int prevN1, int prevN2) {
        if (prevN1 == NULL_NODE)
            prevN1 = 0;

        if (prevN2 == NULL_NODE)
            prevN2 = 0;
        else
            prevN2++;

        if (t1bothLen>coreLen && t2bothLen > coreLen) {
            while (prevN1 < n1 && (core1[prevN1] != NULL_NODE
                    || out1[prevN1]==0
                    || in1[prevN1]==0)) {
                prevN1++;
                prevN2 = 0;
            }
        }
        else if (t1outLen>coreLen && t2outLen>coreLen) {
            while (prevN1<n1 &&
                    (core1[prevN1]!=NULL_NODE || out1[prevN1]==0)) {
                prevN1++;
                prevN2=0;
            }
        }
        else if (t1inLen>coreLen && t2inLen>coreLen) {
            while (prevN1<n1 &&
                    (core1[prevN1]!=NULL_NODE || in1[prevN1]==0)) {
                prevN1++;
                prevN2=0;
            }
        }
        else if (prevN1 == 0 && order != null) {
            int i=0;
            while (i < n1 && core1[prevN1=order[i]] != NULL_NODE)
                i++;
            if (i == n1)
                prevN1=n1;
        }
        else {
            while (prevN1 < n1 && core1[prevN1] != NULL_NODE ) {
                prevN1++;
                prevN2=0;
            }
        }

        if (t1bothLen>coreLen && t2bothLen>coreLen) {
            while (prevN2<n2 && (core2[prevN2]!=NULL_NODE
                    || out2[prevN2]==0
                    || in2[prevN2]==0)) {
                prevN2++;
            }
        }
        else if (t1outLen>coreLen && t2outLen>coreLen) {
            while (prevN2 < n2 && (core2[prevN2] != NULL_NODE
                    || out2[prevN2] == 0)) {
                prevN2++;
            }
        }
        else if (t1inLen>coreLen && t2inLen>coreLen) {
            while (prevN2 < n2 && (core2[prevN2] != NULL_NODE
                    || in2[prevN2] == 0)) {
                prevN2++;
            }
        }
        else {
            while (prevN2 < n2 && core2[prevN2] != NULL_NODE) {
                prevN2++;
            }
        }

//         System.out.printf("prevN1: %d, prevN2: %d%n", prevN1, prevN2);
        if (prevN1 < n1 && prevN2 < n2)
            return new IntPair(prevN1, prevN2);
        else
            return null;

    }

    /**
     * {@inheritDoc}
     */
    public boolean isFeasiblePair(int node1, int node2) {
        assert node1 < n1;
        assert node2 < n2;
        assert core1[node1] == NULL_NODE;
        assert core2[node2] == NULL_NODE;

        // TODO: add checks for compatible nodes here

        // int i = 0;// , other1 = 0, other2 = 0;
        int termout1=0, termout2=0, termin1=0, termin2=0, new1=0, new2=0;

        // Check the 'in' edges of node1
        for (int other1 : getPredecessors(g1, node1)) {
            if (core1[other1] != NULL_NODE) {
                int other2 = core1[other1];
                // If there's node edge to the other node, or if there is some
                // edge incompatability, then the mapping is not feasible
                if (!g2.contains(other2, node2))
                    return false;
            }
            else {
                if (in1[other1] != 0)
                    termin1++;
                if (out1[other1] != 0)
                    termout1++;
                if (in1[other1] == 0 && out1[other1] == 0)
                    new1++;
            }
        }

        // Check the 'in' edges of node2
        for (int other2 : getPredecessors(g2, node2)) {
            if (core2[other2] != NULL_NODE) {
                int other1 = core2[other2];
                if (!g1.contains(other1, node1))
                    return false;
            }

            else {
                if (in2[other2] != 0)
                    termin2++;
                if (out2[other2] != 0)
                    termout2++;
                if (in2[other2] == 0 && out2[other2] == 0)
                    new2++;
            }
        }

        return termin1 == termin2 && termout1 == termout2 && new1 == new2;
    }

    /**
     * {@inheritDoc}
     */
    public void addPair(int node1, int node2) {
        assert node1 < n1;
        assert node2 < n2;
        assert coreLen < n1;
        assert coreLen < n2;

        coreLen++;
        addedNode1 = node1;

        if (in1[node1] == 0) {
            in1[node1] = coreLen;
            t1inLen++;
            if (out1[node1] != 0)
                t1bothLen++;
        }
        if (out1[node1] == 0) {
            out1[node1]=coreLen;
            t1outLen++;
            if (in1[node1] != 0)
                t1bothLen++;
        }

        if (in2[node2] == 0) {
            in2[node2]=coreLen;
            t2inLen++;
            if (out2[node2] != 0)
                t2bothLen++;
        }
        if (out2[node2] == 0) {
            out2[node2]=coreLen;
            t2outLen++;
            if (in2[node2] != 0)
                t2bothLen++;
        }

        core1[node1] = node2;
        core2[node2] = node1;

        for (int other : getPredecessors(g1, node1)) {
            if (in1[other] == 0) {
                in1[other] = coreLen;
                t1inLen++;
                if (out1[other] != 0)
                    t1bothLen++;
            }
        }

        for (int other : getPredecessors(g2, node2)) {
            if (in2[other] == 0) {
                in2[other]=coreLen;
                t2inLen++;
                if (out2[other] != 0)
                    t2bothLen++;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isGoal() {
        return coreLen == n1 && coreLen == n2;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDead() {
        return n1 != n2
                || t1bothLen != t2bothLen
                || t1outLen != t2outLen
                || t1inLen != t2inLen;
    }

    /**
     * {@inheritDoc}
     */
    public Map<Integer,Integer> getVertexMapping() {
        Map<Integer,Integer> vertexMapping = new HashMap<Integer,Integer>();
        for (int i = 0; i < n1; ++i) {
            if (core1[i] != NULL_NODE) {
                vertexMapping.put(i, core1[i]);
            }
        }
        return vertexMapping;
    }

    /**
     * {@inheritDoc}
     */
    public VF2State copy() {
        return new VF2State(this);
    }

    /**
     * {@inheritDoc}
     */
    public void backTrack() {
        //assert coreLen - origCoreLen <= 1;
        assert addedNode1 != NULL_NODE;

        if (origCoreLen < coreLen) {
            int node2;

            if (in1[addedNode1] == coreLen)
                in1[addedNode1] = 0;

            for (int other : getPredecessors(g1, addedNode1)) {
                if (in1[other]==coreLen)
                    in1[other]=0;
            }

            if (out1[addedNode1] == coreLen)
                out1[addedNode1] = 0;

            node2 = core1[addedNode1];

            if (in2[node2] == coreLen)
                in2[node2] = 0;

            for (int other : getPredecessors(g2, node2)) {
                if (in2[other]==coreLen)
                    in2[other]=0;
            }

            if (out2[node2] == coreLen)
                out2[node2] = 0;

            core1[addedNode1] = NULL_NODE;
            core2[node2] = NULL_NODE;

            coreLen = origCoreLen;
            addedNode1 = NULL_NODE;
        }
    }

    /**
     * Returns those vertices that point to from {@code vertex} or all the
     * adjacent vertices if {@code g} is not a directed graph.
     */
    private int[] getPredecessors(Graph g, Integer vertex) {
        return g.getNeighbors(vertex);
    }
}
