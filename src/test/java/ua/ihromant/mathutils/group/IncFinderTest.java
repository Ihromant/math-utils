package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class IncFinderTest {
    @Test
    public void generateCom() {
        int v = 7;
        int k = 3;
        int b = v * (v - 1) / k / (k - 1);
        List<Inc> liners = List.of(beamBlocks(v, k));
        int left = b - liners.getFirst().b();
        long time = System.currentTimeMillis();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size());
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStage(liners, cnt);
            left--;
            System.out.println(left + " " + liners.size() + " " + cnt.get());
        }
        System.out.println("Generated " + left + " " + liners.size());
       // liners.forEach(l -> System.out.println(l + "\n"));
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

    public static List<Inc> nextStage(List<Inc> partials, AtomicLong cnt) {
        Set<Inc> nonIsomorphic = new HashSet<>();
        for (Inc partial : partials) {
            for (int[] block : partial.blocks()) {
                Inc liner = new Inc(partial, block).sorted();
                cnt.incrementAndGet();
                nonIsomorphic.add(liner);
            }
        }
        return new ArrayList<>(nonIsomorphic);
    }
}
