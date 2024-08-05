package ua.ihromant.mathutils;

import ua.ihromant.mathutils.nauty.CanonicalConsumer;
import ua.ihromant.mathutils.nauty.CanonicalConsumerNew;
import ua.ihromant.mathutils.nauty.GraphWrapper;
import ua.ihromant.mathutils.nauty.NautyAlgo;
import ua.ihromant.mathutils.nauty.NautyAlgoNew;
import ua.ihromant.mathutils.util.FixBS;

import java.util.BitSet;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface Inc {
    int v();

    int b();

    boolean inc(int l, int pt);

    void set(int l, int pt);

    Inc removeTwins();

    Inc addLine(int[] line);

    static Inc empty(int v, int b) {
        return new FixInc(IntStream.range(0, b).mapToObj(i -> new FixBS(v)).toArray(FixBS[]::new), v);
    }

    default String toLines() {
        return IntStream.range(0, b()).mapToObj(line -> IntStream.range(0, v()).filter(pt -> inc(line, pt)).mapToObj(String::valueOf)
                .collect(Collectors.joining(" "))).collect(Collectors.joining("\n")) + "\n";
    }

    default Iterable<int[]> blocks() {
        return () -> new BlocksIterator(this);
    }

    class BlocksIterator implements Iterator<int[]> {
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

    default FixBS getCanonicalNew() {
        GraphWrapper graph = GraphWrapper.byInc(this);
        CanonicalConsumerNew cons = new CanonicalConsumerNew(graph);
        NautyAlgoNew.search(graph, cons);
        return cons.canonicalForm();
    }

    default FixBS getCanonicalOld() {
        GraphWrapper graph = GraphWrapper.byInc(this);
        CanonicalConsumer cons = new CanonicalConsumer(graph);
        NautyAlgo.search(graph, cons);
        return cons.canonicalForm();
    }

    default Matrix sqrInc() {
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
}
