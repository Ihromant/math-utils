package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.plane.Quad;
import ua.ihromant.mathutils.util.FixBS;
import ua.ihromant.mathutils.vf2.IntPair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

public class TernaryRingTest {
    @Test
    public void testRecursive() throws IOException {
        String name = "hughes9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            for (int dl1 = 0; dl1 < proj.lineCount(); dl1++) {
                for (int dl2 = 0; dl2 < proj.lineCount(); dl2++) {
                    TernarMapping map = ternars(proj, dl1).map(TernaryRingTest::findTernarMapping).filter(TernarMapping::isInduced).findAny().orElseThrow();
                    boolean isomorphic = ternars(proj, dl2).anyMatch(m -> ringIsomorphic(map, m));
                    System.out.println(dl1 + " " + " " + dl2 + " " + isomorphic);
                }
            }
        }
    }

    private Stream<TernaryRing> ternars(Liner plane, int dl) {
        return IntStream.range(0, plane.pointCount()).filter(o -> !plane.flag(dl, o)).boxed().flatMap(o ->
                IntStream.range(0, plane.pointCount()).filter(u -> u != o && !plane.flag(dl, u)).boxed().flatMap(u -> {
                    int ou = plane.line(o, u);
                    return IntStream.range(0, plane.pointCount()).filter(w -> !plane.flag(dl, w) && !plane.flag(ou, w)).mapToObj(w -> {
                        int ow = plane.line(o, w);
                        int e = plane.intersection(plane.line(u, plane.intersection(dl, ow)), plane.line(w, plane.intersection(dl, ou)));
                        Quad base = new Quad(o, u, w, e);
                        return new ProjectiveTernaryRing(plane, base);
                    });
                }));
    }

    private boolean ringIsomorphic(TernarMapping tm, TernaryRing second) {
        TernaryRing first = tm.ring();
        int[][] functions = new int[tm.functions().size()][];
        functions[0] = new int[second.order()];
        Arrays.fill(functions[0], -1);
        functions[0][0] = 0;
        functions[0][1] = 1;
        FixBS setVals = FixBS.of(second.order(), 0, 1);
        for (int i = 1; i < functions.length; i++) {
            functions[i] = functions[i - 1].clone();
            FixBS xn1 = tm.xl().get(i);
            FixBS xn = tm.xl().get(i - 1);
            FixBS missing = xn1.copy().symDiff(xn);
            for (int x = missing.nextSetBit(0); x >= 0; x = missing.nextSetBit(x + 1)) {
                Triangle tr = tm.functions.get(i)[x];
                int mappedX = second.op(functions[i][tr.o()], functions[i][tr.u()], functions[i][tr.w()]);
                if (setVals.get(mappedX)) {
                    return false;
                }
                functions[i][x] = mappedX;
                setVals.set(mappedX);
            }
            for (int a = xn1.nextSetBit(0); a >= 0; a = xn1.nextSetBit(a + 1)) {
                for (int b = xn1.nextSetBit(0); b >= 0; b = xn1.nextSetBit(b + 1)) {
                    for (int c = xn1.nextSetBit(0); c >= 0; c = xn1.nextSetBit(c + 1)) {
                        if (xn.get(a) && xn.get(b) && xn.get(c)) {
                            continue;
                        }
                        if (second.op(functions[i][a], functions[i][b], functions[i][c]) != first.op(a, b, c)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private record TernarMapping(TernaryRing ring, List<FixBS> xl, List<Triangle[]> functions) {
        private boolean isInduced() {
            return xl.getLast().cardinality() == ring.order();
        }
    }

    private static TernarMapping findTernarMapping(TernaryRing ring) {
        List<FixBS> xl = new ArrayList<>();
        xl.add(FixBS.of(ring.order(), 0, 1));
        List<Triangle[]> functions = new ArrayList<>();
        functions.add(new Triangle[ring.order()]);
        return findTernarMapping(ring, xl, functions);
    }

    private static TernarMapping findTernarMapping(TernaryRing ring, List<FixBS> xl, List<Triangle[]> functions) {
        int order = ring.order();
        FixBS x = xl.getLast();
        if (x.cardinality() == order) {
            return new TernarMapping(ring, xl, functions);
        }
        FixBS nextX = x.copy();
        Triangle[] nextFunction = functions.getLast().clone();
        for (int a = x.nextSetBit(0); a >= 0; a = x.nextSetBit(a + 1)) {
            for (int b = x.nextSetBit(0); b >= 0; b = x.nextSetBit(b + 1)) {
                for (int c = x.nextSetBit(0); c >= 0; c = x.nextSetBit(c + 1)) {
                    int res = ring.op(a, b, c);
                    nextX.set(res);
                    if (nextFunction[res] == null && res > 1) {
                        nextFunction[res] = new Triangle(a, b, c);
                    }
                }
            }
        }
        if (nextX.cardinality() == x.cardinality()) {
            return new TernarMapping(ring, xl, functions);
        }
        xl.add(nextX);
        functions.add(nextFunction);
        return findTernarMapping(ring, xl, functions);
    }

    @Test
    public void testCorrectness() throws IOException {
        String name = "dhall9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            int dl = 1;
            for (int o = 0; o < proj.pointCount(); o++) {
                if (proj.flag(dl, o)) {
                    continue;
                }
                for (int u = 0; u < proj.pointCount(); u++) {
                    if (u == o || proj.flag(dl, u)) {
                        continue;
                    }
                    int ou = proj.line(o, u);
                    for (int w = 0; w < proj.pointCount(); w++) {
                        if (proj.flag(dl, w) || proj.flag(ou, w)) {
                            continue;
                        }
                        int ow = proj.line(o, w);
                        int e = proj.intersection(proj.line(u, proj.intersection(dl, ow)), proj.line(w, proj.intersection(dl, ou)));
                        TernaryRing ring = new ProjectiveTernaryRing(proj, new Quad(o, u, w, e));
                        testCorrectness(ring);
                    }
                }
            }
        }
    }

    private static void testCorrectness(TernaryRing tr) {
        for (int x : tr.elements()) {
            for (int b : tr.elements()) {
                assertEquals(b, tr.op(x, 0, b));
                assertEquals(b, tr.op(0, x, b));
            }
            assertEquals(x, tr.op(x, 1, 0));
            assertEquals(x, tr.op(1, x, 0));
        }
        for (int a : tr.elements()) {
            for (int x : tr.elements()) {
                for (int y : tr.elements()) {
                    int b = IntStream.range(0, tr.order()).filter(c -> tr.op(x, a, c) == y).findAny().orElseThrow();
                    for (int c : tr.elements()) {
                        if (b == c) {
                            continue;
                        }
                        if (tr.op(x, a, c) == y) {
                            fail();
                        }
                    }
                }
            }
        }
        for (int a : tr.elements()) {
            for (int b : tr.elements()) {
                for (int c : tr.elements()) {
                    if (c == a) {
                        continue;
                    }
                    for (int d : tr.elements()) {
                        int x = IntStream.range(0, tr.order()).filter(y -> tr.op(y, a, b) == tr.op(y, c, d)).findAny().orElseThrow();
                        for (int y : tr.elements()) {
                            if (x == y) {
                                continue;
                            }
                            if (tr.op(y, a, b) == tr.op(y, c, d)) {
                                fail();
                            }
                        }
                    }
                }
            }
        }
        for (int x1 : tr.elements()) {
            for (int y1 : tr.elements()) {
                for (int x2 : tr.elements()) {
                    if (x1 == x2) {
                        continue;
                    }
                    for (int y2 : tr.elements()) {
                        IntPair ab = null;
                        for (int a : tr.elements()) {
                            for (int b : tr.elements()) {
                                if (tr.op(x1, a, b) == y1 && tr.op(x2, a, b) == y2) {
                                    ab = new IntPair(a, b);
                                }
                            }
                        }
                        assertNotNull(ab);
                        for (int a1 : tr.elements()) {
                            for (int b1 : tr.elements()) {
                                if (a1 == ab.fst() && b1 == ab.snd()) {
                                    continue;
                                }
                                if (tr.op(x1, a1, b1) == y1 && tr.op(x2, a1, b1) == y2) {
                                    fail();
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
