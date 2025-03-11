package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
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
}
