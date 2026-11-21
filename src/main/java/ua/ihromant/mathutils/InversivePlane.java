package ua.ihromant.mathutils;

import ua.ihromant.jnauty.GraphData;
import ua.ihromant.jnauty.JNauty;
import ua.ihromant.jnauty.NautyGraph;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.nauty.AutomorphismConsumer;
import ua.ihromant.mathutils.nauty.AutomorphismConsumerNew;
import ua.ihromant.mathutils.nauty.NautyAlgo;
import ua.ihromant.mathutils.nauty.NautyAlgoNew;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class InversivePlane implements NautyGraph {
    private final int pointCount;
    private final int[][] lines;
    private final FixBS[] flags;
    private final int[][][] lookup;
    private GraphData gd;

    public InversivePlane(int[][] lines) {
        this(Arrays.stream(lines).mapToInt(arr -> arr[arr.length - 1]).max().orElseThrow() + 1, lines);
    }

    public InversivePlane(int pointCount, int[][] lines) {
        this.pointCount = pointCount;
        this.lines = lines;
        this.flags = IntStream.range(0, lines.length).mapToObj(_ -> new FixBS(pointCount)).toArray(FixBS[]::new);
        for (int i = 0; i < lines.length; i++) {
            int[] line = lines[i];
            for (int pt : line) {
                flags[i].set(pt);
            }
        }
        this.lookup = generateLookup();
    }

    public InversivePlane(FixBS[] incidence) {
        this.flags = incidence;
        int pointCount = 0;
        int lineCount = flags.length;
        this.lines = new int[lineCount][];
        for (int i = 0; i < lineCount; i++) {
            FixBS ln = flags[i];
            int sz = ln.cardinality();
            int[] line = new int[sz];
            int idx = 0;
            for (int pt = ln.nextSetBit(0); pt >= 0; pt = ln.nextSetBit(pt + 1)) {
                line[idx++] = pt;
                pointCount = Math.max(pointCount, pt);
            }
            lines[i] = line;
        }
        pointCount = pointCount + 1;
        this.pointCount = pointCount;
        this.lookup = generateLookup();
    }

    private int[][][] generateLookup() {
        int[][][] result = new int[pointCount][pointCount][pointCount];
        for (int[][] p : result) {
            for (int[] p1 : p) {
                Arrays.fill(p1, -1);
            }
        }
        for (int l = 0; l < lines.length; l++) {
            int[] line = lines[l];
            for (int i = 0; i < line.length; i++) {
                int p1 = line[i];
                for (int j = i + 1; j < line.length; j++) {
                    int p2 = line[j];
                    for (int k = j + 1; k < line.length; k++) {
                        int p3 = line[k];
                        if (result[p1][p2][p3] >= 0) {
                            throw new IllegalStateException();
                        }
                        result[p1][p2][p3] = l;
                        result[p1][p3][p2] = l;
                        result[p2][p1][p3] = l;
                        result[p2][p3][p1] = l;
                        result[p3][p1][p2] = l;
                        result[p3][p2][p1] = l;
                    }
                }
            }
        }
        return result;
    }

    public InversivePlane(InversivePlane prev, int[] newLine) {
        this.pointCount = prev.pointCount;
        int pll = prev.lines.length;
        this.lines = new int[pll + 1][];
        System.arraycopy(prev.lines, 0, this.lines, 0, pll);
        this.lines[pll] = newLine;
        this.flags = new FixBS[pll + 1];
        System.arraycopy(prev.flags, 0, this.flags, 0, pll);
        this.flags[pll] = new FixBS(pointCount);
        for (int p : newLine) {
            flags[pll].set(p);
        }
        this.lookup = generateLookup(); // TODO it can be more performant probably
    }

    public InversivePlane(Inc inc) {
        this(inc.v(), lines(inc));
    }

    private static int[][] lines(Inc inc) {
        int k = 0;
        for (int i = 0; i < inc.v(); i++) {
            if (inc.inc(0, i)) {
                k++;
            }
        }
        int[][] lines = new int[inc.b()][k];
        for (int l = 0; l < inc.b(); l++) {
            int[] newLine = new int[k];
            int idx = 0;
            for (int p = 0; p < inc.v(); p++) {
                if (inc.inc(l, p)) {
                    newLine[idx++] = p;
                }
            }
            lines[l] = newLine;
        }
        return lines;
    }

    public Inc toInc() {
        int b = lines.length;
        Inc res = Inc.empty(pointCount, lines[0].length, b);
        for (int l = 0; l < b; l++) {
            FixBS row = flags[l];
            for (int pt = 0; pt < pointCount; pt++) {
                if (row.get(pt)) {
                    res.set(l, pt);
                }
            }
        }
        return res;
    }

    public int[][] lines() {
        return lines;
    }

    private class BlocksIterator implements Iterator<int[]> {
        private final int[] block;
        private boolean hasNext;

        public BlocksIterator() {
            int ll = lines[0].length;
            this.block = new int[ll];
            int tr = findFirstTriple();
            if (tr < 0) {
                return;
            }
            block[0] = tr / pointCount / pointCount;
            block[1] = tr / pointCount % pointCount;
            block[2] = tr % pointCount;
            for (int i = 3; i < ll; i++) {
                block[i] = i - 3;
            }
            this.hasNext = findNext(ll - 3);
        }

        private int findFirstTriple() {
            int result = -1;
            int minAv = Integer.MAX_VALUE;
            int[][][] available = availableLines();
            for (int i = 0; i < pointCount; i++) {
                int[][] look = lookup[i];
                for (int j = i + 1; j < pointCount; j++) {
                    int[] look1 = look[j];
                    for (int k = j + 1; k < pointCount; k++) {
                        if (look1[k] >= 0) {
                            continue;
                        }
                        int av = available[i][j][k];
                        if (minAv > av) {
                            minAv = av;
                            result = (i * pointCount + j) * pointCount + k;
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        private boolean findNext(int moreNeeded) {
            int len = block.length - moreNeeded;
            ex: for (int p = Math.max(len == 3 ? 0 : block[len - 1] + 1, block[len]); p < pointCount - moreNeeded + 1; p++) {
                if (p == block[0] || p == block[1] || p == block[2]) {
                    continue;
                }
                int[][] look = lookup[p];
                for (int i = 0; i < len; i++) {
                    int[] look1 = look[block[i]];
                    for (int j = i + 1; j < len; j++) {
                        if (look1[block[j]] >= 0) {
                            continue ex;
                        }
                    }
                }
                block[len] = p;
                if (moreNeeded == 1 || findNext(moreNeeded - 1)) {
                    return true;
                }
            }
            int base = ++block[len - 1] - len + 1;
            for (int i = len; i < block.length; i++) {
                block[i] = base + i;
            }
            return false;
        }

        @Override
        public int[] next() {
            int[] res = block.clone();
            Arrays.sort(res);
            block[block.length - 1]++;
            this.hasNext = findNext(block.length - 3);
            return res;
        }
    }

    public Iterable<int[]> blocks() {
        return BlocksIterator::new;
    }

    public int[][][] availableLines() {
        int[][][] result = new int[pointCount][pointCount][pointCount];
        int ll = lines[0].length;
        int[] curr = new int[ll];
        availableLines(result, curr, ll);
        return result;
    }

    private void availableLines(int[][][] dist, int[] arr, int needed) {
        int ll = arr.length;
        int len = ll - needed;
        ex: for (int p = len == 0 ? 0 : arr[len - 1] + 1; p < pointCount; p++) {
            int[][] look = lookup[p];
            for (int i = 0; i < len; i++) {
                int[] look1 = look[arr[i]];
                for (int j = i + 1; j < len; j++) {
                    if (look1[arr[j]] >= 0) {
                        continue ex;
                    }
                }
            }
            arr[len] = p;
            if (needed == 1) {
                for (int i = 0; i < ll; i++) {
                    int a = arr[i];
                    for (int j = i + 1; j < ll; j++) {
                        int b = arr[j];
                        for (int k = j + 1; k < ll; k++) {
                            int c = arr[k];
                            dist[a][b][c]++;
                            dist[a][c][b]++;
                            dist[b][a][c]++;
                            dist[b][c][a]++;
                            dist[c][a][b]++;
                            dist[c][b][a]++;
                        }
                    }
                }
            } else {
                availableLines(dist, arr, needed - 1);
            }
        }
    }

    public int pointCount() {
        return pointCount;
    }

    public int blockCount() {
        return lines.length;
    }

    public boolean flag(int line, int point) {
        return flags[line].get(point);
    }

    public long autCount() {
        AtomicLong counter = new AtomicLong();
        Consumer<int[]> cons = _ -> counter.incrementAndGet();
        AutomorphismConsumerNew aut = new AutomorphismConsumerNew(this, cons);
        NautyAlgoNew.search(this, aut);
        return counter.get();
    }

    public PermutationGroup automorphisms() {
        List<int[]> res = new ArrayList<>();
        Consumer<int[]> cons = res::add;
        AutomorphismConsumer aut = new AutomorphismConsumer(this, cons);
        NautyAlgo.search(this, aut);
        return new PermutationGroup(res.toArray(int[][]::new));
    }

    public Liner derived(int pt) {
        List<int[]> result = new ArrayList<>();
        for (int[] line : lines) {
            int pos = Arrays.binarySearch(line, pt);
            if (pos < 0) {
                continue;
            }
            int[] bl = IntStream.concat(Arrays.stream(line, 0, pos),
                    Arrays.stream(line, pos + 1, line.length).map(i -> i - 1)).toArray();
            result.add(bl);
        }
        return new Liner(pointCount - 1, result.toArray(int[][]::new));
    }

    public Map<Map<Integer, Long>, Integer> fingerprint() {
        Map<Map<Integer, Long>, Integer> result = new HashMap<>();
        for (int pt = 0; pt < pointCount; pt++) {
            Liner lnr = derived(pt);
            Map<Integer, Long> freq = lnr.hyperbolicFreq();
            result.compute(freq, (k, v) -> v == null ? 1 : v + 1);
        }
        return result;
    }

    public List<InversivePlane> paraModifications() {
        GraphData gd = graphData();
        int b = blockCount();
        int v = pointCount;
        int k = lines[0].length;
        int r1 = ((v - 1) * (v - 2)) / ((k - 1) * (k - 2));
        int r2 = (v - 2) / (k - 2);
        int c1 = r1 - 1 - (k - 1) * (r2 - 1);
        int m = (v - k) / (k - 2);
        int numEdges = k * (k - 1) / 2;
        int n1 = k * c1;
        int n2 = numEdges * m;

        // Precompute edges of K_k
        int[] edgeU = new int[numEdges];
        int[] edgeV = new int[numEdges];
        int eIdx = 0;
        for (int u = 0; u < k; u++) {
            for (int w = u + 1; w < k; w++) {
                edgeU[eIdx] = u;
                edgeV[eIdx] = w;
                eIdx++;
            }
        }

        FixBS orbBlocks = new FixBS(b);
        for (int i = v; i < b + v; i++) {
            orbBlocks.set(gd.orbits()[i] - v);
        }

        List<InversivePlane> result = new ArrayList<>();
        Arrays.stream(orbBlocks.toArray()).parallel().forEach(bn -> {
            int[] B = lines[bn];
            FixBS bMask = flags[bn];

            int[] b1Idx = new int[n1];
            FixBS[] b1Trunc = new FixBS[n1];
            int[] b2Idx = new int[n2];
            FixBS[] b2Trunc = new FixBS[n2];

            int cnt1 = 0;
            int cnt2 = 0;
            for (int i = 0; i < b; i++) {
                if (i == bn) continue;
                int inter = bMask.intersection(flags[i]).cardinality();
                if (inter == 1) {
                    b1Idx[cnt1] = i;
                    FixBS trunc = flags[i].copy();
                    trunc.andNot(bMask);
                    b1Trunc[cnt1] = trunc;
                    cnt1++;
                } else if (inter == 2) {
                    b2Idx[cnt2] = i;
                    FixBS trunc = flags[i].copy();
                    trunc.andNot(bMask);
                    b2Trunc[cnt2] = trunc;
                    cnt2++;
                }
            }

            // Precompute B1 compatibility: |L_a' ∩ L_b'| <= 1
            boolean[][] compat = new boolean[n1][n1];
            for (int i = 0; i < n1; i++) {
                for (int j = i + 1; j < n1; j++) {
                    if (b1Trunc[i].intersection(b1Trunc[j]).cardinality() <= 1) {
                        compat[i][j] = compat[j][i] = true;
                    }
                }
            }

            // --- STEP 1: Find all resolutions of B2' ---
            Graph g2 = new Graph(n2);
            for (int i = 0; i < n2; i++) {
                for (int j = i + 1; j < n2; j++) {
                    if (!b2Trunc[i].intersects(b2Trunc[j])) {
                        g2.connect(i, j);
                    }
                }
            }
            List<FixBS> pClasses = new ArrayList<>();
            JNauty.instance().maximalCliques(g2, m, a -> pClasses.add(new FixBS(a)));

            int nPC = pClasses.size();
            Graph gRes = new Graph(nPC);
            for (int i = 0; i < nPC; i++) {
                for (int j = i + 1; j < nPC; j++) {
                    if (!pClasses.get(i).intersects(pClasses.get(j))) {
                        gRes.connect(i, j);
                    }
                }
            }
            List<FixBS> resolutions = new ArrayList<>();
            JNauty.instance().maximalCliques(gRes, numEdges, a -> resolutions.add(new FixBS(a)));

            // --- STEP 2: Map parallel classes to edges of K_k ---
            for (FixBS res : resolutions) {
                int[] resClasses = res.toArray();

                boolean[][] conflict = new boolean[numEdges][numEdges];
                for (int i = 0; i < numEdges; i++) {
                    int[] blocksI = pClasses.get(resClasses[i]).toArray();
                    for (int j = i + 1; j < numEdges; j++) {
                        int[] blocksJ = pClasses.get(resClasses[j]).toArray();
                        outer: for (int a : blocksI) {
                            for (int c : blocksJ) {
                                if (b2Trunc[a].intersection(b2Trunc[c]).cardinality() >= 2) {
                                    conflict[i][j] = conflict[j][i] = true;
                                    break outer;
                                }
                            }
                        }
                    }
                }

                int[] perm = new int[numEdges];
                boolean[] used = new boolean[numEdges];
                perm[0] = 0;
                used[0] = true;

                mapEdges(1, numEdges, edgeU, edgeV, conflict, perm, used, () -> {
                    @SuppressWarnings("unchecked")
                    List<FixBS>[] b2TruncByPt = IntStream.range(0, k)
                            .mapToObj(_ -> new ArrayList<FixBS>()).toArray(List[]::new);

                    for (int e = 0; e < numEdges; e++) {
                        int u = edgeU[e], w = edgeV[e];
                        for (int bi : pClasses.get(resClasses[perm[e]]).toArray()) {
                            b2TruncByPt[u].add(b2Trunc[bi]);
                            b2TruncByPt[w].add(b2Trunc[bi]);
                        }
                    }

                    // --- STEP 3: Partition B1' ---
                    solveB1(k, c1, n1, b1Trunc, compat, b2TruncByPt, chosen -> {
                        FixBS[] altInc = Arrays.stream(flags).map(FixBS::copy).toArray(FixBS[]::new);
                        for (int idx : b1Idx) altInc[idx].andNot(bMask);
                        for (int idx : b2Idx) altInc[idx].andNot(bMask);

                        for (int e = 0; e < numEdges; e++) {
                            int ptU = B[edgeU[e]], ptV = B[edgeV[e]];
                            for (int bi : pClasses.get(resClasses[perm[e]]).toArray()) {
                                altInc[b2Idx[bi]].set(ptU);
                                altInc[b2Idx[bi]].set(ptV);
                            }
                        }
                        for (int p = 0; p < k; p++) {
                            if (chosen[p] != null) {
                                for (int idx : chosen[p].toArray()) {
                                    altInc[b1Idx[idx]].set(B[p]);
                                }
                            }
                        }

                        InversivePlane altSys = new InversivePlane(altInc);
                        synchronized (result) {
                            result.add(altSys);
                        }
                    });
                });
            }
        });
        return result;
    }

    private void mapEdges(int pos, int numEdges, int[] edgeU, int[] edgeV,
                          boolean[][] conflict, int[] perm, boolean[] used, Runnable onValid) {
        if (pos == numEdges) {
            onValid.run();
            return;
        }
        int u = edgeU[pos], v = edgeV[pos];
        for (int c = 0; c < numEdges; c++) {
            if (used[c]) continue;
            boolean ok = true;
            for (int prev = 0; prev < pos; prev++) {
                if (edgeU[prev] == u || edgeU[prev] == v || edgeV[prev] == u || edgeV[prev] == v) {
                    if (conflict[perm[prev]][c]) {
                        ok = false;
                        break;
                    }
                }
            }
            if (ok) {
                perm[pos] = c;
                used[c] = true;
                mapEdges(pos + 1, numEdges, edgeU, edgeV, conflict, perm, used, onValid);
                used[c] = false;
            }
        }
    }

    private void solveB1(int k, int c1, int n1, FixBS[] b1Trunc, boolean[][] compat, List<FixBS>[] b2TruncByPt, Consumer<FixBS[]> onSolution) {
        if (c1 == 0) {
            onSolution.accept(new FixBS[k]);
            return;
        }

        List<FixBS> allCliques = new ArrayList<>();
        List<Integer> cliqueOwner = new ArrayList<>();

        for (int p = 0; p < k; p++) {
            List<Integer> adm = new ArrayList<>();
            for (int i = 0; i < n1; i++) {
                boolean ok = true;
                for (FixBS mTrunc : b2TruncByPt[p]) {
                    if (b1Trunc[i].intersection(mTrunc).cardinality() >= 2) {
                        ok = false;
                        break;
                    }
                }
                if (ok) adm.add(i);
            }
            if (adm.size() < c1) return;

            Graph gp = new Graph(adm.size());
            for (int i = 0; i < adm.size(); i++) {
                for (int j = i + 1; j < adm.size(); j++) {
                    if (compat[adm.get(i)][adm.get(j)]) {
                        gp.connect(i, j);
                    }
                }
            }

            List<FixBS> pCliques = new ArrayList<>();
            JNauty.instance().maximalCliques(gp, c1, a -> {
                FixBS set = new FixBS(a);
                FixBS mapped = new FixBS(n1);
                for (int idx = set.nextSetBit(0); idx >= 0; idx = set.nextSetBit(idx + 1)) {
                    mapped.set(adm.get(idx));
                }
                pCliques.add(mapped);
            });
            if (pCliques.isEmpty()) return;

            for (FixBS cl : pCliques) {
                allCliques.add(cl);
                cliqueOwner.add(p);
            }
        }

        int ncl = allCliques.size();
        Graph gFinal = new Graph(ncl);
        for (int i = 0; i < ncl; i++) {
            for (int j = i + 1; j < ncl; j++) {
                if (!cliqueOwner.get(i).equals(cliqueOwner.get(j)) && !allCliques.get(i).intersects(allCliques.get(j))) {
                    gFinal.connect(i, j);
                }
            }
        }

        List<FixBS> pList = new ArrayList<>();
        JNauty.instance().maximalCliques(gFinal, k, a -> pList.add(new FixBS(a)));

        for (FixBS sol : pList) {
            FixBS[] chosen = new FixBS[k];
            for (int clIdx : sol.toArray()) {
                chosen[cliqueOwner.get(clIdx)] = allCliques.get(clIdx);
            }
            onSolution.accept(chosen);
        }
    }

    @Override
    public int vCount() {
        return pointCount + lines.length;
    }

    @Override
    public int vColor(int idx) {
        return idx < pointCount ? 0 : 1;
    }

    @Override
    public boolean edge(int a, int b) {
        if (a < pointCount) {
            return b >= pointCount && flags[b - pointCount].get(a);
        } else {
            return b < pointCount && flags[a - pointCount].get(b);
        }
    }

    public GraphData graphData() {
        if (gd == null) {
            gd = JNauty.instance().traces(this);
        }
        return gd;
    }
}
