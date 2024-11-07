package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FuzzyBalLinerTest {
    @Test
    public void generate() {
        int v = 15;
        int k = 3;
        int r = (v - 1) / (k - 1);
        int b = v * r / k;
        int[][] lines = beamBlocks(v, k);
        List<FuzzyBalLiner> liners = List.of(FuzzyBalLiner.of(v, k, lines));
        int needed = (b - lines.length) * (k - 2);
        while (needed > 0) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStage(v, k, liners, l -> {}, cnt);
            needed--;
            System.out.println(needed + " " + liners.size() + " " + cnt.get());
        }
    }

    @Test
    public void generateAP() {
        int v = 28;
        int k = 4;
        int r = (v - 1) / (k - 1);
        int b = v * r / k;
        int[][] lines = beamBlocks(v, k);
        List<FuzzyBalLiner> liners = List.of(FuzzyBalLiner.of(v, k, lines));
        int needed = (b - lines.length) * (k - 2);
        while (needed > 0) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStage(v, k, liners, FuzzyBalLinerTest::updateAP, cnt);
            needed--;
            System.out.println(needed + " " + liners.size() + " " + cnt.get());
        }
    }

    @Test
    public void generateUnderCap() {
        int v = 15;
        int k = 3;
        int cap = 7;
        int r = (v - 1) / (k - 1);
        int b = v * r / k;
        int[][] lines = beamBlocks(v, k);
        List<FuzzyBalLiner> liners = List.of(FuzzyBalLiner.of(v, k, lines));
        int needed = (b - lines.length) * (k - 2);
        while (needed > 0) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStage(v, k, liners, l -> checkUnderCap(l, cap), cnt);
            needed--;
            System.out.println(needed + " " + liners.size() + " " + cnt.get());
        }
    }

    private static List<FuzzyBalLiner> nextStage(int v, int k, List<FuzzyBalLiner> partials, Consumer<FuzzyBalLiner> checker, AtomicLong cnt) {
        int r = (v - 1) / (k - 1);
        int allC = r * (k - 1) * (k - 2) / 2;
        int all = (v - 1) * (v - 2) / 2;
        Map<FixBS, FuzzyBalLiner> nonIso = partials.stream().parallel().<FuzzyBalLiner>mapMulti((lnr, sink) -> {
            int[][] chars = lnr.pointChars();
            int min = Integer.MAX_VALUE;
            int pt = -1;
            for (int i = 0; i < v; i++) {
                if (allC == chars[i][0]) {
                    continue;
                }
                int needed = allC - chars[i][0];
                int variants = all - chars[i][0] - chars[i][1];
                if (variants < needed) {
                    return;
                }
                int variety = variants * needed;
                if (variety < min) {
                    min = variety;
                    pt = i;
                }
            }
            for (Col c : lnr.undefinedTriples(pt)) {
                try {
                    FuzzyBalLiner copy = lnr.copy();
                    Queue<Rel> q = new ArrayDeque<>();
                    q.add(c);
                    copy.update(q);
                    checker.accept(copy);
                    cnt.incrementAndGet();
                    sink.accept(copy);
                } catch (IllegalArgumentException e) {
                    // ok
                }
            }
        }).collect(Collectors.toMap(l -> toLiner(l.removeTwins()).getCanonicalOld(), Function.identity(), (a, b) -> a, ConcurrentHashMap::new));
        return new ArrayList<>(nonIso.values());
    }

    private static int[][] beamBlocks(int v, int k) {
        int r = (v - 1) / (k - 1);
        int[][] blocks = new int[r + 1][k];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < k - 1; j++) {
                blocks[i][j + 1] = 1 + i * (k - 1) + j;
            }
        }
        for (int i = 0; i < k; i++) {
            blocks[r][i] = 1 + (k - 1) * i;
        }
        return blocks;
    }

    private static Liner toLiner(FuzzyBalLiner liner) {
        return new Liner(liner.getV(), liner.lines().stream().map(l -> l.stream().toArray()).toArray(int[][]::new));
    }

    private static void updateAP(FuzzyBalLiner liner) {
        int v = liner.getV();
        while (true) {
            Queue<Rel> queue = new ArrayDeque<>(v);
            for (int a = 0; a < v; a++) {
                for (int b = a + 1; b < v; b++) {
                    for (int c = b + 1; c < v; c++) {
                        if (!liner.triangle(a, b, c)) {
                            continue;
                        }
                        for (int d = c + 1; d < v; d++) {
                            if (!liner.triangle(a, b, d) || !liner.triangle(a, c, d) || !liner.triangle(b, c, d)) {
                                continue;
                            }
                            for (int i = 0; i < v; i++) {
                                if (liner.collinear(a, b, i) && liner.collinear(c, d, i)) {
                                    enforceAP(liner, v, a, b, c, d, queue);
                                }
                                if (liner.collinear(a, c, i) && liner.collinear(b, d, i)) {
                                    enforceAP(liner, v, a, c, b, d, queue);
                                }
                                if (liner.collinear(a, d, i) && liner.collinear(b, c, i)) {
                                    enforceAP(liner, v, a, d, b, c, queue);
                                }
                            }
                        }
                    }
                }
            }
            if (queue.isEmpty()) {
                return;
            }
            liner.update(queue);
        }
    }

    private static void enforceAP(FuzzyBalLiner liner, int v, int a, int b, int c, int d, Queue<Rel> queue) {
        for (int j = 0; j < v; j++) {
            if (liner.collinear(a, d, j)) {
                if (!liner.triangle(b, c, j)) {
                    queue.add(new Trg(b, c, j));
                }
            }
            if (liner.collinear(b, c, j)) {
                if (!liner.triangle(a, d, j)) {
                    queue.add(new Trg(a, d, j));
                }
            }
            if (liner.collinear(a, c, j)) {
                if (!liner.triangle(b, d, j)) {
                    queue.add(new Trg(b, d, j));
                }
            }
            if (liner.collinear(b, d, j)) {
                if (!liner.triangle(a, c, j)) {
                    queue.add(new Trg(a, c, j));
                }
            }
        }
    }

    private static void checkUnderCap(FuzzyBalLiner liner, int cap) {
        int v = liner.getV();
        for (int a = 0; a < v; a++) {
            for (int b = a + 1; b < v; b++) {
                for (int c = b + 1; c < v; c++) {
                    if (liner.collinear(a, b, c)) {
                        continue;
                    }
                    FixBS newPts = FixBS.of(v, a, b, c);
                    FixBS base = FixBS.of(v);
                    while (!newPts.isEmpty()) {
                        base.or(newPts);
                        if (base.cardinality() > cap) {
                            throw new IllegalArgumentException();
                        }
                        FixBS nextNew = new FixBS(v);
                        for (int x = newPts.nextSetBit(0); x >= 0; x = newPts.nextSetBit(x+1)) {
                            for (int y = newPts.nextSetBit(x + 1); y >= 0; y = newPts.nextSetBit(y+1)) {
                                for (int z = 0; z < v; z++) {
                                    if (!base.get(z) && liner.collinear(x, y, z)) {
                                        nextNew.set(z);
                                    }
                                }
                            }
                        }
                        newPts = nextNew;
                    }
                }
            }
        }
    }
}
