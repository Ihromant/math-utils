package ua.ihromant.mathutils;

import ua.ihromant.jnauty.NautyGraph;
import ua.ihromant.mathutils.nauty.CanonicalConsumer;
import ua.ihromant.mathutils.nauty.NautyAlgo;
import ua.ihromant.mathutils.util.FixBS;

import java.util.BitSet;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record Inc(FixBS[] lines, int v, int k) implements NautyGraph {
    public static Inc empty(int v, int k, int b) {
        return new Inc(IntStream.range(0, b).mapToObj(_ -> new FixBS(v)).toArray(FixBS[]::new), v, k);
    }

    public int b() {
        return lines.length;
    }

    public boolean inc(int l, int pt) {
        return lines[l].get(pt);
    }

    public void set(int l, int pt) {
        lines[l].set(pt);
    }

    public Inc removeTwins() {
        int[] beamCounts = new int[v];
        for (FixBS line : lines) {
            for (int pt = line.nextSetBit(0); pt >= 0; pt = line.nextSetBit(pt + 1)) {
                beamCounts[pt]++;
            }
        }
        FixBS filtered = new FixBS(v);
        IntStream.range(0, v).filter(i -> beamCounts[i] > 1).forEach(filtered::set);
        int pCard = filtered.cardinality();
        if (v == pCard) {
            return this;
        } else {
            FixBS[] newLines = IntStream.range(0, lines.length).mapToObj(i -> new FixBS(pCard)).toArray(FixBS[]::new);
            int idx = 0;
            for (int pt = filtered.nextSetBit(0); pt >= 0; pt = filtered.nextSetBit(pt + 1)) {
                for (int l = 0; l < lines.length; l++) {
                    if (inc(l, pt)) {
                        newLines[l].set(idx);
                    }
                }
                idx++;
            }
            FixBS filteredLines = new FixBS(lines.length);
            for (int l = 0; l < newLines.length; l++) {
                if (newLines[l].cardinality() > 1) {
                    filteredLines.set(l);
                }
            }
            int fCard = filteredLines.cardinality();
            if (fCard == lines.length) {
                return new Inc(newLines, pCard, k);
            } else {
                FixBS[] res = new FixBS[fCard];
                int lIdx = 0;
                for (int ln = filteredLines.nextSetBit(0); ln >= 0; ln = filteredLines.nextSetBit(ln + 1)) {
                    res[lIdx++] = newLines[ln];
                }
                return new Inc(res, pCard, k);
            }
        }
    }

    public Inc addLine(int[] line) {
        FixBS[] next = new FixBS[lines.length + 1];
        System.arraycopy(lines, 0, next, 0, lines.length);
        next[lines.length] = FixBS.of(v, line);
        return new Inc(next, v, k);
    }

    @Override
    public int vCount() {
        return v() + b();
    }

    @Override
    public int vColor(int idx) {
        return idx < v() ? 0 : 1;
    }

    @Override
    public boolean edge(int a, int b) {
        int pc = v();
        if (a < pc) {
            return b >= pc && inc(b - pc, a);
        } else {
            return b < pc && inc(a - pc, b);
        }
    }

    @Override
    public int eCount() {
        return lines.length * k * 2;
    }

    public String toLines() {
        return IntStream.range(0, b()).mapToObj(line -> IntStream.range(0, v()).filter(pt -> inc(line, pt)).mapToObj(String::valueOf)
                .collect(Collectors.joining(" "))).collect(Collectors.joining("\n")) + "\n";
    }

    public Iterable<int[]> blocks() {
        return () -> new BlocksIterator(this);
    }

    public FixBS getCanonicalOld() {
        CanonicalConsumer cons = new CanonicalConsumer(this);
        NautyAlgo.search(this, cons);
        return cons.canonicalForm();
    }

    public Matrix sqrInc() {
        int v = v();
        int b = b();
        int[][] res = new int[v][v];
        for (int i = 0; i < v; i++) {
            for (int j = i + 1; j < v; j++) {
                int cnt = 0;
                for (int l = 0; l < b; l++) {
                    if (inc(l, i) && inc(l, j)) {
                        cnt++;
                    }
                }
                res[i][j] = res[j][i] = cnt;
            }
        }
        return new Matrix(res);
    }

    public static class BlocksIterator implements Iterator<int[]> {
        private final Inc inc;
        private final int v;
        private final int b;
        private final int[] block;
        private boolean hasNext;

        public BlocksIterator(Inc inc) {
            this.inc = inc;
            this.v = inc.v();
            this.b = inc.b();
            int ll = (int) IntStream.range(0, v).filter(pt -> inc.inc(0, pt)).count();
            this.block = new int[ll];
            int fst = -1;
            for (int i = 0; i < v; i++) {
                if (inc.inc(b - 1, i)) {
                    fst = i;
                    break;
                }
            }
            int snd;
            do {
                BitSet cand = new BitSet(v);
                cand.set(fst + 1, v);
                for (int l = 0; l < b; l++) {
                    if (!inc.inc(l, fst)) {
                        continue;
                    }
                    for (int pt = fst + 1; pt < v; pt++) {
                        if (inc.inc(l, pt)) {
                            cand.set(pt, false);
                        }
                    }
                }
                snd = cand.nextSetBit(0);
            } while (snd < 0 && ++fst < v);
            block[0] = fst;
            block[1] = snd;
            for (int i = 2; i < ll; i++) {
                block[i] = snd + i - 1;
            }
            this.hasNext = fst < v && findNext(ll - 2);
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        private boolean findNext(int moreNeeded) {
            int len = block.length - moreNeeded;
            ex: for (int p = Math.max(block[len - 1] + 1, block[len]); p < v - moreNeeded + 1; p++) {
                for (int l = 0; l < b; l++) {
                    if (!inc.inc(l, p)) {
                        continue;
                    }
                    for (int i = 0; i < len; i++) {
                        if (inc.inc(l, block[i])) {
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
            block[block.length - 1]++;
            this.hasNext = findNext(block.length - 2);
            return res;
        }
    }
}
