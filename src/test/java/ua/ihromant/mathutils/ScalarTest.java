package ua.ihromant.mathutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.auto.TernaryAutomorphisms;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.plane.NumeratedAffinePlane;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ScalarTest {
    @Test
    public void testAffineScalars() throws IOException {
        String name = "dhall9";
        int order = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + order + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            int line = TernaryAutomorphisms.findTranslationLine(proj);
            if (line < 0) {
                throw new IllegalArgumentException("Not translation");
            } else {
                System.out.println(name + " dropped line " + line);
            }
            NumeratedAffinePlane aff = new NumeratedAffinePlane(proj, line);
            int lc = aff.lineCount();
            int[][] triples = aff.triples();
            QuickFind find = new QuickFind(triples.length * lc);
            for (int l1 = 0; l1 < lc; l1++) {
                for (int l2 = l1 + 1; l2 < lc; l2++) {
                    int inter = aff.intersection(l1, l2);
                    if (inter < 0) {
                        continue;
                    }
                    int[] fPts = aff.line(l1);
                    int[] sPts = aff.line(l2);
                    int fst = fPts[0] == inter ? fPts[1] : fPts[0];
                    for (int i = 0; i < order; i++) {
                        int snd = sPts[i];
                        if (snd == inter) {
                            continue;
                        }
                        int[] map = new int[order];
                        int par = aff.line(fst, snd);
                        for (int j = 0; j < order; j++) {
                            int aSnd = sPts[j];
                            if (aSnd == inter) {
                                map[j] = aff.idxOf(l1, inter);
                                continue;
                            }
                            int parInter = aff.intersection(l1, aff.parallel(par, aSnd));
                            map[j] = aff.idxOf(l1, parInter);
                        }
                        for (int[] tr : triples) {
                            int p = l2 * triples.length + aff.trIdx(tr[0], tr[1], tr[2]);
                            int q = l1 * triples.length + aff.trIdx(map[tr[0]], map[tr[1]], map[tr[2]]);
                            find.union(p, q);
                        }
                    }
                }
            }
            System.out.println(find.components().stream().map(FixBS::cardinality).toList());
        }
    }

    @Test
    public void testProjectiveScalars() throws IOException {
        String name = "dhall9";
        int order = 9;
        int[][] quads = new int[(order + 1) * order * (order - 1) * (order - 2)][4];
        int[][][][] quadRev = new int[order + 1][order + 1][order + 1][order + 1];
        int idx = 0;
        for (int i = 0; i <= order; i++) {
            for (int j = 0; j <= order; j++) {
                if (i == j) {
                    continue;
                }
                for (int k = 0; k <= order; k++) {
                    if (i == k || j == k) {
                        continue;
                    }
                    for (int l = 0; l <= order; l++) {
                        if (i == l || j == l || k == l) {
                            continue;
                        }
                        quads[idx][0] = i;
                        quads[idx][1] = j;
                        quads[idx][2] = k;
                        quads[idx][3] = l;
                        quadRev[i][j][k][l] = idx++;
                    }
                }
            }
        }
        try (InputStream is = getClass().getResourceAsStream("/proj" + order + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            int lc = proj.lineCount();
            int[][] ptIdxes = new int[lc][lc];
            for (int l = 0; l < lc; l++) {
                Arrays.fill(ptIdxes[l], -1);
                int[] pts = proj.line(l);
                for (int i = 0; i < pts.length; i++) {
                    ptIdxes[l][pts[i]] = i;
                }
            }
            QuickFind find = new QuickFind(quads.length * lc);
            for (int l1 = 0; l1 < lc; l1++) {
                for (int l2 = l1 + 1; l2 < lc; l2++) {
                    for (int pt = 0; pt < lc; pt++) {
                        if (proj.flag(l1, pt) || proj.flag(l2, pt)) {
                            continue;
                        }
                        int[] sPts = proj.line(l2);
                        int[] map = new int[order + 1];
                        for (int i = 0; i <= order; i++) {
                            int snd = sPts[i];
                            int fst = proj.intersection(l1, proj.line(pt, snd));
                            map[i] = ptIdxes[l1][fst];
                        }
                        for (int[] quad : quads) {
                            int p = l2 * quads.length + quadRev[quad[0]][quad[1]][quad[2]][quad[3]];
                            int q = l1 * quads.length + quadRev[map[quad[0]]][map[quad[1]]][map[quad[2]]][map[quad[3]]];
                            find.union(p, q);
                        }
                    }
                }
            }
            System.out.println(find.components().stream().map(FixBS::cardinality).toList());
        }
    }

    @Test
    public void testVectors() throws IOException {
        String name = "bbh1";
        int order = 16;
        try (InputStream is = getClass().getResourceAsStream("/proj" + order + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            for (int line = 0; line < proj.lineCount(); line++) {
                NumeratedAffinePlane aff = new NumeratedAffinePlane(proj, line);
                int lc = aff.lineCount();
                int[][] pairs = aff.pairs();
                QuickFind find = new QuickFind(pairs.length * lc);
                for (int l1 = 0; l1 < lc; l1++) {
                    for (int l2 = l1 + 1; l2 < lc; l2++) {
                        int inter = aff.intersection(l1, l2);
                        if (inter >= 0) {
                            continue;
                        }
                        int[] fPts = aff.line(l1);
                        int[] sPts = aff.line(l2);
                        int fst = fPts[0] == inter ? fPts[1] : fPts[0];
                        for (int i = 0; i < order; i++) {
                            int snd = sPts[i];
                            if (snd == inter) {
                                continue;
                            }
                            int[] map = new int[order];
                            int par = aff.line(fst, snd);
                            for (int j = 0; j < order; j++) {
                                int aSnd = sPts[j];
                                if (aSnd == inter) {
                                    map[j] = aff.idxOf(l1, inter);
                                    continue;
                                }
                                int parInter = aff.intersection(l1, aff.parallel(par, aSnd));
                                map[j] = aff.idxOf(l1, parInter);
                            }
                            for (int[] tr : pairs) {
                                int p = l2 * pairs.length + aff.pairIdx(tr[0], tr[1]);
                                int q = l1 * pairs.length + aff.pairIdx(map[tr[0]], map[tr[1]]);
                                find.union(p, q);
                            }
                        }
                    }
                }
                List<FixBS> components = find.components();
                int[] zeroBeam = IntStream.range(0, aff.lineCount()).filter(i -> aff.line(i)[0] == 0).toArray();
                FixBS l = FixBS.of(pairs.length * lc);
                for (int ln : zeroBeam) {
                    l.set(ln * pairs.length, ln * pairs.length + order - 1);
                }
                FixBS res = new FixBS(aff.pointCount());
                res.set(0);
                for (FixBS comp : components) {
                    if (comp.cardinality() != aff.pointCount()) {
                        continue;
                    }
                    FixBS prs = comp.intersection(l);
                    for (int un = prs.nextSetBit(0); un >= 0; un = prs.nextSetBit(un + 1)) {
                        int ln = un / pairs.length;
                        int[] pair = pairs[un % pairs.length];
                        int pt = aff.line(ln)[pair[1]];
                        res.set(pt);
                    }
                }
                System.out.println(line);
                System.out.println(components.stream().map(FixBS::cardinality).toList());
                if (res.cardinality() == 1) {
                    System.out.println("Empty func vectors");
                    continue;
                }
                Liner funcVectors = aff.subPlane(res);
                System.out.println(funcVectors.pointCount() + " " + funcVectors.lineCount());
            }
        }
    }

    private static final Map<String, int[]> dropped = Map.ofEntries(
            Map.entry("pg29", new int[]{0}),
            Map.entry("dhall9", new int[]{0, 1}),
            Map.entry("hall9", new int[]{0, 81}),
            Map.entry("hughes9", new int[]{0, 3}),
            Map.entry("bbh1", new int[]{0, 192, 193, 205, 269}), // TODO test
             //"bbh2", new int[]{0, 28},
            Map.entry("dbbh2", new int[]{0, 1, 21, 41, 233}),
            //"bbs4", new int[]{0, 108, 270},
            //"dbbs4", new int[]{0, 228, 241}
            Map.entry("semi2", new int[]{0, 256, 272}),
            Map.entry("semi4", new int[]{0, 256, 272}),
            Map.entry("math", new int[]{0, 1, 17}),
            Map.entry("dmath", new int[]{0, 16, 272}),
            Map.entry("hall", new int[]{0, 5, 17}),
            Map.entry("dhall", new int[]{0, 80, 81}),
            Map.entry("twisted", new int[]{0, 1, 28}),
            Map.entry("", new int[]{})
    );

    @Test
    public void testPentagon() throws IOException {
        String name = "hall9";
        int order = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + order + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            for (int l : dropped.getOrDefault(name, IntStream.range(0, proj.lineCount()).toArray())) {
                NumeratedAffinePlane aff = new NumeratedAffinePlane(proj, l);
                int all = 0;
                int pentagon = 0;
                for (int a = 0; a < aff.pointCount(); a++) {
                    for (int b = a + 1; b < aff.pointCount(); b++) {
                        int ab = aff.line(a, b);
                        for (int c = 0; c < aff.pointCount(); c++) {
                            if (aff.flag(ab, c)) {
                                continue;
                            }
                            int ac = aff.line(a, c);
                            for (int d : aff.line(aff.parallel(ab, c))) {
                                if (c == d) {
                                    continue;
                                }
                                int e = aff.intersection(aff.parallel(ac, b), aff.parallel(aff.line(b, d), a));
                                if (e < 0) {
                                    continue;
                                }
                                if (aff.intersection(aff.line(a, d), aff.line(c, e)) >= 0) {
                                    continue;
                                }
                                all++;
                                if (aff.intersection(aff.line(b, c), aff.line(d, e)) < 0) {
                                    pentagon++;
                                }
                            }
                        }
                    }
                }
                System.out.println(l + " all " + all + " pentagon " + pentagon);
            }
        }
    }

    @Test
    public void testBoolean() throws IOException {
        String name = "dhall9";
        int order = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + order + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            for (int l : dropped.getOrDefault(name, IntStream.range(0, proj.lineCount()).toArray())) {
                NumeratedAffinePlane aff = new NumeratedAffinePlane(proj, l);
                int all = 0;
                int bool = 0;
                for (int a = 0; a < aff.pointCount(); a++) {
                    for (int b = a + 1; b < aff.pointCount(); b++) {
                        int ab = aff.line(a, b);
                        for (int c = 0; c < aff.pointCount(); c++) {
                            if (aff.flag(ab, c)) {
                                continue;
                            }
                            int bd = aff.parallel(aff.line(a, c), b);
                            int cd = aff.parallel(ab, c);
                            int d = aff.intersection(bd, cd);
                            all++;
                            if (aff.intersection(aff.line(b, c), aff.line(a, d)) < 0) {
                                bool++;
                            }
                        }
                    }
                }
                System.out.println(l + " all " + all + " boolean " + bool);
            }
        }
    }

    @Test
    public void testWindow() throws IOException {
        String name = "dhall";
        int order = 16;
        try (InputStream is = getClass().getResourceAsStream("/proj" + order + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            for (int l : dropped.getOrDefault(name, IntStream.range(0, proj.lineCount()).toArray())) {
                NumeratedAffinePlane aff = new NumeratedAffinePlane(proj, l);
                int all = 0;
                int window = 0;
                for (int a = 0; a < aff.pointCount(); a++) {
                    for (int b = a + 1; b < aff.pointCount(); b++) {
                        int ab = aff.line(a, b);
                        for (int c = 0; c < aff.pointCount(); c++) {
                            if (aff.flag(ab, c)) {
                                continue;
                            }
                            int bo = aff.parallel(aff.line(a, c), b);
                            int co = aff.parallel(ab, c);
                            int o = aff.intersection(bo, co);
                            int ao = aff.line(a, o);
                            int a1c = aff.parallel(ao, c);
                            int b1 = aff.intersection(a1c, bo);
                            int b1a1 = aff.parallel(co, b1);
                            int a1 = aff.intersection(b1a1, ao);
                            int a1c1 = aff.parallel(bo, a1);
                            int c1 = aff.intersection(a1c1, co);
                            if (aff.intersection(aff.line(b, c1), ao) < 0) {
                                window++;
                            }
                            all++;
                        }
                    }
                }
                System.out.println(l + " all " + all + " window " + window);
            }
        }
    }

    @Test
    public void testHexagon() throws IOException {
        String name = "semi4";
        int order = 16;
        try (InputStream is = getClass().getResourceAsStream("/proj" + order + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            for (int l : dropped.getOrDefault(name, IntStream.range(0, proj.lineCount()).toArray())) {
                NumeratedAffinePlane aff = new NumeratedAffinePlane(proj, l);
                int all = 0;
                int hexagon = 0;
                for (int a = 0; a < aff.pointCount(); a++) {
                    for (int b = a + 1; b < aff.pointCount(); b++) {
                        int ab = aff.line(a, b);
                        for (int c = 0; c < aff.pointCount(); c++) {
                            if (aff.flag(ab, c)) {
                                continue;
                            }
                            int ac = aff.line(a, c);
                            int bc = aff.line(b, c);
                            for (int d = 0; d < aff.pointCount(); d++) {
                                if (aff.flag(ac, d) || aff.flag(bc, d) || aff.flag(ab, d)) {
                                    continue;
                                }
                                int cd = aff.line(c, d);
                                if (aff.intersection(ab, cd) < 0) {
                                    continue;
                                }
                                int bd = aff.line(b, d);
                                int f = aff.intersection(aff.parallel(cd, a), aff.parallel(ac, d));
                                int e = aff.intersection(aff.parallel(bd, a), aff.parallel(ab, d));
                                int ef = aff.line(e, f);
                                if (e < 0 || f < 0 || f == e || bc == ef || aff.intersection(bc, ef) >= 0) {
                                    continue;
                                }
                                if (aff.intersection(aff.line(c, e), aff.line(b, f)) < 0) {
                                    hexagon++;
                                }
                                all++;
                            }
                        }
                    }
                }
                System.out.println(l + " all " + all + " hexagon " + hexagon);
            }
        }
    }

    @Test
    public void testK3() throws IOException {
        String name = "dhall9";
        int order = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + order + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            for (int l : dropped.getOrDefault(name, IntStream.range(0, proj.lineCount()).toArray())) {
                NumeratedAffinePlane aff = new NumeratedAffinePlane(proj, l);
                int all = 0;
                int k3 = 0;
                for (int a = 0; a < aff.pointCount(); a++) {
                    for (int b = a + 1; b < aff.pointCount(); b++) {
                        int ab = aff.line(a, b);
                        for (int c = 0; c < aff.pointCount(); c++) {
                            if (aff.flag(ab, c)) {
                                continue;
                            }
                            int bc = aff.line(b, c);
                            for (int d : aff.line(aff.parallel(ab, c))) {
                                int ad = aff.line(a, d);
                                if (c == d || aff.intersection(ad, bc) < 0) {
                                    continue;
                                }
                                int be = aff.parallel(ad, b);
                                int de = aff.parallel(bc, d);
                                int af = aff.parallel(bc, a);
                                int cf = aff.parallel(ad, c);
                                int e = aff.intersection(be, de);
                                int f = aff.intersection(af, cf);
                                if (e < 0 || f < 0 || e == f) {
                                    continue;
                                }
                                int ef = aff.line(e, f);
                                if (aff.intersection(aff.line(c, d), ef) < 0) {
                                    k3++;
                                }
                                all++;
                            }
                        }
                    }
                }
                System.out.println(l + " all " + all + " k3 " + k3);
            }
        }
    }

    @Test
    public void testStrongK3() throws IOException {
        String name = "dhall";
        int order = 16;
        try (InputStream is = getClass().getResourceAsStream("/proj" + order + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            for (int l : dropped.getOrDefault(name, IntStream.range(0, proj.lineCount()).toArray())) {
                NumeratedAffinePlane aff = new NumeratedAffinePlane(proj, l);
                int all = 0;
                int k3 = 0;
                for (int a = 0; a < aff.pointCount(); a++) {
                    for (int b = a + 1; b < aff.pointCount(); b++) {
                        int ab = aff.line(a, b);
                        for (int c = 0; c < aff.pointCount(); c++) {
                            if (aff.flag(ab, c)) {
                                continue;
                            }
                            int ac = aff.line(a, c);
                            int bc = aff.line(b, c);
                            int cd = aff.parallel(ab, c);
                            int bd = aff.parallel(ac, b);
                            int d = aff.intersection(cd, bd);
                            int ad = aff.line(a, d);
                            if (aff.intersection(ad, bc) < 0) {
                                continue;
                            }
                            int be = aff.parallel(ad, b);
                            int de = aff.parallel(bc, d);
                            int af = aff.parallel(bc, a);
                            int cf = aff.parallel(ad, c);
                            int e = aff.intersection(be, de);
                            int f = aff.intersection(af, cf);
                            if (e < 0 || f < 0 || e == f || aff.line(c, e) != ac || aff.line(d, f) != bd) {
                                continue;
                            }
                            int ef = aff.line(e, f);
                            if (aff.intersection(cd, ef) < 0) {
                                k3++;
                            }
                            all++;
                        }
                    }
                }
                System.out.println(l + " all " + all + " k3 " + k3);
            }
        }
    }

    @Test
    public void testOrbits() throws IOException {
        String name = "hughes9";
        int order = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + order + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            int dl = 3;
            PermutationGroup auto = new PermutationGroup(TernaryAutomorphisms.automorphismsAffine(proj, dl).toArray(int[][]::new));
            Group table = auto.asTable();
            System.out.println(table.order());
            List<SubGroup> subGroups = table.subGroups();
            System.out.println(subGroups.size());
            Map<Integer, List<SubGroup>> bySize = subGroups.stream().collect(Collectors.groupingBy(sg -> sg.arr().length));
            System.out.println(subGroups.stream().collect(Collectors.groupingBy(sg -> sg.arr().length, Collectors.counting())));
            auto = auto.subset(bySize.get(auto.order()).getFirst().elems());
            System.out.println("Calculated automorphisms " + auto.order());
            QuickFind pts = new QuickFind(proj.pointCount());
            for (int a = 0; a < auto.order(); a++) {
                int[] arr = auto.permutation(a);
                for (int p1 = 0; p1 < proj.pointCount(); p1++) {
                    pts.union(p1, arr[p1]);
                }
            }
            System.out.println("Group " + GroupIndex.identify(table) + " elems " + pts.components() + " infty " + Arrays.toString(proj.line(dl)));
        }
    }

    @Test
    public void testArc() throws IOException {
        int[][] auths = new ObjectMapper().readValue(getClass().getResourceAsStream("/denniston.txt"), int[][].class);
        Liner arc = HyperbolicPlaneTest.dennistonArc(16, 8);
        int pc = arc.pointCount();
        QuickFind qf = new QuickFind(pc * pc);
        for (int[] auth : auths) {
            for (int p1 = 0; p1 < pc; p1++) {
                for (int p2 = 0; p2 < pc; p2++) {
                    qf.union(p1 * pc + p2, auth[p1] * pc + auth[p2]);
                }
            }
        }
        Arrays.stream(arc.lines()).sorted(Combinatorics::compareArr).forEach(l -> System.out.println(Arrays.toString(l)));
        List<FixBS> components = qf.components();
        components.forEach(l -> System.out.println(l.stream().mapToObj(p -> "(" + p / pc + "," + p % pc + ")").collect(Collectors.joining(", ", "[", "]"))));
        int x1 = 0;
        int y1 = 2;
        int z1 = 8;
        int x1y1 = idx(x1 * pc + y1, components);
        int x1z1 = idx(x1 * pc + z1, components);
        int y1z1 = idx(y1 * pc + z1, components);
        for (int x = 0; x < pc; x++) {
            for (int y = x + 1; y < pc; y++) {
                if (idx(x * pc + y, components) != x1y1) {
                    continue;
                }
                for (int z = y + 1; z < pc; z++) {
                    if (arc.flag(arc.line(x, y), z)) {
                        continue;
                    }
                    if (idx(x * pc + z, components) != x1z1 || idx(y * pc + z, components) != y1z1) {
                        continue;
                    }
                    int cnt = 0;
                    for (int[] auth : auths) {
                        if (auth[x] == x1 && auth[y] == y1 && auth[z] == z1) {
                            cnt++;
                        }
                    }
                    if (cnt != 1) {
                        System.out.println(cnt + " " + x + " " + y + " " + z);
                    }
                }
            }
        }
    }

    private int idx(int xy, List<FixBS> components) {
        return IntStream.range(0, components.size()).filter(i -> components.get(i).get(xy)).findAny().orElseThrow();
    }
}
