package ua.ihromant.mathutils.vector;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TranslationPlaneTest {
    @Test
    public void test() {
        int p = 3;
        int n = 4;
        int h = n / 2;
        int crd = LinearSpace.pow(p, n);
        int halfCrd = LinearSpace.pow(p, h) - 1;
        int cnt = (crd - 1) / halfCrd;
        LinearSpace sp = new LinearSpace(p, n);
        FixBS[] curr = new FixBS[cnt];
        FixBS union = new FixBS(crd);
        Set<Set<FixBS>> unique = new HashSet<>();
        AtomicInteger counter = new AtomicInteger();
        Consumer<FixBS[]> cons = arr -> {
            Set<FixBS> set = Arrays.stream(arr).collect(Collectors.toSet());
            if (!unique.add(set)) {
                return;
            }
            counter.incrementAndGet();
            int[][] lines = toProjective(sp, arr);
            Liner l = new Liner(lines.length, lines);
            System.out.println(isDesargues(l) + " " + set);
        };
        generate(sp, curr, union, cnt, cons);
        System.out.println(counter);
    }

    private static void generate(LinearSpace space, FixBS[] curr, FixBS union, int needed, Consumer<FixBS[]> cons) {
        if (needed == 0) {
            cons.accept(curr);
            return;
        }
        int halfCrd = space.half() - 1;
        Set<FixBS> unique = new HashSet<>();
        Consumer<int[]> consumer = arr -> {
            FixBS bs = space.hull(arr);
            if (bs.intersects(union) || bs.cardinality() != halfCrd || !unique.add(bs)) {
                return;
            }
            FixBS[] newCurr = curr.clone();
            newCurr[curr.length - needed] = bs;
            FixBS newUnion = union.union(bs);
            generate(space, newCurr, newUnion, needed - 1, cons);
        };
        int half = space.getN() / 2;
        int[] arr = new int[half];
        arr[0] = union.nextClearBit(1);
        generateOne(space, arr, half - 1, consumer);
    }

    private static void generateOne(LinearSpace sp, int[] curr, int needed, Consumer<int[]> cons) {
        if (needed == 0) {
            cons.accept(curr);
            return;
        }
        int prev = curr[curr.length - needed - 1];
        for (int i = prev + 1; i < sp.cardinality(); i++) {
            int[] newCurr = curr.clone();
            newCurr[curr.length - needed] = i;
            generateOne(sp, newCurr, needed - 1, cons);
        }
    }

    public static int[][] toProjective(LinearSpace space, FixBS[] spread) {
        int half = space.half();
        int pc = half * half + half + 1;
        int[][] lines = new int[pc][];
        for (int i = 0; i < spread.length; i++) {
            Set<FixBS> unique = new HashSet<>();
            FixBS el = spread[i].copy();
            el.set(0);
            int cnt = 0;
            for (int j = 0; j < space.cardinality(); j++) {
                FixBS bs = new FixBS(space.cardinality());
                for (int k = el.nextSetBit(0); k >= 0; k = el.nextSetBit(k + 1)) {
                    bs.set(space.add(k, j));
                }
                if (unique.add(bs)) {
                    lines[half * i + cnt++] = IntStream.concat(bs.stream(), IntStream.of(half * half + i)).toArray();
                }
                if (cnt == half) {
                    break;
                }
            }
        }
        lines[half * half + half] = IntStream.range(half * half, half * half + half + 1).toArray();
        return lines;
    }

    public static boolean isDesargues(Liner liner) {
        int l0 = 0;
        int o = 0;
        for (int la : liner.lines(o)) {
            if (la == o) {
                continue;
            }
            for (int lb : liner.lines(o)) {
                if (lb == o || lb == la) {
                    continue;
                }
                for (int lc : liner.lines(o)) {
                    if (lc == o || lc == la || lc == lb) {
                        continue;
                    }
                    for (int a : liner.points(la)) {
                        if (a == o) {
                            continue;
                        }
                        for (int b : liner.points(lb)) {
                            if (b == o) {
                                continue;
                            }
                            int x = liner.intersection(liner.line(a, b), l0);
                            for (int c : liner.points(lc)) {
                                if (c == o || liner.collinear(a, b, c)) {
                                    continue;
                                }
                                int y = liner.intersection(liner.line(a, c), l0);
                                int z = liner.intersection(liner.line(b, c), l0);
                                for (int a1 : liner.point(la)) {
                                    if (a1 == a || a1 == o) {
                                        continue;
                                    }
                                    int b1 = liner.intersection(liner.line(a1, x), lb);
                                    if (b1 == o) {
                                        continue;
                                    }
                                    int c1 = liner.intersection(liner.line(a1, y), lc);
                                    if (!liner.collinear(b1, c1, z)) {
                                        return false;
                                    }
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
