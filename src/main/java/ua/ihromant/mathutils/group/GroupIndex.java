package ua.ihromant.mathutils.group;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class GroupIndex {
    public static Group group(int order, int index) throws IOException {
        System.out.println("Reading SmallGroup(" + order + "," + index + ")");
        return new GapInteractor().smallGroup(order, index);
    }

    public static String identify(Group g) throws IOException {
        if (g.order() > 1500) {
            return "Large order " + g.order();
        }
        return new GapInteractor().identifyGroup(g);
    }

    public static int groupId(Group g) throws IOException {
        if (g.order() > 1500) {
            return -1;
        }
        return new GapInteractor().groupId(g);
    }

    public static int groupCount(int order) throws IOException {
        return new GapInteractor().groupCount(order);
    }

    public static GapGroup gapGroup(int order, int idx) throws IOException {
        List<List<List<Integer>>> cycles = new GapInteractor().gapCycles(order, idx);
        int[][] gens = toGens(cycles);
        return new GapGroup(permGroup(gens).asTable(), cycles);
    }

    public static String identify(GapGroup gg) throws IOException {
        return new GapInteractor().identifyByCycles(gg.cycles());
    }

    private static int[][] toGens(List<List<List<Integer>>> cycles) {
        int max = cycles.stream().flatMap(List::stream).flatMap(List::stream)
                .mapToInt(Integer::intValue).max().orElseThrow();
        List<int[]> perms = new ArrayList<>();
        for (List<List<Integer>> base : cycles) {
            int[] perm = IntStream.range(0, max).toArray();
            for (List<Integer> cycle : base) {
                for (int i = 0; i < cycle.size() - 1; i++) {
                    perm[cycle.get(i) - 1] = cycle.get(i + 1) - 1;
                }
                perm[cycle.getLast() - 1] = cycle.getFirst() - 1;
            }
            perms.add(perm);
        }
        return perms.toArray(int[][]::new);
    }

    private static PermutationGroup permGroup(int[][] gens) {
        Set<ArrWrap> result = new HashSet<>();
        result.add(new ArrWrap(IntStream.range(0, gens[0].length).toArray()));
        boolean added;
        do {
            added = false;
            for (ArrWrap el : result.toArray(ArrWrap[]::new)) {
                for (int[] gen : gens) {
                    ArrWrap xy = new ArrWrap(combine(gen, el.map));
                    ArrWrap yx = new ArrWrap(combine(el.map, gen));
                    added = result.add(xy) || added;
                    added = result.add(yx) || added;
                }
            }
        } while (added);
        return new PermutationGroup(result.stream().map(ArrWrap::map).toArray(int[][]::new));
    }

    private static int[] combine(int[] a, int[] b) {
        int[] result = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[b[i]];
        }
        return result;
    }

    private record ArrWrap(int[] map) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ArrWrap(int[] map1))) return false;
            return Arrays.equals(map, map1);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(map) >>> 1;
        }
    }
}
