package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.jnauty.GraphData;
import ua.ihromant.jnauty.JNauty;
import ua.ihromant.jnauty.NautyGraph;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class IncFinderAltTest {
    private static class IncState implements NautyGraph {
        private final long[] lines;
        private final int v;
        private final int k;
        private int lc;

        private IncState(long[] lines, int v, int k, int lc) {
            this.lines = lines;
            this.v = v;
            this.k = k;
            this.lc = lc;
        }

        private IncState(int v, int k) {
            this.v = v;
            this.k = k;
            this.lines = new long[v * (v - 1) / k / (k - 1)];
        }

        @Override
        public int vCount() {
            return v + lc;
        }

        @Override
        public boolean edge(int a, int b) {
            if (a < v) {
                return b >= v && (lines[b - v] & (1L << a)) != 0;
            } else {
                return b < v && (lines[a - v] & (1L << b)) != 0;
            }
        }

        @Override
        public int vColor(int idx) {
            return idx < v ? 0 : 1;
        }

        @Override
        public int eCount() {
            return 2 * k * lc;
        }

        private void addLine(int... line) {
            long newLn = 0;
            for (int pt : line) {
                newLn = newLn | (1L << pt);
            }
            lines[lc++] = newLn;
        }

        private void removeLine() {
            lc--;
        }

        private static final long WORD_MASK = 0xffffffffffffffffL;

        private static int nextSetBit(long fbs, int fromIndex) {
            long word = fbs & (WORD_MASK << fromIndex);
            return word == 0 ? -1 : Long.numberOfTrailingZeros(word);
        }

        public int[][] toLines() {
            int[][] result = new int[lc][k];
            for (int i = 0; i < lc; i++) {
                long fbs = lines[i];
                int cnt = 0;
                for (int j = nextSetBit(fbs, 0); j >= 0; j = nextSetBit(fbs, j + 1)) {
                    result[i][cnt++] = j;
                }
            }
            return result;
        }

        private IncState copy() {
            return new IncState(lines.clone(), v, k, lc);
        }

        private List<int[]> possible() {
            long msk = (1L << v) - 1;
            List<int[]> result = new ArrayList<>();
            int fst = 0;
            long ms = msk;
            for (int i = 0; i < v; i++) {
                ms = msk;
                for (int j = 0; j < lc; j++) {
                    long ln = lines[j];
                    if ((ln & (1L << i)) != 0) {
                        ms = ms & ~ln;
                    }
                }
                if (ms != 0) {
                    fst = i;
                    break;
                }
            }
            int snd = nextSetBit(ms, 0);
            int[] base = new int[k];
            base[0] = fst;
            base[1] = snd;
            for (int j = 0; j < lc; j++) {
                long ln = lines[j];
                if ((ln & (1L << snd)) != 0) {
                    ms = ms & ~ln;
                }
            }
            recur(base, ms, 2, result::add);
            return result;
        }

        private void recur(int[] arr, long msk, int idx, Consumer<int[]> cons) {
            if (idx == k) {
                cons.accept(arr.clone());
                return;
            }
            for (int nxt = nextSetBit(msk, arr[idx - 1] + 1); nxt >= 0; nxt = nextSetBit(msk, nxt + 1)) {
                long ms = msk;
                for (int j = 0; j < lc; j++) {
                    long ln = lines[j];
                    if ((ln & (1L << nxt)) != 0) {
                        ms = ms & ~ln;
                    }
                }
                arr[idx] = nxt;
                recur(arr, ms, idx + 1, cons);
            }
        }

        private long[] canon(long[] canon) {
            int sm = lc + v;
            int sh = (sm + 63) / 64;
            long[] inc = new long[lc];
            for (int i = 0; i < lc; i++) {
                inc[i] = canon[(v + i) * sh];
            }
            return inc;
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
                        for (int ol = 0; ol < lc; ol++) {
                            long oLine = lines[ol];
                            if ((ol & (1L << p)) == 0) {
                                continue;
                            }
                            for (int po1 = nextSetBit(oLine, 0); po1 >= 0; po1 = nextSetBit(oLine, po1 + 1)) {
                                if (po1 == p) {
                                    continue;
                                }
                                int l1 = -1;
                                int l2 = -1;
                                for (int l = 0; l < lc; l++) {
                                    long ln = lines[l];
                                    if ((ln & (1L << po1)) == 0) {
                                        continue;
                                    }
                                    if ((ln & (1L << pl1)) == 0) {
                                        l1 = l;
                                    }
                                    if ((ln & (1L << pl2)) == 0) {
                                        l2 = l;
                                    }
                                }
                                if (l1 < 0 && l2 < 0) {
                                    continue;
                                }
                                for (int po2 = nextSetBit(oLine, po1 + 1); po2 >= 0; po2 = nextSetBit(oLine, po2 + 1)) {
                                    if (po2 == p) {
                                        continue;
                                    }
                                    int l3 = -1;
                                    int l4 = -1;
                                    for (int l = 0; l < lc; l++) {
                                        long ln = lines[l];
                                        if ((ln & (1L << po2)) == 0) {
                                            continue;
                                        }
                                        if ((ln & (1L << pl2)) == 0) {
                                            l4 = l;
                                        }
                                        if ((ln & (1L << pl1)) == 0) {
                                            l3 = l;
                                        }
                                    }
                                    if (l1 >= 0 && l4 >= 0 && (lines[l1] & lines[l4]) != 0 ) {
                                        return false;
                                    }
                                    if (l2 >= 0 && l3 >= 0 && (lines[l2] & lines[l3]) != 0) {
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
    }

    @Test
    public void generateInitial() {
        int v = 15;
        int k = 3;
        BiPredicate<IncState, int[]> checker = (_, _) -> true;
        IncState st = new IncState(v, k);
        int r = (v - 1) / (k - 1);
        int b = v * (v - 1) / k / (k - 1);
        IntStream.range(0, r).forEach(l -> {
            int[] line = IntStream.concat(IntStream.of(0), IntStream.range(0, k - 1).map(j -> l * (k - 1) + j + 1)).toArray();
            st.addLine(line);
        });
        st.addLine(IntStream.range(0, k).map(i -> i * (k - 1) + 1).toArray());
        Set<FixBS> unique = new HashSet<>();
        List<IncState> incs = new ArrayList<>();
        recur(st, unique, checker, inc -> {
            if (inc.lc < 2 * r - 1) {
                return false;
            }
            incs.add(inc.copy());
            return true;
        });
        System.out.println(incs.size());
        Set<FixBS> nextUnique = ConcurrentHashMap.newKeySet();
        incs.parallelStream().forEach(baseInc -> {
            recur(baseInc, nextUnique, checker, inc -> {
                if (inc.lc < b) {
                    return false;
                }
                GraphData gd = JNauty.instance().traces(inc);
                System.out.println(gd.autCount() + " " + Arrays.deepToString(inc.toLines()));
                return true;
            });
        });
    }

    private static void recur(IncState inc, Set<FixBS> unique, BiPredicate<IncState, int[]> checker, Predicate<IncState> pred) {
        if (pred.test(inc)) {
            return;
        }
        List<int[]> lns = inc.possible();
        for (int[] ln : lns) {
            if (!checker.test(inc, ln)) {
                continue;
            }
            inc.addLine(ln);
            GraphData data = JNauty.instance().traces(inc);
            if (unique.add(new FixBS(inc.canon(data.canonical())))) {
                recur(inc, unique, checker, pred);
            }
            inc.removeLine();
        }
    }
}
