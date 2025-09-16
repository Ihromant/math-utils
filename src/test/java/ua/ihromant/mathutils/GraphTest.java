package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;

public class GraphTest {
    @Test
    public void test() {
        Graph g = new Graph(6);
        g.connect(0, 1);
        g.connect(0, 4);
        g.connect(1, 4);
        g.connect(1, 2);
        g.connect(3, 4);
        g.connect(2, 3);
        g.connect(3, 5);
        FixBS p = new FixBS(6);
        p.set(0, 6);
        IntList l = new IntList(6);
        g.bronKerb(l, p, new FixBS(6), arr -> System.out.println(Arrays.toString(arr.toArray())));
        System.out.println();
        FixBS p1 = new FixBS(6);
        p1.set(0, 6);
        IntList l1 = new IntList(6);
        g.bronKerbPivot(l1, p1, new FixBS(6), arr -> System.out.println(Arrays.toString(arr.toArray())));
    }
}
