package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AutomorphismsTest {
    @Test
    public void testAutomorphisms() {
        assertEquals(168, Automorphisms.automorphisms(new Liner(new GaloisField(2).generatePlane())).count()); // Fano
        assertEquals(5616, Automorphisms.automorphisms(new Liner(new GaloisField(3).generatePlane())).count()); // projective 3
        //assertEquals(120960, Automorphisms.automorphisms(new Liner(new GaloisField(4).generatePlane())).count()); // projective 4
        assertEquals(432, Automorphisms.automorphisms(new Liner(new String[]{ // affine 3
                "000011122236",
                "134534534547",
                "268787676858"
        })).count());
        assertEquals(20160, Automorphisms.automorphisms(new Liner(new String[]{ // smallest 3-dim projective
                "00000001111112222223333444455556666",
                "13579bd3478bc3478bc789a789a789a789a",
                "2468ace569ade65a9edbcdecbeddebcedcb"
        })).count());
        assertEquals(6, Automorphisms.automorphisms(new Liner(new String[]{
                "00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95acbbacc9bbac9"
        })).count());
        assertEquals(39, Automorphisms.automorphisms(new Liner(new String[]{
                "00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95abcbcac9babc9"
        })).count());
    }

    @Test
    public void testPerformance() {
        long time = System.currentTimeMillis();
        assertEquals(120960, Automorphisms.automorphisms(new Liner(new GaloisField(4).generatePlane())).count()); // projective 4
        System.out.println(System.currentTimeMillis() - time);
    }

    @Test
    public void testDilations() {
        Liner proj = new Liner(new GaloisField(3).generatePlane());
        BitSet infty = proj.line(0);
        int[] partial = new int[proj.pointCount()];
        Arrays.fill(partial, -1);
        infty.stream().forEach(i -> partial[i] = i);
        System.out.println(Automorphisms.automorphisms(proj, partial).count());
    }

    @Test
    public void testCycles() {
        Liner plane = new Liner(new String[] {
                "00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95abcbcac9babc9"});
        Set<Set<BitSet>> cycles = new HashSet<>();
        BitSet lengths = new BitSet();
        for (int i = 0; i < plane.pointCount(); i++) {
            for (int j = i + 1; j < plane.pointCount(); j++) {
                for (int k = j + 1; k < plane.pointCount(); k++) {
                    List<BitSet> cycled = iterate(plane, of(i, j, k));
                    cycles.add(new LinkedHashSet<>(cycled.subList(cycled.indexOf(cycled.get(cycled.size() - 1)) + 1, cycled.size())));
                    //System.out.println(cycled);
                    lengths.set(cycled.size() - 1);
                }
            }
        }
        System.out.println(lengths);
        System.out.println(cycles.stream().filter(c -> c.size() > 1).peek(System.out::println).collect(Collectors.groupingBy(Set::size, Collectors.counting())));
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
        return pl.line(pl.line(x, y)).stream().filter(p -> p != x && p != y).findAny().orElseThrow();
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
}