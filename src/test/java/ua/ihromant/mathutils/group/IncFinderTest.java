package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Inc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class IncFinderTest {
    @Test
    public void generateCom1() {
        int v = 25;
        int k = 4;
        int b = v * (v - 1) / k / (k - 1);
        List<Inc> liners = List.of(beamBlocks(v, k));
        int left = b - liners.getFirst().b();
        long time = System.currentTimeMillis();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size());
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStageCanon(liners, cnt);
            left--;
            System.out.println(left + " " + liners.size() + " " + cnt.get());
        }
        System.out.println("Generated " + left + " " + liners.size());
        System.out.println(System.currentTimeMillis() - time);
    }

    public static Inc beamBlocks(int v, int k) {
        int r = (v - 1) / (k - 1);
        BitSet inc = new BitSet(r * v + v);
        for (int i = 0; i < r; i++) {
            inc.set(i * v);
            for (int j = 0; j < k - 1; j++) {
                inc.set(i * v + 1 + i * (k - 1) + j);
            }
        }
        for (int i = 0; i < k; i++) {
            inc.set(r * v + 1 + (k - 1) * i);
        }
        return new Inc(inc, v, r + 1);
    }

    public static List<Inc> nextStageCanon(List<Inc> partials, AtomicLong cnt) {
        Map<BitSet, Inc> nonIsomorphic = new ConcurrentHashMap<>();
        partials.stream().parallel().forEach(partial -> {
            for (int[] block : partial.blocks()) {
                Inc liner = new Inc(partial, block);
                nonIsomorphic.putIfAbsent(liner.getCanonical(), liner);
                cnt.incrementAndGet();
            }
        });
        return new ArrayList<>(nonIsomorphic.values());
    }
}
