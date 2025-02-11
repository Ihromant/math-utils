package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PermutationGroup implements Group {
    private final int[][] permutations;
    private final Map<Map<Integer, Integer>, Integer> lookup;

    public PermutationGroup(int cnt, boolean even) {
        this(GaloisField.permutations(IntStream.range(0, cnt).toArray())
                .filter(perm -> !even || parity(perm) % 2 == 0).toArray(int[][]::new));
    }

    public PermutationGroup(int[][] permutations) {
        this.permutations = permutations;
        Arrays.sort(permutations, Group::compareArr);
        this.lookup = new HashMap<>();
        for (int i = 0; i < permutations.length; i++) {
            int[] perm = permutations[i];
            Map<Integer, Integer> map = toMap(perm);
            lookup.put(map, i);
        }
    }

    public static int parity(int[] arr) {
        int cnt = 0;
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                if (arr[i] > arr[j]) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    public PermutationGroup subset(FixBS set) {
        int[][] res = new int[set.cardinality()][];
        int cnt = 0;
        for (int idx = set.nextSetBit(0); idx >= 0; idx = set.nextSetBit(idx + 1)) {
            res[cnt++] = permutations[idx];
        }
        return new PermutationGroup(res);
    }
    
    public FixBS apply(int elem, FixBS fbs) {
        FixBS result = new FixBS(fbs.size());
        int[] perm = permutations[elem];
        for (int x = fbs.nextSetBit(0); x >= 0; x = fbs.nextSetBit(x + 1)) {
            result.set(perm[x]);
        }
        return result;
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
    public int[][] auth() { // this is correct only for most S_n, but still generates specific Auth subgroup
        int ord = order();
        int[][] result = new int[ord][ord];
        for (int conj = 0; conj < ord; conj++) {
            for (int x = 0; x < ord; x++) {
                result[conj][x] = op(inv(conj), op(x, conj));
            }
        }
        return result;
    }
}
