package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FuzzyBalLinerTest {
    @Test
    public void generateAlt() {
        int v = 15;
        int k = 3;
        System.out.println("com " + v + " " + k);
        FuzzyBalLiner lb = FuzzyBalLiner.of(v, k, new int[][]{{0, 1, 3}, {0, 2, 4}, {1, 2, 5}});
        List<FuzzyBalLiner> liners = List.of(lb);
        int currPoint = 3;
        while (currPoint < v - 1) {
            liners = nextStageAlt(liners, currPoint, (l, c) -> {});
            System.out.println(currPoint++ + " " + liners.size());
        }
    }

    @Test
    public void generate() {
        int v = 57;
        int k = 8;
        System.out.println("com " + v + " " + k);
        int r = (v - 1) / (k - 1);
        int b = v * r / k;
        int[][][] lines = readLast("com", v, k, () -> new int[][][]{beamBlocks(v, k)});
        List<FuzzyBalLiner> liners = Arrays.stream(lines).map(lns -> FuzzyBalLiner.of(v, k, lns)).toList();
        int needed = (b - lines[0].length) * (k - 2);
        while (needed > 0) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStage(v, k, liners, (l, c) -> {}, cnt);
            needed--;
            System.out.println(needed + " " + liners.size() + " " + cnt.get());
        }
    }

    @Test
    public void generateAP() {
        int v = 28;
        int k = 4;
        System.out.println("ap " + v + " " + k);
        int r = (v - 1) / (k - 1);
        int b = v * r / k;
        int[][][] lines = readLast("ap", v, k, () -> new int[][][]{beamBlocks(v, k)});
        List<FuzzyBalLiner> liners = Arrays.stream(lines).map(lns -> FuzzyBalLiner.of(v, k, lns)).toList();
        int needed = (b - lines[0].length) * (k - 2);
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
            liners = nextStage(v, k, liners, (l, c) -> checkUnderCap(l, cap), cnt);
            needed--;
            System.out.println(needed + " " + liners.size() + " " + cnt.get());
        }
    }

    private static List<FuzzyBalLiner> nextStage(int v, int k, List<FuzzyBalLiner> partials, BiConsumer<FuzzyBalLiner, Col> checker, AtomicLong cnt) {
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
//                if (chars[i][0] % (k - 1) == 0) {
//                    variety = 2 * variety / 3;
//                }
                if (variety < min) {
                    min = variety;
                    pt = i;
                }
            }
            for (Col c : lnr.calcSmallest()) {
                try {
                    FuzzyBalLiner copy = lnr.copy();
                    Queue<Rel> q = new ArrayDeque<>();
                    q.add(c);
                    copy.update(q);
                    //additionalCheck(copy, c);
                    checker.accept(copy, c);
                    cnt.incrementAndGet();
                    sink.accept(copy);
                } catch (IllegalArgumentException e) {
                    // ok
                }
            }
        }).collect(Collectors.toMap(l -> toLiner(l.removeTwins()).getCanonicalOld(), Function.identity(), (a, b) -> a, ConcurrentHashMap::new));
        return new ArrayList<>(nonIso.values());
    }

    private static List<FuzzyBalLiner> nextStageAlt(List<FuzzyBalLiner> partials, int currPoint, BiConsumer<FuzzyBalLiner, Col> checker) {
        Map<FixBS, FuzzyBalLiner> nonIso = partials.stream().parallel().<FuzzyBalLiner>mapMulti((lnr, sink) -> {
            int v = lnr.getV();
            int k = lnr.getK();
            int r = (v - 1) / (k - 1);
            for (int i = currPoint + 1; i < lnr.getV(); i++) {
                int[] permutation = IntStream.range(0, lnr.getV()).toArray();
                permutation[i] = currPoint;
                permutation[currPoint] = i;
                FuzzyBalLiner liner = lnr.permute(permutation);
                Consumer<FuzzyBalLiner> cons = next -> {
                    Set<FixBS> lines = next.lines();
                    int[] lc = new int[v]; // TODO this is wrong for k >= 5
                    for (FixBS l : lines) {
                        for (int pt = l.nextSetBit(0); pt >= 0; pt = l.nextSetBit(pt+1)) {
                            if (++lc[pt] > r) {
                                return;
                            }
                        }
                    }
                    sink.accept(next);
                };
                fillMissing(liner, currPoint, checker, cons);
            }
        }).collect(Collectors.toMap(l -> toLiner(l.removeEmpty()).getCanonicalOld(), Function.identity(), (a, b) -> a, ConcurrentHashMap::new));
        return new ArrayList<>(nonIso.values());
    }

    private static Integer findNotJoined(FuzzyBalLiner liner, int currPoint) {
        ex: for (int i = 0; i < currPoint; i++) {
            for (int j = 0; j < liner.getV(); j++) {
                if (liner.collinear(currPoint, i, j)) {
                    continue ex;
                }
            }
            return i;
        }
        return null;
    }

    private static void fillMissing(FuzzyBalLiner liner, int currPoint, BiConsumer<FuzzyBalLiner, Col> checker, Consumer<FuzzyBalLiner> cons) {
        Integer nj = findNotJoined(liner, currPoint);
        if (nj == null) {
            cons.accept(liner);
            return;
        }
        List<Col> undefined = liner.undefinedTriples(currPoint, nj);
        for (Col c : undefined) {
            try {
                FuzzyBalLiner copy = liner.copy();
                Queue<Rel> q = new ArrayDeque<>();
                q.add(c);
                copy.update(q);
                checker.accept(copy, c);
                fillMissing(copy, currPoint, checker, cons);
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
    }

    private static void additionalCheck(FuzzyBalLiner copy, Col c) {
        FixBS nl = copy.line(c.f(), c.s());
        int crd = nl.cardinality();
        int k = copy.getK();
        if (crd == k) {
            return;
        }
        Queue<Rel> q1 = new ArrayDeque<>();
        for (FixBS ol : copy.lines()) {
            int oc = ol.cardinality();
            if (ol.equals(nl) || oc == k) {
                continue;
            }
            boolean intersects = ol.intersects(nl);
            if (intersects && oc + crd - 1 <= k) {
                continue;
            }
            if (!intersects && oc + crd <= k) {
                continue;
            }
            for (int x = ol.nextSetBit(0); x >= 0; x = ol.nextSetBit(x + 1)) {
                for (int y = nl.nextSetBit(0); y >= 0; y = nl.nextSetBit(y + 1)) {
                    for (int z = ol.nextSetBit(x + 1); z >= 0; z = ol.nextSetBit(z + 1)) {
                        if (z == y) {
                            continue;
                        }
                        q1.add(new Trg(x, y, z));
                    }
                    for (int z = nl.nextSetBit(y + 1); z >= 0; z = nl.nextSetBit(z + 1)) {
                        if (z == x) {
                            continue;
                        }
                        q1.add(new Trg(x, y, z));
                    }
                }
            }
        }
        copy.update(q1);
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

    private static void updateAP(FuzzyBalLiner liner, Col col) {
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

    private static int[][][] readLast(String prefix, int v, int k, Supplier<int[][][]> fallback) {
        try (FileInputStream fis = new FileInputStream("/home/ihromant/maths/partials/bases/" + prefix + "-" + v + "-" + k + ".txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            int left;
            int lineCount = v * (v - 1) / k / (k - 1);
            int[][][] partials = null;
            while ((line = br.readLine()) != null) {
                left = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                line = br.readLine();
                int partialsCount = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                int partialSize = lineCount - left;
                partials = new int[partialsCount][partialSize][k];
                for (int i = 0; i < partialsCount; i++) {
                    int[][] partial = partials[i];
                    for (int j = 0; j < partialSize; j++) {
                        String[] pts = br.readLine().split(" ");
                        for (int l = 0; l < k; l++) {
                            partial[j][l] = Integer.parseInt(pts[l]);
                        }
                    }
                    br.readLine();
                }
            }
            return partials;
        } catch (FileNotFoundException e) {
            return fallback.get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
