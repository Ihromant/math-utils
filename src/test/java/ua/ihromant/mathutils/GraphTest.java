package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GraphTest {
    @Test
    public void testBronKerbosh() {
        for (int n = 4; n < 10; n++) {
            Graph g = new Graph(n);
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    g.connect(i, j);
                }
            }
            List<FixBS> lst = new ArrayList<>();
            g.bronKerbPivot(lst::add);
            assertEquals(List.of(FixBS.of(n, IntStream.range(0, n).toArray())), lst);
            Graph g1 = new Graph(n);
            for (int i = 0; i < n; i++) {
                g1.connect(i, (i + 1) % n);
            }
            lst.clear();
            g1.bronKerbPivot(lst::add);
            assertEquals(n, lst.size());
        }
        FixBS[] arr = new FixBS[] {
                FixBS.of(12, 1, 2, 3),
                FixBS.of(12, 0, 2, 3, 4),
                FixBS.of(12, 0, 1, 3, 4),
                FixBS.of(12, 0, 1, 2, 5),
                FixBS.of(12, 1, 2, 6),
                FixBS.of(12, 3, 6, 7),
                FixBS.of(12, 4, 5, 7, 8),
                FixBS.of(12, 5, 6, 8),
                FixBS.of(12, 6, 7, 9),
                FixBS.of(12, 8, 10, 11),
                FixBS.of(12, 9, 11),
                FixBS.of(12, 9, 10),
        };
        Graph g = new Graph(arr);
        List<FixBS> lst = new ArrayList<>();
        g.bronKerbPivot(lst::add);
        System.out.println(lst);

        Graph g2 = new Graph(6);
        g2.connect(0, 1);
        g2.connect(0, 4);
        g2.connect(1, 4);
        g2.connect(1, 2);
        g2.connect(3, 4);
        g2.connect(2, 3);
        g2.connect(3, 5);
        g2.bronKerb(a -> System.out.println(Arrays.toString(a.toArray())));
        System.out.println();

        g2.bronKerbPivot(a -> System.out.println(Arrays.toString(a.toArray())));
        System.out.println();
    }
}
