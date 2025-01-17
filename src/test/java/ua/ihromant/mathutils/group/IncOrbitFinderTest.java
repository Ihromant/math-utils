package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.PartialLiner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

public class IncOrbitFinderTest {
    @Test
    public void generateCom() {
        int v = 13;
        int k = 3;
        int b = v * (v - 1) / k / (k - 1);
        OrbitConf conf = new OrbitConf(1, 3, v, b);
        PartialLiner empty = new PartialLiner(v, k);
        List<PartialLiner> next = List.of(empty);
        long time = System.currentTimeMillis();
        System.out.println("Started generation for v = " + v + ", k = " + k);
        Map<FixBS, PartialLiner> unique = new ConcurrentHashMap<>();
        while (!next.isEmpty()) {
            next = nextStage(next, unique, conf);
            System.out.println(next.size());
        }
        System.out.println("Generated " + unique.size());
        System.out.println(System.currentTimeMillis() - time);
    }

    @Test
    public void depthFirstSearch() {
        int v = 13;
        int k = 3;
        int b = v * (v - 1) / k / (k - 1);
        OrbitConf conf = new OrbitConf(3, 2, v, b);
        PartialLiner empty = new PartialLiner(v, k);
        long time = System.currentTimeMillis();
        System.out.println("Started generation for v = " + v + ", k = " + k);
        depthFirstSearchPar(empty, conf, 0, liner -> {
            System.out.println(Arrays.deepToString(liner.lines()));
        });
        System.out.println(System.currentTimeMillis() - time);
    }

    private record OrbitConf(int fixed, int orbitLength, int v, int full) {
        private int[][] permute(int[] block) {
            Set<FixBS> result = new HashSet<>();
            for (int i = 0; i < orbitLength; i++) {
                FixBS orb = new FixBS(v);
                for (int k : block) {
                    orb.set(permutePoint(k, i));
                }
                result.add(orb);
            }
            return result.stream().map(FixBS::toArray).toArray(int[][]::new);
        }

        private int permutePoint(int pt, int idx) {
            if (pt < fixed) {
                return pt;
            }
            int rest = (pt - fixed) % orbitLength;
            int base = pt - rest;
            return base + ((rest + idx) % orbitLength);
        }
    }

    private static List<PartialLiner> nextStage(List<PartialLiner> partials, Map<FixBS, PartialLiner> unique, OrbitConf conf) {
        List<PartialLiner> result = new ArrayList<>();
        for (PartialLiner pl : partials) {
            if (pl.lineCount() == conf.full()) {
                FixBS canon = pl.getCanonical();
                unique.putIfAbsent(canon, pl);
                continue;
            }
            ex: for (int[] block : pl.blocks(true)) {
                PartialLiner base = pl;
                int[][] permuted = conf.permute(block);
                for (int[] possible : permuted) {
                    for (int i = 0; i < possible.length; i++) {
                        for (int j = i + 1; j < possible.length; j++) {
                            if (base.line(possible[i], possible[j]) >= 0) {
                                continue ex;
                            }
                        }
                    }
                    base = new PartialLiner(base, possible);
                }
                result.add(base);
            }
        }
        return result;
    }

    private static void depthFirstSearch(PartialLiner partial, OrbitConf conf, Consumer<PartialLiner> cons) {
        if (partial.lineCount() == conf.full()) {
            cons.accept(partial);
            return;
        }
        ex: for (int[] block : partial.blocks(true)) {
            PartialLiner base = partial;
            int[][] permuted = conf.permute(block);
            for (int[] possible : permuted) {
                for (int i = 0; i < possible.length; i++) {
                    for (int j = i + 1; j < possible.length; j++) {
                        if (base.line(possible[i], possible[j]) >= 0) {
                            continue ex;
                        }
                    }
                }
                base = new PartialLiner(base, possible);
            }
            depthFirstSearch(base, conf, cons);
        }
    }

    private static void depthFirstSearchPar(PartialLiner partial, OrbitConf conf, int depth, Consumer<PartialLiner> cons) {
        if (partial.lineCount() == conf.full()) {
            cons.accept(partial);
            return;
        }
        int[][] blocks = StreamSupport.stream(partial.blocks(true).spliterator(), false).toArray(int[][]::new);
        if (depth >= 0) {
            System.out.println("Parallel search for depth " + depth + " and " + blocks.length + " variants");
        }
        Arrays.stream(blocks).parallel().forEach(block -> {
            PartialLiner base = partial;
            int[][] permuted = conf.permute(block);
            for (int[] possible : permuted) {
                for (int i = 0; i < possible.length; i++) {
                    for (int j = i + 1; j < possible.length; j++) {
                        if (base.line(possible[i], possible[j]) >= 0) {
                            return;
                        }
                    }
                }
                base = new PartialLiner(base, possible);
            }
            if (depth >= 0) {
                depthFirstSearchPar(base, conf, depth - 1, cons);
            } else {
                depthFirstSearch(base, conf, cons);
            }
        });
    }
}
