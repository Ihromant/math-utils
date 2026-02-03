package ua.ihromant.mathutils;

import ua.ihromant.jnauty.GraphWrapper;
import ua.ihromant.mathutils.nauty.CanonicalConsumer;
import ua.ihromant.mathutils.nauty.NautyAlgo;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SimpleLiner implements GraphWrapper {
    private final int pointCount;
    private final int ll;
    private final int[][] lines;
    private final boolean[][] flags;
    private final int[][] lookup;
    private final int[][] beams;
    private final int[][] intersections;

    public SimpleLiner(int pointCount, int ll) {
        this.pointCount = pointCount;
        this.ll = ll;
        this.lines = new int[0][ll];
        this.lookup = new int[pointCount][pointCount];
        Arrays.stream(lookup).forEach(arr -> Arrays.fill(arr, -1));
        this.flags = new boolean[0][pointCount];
        this.beams = new int[pointCount][0];
        this.intersections = new int[0][0];
    }

    public SimpleLiner(int pointCount, int[][] lines) {
        this.pointCount = pointCount;
        this.ll = lines[0].length;
        this.lines = lines;
        this.flags = new boolean[lines.length][pointCount];
        int[] beamCounts = new int[pointCount];
        for (int i = 0; i < lines.length; i++) {
            int[] line = lines[i];
            for (int pt : line) {
                flags[i][pt] = true;
                beamCounts[pt]++;
            }
        }
        this.beams = new int[pointCount][];
        for (int pt = 0; pt < pointCount; pt++) {
            int bc = beamCounts[pt];
            beams[pt] = new int[bc];
            int idx = 0;
            for (int ln = 0; ln < lines.length; ln++) {
                if (flags[ln][pt]) {
                    beams[pt][idx++] = ln;
                }
            }
        }
        this.lookup = generateLookup();
        this.intersections = generateIntersections();
    }

    public SimpleLiner(SimpleLiner base, int[] newLine) {
        this.pointCount = base.pointCount;
        this.ll = base.ll;
        int pll = base.lines.length;
        int nll = base.flags.length + 1;
        this.lines = new int[nll][];
        System.arraycopy(base.lines, 0, this.lines, 0, pll);
        this.lines[pll] = newLine;
        this.flags = new boolean[nll][];
        System.arraycopy(base.flags, 0, this.flags, 0, pll);
        this.flags[pll] = new boolean[pointCount];
        this.lookup = base.lookup.clone();
        this.beams = base.beams.clone();
        int ni = 0;
        this.intersections = new int[nll][nll];
        for (int i = 0; i < pll; i++) {
            System.arraycopy(base.intersections[i], 0, this.intersections[i], 0, pll);
            intersections[i][pll] = -1;
        }
        Arrays.fill(intersections[pll], -1);
        for (int p : newLine) {
            flags[pll][p] = true; // flags
            int[] nl = this.lookup[p].clone(); // lookup
            for (int p1 : newLine) {
                if (p1 == p) {
                    continue;
                }
                nl[p1] = pll;
            }
            this.lookup[p] = nl;

            int[] ob = this.beams[p]; // beams
            int[] nb = new int[ob.length + 1];
            System.arraycopy(ob, 0, nb, 0, ob.length);
            nb[ob.length] = pll;
            this.beams[p] = nb;

            int[] beam = base.beams[p];
            for (int l : beam) {
                intersections[l][pll] = p; // intersections
                intersections[pll][l] = p;
            }
            ni = ni + beam.length;
        }
    }

    private int[][] generateLookup() {
        int[][] result = new int[pointCount][pointCount];
        for (int[] p : result) {
            Arrays.fill(p, -1);
        }
        for (int l = 0; l < lines.length; l++) {
            int[] line = lines[l];
            for (int i = 0; i < line.length; i++) {
                int p1 = line[i];
                for (int j = i + 1; j < line.length; j++) {
                    int p2 = line[j];
                    if (result[p1][p2] >= 0) {
                        throw new IllegalStateException();
                    }
                    result[p1][p2] = l;
                    result[p2][p1] = l;
                }
            }
        }
        return result;
    }

    private int[][] generateIntersections() {
        int[][] result = new int[lines.length][lines.length];
        for (int[] arr : result) {
            Arrays.fill(arr, -1);
        }
        int[] freq = new int[lines.length];
        for (int p = 0; p < pointCount; p++) {
            int[] beam = beams[p];
            for (int i = 0; i < beam.length; i++) {
                int l1 = beam[i];
                for (int j = i + 1; j < beam.length; j++) {
                    int l2 = beam[j];
                    result[l1][l2] = p;
                    result[l2][l1] = p;
                    freq[l1]++;
                    freq[l2]++;
                }
            }
        }
        int maxFreq = 0;
        for (int f : freq) {
            if (f > maxFreq) {
                maxFreq = f;
            }
        }
        return result;
    }

    public int pointCount() {
        return pointCount;
    }

    public int lineCount() {
        return lines.length;
    }

    public boolean flag(int line, int point) {
        return flags[line][point];
    }

    public int[] line(int line) {
        return lines[line];
    }

    public int line(int p1, int p2) {
        return lookup[p1][p2];
    }

    public int[][] lines() {
        return lines;
    }

    public boolean checkAP(int[] line) {
        int ll = line.length;
        for (int p : line) {
            for (int a = 0; a < ll; a++) {
                int pl1 = line[a];
                if (pl1 == p) {
                    continue;
                }
                for (int b = a + 1; b < ll; b++) {
                    int pl2 = line[b];
                    if (pl2 == p) {
                        continue;
                    }
                    for (int ol : beams[p]) {
                        int[] oLine = lines[ol];
                        for (int c = 0; c < ll; c++) {
                            int po1 = oLine[c];
                            if (po1 == p) {
                                continue;
                            }
                            int[] lk = lookup[po1];
                            int l1 = lk[pl1];
                            int l2 = lk[pl2];
                            if (l1 < 0 && l2 < 0) {
                                continue;
                            }
                            for (int d = c + 1; d < ll; d++) {
                                int po2 = oLine[d];
                                if (po2 == p) {
                                    continue;
                                }
                                int[] lk1 = lookup[po2];
                                int l4 = lk1[pl2];
                                if (l1 >= 0 && l4 >= 0 && intersections[l1][l4] >= 0) {
                                    return false;
                                }
                                int l3 = lk1[pl1];
                                if (l2 >= 0 && l3 >= 0 && intersections[l2][l3] >= 0) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public BitSet hyperbolicIndex() {
        int maximum = Arrays.stream(lines).mapToInt(arr -> arr.length).max().orElseThrow() - 1;
        BitSet result = new BitSet();
        for (int o = 0; o < pointCount; o++) {
            for (int x = 0; x < pointCount; x++) {
                if (o == x) {
                    continue;
                }
                int ox = lookup[o][x];
                for (int y = 0; y < pointCount; y++) {
                    if (flags[ox][y]) {
                        continue;
                    }
                    for (int p : lines[lookup[x][y]]) {
                        if (p == x || p == y) {
                            continue;
                        }
                        int counter = 0;
                        for (int u : lines[lookup[o][y]]) {
                            if (u == o || u == y) {
                                continue;
                            }
                            if (intersections[lookup[p][u]][ox] < 0) {
                                counter++;
                            }
                        }
                        result.set(counter);
                    }
                    if (result.cardinality() == maximum) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

    public Map<Integer, Integer> hyperbolicFreq() {
        Map<Integer, Integer> result = new HashMap<>();
        for (int o = 0; o < pointCount; o++) {
            for (int x = 0; x < pointCount; x++) {
                if (o == x) {
                    continue;
                }
                int ox = lookup[o][x];
                for (int y = 0; y < pointCount; y++) {
                    if (flags[ox][y]) {
                        continue;
                    }
                    for (int p : lines[lookup[x][y]]) {
                        if (p == x || p == y) {
                            continue;
                        }
                        int counter = 0;
                        for (int u : lines[lookup[o][y]]) {
                            if (u == o || u == y) {
                                continue;
                            }
                            if (intersections[lookup[p][u]][ox] < 0) {
                                counter++;
                            }
                        }
                        result.compute(counter, (k, v) -> v == null ? 1 : v + 1);
                    }
                }
            }
        }
        return result;
    }

    public Iterable<int[]> blocks() {
        return BlocksIterator::new;
    }

    private class BlocksIterator implements Iterator<int[]> {
        private final int[] block;
        private boolean hasNext;

        public BlocksIterator() {
            this.block = new int[ll];
            int fst = pointCount;
            int snd = pointCount;
            ex: for (int i = 0; i < pointCount; i++) {
                for (int j = i + 1; j < pointCount; j++) {
                    if (line(i, j) < 0) {
                        fst = i;
                        snd = j;
                        break ex;
                    }
                }
            }
            block[0] = fst;
            block[1] = snd;
            for (int i = 2; i < ll; i++) {
                block[i] = snd + i - 1;
            }
            this.hasNext = fst < pointCount && findNext(ll - 2);
        }

        private static int getUnassigned(int[] look, int pt) {
            for (int i = pt + 1; i < look.length; i++) {
                if (look[i] < 0) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        private boolean findNext(int moreNeeded) {
            int len = block.length - moreNeeded;
            ex: for (int p = Math.max(block[len - 1] + 1, block[len]); p < pointCount - moreNeeded + 1; p++) {
                int[] look = lookup[p];
                for (int i = 0; i < len; i++) {
                    if (look[block[i]] >= 0) {
                        continue ex;
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
            block[block.length - 1]++;
            this.hasNext = findNext(block.length - 2);
            return res;
        }
    }

    public FixBS getCanonical() {
        CanonicalConsumer cons = new CanonicalConsumer(this);
        NautyAlgo.search(this, cons);
        return cons.canonicalForm();
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
        int pc = pointCount;
        if (a < pc) {
            return b >= pc && flags[b - pc][a];
        } else {
            return b < pc && flags[a - pc][b];
        }
    }
}
