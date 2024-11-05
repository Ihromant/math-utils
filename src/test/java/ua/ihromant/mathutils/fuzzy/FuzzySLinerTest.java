package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class FuzzySLinerTest {
    @Test
    public void testMoufang() {
        int[][] antiMoufang = {
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {0, 7, 8},
                {1, 3, 7},
                {1, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9},
                {4, 6, 9}
        };
        FuzzySLiner base = FuzzySLiner.of(antiMoufang, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 7, 9)});
        int pc = base.getPc();
        Set<FixBS> lines = Arrays.stream(antiMoufang).map(l -> FixBS.of(pc, l)).collect(Collectors.toSet());
        base.printChars();
        FuzzySLiner next = base.intersectLines();
        next.printChars();
        next = enhanceFullFano(next);
        next.printChars();
        base = next.subLiner(base.getPc());
        base.printChars();
        List<int[]> configs = findAntiMoufang(next);
        System.out.println("Antimoufang " + configs.size());
        Queue<Rel> queue = new ArrayDeque<>(next.getPc());
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
                for (int k = j + 1; k < pc; k++) {
                    FixBS bs = FixBS.of(pc, i, j, k);
                    boolean line = lines.contains(bs);
                    for (int[] config : configs) {
                        if (line) {
                            queue.add(new Col(config[i], config[j], config[k]));
                        } else {
                            queue.add(new Trg(config[i], config[j], config[k]));
                        }
                    }
                }
            }
        }
        next.update(queue);
        System.out.println("Enhanced Antimoufang");
        next = enhanceFullFano(next);
        FixBS determined = next.determinedSet();
        configs = findAntiMoufang(next);
        next.printChars();
        System.out.println("Antimoufang " + configs.size());
    }

    public FuzzySLiner enhanceFullFano(FuzzySLiner liner) {
        int cnt = 0;
        while (true) {
            Queue<Rel> queue = new ArrayDeque<>(liner.getPc());
            for (int a = 0; a < liner.getPc(); a++) {
                for (int b = a + 1; b < liner.getPc(); b++) {
                    if (!liner.distinct(a, b)) {
                        continue;
                    }
                    for (int c = b + 1; c < liner.getPc(); c++) {
                        if (!liner.triangle(a, b, c)) {
                            continue;
                        }
                        for (int d = c + 1; d < liner.getPc(); d++) {
                            if (!liner.triangle(a, b, d) || !liner.triangle(a, c, d) || !liner.triangle(b, c, d)) {
                                continue;
                            }
                            int abcd = -1;
                            int acbd = -1;
                            int adbc = -1;
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(a, b, i) && liner.collinear(c, d, i)) {
                                    if (abcd < 0) {
                                        abcd = i;
                                    } else {
                                        queue.add(new Same(abcd, i));
                                    }
                                    if (acbd >= 0 && adbc >= 0 && !liner.collinear(acbd, adbc, i)) {
                                        queue.add(new Col(acbd, adbc, i));
                                    }
                                }
                                if (liner.collinear(a, c, i) && liner.collinear(b, d, i)) {
                                    if (acbd < 0) {
                                        acbd = i;
                                    } else {
                                        queue.add(new Same(acbd, i));
                                    }
                                    if (abcd >= 0 && adbc >= 0 && !liner.collinear(abcd, adbc, i)) {
                                        queue.add(new Col(abcd, adbc, i));
                                    }
                                }
                                if (liner.collinear(a, d, i) && liner.collinear(b, c, i)) {
                                    if (adbc < 0) {
                                        adbc = i;
                                    } else {
                                        queue.add(new Same(adbc, i));
                                    }
                                    if (abcd >= 0 && acbd >= 0 && !liner.collinear(abcd, acbd, i)) {
                                        queue.add(new Col(abcd, acbd, i));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (queue.isEmpty()) {
                return liner;
            }
            System.out.println("Enhancing fano " + cnt++ + " iteration, changes " + queue.size());
            liner.update(queue);
            System.out.println("Before " + liner.getPc());
            liner = liner.quotient();
            System.out.println("After " + liner.getPc());
        }
    }

    public List<int[]> findAntiMoufang(FuzzySLiner liner) {
        int pc = liner.getPc();
        List<int[]> result = new ArrayList<>();
        for (int o = 0; o < pc; o++) {
            for (int a = 0; a < pc; a++) {
                if (!liner.distinct(o, a)) {
                    continue;
                }
                for (int a1 = 0; a1 < pc; a1++) {
                    if (!liner.collinear(0, a, a1)) {
                        continue;
                    }
                    for (int b = 0; b < pc; b++) {
                        if (!liner.triangle(0, a, b)) {
                            continue;
                        }
                        for (int b1 = 0; b1 < pc; b1++) {
                            if (!liner.collinear(o, b, b1)) {
                                continue;
                            }
                            for (int c = 0; c < pc; c++) {
                                if (!liner.triangle(o, a, c) || !liner.triangle(o, b, c)) {
                                    continue;
                                }
                                for (int c1 = 0; c1 < pc; c1++) {
                                    if (!liner.collinear(o, c, c1)) {
                                        continue;
                                    }
                                    for (int x = 0; x < pc; x++) {
                                        if (!liner.collinear(x, a, b) || !liner.collinear(x, a1, b1) || !liner.triangle(x, o, c)) {
                                            continue;
                                        }
                                        for (int y = 0; y < pc; y++) {
                                            if (!liner.collinear(y, a, c) || !liner.collinear(y, a1, c1) || !liner.collinear(o, x, y)) {
                                                continue;
                                            }
                                            for (int z = 0; z < pc; z++) {
                                                if (!liner.collinear(z, b, c) || !liner.collinear(z, b1, c1) || !liner.triangle(z, x, y)) {
                                                    continue;
                                                }
                                                result.add(new int[]{o, a, a1, b, b1, c, c1, x, y, z});
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public List<int[]> findAntiMoufangQuick(FuzzySLiner liner, int cap) {
        List<int[]> result = new ArrayList<>();
        Liner l = new Liner(liner.getPc(), liner.capLines(cap).stream().filter(ln -> ln.cardinality() > 2).map(ln -> ln.stream().toArray()).toArray(int[][]::new));
        for (int o = 0; o < l.pointCount(); o++) {
            int[] lines = l.lines(o);
            if (lines.length < 4) {
                continue;
            }
            for (int l1 : lines) {
                for (int l2 : lines) {
                    if (l1 == l2) {
                        continue;
                    }
                    for (int l3 : lines) {
                        if (l1 == l3 || l2 == l3) {
                            continue;
                        }
                        for (int l4 : lines) {
                            if (l1 == l4 || l2 == l4 || l3 == l4) {
                                continue;
                            }
                            int[] line1 = l.line(l1);
                            int[] line2 = l.line(l2);
                            int[] line3 = l.line(l3);
                            for (int a : line1) {
                                if (o == a) {
                                    continue;
                                }
                                for (int a1 : line1) {
                                    if (o == a1 || a == a1) {
                                        continue;
                                    }
                                    for (int b : line2) {
                                        if (b == o || l.line(a, b) < 0 || !liner.triangle(o, a, b)) {
                                            continue;
                                        }
                                        for (int b1 : line2) {
                                            if (b1 == o || b1 == b || l.line(a1, b1) < 0) {
                                                continue;
                                            }
                                            for (int c : line3) {
                                                if (c == o || l.line(a, c) < 0 || l.line(b, c) < 0 || !liner.triangle(o, a, c) || !liner.triangle(o, b, c)) {
                                                    continue;
                                                }
                                                for (int c1 : line3) {
                                                    if (c1 == c || c1 == o || l.line(a1, c1) < 0 || l.line(b1, c1) < 0) {
                                                        continue;
                                                    }
                                                    int x = l.intersection(l.line(a, b), l.line(a1, b1));
                                                    int y = l.intersection(l.line(a, c), l.line(a1, c1));
                                                    int z = l.intersection(l.line(b, c), l.line(b1, c1));
                                                    if (x < 0 || y < 0 || z < 0) {
                                                        continue;
                                                    }
                                                    if (l.flag(l4, x) && l.flag(l4, y)
                                                            && (liner.triangle(o, x, z) || liner.triangle(o, y, z) || liner.triangle(x, y, z))) {
                                                        result.add(new int[]{o, a, a1, b, b1, c, c1, x, y, z});
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
