package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BibdPointTest {
    @Test
    public void testCorrectness() {
        for (int p : BibdPoint.points()) {
            assertEquals(6, BibdPoint.point(p).cardinality());
        }
        for (int l : BibdPoint.lines()) {
            assertEquals(3, BibdPoint.line(l).cardinality());
        }
        for (int p1 : BibdPoint.points()) {
            for (int p2 : BibdPoint.points()) {
                if (p1 != p2) {
                    BitSet line = BibdPoint.line(BibdPoint.line(p1, p2));
                    assertTrue(line.get(p1));
                    assertTrue(line.get(p2));
                }
            }
        }
        for (int p : BibdPoint.points()) {
            for (int l : BibdPoint.lines(p)) {
                assertTrue(BibdPoint.line(l).get(p));
            }
        }
        for (int l : BibdPoint.lines()) {
            for (int p : BibdPoint.points(l)) {
                assertTrue(BibdPoint.point(p).get(l));
            }
        }
    }

    @Test
    public void testHyperbolicity() {
        int max = 0;
        int min = Integer.MAX_VALUE;
        for (int l : BibdPoint.lines()) {
            BitSet line = BibdPoint.line(l);
            for (int p : BibdPoint.points()) {
                if (line.get(p)) {
                    continue;
                }
                int counter = 0;
                for (int parallel : BibdPoint.lines(p)) {
                    if (BibdPoint.intersection(parallel, l) == -1) {
                        counter++;
                    }
                }
                //assertEquals(1, counter);
                max = Math.max(max, counter);
                min = Math.min(min, counter);
            }
        }
        System.out.println(min + " " + max);
    }

    @Test
    public void findQuadruples() {
        int SIZE = 41;
        Map<Quadruple, BitSet> sets = getQuadruples(SIZE);
        System.out.println(sets.size());
        List<Map.Entry<Quadruple, BitSet>> entries = new ArrayList<>(sets.entrySet());
        for (int a = 0; a < entries.size(); a++) {
            for (int b = a + 1; b < entries.size(); b++) {
                BitSet bs = new BitSet();
                bs.or(entries.get(a).getValue());
                bs.or(entries.get(b).getValue());
                if (bs.cardinality() == SIZE / 2) {
                    System.out.println(entries.get(a) + " " + entries.get(b));
                }
            }
        }
    }

    @Test
    public void findQuadruples2() {
        int size = 61;
        Map<Quadruple, BitSet> sets = getQuadruples(size);
        System.out.println(sets.size());
        List<Map.Entry<Quadruple, BitSet>> entries = new ArrayList<>(sets.entrySet());
        for (int a = 0; a < entries.size(); a++) {
            System.out.println(a);
            BitSet aSet = entries.get(a).getValue();
            for (int b = a + 1; b < entries.size(); b++) {
                BitSet bSet = entries.get(b).getValue();
                if (aSet.intersects(bSet)) {
                    continue;
                }
                for (int c = b + 1; c < entries.size(); c++) {
                    BitSet cSet = entries.get(c).getValue();
                    if (aSet.intersects(cSet) || bSet.intersects(cSet)) {
                        continue;
                    }
                    System.out.println(entries.get(a) + " " + entries.get(b) + " " + entries.get(c));
                }
            }
        }
    }

    private static Map<Quadruple, BitSet> getQuadruples(int SIZE) {
        Map<Quadruple, BitSet> sets = new HashMap<>();
        for (int i = 1; i < SIZE; i++) {
            for (int j = i + 1; j < SIZE; j++) {
                for (int k = j + 1; k < SIZE; k++) {
                    for (int l = k + 1; l < SIZE; l++) {
                        BitSet bs = new BitSet();
                        bs.set(diff(0, i, SIZE));
                        bs.set(diff(0, j, SIZE));
                        bs.set(diff(0, k, SIZE));
                        bs.set(diff(0, l, SIZE));
                        bs.set(diff(i, k, SIZE));
                        bs.set(diff(i, j, SIZE));
                        bs.set(diff(j, k, SIZE));
                        bs.set(diff(i, l, SIZE));
                        bs.set(diff(j, l, SIZE));
                        bs.set(diff(k, l, SIZE));
                        if (bs.cardinality() == 10) {
                            sets.put(new Quadruple(i, j, k, l), bs);
                        }
                    }
                }
            }
        }
        return sets;
    }

    @Test
    public void findTriples() {
        int SIZE = 49;
        Map<Triple, BitSet> sets = getTriples(SIZE);
        System.out.println(sets.size());
        List<Map.Entry<Triple, BitSet>> entries = new ArrayList<>(sets.entrySet());
        for (int a = 0; a < entries.size(); a++) {
            System.out.println(a);
            BitSet aSet = entries.get(a).getValue();
            for (int b = a + 1; b < entries.size(); b++) {
                BitSet bSet = entries.get(b).getValue();
                if (aSet.intersects(bSet)) {
                    continue;
                }
                for (int c = b + 1; c < entries.size(); c++) {
                    BitSet cSet = entries.get(c).getValue();
                    if (aSet.intersects(cSet) || bSet.intersects(cSet)) {
                        continue;
                    }
                    for (int d = c + 1; d < entries.size(); d++) {
                        BitSet dSet = entries.get(d).getValue();
                        if (aSet.intersects(dSet) || bSet.intersects(dSet) || cSet.intersects(dSet)) {
                            continue;
                        }
                        System.out.println(entries.get(a) + " " + entries.get(b) + " " + entries.get(c) + " " + entries.get(d));
                    }
                }
            }
        }
    }

    private static Map<Triple, BitSet> getTriples(int size) {
        Map<Triple, BitSet> sets = new HashMap<>();
        for (int i = 1; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                for (int k = j + 1; k < size; k++) {
                    BitSet bs = new BitSet();
                    bs.set(diff(0, i, size));
                    bs.set(diff(0, j, size));
                    bs.set(diff(0, k, size));
                    bs.set(diff(i, k, size));
                    bs.set(diff(i, j, size));
                    bs.set(diff(j, k, size));
                    if (bs.cardinality() == 6) {
                        sets.put(new Triple(i, j, k), bs);
                    }
                }
            }
        }
        return sets;
    }

    @Test
    public void findQuintuples3() {
        int size = 151;
        Map<Quintuple, BitSet> sets = getQuintuples(size);
        System.out.println(sets.size());
        List<Map.Entry<Quintuple, BitSet>> entries = new ArrayList<>(sets.entrySet());
        for (int a = 0; a < entries.size(); a++) {
            System.out.println(a);
            BitSet aSet = entries.get(a).getValue();
            for (int b = a + 1; b < entries.size(); b++) {
                BitSet bSet = entries.get(b).getValue();
                if (aSet.intersects(bSet)) {
                    continue;
                }
                for (int c = b + 1; c < entries.size(); c++) {
                    BitSet cSet = entries.get(c).getValue();
                    if (aSet.intersects(cSet) || bSet.intersects(cSet)) {
                        continue;
                    }
                    for (int d = c + 1; d < entries.size(); d++) {
                        BitSet dSet = entries.get(d).getValue();
                        if (aSet.intersects(dSet) || bSet.intersects(dSet) || cSet.intersects(dSet)) {
                            continue;
                        }
                        for (int e = d + 1; e < entries.size(); e++) {
                            BitSet eSet = entries.get(e).getValue();
                            if (aSet.intersects(eSet) || bSet.intersects(eSet) || cSet.intersects(eSet) || dSet.intersects(eSet)) {
                                continue;
                            }
                            System.out.println(entries.get(a) + " " + entries.get(b) + " " + entries.get(c) + " " + entries.get(d) + " " + entries.get(e));
                        }
                    }
                }
            }
        }
    }

    private static Map<Quintuple, BitSet> getQuintuples(int size) {
        Map<Quintuple, BitSet> sets = new HashMap<>();
        for (int i = 1; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                for (int k = j + 1; k < size; k++) {
                    for (int l = k + 1; l < size; l++) {
                        for (int m = l + 1; m < size; m++) {
                            BitSet bs = new BitSet();
                            bs.set(diff(0, i, size));
                            bs.set(diff(0, j, size));
                            bs.set(diff(0, k, size));
                            bs.set(diff(0, l, size));
                            bs.set(diff(0, m, size));
                            bs.set(diff(i, k, size));
                            bs.set(diff(i, j, size));
                            bs.set(diff(j, k, size));
                            bs.set(diff(i, l, size));
                            bs.set(diff(j, l, size));
                            bs.set(diff(k, l, size));
                            bs.set(diff(i, m, size));
                            bs.set(diff(j, m, size));
                            bs.set(diff(k, m, size));
                            bs.set(diff(l, m, size));
                            if (bs.cardinality() == size / 10) {
                                sets.put(new Quintuple(i, j, k, l, m), bs);
                            }
                        }
                    }
                }
            }
        }
        return sets;
    }

    @Test
    public void findMutant() {
        int SIZE = 19;
        Map<Tuple, BitSet> tuples = new HashMap<>();
        for (int i = 1; i < SIZE; i++) {
            for (int j = i + 1; j < SIZE; j++) {
                BitSet bs = new BitSet();
                bs.set(diff(0, i, SIZE));
                bs.set(diff(0, j, SIZE));
                bs.set(diff(i, j, SIZE));
                if (bs.cardinality() == 3) {
                    tuples.put(new Tuple(i, j), bs);
                }
            }
        }
        Map<Triple, BitSet> triples = new HashMap<>();
        for (int i = 1; i < SIZE; i++) {
            for (int j = i + 1; j < SIZE; j++) {
                for (int k = j + 1; k < SIZE; k++) {
                    BitSet bs = new BitSet();
                    bs.set(diff(0, i, SIZE));
                    bs.set(diff(0, j, SIZE));
                    bs.set(diff(0, k, SIZE));
                    bs.set(diff(i, k, SIZE));
                    bs.set(diff(i, j, SIZE));
                    bs.set(diff(j, k, SIZE));
                    if (bs.cardinality() == 6) {
                        triples.put(new Triple(i, j, k), bs);
                    }
                }
            }
        }
        for (Map.Entry<Tuple, BitSet> a1 : tuples.entrySet()) {
            for (Map.Entry<Triple, BitSet> a2 : triples.entrySet()) {
                BitSet bs = new BitSet();
                bs.or(a1.getValue());
                bs.or(a2.getValue());
                if (bs.cardinality() == SIZE / 2) {
                    System.out.println(a1 + " " + a2);
                }
            }
        }
    }

    @Test
    public void findTuples() {
        int SIZE = 19;
        Map<Tuple, BitSet> sets = new HashMap<>();
        for (int i = 1; i < SIZE; i++) {
            for (int j = i + 1; j < SIZE; j++) {
                BitSet bs = new BitSet();
                bs.set(diff(0, i, SIZE));
                bs.set(diff(0, j, SIZE));
                bs.set(diff(i, j, SIZE));
                if (bs.cardinality() == SIZE / 6) {
                    sets.put(new Tuple(i, j), bs);
                }
            }
        }
        for (Map.Entry<Tuple, BitSet> a1 : sets.entrySet()) {
            for (Map.Entry<Tuple, BitSet> a2 : sets.entrySet()) {
                for (Map.Entry<Tuple, BitSet> a3 : sets.entrySet()) {
                    BitSet bs = new BitSet();
                    bs.or(a1.getValue());
                    bs.or(a2.getValue());
                    bs.or(a3.getValue());
                    if (bs.cardinality() == SIZE / 2) {
                        System.out.println(a1 + " " + a2 + " " + a3);
                    }
                }
            }
        }
    }

    @Test
    public void findSextuples() {
        int size = 127;
        Map<Sextuple, BitSet> sets = new HashMap<>();
        for (int i = 1; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                for (int k = j + 1; k < size; k++) {
                    for (int l = k + 1; l < size; l++) {
                        for (int m = l + 1; m < size; m++) {
                            for (int n = m + 1; n < size; n++) {
                                BitSet bs = new BitSet();
                                bs.set(diff(0, i, size));
                                bs.set(diff(0, j, size));
                                bs.set(diff(0, k, size));
                                bs.set(diff(0, l, size));
                                bs.set(diff(0, m, size));
                                bs.set(diff(0, n, size));
                                bs.set(diff(i, k, size));
                                bs.set(diff(i, j, size));
                                bs.set(diff(j, k, size));
                                bs.set(diff(i, l, size));
                                bs.set(diff(j, l, size));
                                bs.set(diff(k, l, size));
                                bs.set(diff(i, m, size));
                                bs.set(diff(j, m, size));
                                bs.set(diff(k, m, size));
                                bs.set(diff(l, m, size));
                                bs.set(diff(i, n, size));
                                bs.set(diff(j, n, size));
                                bs.set(diff(k, n, size));
                                bs.set(diff(l, n, size));
                                bs.set(diff(m, n, size));
                                if (bs.cardinality() == size / 6) {
                                    sets.put(new Sextuple(i, j, k, l, m, n), bs);
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println(sets.size());
        List<Map.Entry<Sextuple, BitSet>> entries = new ArrayList<>(sets.entrySet());
        for (int a = 0; a < entries.size(); a++) {
            System.out.println(a);
            BitSet aSet = entries.get(a).getValue();
            for (int b = a + 1; b < entries.size(); b++) {
                BitSet bSet = entries.get(b).getValue();
                if (aSet.intersects(bSet)) {
                    continue;
                }
                for (int c = b + 1; c < entries.size(); c++) {
                    BitSet cSet = entries.get(c).getValue();
                    if (aSet.intersects(cSet) || bSet.intersects(cSet)) {
                        continue;
                    }
                    System.out.println(entries.get(a) + " " + entries.get(b) + " " + entries.get(c));
                }
            }
        }
    }

    private record Tuple(int i, int j) {}

    private record Triple(int i, int j, int k) {}

    private record Quadruple(int i, int j, int k, int l) {}

    private record Quintuple(int i, int j, int k, int l, int m) {}

    private record Sextuple(int i, int j, int k, int l, int m, int n) {}

    private static int diff(int a, int b, int size) {
        return Math.min(Math.abs(a - b), Math.abs(Math.abs(a - b) - size));
    }
}
