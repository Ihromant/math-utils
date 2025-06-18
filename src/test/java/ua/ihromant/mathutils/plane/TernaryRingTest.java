package ua.ihromant.mathutils.plane;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.BatchAffineTest;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.Triangle;
import ua.ihromant.mathutils.auto.TernaryAutomorphisms;
import ua.ihromant.mathutils.util.FixBS;
import ua.ihromant.mathutils.vf2.IntPair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TernaryRingTest {
    @Test
    public void splitByPlanes() throws IOException {
        int k = 9;
        String desargues = "pg29.txt";
        for (File f : Objects.requireNonNull(new File("/home/ihromant/workspace/math-utils/src/test/resources/proj" + k).listFiles())) {
            String name = f.getName();
            if (desargues.equals(name)) {
                continue;
            }
            try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name);
                 InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
                 BufferedReader br = new BufferedReader(isr)) {
                Liner proj = BatchAffineTest.readProj(br);
                List<MappingList> mapping = ternarsOfProjective(proj, name);
                System.out.println(mapping);
                for (MappingList ml : mapping) {
                    System.out.println("Dropped " + ml.dl());
                    System.out.println("1plus " + ml.ternars().values().stream().flatMap(List::stream).filter(TernarMapping::onePlus).count());
                    System.out.println("puls1 " + ml.ternars().values().stream().flatMap(List::stream).filter(TernarMapping::pulsOne).count());
                    System.out.println("plus1 " + ml.ternars().values().stream().flatMap(List::stream).filter(TernarMapping::plusOne).count());
                    System.out.println("1gen " + ml.ternars().values().stream().flatMap(List::stream).filter(TernarMapping::oneGen).count());
                    System.out.println("2mul " + ml.ternars().values().stream().flatMap(List::stream).filter(TernarMapping::twoMul).count());
                    System.out.println("mul2 " + ml.ternars().values().stream().flatMap(List::stream).filter(TernarMapping::mulTwo).count());
                    System.out.println("2gen " + ml.ternars().values().stream().flatMap(List::stream).filter(TernarMapping::twoGen).count());
                    System.out.println("gen " + ml.ternars().values().stream().flatMap(List::stream).filter(TernarMapping::generated).count());
                }
            }
        }
    }

    private static Map<Characteristic, List<TernarMapping>> ternarsOfAffine(String name, Liner proj, int dl) {
        Map<Characteristic, List<TernarMapping>> result = new HashMap<>();
        ternars(name, proj, dl).forEach(tr -> {
            TernarMapping mapping = TernaryAutomorphisms.findTernarMapping(tr);
            Characteristic chr = mapping.chr();
            if (!mapping.isInduced()) {
                return;
            }
            TernaryRing ring = mapping.ring().toMatrix();
            if (result.computeIfAbsent(chr, k -> new ArrayList<>()).stream()
                    .anyMatch(m -> ringIsomorphic(m, ring))) {
                return;
            }
            result.get(chr).add(new TernarMapping(ring, mapping.xl(), mapping.function(), mapping.chr()));
        });
        return result;
    }

    private static TernarMapping findInduced(String name, Liner proj, int dl) {
        return ternars(name, proj, dl).map(TernaryAutomorphisms::findTernarMapping).filter(TernarMapping::isInduced).findAny().orElse(null);
    }

    public static List<MappingList> ternarsOfProjective(Liner proj, String name) {
        List<MappingList> result = new ArrayList<>();
        for (int dl = 0; dl < proj.lineCount(); dl++) {
            System.out.println("Generating for " + name + " line " + dl);
            TernarMapping induced = findInduced(name, proj, dl);
            if (induced == null) {
                result.add(new MappingList(name, true, dl, Map.of()));
                continue;
            }
            if (result.stream().flatMap(ml -> ml.ternars().getOrDefault(induced.chr(), List.of()).stream())
                    .anyMatch(m -> ringIsomorphic(m, induced.ring()))) {
                continue;
            }
            result.add(new MappingList(name, false, dl, ternarsOfAffine(name, proj, dl)));
        }
        return result;
    }

    @Test
    public void orbitLines() throws IOException {
        String name = "hughes9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            Map<Integer, Iso> grouped = new HashMap<>();
            int trans = TernaryAutomorphisms.findTranslationLine(proj);
            for (int dl = 0; dl < proj.lineCount(); dl++) {
                System.out.println(dl);
                if (dl == trans) {
                    grouped.put(dl, new Iso(null, List.of(dl)));
                    continue;
                }
                AtomicReference<TernarMapping> induced = new AtomicReference<>();
                Optional<Integer> iso = ternars(name, proj, dl).parallel().flatMap(rng -> {
                    if (induced.get() == null) {
                        TernarMapping tm = TernaryAutomorphisms.findTernarMapping(rng);
                        if (tm.isInduced()) {
                            induced.set(tm);
                        }
                    }
                    return grouped.entrySet().stream().filter(e -> e.getValue().tm() != null && ringIsomorphic(e.getValue().tm(), rng)).map(Map.Entry::getKey).findAny().stream();
                }).findAny();
                if (iso.isPresent()) {
                    grouped.get(iso.get()).ints().add(dl);
                } else {
                    List<Integer> lst = new ArrayList<>();
                    lst.add(dl);
                    grouped.put(dl, new Iso(induced.get(), lst));
                }
            }
            grouped.forEach((key, v) -> System.out.println(key + " " + v.ints()));
        }
    }

    private record Iso(TernarMapping tm, List<Integer> ints) {}

    private static Stream<TernaryRing> ternars(String name, Liner plane, int dl) {
        int pc = plane.pointCount();
        return IntStream.range(0, pc * pc * pc).<TernaryRing>mapToObj(idx -> {
            int o = idx / pc / pc;
            int u = idx / pc % pc;
            int w = idx % pc;
            if (u == o || plane.flag(dl, o) || plane.flag(dl, u) || plane.flag(dl, w)) {
                return null;
            }
            int ou = plane.line(o, u);
            if (plane.flag(ou, w)) {
                return null;
            }
            int ow = plane.line(o, w);
            int e = plane.intersection(plane.line(u, plane.intersection(dl, ow)), plane.line(w, plane.intersection(dl, ou)));
            Quad base = new Quad(o, u, w, e);
            return new ProjectiveTernaryRing(name, plane, base);
        }).filter(Objects::nonNull);
    }

    private static boolean isBijective(int[] partialFunc) {
        int[] idxes = new int[partialFunc.length];
        Arrays.fill(idxes, -1);
        for (int i = 0; i < partialFunc.length; i++) {
            int val = partialFunc[i];
            if (val >= 0) {
                if (idxes[val] >= 0) {
                    return false;
                }
                idxes[val] = i;
            } else {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testBijective() {
        int[] notInjective = new int[]{0, -1, 2, 2};
        int[] injective = new int[]{3, 0, -1, 2};
        int[] bijective = new int[]{3, 0, 1, 2};
        assertFalse(isBijective(notInjective));
        assertFalse(isBijective(injective));
        assertTrue(isBijective(bijective));
    }

    public static boolean ringIsomorphic(TernarMapping tm, TernaryRing second) {
        TernaryRing first = tm.ring();
        int[] function = new int[second.order()];
        Arrays.fill(function, -1);
        function[0] = 0;
        function[1] = 1;
        for (int i = 1; i < tm.xl().size(); i++) {
            FixBS xn1 = tm.xl().get(i);
            FixBS xn = tm.xl().get(i - 1);
            FixBS missing = xn1.copy().symDiff(xn);
            for (int x = missing.nextSetBit(0); x >= 0; x = missing.nextSetBit(x + 1)) {
                Triangle tr = tm.function()[x];
                int mappedX = second.op(function[tr.o()], function[tr.u()], function[tr.w()]);
                function[x] = mappedX;
            }
        }
        if (!isBijective(function)) {
            return false;
        }
        for (int a = 1; a < first.order(); a++) {
            for (int b = 0; b < first.order(); b++) {
                for (int c = 0; c < first.order(); c++) {
                    if (second.op(function[a], function[b], function[c]) != function[first.op(a, b, c)]) {
                        return false;
                    }
                }
            }
        }
        return true;
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
                        TernaryRing ring = new ProjectiveTernaryRing(name, proj, new Quad(o, u, w, e));
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
