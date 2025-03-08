package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

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
}
