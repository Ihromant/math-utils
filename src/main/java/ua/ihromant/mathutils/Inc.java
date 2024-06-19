package ua.ihromant.mathutils;

import ua.ihromant.mathutils.nauty.CanonicalConsumer;
import ua.ihromant.mathutils.nauty.GraphWrapper;
import ua.ihromant.mathutils.nauty.NautyAlgo;

import java.util.BitSet;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record Inc(BitSet bs, int v, int b) {
    public Inc(Inc that, int[] row) {
        this(mergeRow(that, row), that.v, that.b + 1);
    }

    private static BitSet mergeRow(Inc that, int[] row) {
        int all = that.v * that.b;
        BitSet result = new BitSet(all + that.v);
        result.or(that.bs);
        for (int p : row) {
            result.set(all + p);
        }
        return result;
    }

    public boolean inc(int l, int pt) {
        return bs.get(idx(l, pt));
    }

    public void set(int l, int pt) {
        bs.set(idx(l, pt));
    }

    private int idx(int l, int pt) {
        return l * v + pt;
    }

    @Override
    public String toString() {
        return IntStream.range(0, b).mapToObj(row -> IntStream.range(0, v).mapToObj(col -> inc(row, col) ? "1" : "0")
                .collect(Collectors.joining())).collect(Collectors.joining("\n"));
    }

    public Iterable<int[]> blocks() {
        return BlocksIterator::new;
    }

    private class BlocksIterator implements Iterator<int[]> {
        private final int[] block;
        private boolean hasNext;

        public BlocksIterator() {
            int ll = (int) IntStream.range(0, v).filter(bs::get).count();
            this.block = new int[ll];
            int fst = -1;
            for (int i = 0; i < v; i++) {
                if (inc(b - 1, i)) {
                    fst = i;
                    break;
                }
            }
            int snd;
            do {
                BitSet cand = new BitSet(v);
                cand.set(fst + 1, v);
                for (int l = 0; l < b; l++) {
                    if (!inc(l, fst)) {
                        continue;
                    }
                    for (int pt = fst + 1; pt < v; pt++) {
                        if (inc(l, pt)) {
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
                    if (!inc(l, p)) {
                        continue;
                    }
                    for (int i = 0; i < len; i++) {
                        if (inc(l, block[i])) {
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

    public BitSet getCanonical() {
        GraphWrapper graph = GraphWrapper.byInc(this);
        CanonicalConsumer cons = new CanonicalConsumer(graph);
        NautyAlgo.search(graph, cons);
        return cons.canonicalForm();
    }

    public boolean checkAP() {
        int last = b - 1;
        int[] lLine = IntStream.range(0, v).filter(p -> inc(last, p)).toArray();
        int ll = lLine.length;
        for (int p : lLine) {
            for (int ol = 0; ol < b; ol++) {
                if (ol == last || !inc(ol, p)) {
                    continue;
                }
                int olf = ol;
                int[] oLine = IntStream.range(0, v).filter(p1 -> inc(olf, p1)).toArray();
                for (int a = 0; a < ll; a++) {
                    int pl1 = lLine[a];
                    if (pl1 == p) {
                        continue;
                    }
                    for (int b = a + 1; b < ll; b++) {
                        int pl2 = lLine[b];
                        if (pl2 == p) {
                            continue;
                        }
                        for (int c = 0; c < ll; c++) {
                            int po1 = oLine[c];
                            if (po1 == p) {
                                continue;
                            }
                            int l1 = line(pl1, po1);
                            int l2 = line(pl2, po1);
                            if (l1 < 0 && l2 < 0) {
                                continue;
                            }
                            for (int d = c + 1; d < ll; d++) {
                                int po2 = oLine[d];
                                if (po2 == p) {
                                    continue;
                                }
                                int l4 = line(pl2, po2);
                                if (l1 >= 0 && l4 >= 0 && intersection(l1, l4) >= 0) {
                                    return false;
                                }
                                int l3 = line(pl1, po2);
                                if (l2 >= 0 && l3 >= 0 && intersection(l2, l3) >= 0) {
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

    private int line(int p1, int p2) {
        for (int l = 0; l < b; l++) {
            if (inc(l, p1) && inc(l, p2)) {
                return l;
            }
        }
        return -1;
    }

    private int intersection(int l1, int l2) {
        for (int p = 0; p < v; p++) {
            if (inc(l1, p) && inc(l2, p)) {
                return p;
            }
        }
        return -1;
    }
}
