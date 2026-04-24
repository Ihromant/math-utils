package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class PermutationGroup implements Group {
    private final int[][] permutations;
    private final Map<Wrap, Integer> lookup;

    public PermutationGroup(int cnt, boolean even) {
        this(Combinatorics.permutations(IntStream.range(0, cnt).toArray())
                .filter(perm -> !even || Combinatorics.parity(perm) % 2 == 0).toArray(int[][]::new));
    }

    public PermutationGroup(int[][] permutations) {
        this.permutations = permutations;
        Arrays.sort(permutations, Combinatorics::compareArr);
        this.lookup = new HashMap<>();
        for (int i = 0; i < permutations.length; i++) {
            lookup.put(new Wrap(permutations[i]), i);
        }
    }

    public PermutationGroup subset(FixBS set) {
        int[][] res = new int[set.cardinality()][];
        int cnt = 0;
        for (int idx = set.nextSetBit(0); idx >= 0; idx = set.nextSetBit(idx + 1)) {
            res[cnt++] = permutations[idx];
        }
        return new PermutationGroup(res);
    }

    public static PermutationGroup byGenerators(int[][] base) {
        Set<Wrap> result = new HashSet<>();
        result.add(new Wrap(IntStream.range(0, base[0].length).toArray()));
        boolean added;
        do {
            added = false;
            for (Wrap el : result.toArray(Wrap[]::new)) {
                for (int[] gen : base) {
                    Wrap xy = new Wrap(comb(gen, el.arr));
                    Wrap yx = new Wrap(comb(el.arr, gen));
                    added = result.add(xy) || added;
                    added = result.add(yx) || added;
                }
            }
        } while (added);
        return new PermutationGroup(result.stream().map(Wrap::arr).toArray(int[][]::new));
    }

    public boolean contains(int[] perm) {
        return lookup.containsKey(new Wrap(perm));
    }

    public static int[] comb(int[] pa, int[] pb) {
        int[] result = new int[pa.length];
        for (int i = 0; i < pa.length; i++) {
            result[i] = pa[pb[i]];
        }
        return result;
    }
    
    public FixBS apply(int elem, FixBS fbs) {
        FixBS result = new FixBS(fbs.size());
        int[] perm = permutations[elem];
        for (int x = fbs.nextSetBit(0); x >= 0; x = fbs.nextSetBit(x + 1)) {
            result.set(perm[x]);
        }
        return result;
    }

    @Override
    public int op(int a, int b) {
        return lookup.get(new Wrap(comb(permutations[a], permutations[b])));
    }

    @Override
    public int inv(int a) {
        int[] perm = permutations[a];
        int[] map = new int[perm.length];
        for (int i = 0; i < perm.length; i++) {
            map[perm[i]] = i;
        }
        return lookup.get(new Wrap(map));
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

    public int[][] permutations() {
        return permutations;
    }

    public static boolean identity(int[] perm) {
        return IntStream.range(0, perm.length).allMatch(i -> i == perm[i]);
    }

    public static PermutationGroup mathieu11() {
        int[] cycle = IntStream.range(0, 11).map(i -> (i + 1) % 11).toArray();
        int[] snd = IntStream.range(0, 11).toArray();
        snd[2] = 6;
        snd[6] = 10;
        snd[10] = 7;
        snd[7] = 2;
        snd[3] = 9;
        snd[9] = 4;
        snd[4] = 5;
        snd[5] = 3;
        return PermutationGroup.byGenerators(new int[][]{cycle, snd});
    }

    private record Wrap(int[] arr) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Wrap(int[] arr1))) return false;
            return Arrays.equals(arr, arr1);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(arr) >>> 1;
        }
    }
}
