package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FuzzyBalLinerTest {
    @Test
    public void generate() {
        int v = 15;
        int k = 3;
        int r = (v - 1) / (k - 1);
        int b = v * r / k;
        int[][] lines = beamBlocks(v, k);
        List<FuzzyBalLiner> liners = List.of(FuzzyBalLiner.of(v, k, lines));
        int needed = (b - lines.length) * (k - 2);
        while (needed > 0) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStage(liners, cnt);
            needed--;
            System.out.println(needed + " " + liners.size() + " " + cnt.get());
        }
    }

    private static List<FuzzyBalLiner> nextStage(List<FuzzyBalLiner> partials, AtomicLong cnt) {
        Map<FixBS, FuzzyBalLiner> nonIso = partials.stream().parallel().<FuzzyBalLiner>mapMulti((lnr, sink) -> {
            int[][] chars = lnr.pointChars();
            int min = Integer.MAX_VALUE;
            int pt = -1;
            int v = lnr.getV();
            int k = lnr.getK();
            int r = (v - 1) / (k - 1);
            int allC = r * (k - 2);
            int all = (v - 1) * (v - 2) / 2;
            for (int i = 0; i < v; i++) {
                if (allC == chars[i][0]) {
                    continue;
                }
                int needed = allC - chars[i][0];
                int variants = all - chars[i][0] - chars[i][1];
                int variety = variants - needed;
                if (variety < 0) {
                    return;
                }
                if (variety < min) {
                    min = variety;
                    pt = i;
                }
            }
            for (Col c : lnr.undefinedTriples(pt)) {
                FuzzyBalLiner copy = lnr.copy();
                Queue<Rel> q = new ArrayDeque<>();
                q.add(c);
                copy.update(q);
                cnt.incrementAndGet();
                sink.accept(copy);
            }
        }).collect(Collectors.toMap(l -> toLiner(l).getCanonical(), Function.identity(), (a, b) -> a, ConcurrentHashMap::new));
        return new ArrayList<>(nonIso.values());
    }

    private static int[][] beamBlocks(int v, int k) {
        int r = (v - 1) / (k - 1);
        int[][] blocks = new int[r + 1][k];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < k - 1; j++) {
                blocks[i][j + 1] = 1 + i * (k - 1) + j;
            }
        }
        for (int i = 0; i < k; i++) {
            blocks[r][i] = 1 + (k - 1) * i;
        }
        return blocks;
    }

    private static Liner toLiner(FuzzyBalLiner liner) {
        return new Liner(liner.getV(), liner.lines().stream().map(l -> l.stream().toArray()).toArray(int[][]::new));
    }
}
