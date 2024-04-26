package ua.ihromant.mathutils.group;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PermutationGroup implements Group {
    private final int[][] permutations;
    private final Map<Map<Integer, Integer>, Integer> lookup;

    public PermutationGroup(int[][] permutations) {
        this.permutations = permutations;
        this.lookup = new HashMap<>();
        for (int i = 0; i < permutations.length; i++) {
            int[] perm = permutations[i];
            Map<Integer, Integer> map = toMap(perm);
            lookup.put(map, i);
        }
    }

    private static Map<Integer, Integer> toMap(int[] perm) {
        return IntStream.range(0, perm.length).boxed().collect(Collectors.toMap(Function.identity(), p -> perm[p]));
    }

    @Override
    public int op(int a, int b) {
        Map<Integer, Integer> map = new HashMap<>();
        int[] pa = permutations[a];
        int[] pb = permutations[b];
        for (int i = 0; i < pa.length; i++) {
            map.put(i, pa[pb[i]]);
        }
        return lookup.get(map);
    }

    @Override
    public int inv(int a) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < permutations[a].length; i++) {
            map.put(permutations[a][i], i);
        }
        return lookup.get(map);
    }

    @Override
    public int order() {
        return permutations.length;
    }

    @Override
    public String name() {
        return "Permutation group";
    }

    @Override
    public String elementName(int a) {
        return Arrays.toString(permutations[a]);
    }

    public int[] permutation(int a) {
        return permutations[a];
    }

    public static boolean identity(int[] perm) {
        return IntStream.range(0, perm.length).allMatch(i -> i == perm[i]);
    }

    @Override
    public int[][] auth() {
        throw new UnsupportedOperationException();
    }
}
