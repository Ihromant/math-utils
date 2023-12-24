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

public class AuthomorphismsTest {
    @Test
    public void testPlaneAuthomorphisms() {
        BitSet[] lines = new BitSet[0];
        HyperbolicPlane plane = new HyperbolicPlane(lines);
        int[] triple = null;
        for (int x : plane.points()) {
            for (int y : plane.points()) {
                if (x == y) {
                    continue;
                }
                for (int z : plane.points()) {
                    if (plane.collinear(x, y, z)) {
                        continue;
                    }
                    if (triple == null) {
                        BitSet hull = plane.hull(x, y, z);
                        if (hull.cardinality() == plane.pointCount()) {
                            triple = new int[]{x, y, z};
                        } else {
                            continue;
                        }
                    }
                    int[] auth = new int[plane.pointCount()];
                    Arrays.fill(auth, -1);
                    BitSet used = of(x, y, z);
                    auth[triple[0]] = x;
                    auth[triple[1]] = y;
                    auth[triple[2]] = z;
                    for (int i = 0; i < auth.length; i++) {
                        for (int j = i + 1; j < auth.length; j++) {

                        }
                    }
                }
            }
        }
    }

    private static int quasiOp(HyperbolicPlane pl, int x, int y) {
        return pl.line(pl.line(x, y)).stream().filter(p -> p != x && p != y).findAny().orElseThrow();
    }

    private static BitSet next(HyperbolicPlane plane, BitSet prev) {
        return of(prev.stream().flatMap(x -> prev.stream().filter(y -> y > x).map(y -> quasiOp(plane, x, y))));
    }

    private List<BitSet> iterate(HyperbolicPlane plane, BitSet set) {
        List<BitSet> result = new ArrayList<>();
        do {
            result.add(set);
            set = next(plane, set);
        } while (!result.contains(set));
        result.add(set);
        return result;
    }

    @Test
    public void testCycles() {
        HyperbolicPlane plane = new HyperbolicPlane(new String[] {
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

//    public BitSet cardSubPlanes(boolean full) {
//        BitSet result = new BitSet();
//        for (int x : points()) {
//            for (int y : points()) {
//                if (x >= y) {
//                    continue;
//                }
//                for (int z : points()) {
//                    if (y >= z || line(x, y) == line(y, z)) {
//                        continue;
//                    }
//                    int card = hull(x, y, z).cardinality();
//                    result.set(card);
//                    if (!full && card == pointCount) {
//                        return result; // it's either plane or has no exchange property
//                    }
//                }
//            }
//        }
//        return result;
//    }

//    public BitSet hull(int... points) {
//        BitSet base = new BitSet();
//        for (int point : points) {
//            base.set(point);
//        }
//        BitSet additional = base;
//        while (!(additional = additional(base, additional)).isEmpty()) {
//            base.or(additional);
//        }
//        return base;
//    }

//    public BitSet additional(BitSet first, BitSet second) {
//        BitSet result = new BitSet();
//        first.stream().forEach(x -> second.stream().filter(y -> x != y).forEach(y -> result.or(lines[line(x, y)])));
//        BitSet removal = new BitSet();
//        removal.or(first);
//        removal.or(second);
//        result.xor(removal);
//        return result;
//    }
}
