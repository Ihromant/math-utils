package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

/**
 * (c) Jarlax
 */
public class CFinderTest {
    @Test
    public void test() {
        int v = 7;
        int k = 3;
        int r = (v - 1) / (k - 1);
        int b = r * v / k;
        BitSet[] adj = IntStream.range(0, b).mapToObj(BitSet::new).toArray(BitSet[]::new);
        BitSet[] radj = IntStream.range(0, v).mapToObj(BitSet::new).toArray(BitSet[]::new);
        BitSet[] conn = IntStream.range(0, v).mapToObj(BitSet::new).toArray(BitSet[]::new);
        for (int i = 0; i < v; i++) {
            conn[i].set(i);
        }
        BiConsumer<Integer, Integer> flip = (i, j) -> {
            conn[j].xor(adj[i]);
            conn[j].set(j);
            adj[i].flip(j);
            radj[j].flip(i);
        };

        BiConsumer<Integer, Integer> f = new BiConsumer<>() {
            @Override
            public void accept(Integer idx, Integer sj) {
                if (idx == b) {
                    for (int i = 0; i < b; ++i) {
                        for (int j = 0; j < v; ++j) {
                            if (adj[i].get(j)) {
                                System.out.print(j + 1);
                            }
                        }
                        System.out.print(' ');
                    }
                    System.out.println('\n');
                }

                for (int j = sj; j < v; ++j) {
                    if (radj[j].cardinality() == r) continue;
                    if ((conn[j].intersects(adj[idx]))) continue;
                    flip.accept(idx, j);
                    if (adj[idx].cardinality() < k) {
                        accept(idx, j + 1);
                    } else if (idx == 0 || adj[idx].toLongArray()[0] > adj[idx - 1].toLongArray()[0]) {
                        accept(idx + 1, 0);
                    }
                    flip.accept(idx, j);
                }
            };
        };
        f.accept(0, 0);
    }
}
