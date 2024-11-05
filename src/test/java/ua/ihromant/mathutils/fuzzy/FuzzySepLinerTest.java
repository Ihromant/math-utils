package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.plane.Quad;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FuzzySepLinerTest {
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
        Set<FixBS> lines = Arrays.stream(antiMoufang).map(l -> FixBS.of(10, l)).collect(Collectors.toSet());
        FuzzySepLiner base = new FuzzySepLiner(antiMoufang, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 7, 9)});
        int pc = base.getPc();
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        FuzzySepLiner next = base.intersectLines();
        System.out.println(next.getD().size() + " " + next.getL().size() + " " + next.getT().size() + " " + (next.getL().size() + next.getT().size()));
        next = enhanceFullFano(next);
        System.out.println(next.getS().size() + " " + next.getD().size() + " " + next.getL().size() + " " + next.getT().size() + " " + (next.getL().size() + next.getT().size()));
//        base = next.subLiner(FixBS.of(153, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 32, 43, 54, 94, 95));
//        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        List<int[]> configs = findAntiMoufang(next);
        System.out.println("Antimoufang " + configs.size());
        for (int i = 0; i < 10; i++) {
            for (int j = i + 1; j < 10; j++) {
                for (int k = j + 1; k < 10; k++) {
                    int[] triple = new int[]{i, j, k};
                    FixBS bs = FixBS.of(10, triple);
                    boolean line = lines.contains(bs);
                    for (int[] config : configs) {
                        if (line) {
                            next.colline(config[i], config[j], config[k]);
                        } else {
                            next.triangule(config[i], config[j], config[k]);
                        }
                    }
                }
            }
        }
        System.out.println("Enhanced");
        next.update();
        next = enhanceFullFano(next);
        configs = findAntiMoufang(next);
        System.out.println("Antimoufang " + configs.size());
        System.out.println(next.getS().size() + " " + next.getD().size() + " " + next.getL().size() + " " + next.getT().size() + " " + (next.getL().size() + next.getT().size()));
        for (int i = 0; i < 10; i++) {
            for (int j = i + 1; j < 10; j++) {
                for (int k = j + 1; k < 10; k++) {
                    int[] triple = new int[]{i, j, k};
                    FixBS bs = FixBS.of(10, triple);
                    boolean line = lines.contains(bs);
                    for (int[] config : configs) {
                        if (line) {
                            next.colline(config[i], config[j], config[k]);
                        } else {
                            next.triangule(config[i], config[j], config[k]);
                        }
                    }
                }
            }
        }
        System.out.println("Enhanced");
        next.update();
        next = enhanceFullFano(next);
        configs = findAntiMoufang(next);
        System.out.println("Antimoufang " + configs.size());
        System.out.println(next.getS().size() + " " + next.getD().size() + " " + next.getL().size() + " " + next.getT().size() + " " + (next.getL().size() + next.getT().size()));
        for (int i = 0; i < 10; i++) {
            for (int j = i + 1; j < 10; j++) {
                for (int k = j + 1; k < 10; k++) {
                    int[] triple = new int[]{i, j, k};
                    FixBS bs = FixBS.of(10, triple);
                    boolean line = lines.contains(bs);
                    for (int[] config : configs) {
                        if (line) {
                            next.colline(config[i], config[j], config[k]);
                        } else {
                            next.triangule(config[i], config[j], config[k]);
                        }
                    }
                }
            }
        }
        System.out.println("Enhanced");
        next.update();
        next = enhanceFullFano(next);
        configs = findAntiMoufang(next);
        System.out.println("Antimoufang " + configs.size());
        System.out.println(next.getS().size() + " " + next.getD().size() + " " + next.getL().size() + " " + next.getT().size() + " " + (next.getL().size() + next.getT().size()));
        for (int i = 0; i < 10; i++) {
            for (int j = i + 1; j < 10; j++) {
                for (int k = j + 1; k < 10; k++) {
                    int[] triple = new int[]{i, j, k};
                    FixBS bs = FixBS.of(10, triple);
                    boolean line = lines.contains(bs);
                    for (int[] config : configs) {
                        if (line) {
                            next.colline(config[i], config[j], config[k]);
                        } else {
                            next.triangule(config[i], config[j], config[k]);
                        }
                    }
                }
            }
        }
        System.out.println("Enhanced");
        next.update();
        next = enhanceFullFano(next);
        configs = findAntiMoufang(next);
        System.out.println("Antimoufang " + configs.size());
        System.out.println(next.getS().size() + " " + next.getD().size() + " " + next.getL().size() + " " + next.getT().size() + " " + (next.getL().size() + next.getT().size()));
        for (int i = 0; i < 10; i++) {
            for (int j = i + 1; j < 10; j++) {
                for (int k = j + 1; k < 10; k++) {
                    int[] triple = new int[]{i, j, k};
                    FixBS bs = FixBS.of(10, triple);
                    boolean line = lines.contains(bs);
                    for (int[] config : configs) {
                        if (line) {
                            next.colline(config[i], config[j], config[k]);
                        } else {
                            next.triangule(config[i], config[j], config[k]);
                        }
                    }
                }
            }
        }
        System.out.println("Enhanced");
        next.update();
        next = enhanceFullFano(next);
        configs = findAntiMoufang(next);
        System.out.println("Antimoufang " + configs.size());
        System.out.println(next.getS().size() + " " + next.getD().size() + " " + next.getL().size() + " " + next.getT().size() + " " + (next.getL().size() + next.getT().size()));
        System.out.println(next.logLines().stream().filter(ln -> ln.cardinality() > 2).collect(Collectors.toList()));
    }

    public FuzzySepLiner enhanceFullFano(FuzzySepLiner liner) {
        while (true) {
            boolean incorrect = false;
            List<Quad> full = liner.quads(3);
            System.out.println("Quads " + full.size());
            for (Quad q : full) {
                FixBS abcd = liner.intersection(new Pair(q.a(), q.b()), new Pair(q.c(), q.d()));
                FixBS acbd = liner.intersection(new Pair(q.a(), q.c()), new Pair(q.b(), q.d()));
                FixBS adbc = liner.intersection(new Pair(q.a(), q.d()), new Pair(q.b(), q.c()));
                for (int x = abcd.nextSetBit(0); x >= 0; x = abcd.nextSetBit(x + 1)) {
                    for (int x1 = abcd.nextSetBit(x + 1); x1 >= 0; x1 = abcd.nextSetBit(x1 + 1)) {
                        if (!liner.same(x, x1)) {
                            incorrect = true;
                            liner.merge(x, x1);
                            //System.out.println("Points " + x + " " + x1 + " were merged");
                        }
                    }
                    for (int y = acbd.nextSetBit(0); y >= 0; y = acbd.nextSetBit(y + 1)) {
                        for (int y1 = acbd.nextSetBit(y + 1); y1 >= 0; y1 = acbd.nextSetBit(y1 + 1)) {
                            if (!liner.same(y, y1)) {
                                incorrect = true;
                                liner.merge(y, y1);
                                //System.out.println("Points " + y + " " + y1 + " were merged");
                            }
                        }
                        for (int z = adbc.nextSetBit(0); z >= 0; z = adbc.nextSetBit(z + 1)) {
                            for (int z1 = adbc.nextSetBit(z + 1); z1 >= 0; z1 = adbc.nextSetBit(z1 + 1)) {
                                if (!liner.same(z, z1)) {
                                    incorrect = true;
                                    liner.merge(z, z1);
                                    //System.out.println("Points " + z + " " + z1 + " were merged");
                                }
                            }
                            if (!liner.collinear(x, y, z)) {
                                incorrect = true;
                                liner.colline(x, y, z);
                            }
                        }
                    }
                }
            }
            if (!incorrect) {
                return liner;
            }
            liner.update();
            System.out.println("Before " + liner.getPc());
            liner = liner.quotient();
            liner.update();
            System.out.println("After " + liner.getPc());
        }
    }

    public List<int[]> findAntiMoufang(FuzzySepLiner liner) {
        List<int[]> result = new ArrayList<>();
        Liner l = new Liner(liner.getPc(), liner.logLines().stream().filter(ln -> ln.cardinality() > 2).map(ln -> ln.stream().toArray()).toArray(int[][]::new));
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
                                        if (b == o || l.line(a, b) < 0) {
                                            continue;
                                        }
                                        for (int b1 : line2) {
                                            if (b1 == o || b1 == b || l.line(a1, b1) < 0) {
                                                continue;
                                            }
                                            for (int c : line3) {
                                                if (c == o || l.line(a, c) < 0 || l.line(b, c) < 0) {
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
