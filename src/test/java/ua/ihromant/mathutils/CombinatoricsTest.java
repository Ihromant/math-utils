package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CombinatoricsTest {
    @Test
    public void testPermutations() {
        assertArrayEquals(new int[][]{{0, 2, 4}, {0, 4, 2}, {2, 0, 4}, {2, 4, 0}, {4, 0, 2}, {4, 2, 0}}, Combinatorics.permutations(new int[]{0, 2, 4}).toArray(int[][]::new));
        assertEquals(120, Combinatorics.permutations(IntStream.range(0, 5).toArray()).count());
        assertEquals(39916800, Combinatorics.permutations(IntStream.range(0, 11).toArray()).count());
    }

    @Test
    public void testChoices() {
        assertArrayEquals(new int[][]{{0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3}}, Combinatorics.choices(4, 2).toArray(int[][]::new));
        assertEquals(35, Combinatorics.choices(7, 4).count());
        assertEquals(126, Combinatorics.choices(9, 5).count());
    }

    @Test
    public void printGood() {
        for (int p = 1; p < 20; p++) {
            int k = 6 * p + 1;
            int v = 18 * p + 7;
            if (v % 12 == 1) {
                System.out.println(k + " " + v);
            }
        }
        for (int p = 1; p < 20; p++) {
            int k = 6 * p + 3;
            int v = 18 * p + 13;
            if (v % 12 == 1) {
                System.out.println(k + " " + v);
            }
        }
    }

    @Test
    public void testParity() {
        System.out.println(Combinatorics.permutations(new int[]{0, 1, 2, 3}).collect(Collectors.groupingBy(Combinatorics::parity, Collectors.counting())));
        System.out.println(Combinatorics.permutations(new int[]{0, 1, 2, 3, 4}).collect(Collectors.groupingBy(Combinatorics::parity, Collectors.counting())));
        System.out.println(Combinatorics.permutations(new int[]{0, 1, 2, 3, 4, 5}).collect(Collectors.groupingBy(Combinatorics::parity, Collectors.counting())));
        System.out.println(Combinatorics.permutations(new int[]{0, 1, 2, 3, 4, 5, 6, 7}).collect(Collectors.groupingBy(Combinatorics::parity, Collectors.counting())));
        System.out.println(Combinatorics.permutations(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8}).collect(Collectors.groupingBy(Combinatorics::parity, Collectors.counting())));
    }

    @Test
    public void testLucas() {
        int n = 38;
        int[] lucas = new int[n];
        lucas[0] = 2;
        lucas[1] = 1;
        for (int i = 2; i < n; i++) {
            lucas[i] = lucas[i - 1] + lucas[i - 2];
        }
        System.out.println(Arrays.toString(lucas));
        int[] evenLucas = IntStream.range(0, n / 2).map(i -> lucas[2 * i + 1]).toArray();
        boolean[] accessed = new boolean[10_000_000];
        IntStream.range(0, (int) Math.pow(3, n / 2)).parallel().forEach(i -> {
            int[] toTriple = new int[n / 2];
            int b = i;
            for (int j = 0; j < n / 2; j++) {
                toTriple[j] = b % 3;
                b = b / 3;
            }
            int sum = 0;
            for (int j = 0; j < n / 2; j++) {
                int d = switch (toTriple[j]) {
                    case 0 -> 0;
                    case 1 -> evenLucas[j];
                    case 2 -> -evenLucas[j];
                    default -> throw new IllegalStateException();
                };
                sum = sum + d;
            }
            if (sum >= 0 && sum < accessed.length) {
                accessed[sum] = true;
            }
        });
        for (int i = 0; i < accessed.length; i++) {
            if (!accessed[i]) {
                System.out.println(i);
            }
        }
    }

    private static final int cnt = 128;

    @Test
    public void test127() {
        PlaneSet[][] planes = new PlaneSet[cnt][cnt];
        for (int i = 1; i < cnt; i++) {
            for (int j = i + 1; j < cnt; j++) {
                planes[i][j] = getPlanes(i, j);
            }
        }
        int[] possible = new int[cnt * cnt];
        for (int i = 1; i < cnt; i++) {
            for (int j = i + 1; j < cnt; j++) {
                possible[i * cnt + j] = Integer.MAX_VALUE;
            }
        }
        Planes ps = new Planes(planes, possible);
        FixBS fst = new FixBS(cnt);
        fst.set(1, 8);
        LinkedHashMap<FixBS, Boolean> queue = new LinkedHashMap<>();
        queue.put(fst, true);
        ps.update(queue);
        search(ps, ps.nextFrom(128));
    }

    private static PlaneSet getPlanes(int i, int j) {
        int sum = i ^ j;
        Set<FixBS> planes = new HashSet<>();
        List<FixBS> lst = new ArrayList<>();
        Map<FixBS, Integer> rev = new HashMap<>();
        for (int k = 1; k < cnt; k++) {
            if (k == i || k == j || k == sum) {
                continue;
            }
            FixBS plane = new FixBS(cnt);
            plane.set(i);
            plane.set(j);
            plane.set(sum);
            plane.set(k);
            plane.set(i ^ k);
            plane.set(j ^ k);
            plane.set(sum ^ k);
            if (planes.add(plane)) {
                rev.put(plane, lst.size());
                lst.add(plane);
            }
        }
        return new PlaneSet(lst.toArray(FixBS[]::new), rev);
    }

    private static final int WORD_MASK = 0xffffffff;

    private record PlaneSet(FixBS[] map, Map<FixBS, Integer> rev) {}

    private static boolean empty(int mask, int idx) {
        return (mask & (1 << idx)) == 0;
    }

    private static int clear(int mask, int idx) {
        return mask & ~(1 << idx);
    }

    private static int nextSetBit(int mask, int from) {
        mask = mask & (WORD_MASK << from);
        return mask != 0 ? Integer.numberOfTrailingZeros(mask) : -1;
    }

    private record Planes(PlaneSet[][] planes, int[] possible) {
        private Planes copy() {
            return new Planes(planes, possible.clone());
        }

        private void update(SequencedMap<FixBS, Boolean> queue) {
            while (!queue.isEmpty()) {
                Map.Entry<FixBS, Boolean> e = queue.pollFirstEntry();
                if (e.getValue()) {
                    setPlane(e.getKey(), queue);
                } else {
                    removePlane(e.getKey(), queue);
                }
            }
        }

        private void removePlane(FixBS plane, SequencedMap<FixBS, Boolean> queue) {
            for (int i = plane.nextSetBit(0); i >= 0; i = plane.nextSetBit(i + 1)) {
                for (int j = plane.nextSetBit(i + 1); j >= 0; j = plane.nextSetBit(j + 1)) {
                    int planeIdx = planes[i][j].rev.get(plane);
                    int idx = i * cnt + j;
                    int available = possible[idx];
                    if (empty(available, planeIdx)) {
                        continue;
                    }
                    int newAvailable = clear(available, planeIdx);
                    possible[idx] = newAvailable;
                    int bitCount = Integer.bitCount(newAvailable);
                    if (bitCount == 0) {
                        throw new IllegalStateException();
                    }
                    if (bitCount == 1) {
                        queue.putLast(planes[i][j].map[nextSetBit(newAvailable, 0)], true);
                    }
                }
            }
        }

        private void setPlane(FixBS plane, SequencedMap<FixBS, Boolean> queue) {
            for (int i = plane.nextSetBit(0); i >= 0; i = plane.nextSetBit(i + 1)) {
                for (int j = plane.nextSetBit(i + 1); j >= 0; j = plane.nextSetBit(j + 1)) {
                    int planeIdx = planes[i][j].rev.get(plane);
                    int idx = i * cnt + j;
                    int available = possible[idx];
                    if (empty(available, planeIdx)) {
                        throw new IllegalStateException();
                    }
                    possible[idx] = 1 << planeIdx;
                    int toClear = clear(available, planeIdx);
                    for (int k = nextSetBit(toClear, 0); k >= 0; k = nextSetBit(toClear, k + 1)) {
                        queue.putLast(planes[i][j].map[k], false);
                    }
                }
            }
        }

        private int nextFrom(int prev) {
            for (int next = prev + 1; next < cnt * cnt; next++) {
                int i = next / cnt;
                int j = next % cnt;
                if (i >= j) {
                    continue;
                }
                int bCnt = Integer.bitCount(possible[next]);
                if (bCnt > 1) {
                    return next;
                }
            }
            return -1;
        }
    }

    private static void search(Planes planes, int next) {
        if (next < 0) {
            System.out.println(Arrays.toString(planes.possible));
            return;
        }
        int possible = planes.possible[next];
        int i = next / cnt;
        int j = next % cnt;
        FixBS[] map = planes.planes[i][j].map;
        for (int k = nextSetBit(possible, 0); k >= 0; k = nextSetBit(possible, k + 1)) {
            try {
                LinkedHashMap<FixBS, Boolean> queue = new LinkedHashMap<>();
                queue.put(map[k], true);
                Planes copy = planes.copy();
                copy.update(queue);
                search(copy, copy.nextFrom(next));
            } catch (IllegalStateException e) {
                // ok
            }
        }
    }

    @Test
    public void testAdmissibleArcs() {
        for (int q = 3; q < 65; q++) {
            int[] vals = new int[q];
            for (int n = 3; n < q; n++) {
                int v = n * q - q + n;
                if ((v - 1) % (n - 1) == 0 && (v * v - v) % (n * n - n) == 0) {
                    vals[n] = v;
                }
            }
            System.out.println(q + " " + IntStream.range(0, vals.length).filter(i -> vals[i] != 0).mapToObj(i -> i + ":" + vals[i]).collect(Collectors.joining(" ")));
        }
    }
}
