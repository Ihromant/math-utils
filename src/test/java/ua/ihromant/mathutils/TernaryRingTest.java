package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.plane.Quad;
import ua.ihromant.mathutils.vf2.IntPair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

public class TernaryRingTest {
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
