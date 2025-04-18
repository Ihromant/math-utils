package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.auto.Automorphisms;
import ua.ihromant.mathutils.auto.TernaryAutomorphisms;
import ua.ihromant.mathutils.plane.AffinePlane;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class AutomorphismsTest {
    @Test
    public void testAutomorphisms() {
        testSample(Automorphisms::autCount);
        testSample(Automorphisms::autCountOld);
        testSample(Liner::autCountNew);
        testSample(Liner::autCountOld);
    }

    private void testSample(ToLongFunction<Liner> autCount) {
        assertEquals(168, autCount.applyAsLong(new Liner(new GaloisField(2).generatePlane())));
        assertEquals(432, autCount.applyAsLong(Liner.byStrings(new String[]{ // affine 3
                "000011122236",
                "134534534547",
                "268787676858"
        })));
        assertEquals(5616, autCount.applyAsLong(new Liner(new GaloisField(3).generatePlane()))); // projective 3
        //assertEquals(120960, autCount.applyAsLong(new Liner(new GaloisField(4).generatePlane())).count()); // projective 4
        assertEquals(432, autCount.applyAsLong(Liner.byStrings(new String[]{ // affine 3
                "000011122236",
                "134534534547",
                "268787676858"
        })));
        assertEquals(20160, autCount.applyAsLong(Liner.byStrings(new String[]{ // smallest 3-dim projective
                "00000001111112222223333444455556666",
                "13579bd3478bc3478bc789a789a789a789a",
                "2468ace569ade65a9edbcdecbeddebcedcb"
        })));
        assertEquals(6, autCount.applyAsLong(Liner.byStrings(new String[]{
                "00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95acbbacc9bbac9"
        })));
        assertEquals(39, autCount.applyAsLong(Liner.byStrings(new String[]{
                "00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95abcbcac9babc9"
        })));
        long time = System.currentTimeMillis();
        assertEquals(12096, autCount.applyAsLong(Liner.byStrings(new String[]{
                "0000000001111111122222222333333334444455556666777788899aabbcgko",
                "14567ghij4567cdef456789ab456789ab59adf8bce9bcf8ade9decfdfcedhlp",
                "289abklmnba89lknmefdchgjijighfecd6klhilkgjnmhjmngiajgihigjheimq",
                "3cdefopqrghijrqopqrponmklporqklmn7romnqpnmqoklrplkbopporqqrfjnr"
        })));
        System.out.println(System.currentTimeMillis() - time);
    }

    private static final int[] AUTH_COUNTS = {0, 0, 168, 5616, 120960, 372000, 0, 5630688, 49448448, 84913920};

    @Test
    public void testPerformance() {
        int order = 5;
        Liner liner = new Liner(new GaloisField(order).generatePlane());
        long time = System.currentTimeMillis();
        assertEquals(AUTH_COUNTS[order], Automorphisms.autCount(liner));
        System.out.println(System.currentTimeMillis() - time);
        time = System.currentTimeMillis();
        assertEquals(AUTH_COUNTS[order], Automorphisms.autCountOld(liner));
        System.out.println(System.currentTimeMillis() - time);
        time = System.currentTimeMillis();
        assertEquals(AUTH_COUNTS[order], liner.autCountNew());
        System.out.println(System.currentTimeMillis() - time);
        time = System.currentTimeMillis();
        assertEquals(AUTH_COUNTS[order], liner.autCountOld());
        System.out.println(System.currentTimeMillis() - time);
        Liner aff1 = new AffinePlane(liner, 0).toLiner();
        Liner aff2 = new AffinePlane(liner, 1).toLiner();
        time = System.currentTimeMillis();
        assertNotNull(Automorphisms.isomorphism(aff1, aff2));
        System.out.println(System.currentTimeMillis() - time);
    }

    @Test
    public void testCycles() {
        Liner plane = Liner.byStrings(new String[] {
                "00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95abcbcac9babc9"});
        Set<Set<BitSet>> cycles = new HashSet<>();
        BitSet lengths = new BitSet();
        for (int i = 0; i < plane.pointCount(); i++) {
            for (int j = i + 1; j < plane.pointCount(); j++) {
                for (int k = j + 1; k < plane.pointCount(); k++) {
                    List<BitSet> cycled = iterate(plane, of(i, j, k));
                    cycles.add(new LinkedHashSet<>(cycled.subList(cycled.indexOf(cycled.getLast()) + 1, cycled.size())));
                    //System.out.println(cycled);
                    lengths.set(cycled.size() - 1);
                }
            }
        }
        System.out.println(lengths);
        System.out.println(cycles.stream().filter(c -> c.size() > 1).peek(System.out::println).collect(Collectors.groupingBy(Set::size, Collectors.counting())));
    }

    @Test
    public void testIsomorphism() {
        Liner first7 = Liner.byStrings(new String[]{
                "0001123",
                "1242534",
                "3654656"
        });
        Liner second7 = new Liner(new GaloisField(2).generatePlane());
        assertNotNull(Automorphisms.isomorphism(first7, second7));
        assertNotNull(Automorphisms.isomorphism(second7, first7));
        Liner first13 = Liner.byStrings(new String[]{
                "00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95abcbcac9babc9"
        });
        Liner alt13 = Liner.byStrings(new String[]{
                "00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95acbbacc9bbac9"
        });
        Liner second13 = Liner.byDiffFamily(new int[]{0, 6, 8}, new int[]{0, 9, 10});
        assertNotNull(Automorphisms.isomorphism(first13, second13));
        assertNotNull(Automorphisms.isomorphism(second13, first13));
        assertNull(Automorphisms.isomorphism(alt13, first13));
        Liner first9 = Liner.byStrings(new String[]{
                "000011122236",
                "134534534547",
                "268787676858"
        });
        Liner second9 = new AffinePlane(new Liner(new GaloisField(3).generatePlane()), 0).toLiner();
        assertNotNull(Automorphisms.isomorphism(first9, second9));
        Liner firstFlat15 = Liner.byStrings(new String[] {
                "00000001111112222223333444455566678",
                "13579bd3469ac34578b678a58ab78979c9a",
                "2468ace578bde96aecdbcded9cebecaeddb"
        });
        Liner firstSpace15 = Liner.byStrings(new String[]{
               "00000001111112222223333444455556666",
               "13579bd3478bc3478bc789a789a789a789a",
               "2468ace569ade65a9edbcdecbeddebcedcb"
        });
        Liner secondFlat15 = Liner.byDiffFamily(15, new int[]{0, 6, 8}, new int[]{0, 1, 4}, new int[]{0, 5, 10});
        Liner secondSpace15 = Liner.byDiffFamily(15, new int[]{0, 2, 8}, new int[]{0, 1, 4}, new int[]{0, 5, 10});
        Liner thirdSpace15 = new Liner(new GaloisField(2).generateSpace());
        assertNotNull(Automorphisms.isomorphism(firstFlat15, secondFlat15));
        assertNotNull(Automorphisms.isomorphism(firstSpace15, secondSpace15));
        assertNotNull(Automorphisms.isomorphism(firstSpace15, thirdSpace15));
        assertNotNull(Automorphisms.isomorphism(firstSpace15, thirdSpace15));
        assertNull(Automorphisms.isomorphism(firstFlat15, firstSpace15));
        Liner firstPartial = new Liner(9, new int[][]{{0, 1, 2}, {0, 3, 4}});
        Liner secondPartial = new Liner(9, new int[][]{{6, 7, 8}, {4, 5, 8}});
        Liner thirdPartial = new Liner(9, new int[][]{{0, 1, 2}, {3, 4, 5}});
        Liner fourthPartial = new Liner(9, new int[][]{{0, 3, 6}, {1, 4, 7}});
        Liner fifthPartial = new Liner(9, new int[][]{{0, 1, 2}, {0, 3, 4}, {1, 3, 5}});
        Liner sixthPartial = new Liner(9, new int[][]{{0, 1, 2}, {0, 5, 6}, {2, 4, 6}});
        assertNotNull(Automorphisms.isomorphism(firstPartial, secondPartial));
        assertNull(Automorphisms.isomorphism(firstPartial, thirdPartial));
        assertNotNull(Automorphisms.isomorphism(thirdPartial, fourthPartial));
        assertNotNull(Automorphisms.isomorphism(fifthPartial, sixthPartial));
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet();
        IntStream.of(values).forEach(bs::set);
        return bs;
    }

    private static BitSet of(IntStream values) {
        BitSet bs = new BitSet();
        values.forEach(bs::set);
        return bs;
    }

    private static int quasiOp(Liner pl, int x, int y) {
        return Arrays.stream(pl.line(pl.line(x, y))).filter(p -> p != x && p != y).findAny().orElseThrow();
    }

    private static BitSet next(Liner plane, BitSet prev) {
        return of(prev.stream().flatMap(x -> prev.stream().filter(y -> y > x).map(y -> quasiOp(plane, x, y))));
    }

    private List<BitSet> iterate(Liner plane, BitSet set) {
        List<BitSet> result = new ArrayList<>();
        do {
            result.add(set);
            set = next(plane, set);
        } while (!result.contains(set));
        result.add(set);
        return result;
    }

    @Test
    public void testAutomorphismsAlt() throws IOException {
        int k = 9;
        for (File f : Objects.requireNonNull(new File("/home/ihromant/workspace/math-utils/src/test/resources/proj" + k).listFiles())) {
            String name = f.getName();
            if ("pg29.txt".equals(name)) {
                continue;
            }
            try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name);
                 InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
                 BufferedReader br = new BufferedReader(isr)) {
                Liner proj = BatchAffineTest.readProj(br);
                //long time = System.currentTimeMillis();
                List<int[]> auto = TernaryAutomorphisms.automorphismsProj(proj);
                for (int i = 0; i < auto.size(); i++) {
                    assertTrue(isAutomorphism(proj, auto.get(i)), i + " " + Arrays.toString(auto.get(i)));
                }
                QuickFind pts = new QuickFind(proj.pointCount());
                QuickFind lns = new QuickFind(proj.lineCount());
                int ll = proj.line(0).length;
                QuickFind flags = new QuickFind(proj.lineCount() * ll);
                for (int[] arr : auto) {
                    int[] lineMap = new int[proj.lineCount()];
                    for (int p1 = 0; p1 < proj.pointCount(); p1++) {
                        pts.union(p1, arr[p1]);
                        for (int p2 = p1 + 1; p2 < proj.pointCount(); p2++) {
                            lineMap[proj.line(p1, p2)] = proj.line(arr[p1], arr[p2]);
                        }
                    }
                    for (int ln = 0; ln < proj.lineCount(); ln++) {
                        int ml = lineMap[ln];
                        lns.union(ln, ml);
                        int[] line = proj.line(ln);
                        int[] mappedLine = proj.line(ml);
                        for (int pt = 0; pt < line.length; pt++) {
                            flags.union(ln * ll + pt, lineMap[ln] * ll + Arrays.binarySearch(mappedLine, arr[line[pt]]));
                        }
                    }
                }
                System.out.println("Liner " + name + " auths " + auto.size());
                System.out.println("Points " + pts.components());
                System.out.println("Lines " + lns.components());
                System.out.println("Flags " + flags.components().stream().map(bs -> bs.stream().mapToObj(fl -> "(" + fl / ll + ", " + proj.line(fl / ll)[fl % ll] + ")")
                        .collect(Collectors.joining(", ", "[", "]"))).collect(Collectors.joining(", ", "[", "]")));
            }
        }
    }

    private static boolean isAutomorphism(Liner liner, int[] map) {
        for (int l = 0; l < liner.lineCount(); l++) {
            int[] line = liner.line(l);
            int[] mapped = Arrays.stream(line).map(pt -> map[pt]).toArray();
            int[] mappedLine = liner.line(liner.line(mapped[0], mapped[1]));
            if (!FixBS.of(liner.pointCount(), mapped).equals(FixBS.of(liner.pointCount(), mappedLine))) {
                return false;
            }
        }
        return true;
    }
}
