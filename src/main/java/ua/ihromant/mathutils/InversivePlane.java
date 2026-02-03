package ua.ihromant.mathutils;

import ua.ihromant.jnauty.GraphWrapper;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.nauty.AutomorphismConsumer;
import ua.ihromant.mathutils.nauty.AutomorphismConsumerNew;
import ua.ihromant.mathutils.nauty.NautyAlgo;
import ua.ihromant.mathutils.nauty.NautyAlgoNew;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class InversivePlane implements GraphWrapper {
    private final int pointCount;
    private final int[][] lines;
    private final boolean[][] flags;
    private final int[][][] lookup;

    public InversivePlane(int[][] lines) {
        this(Arrays.stream(lines).mapToInt(arr -> arr[arr.length - 1]).max().orElseThrow() + 1, lines);
    }

    public InversivePlane(int pointCount, int[][] lines) {
        this.pointCount = pointCount;
        this.lines = lines;
        this.flags = new boolean[lines.length][pointCount];
        for (int i = 0; i < lines.length; i++) {
            int[] line = lines[i];
            for (int pt : line) {
                flags[i][pt] = true;
            }
        }
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
        this.flags = new boolean[pll + 1][];
        System.arraycopy(prev.flags, 0, this.flags, 0, pll);
        this.flags[pll] = new boolean[pointCount];
        for (int p : newLine) {
            flags[pll][p] = true;
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
        Inc res = Inc.empty(pointCount, b);
        for (int l = 0; l < b; l++) {
            boolean[] row = flags[l];
            for (int pt = 0; pt < pointCount; pt++) {
                if (row[pt]) {
                    res.set(l, pt);
                }
            }
        }
        return res;
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
        return flags[line][point];
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

    public Map<Map<Integer, Integer>, Integer> fingerprint() {
        Map<Map<Integer, Integer>, Integer> result = new HashMap<>();
        for (int pt = 0; pt < pointCount; pt++) {
            Liner lnr = derived(pt);
            Map<Integer, Integer> freq = lnr.hyperbolicFreq();
            result.compute(freq, (k, v) -> v == null ? 1 : v + 1);
        }
        return result;
    }

    @Override
    public int size() {
        return pointCount + lines.length;
    }

    @Override
    public int color(int idx) {
        return idx < pointCount ? 0 : 1;
    }

    @Override
    public boolean edge(int a, int b) {
        if (a < pointCount) {
            return b >= pointCount && flags[b - pointCount][a];
        } else {
            return b < pointCount && flags[a - pointCount][b];
        }
    }
}
