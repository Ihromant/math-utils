package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BibdPointTest {
    @Test
    public void findQuadruples() {
        int size = 41;
        Map<Quadruple, BitSet> sets = getQuadruples(size);
        List<Map.Entry<Quadruple, BitSet>> entries = new ArrayList<>(sets.entrySet());
        for (int a = 0; a < entries.size(); a++) {
            BitSet aSet = entries.get(a).getValue();
            for (int b = a + 1; b < entries.size(); b++) {
                BitSet bSet = entries.get(b).getValue();
                if (aSet.intersects(bSet)) {
                    continue;
                }
                System.out.println(entries.get(a) + " " + entries.get(b));
            }
        }
    }

    @Test
    public void findQuadruples2() {
        int size = 71;
        Map<Quadruple, BitSet> sets = getQuadruples(size);
        Map<Quintuple, BitSet> quins = getQuintuples(size);
        List<Map.Entry<Quadruple, BitSet>> entries = new ArrayList<>(sets.entrySet());
        List<Map.Entry<Quintuple, BitSet>> quinList = new ArrayList<>(quins.entrySet());
        for (int a = 0; a < quinList.size(); a++) {
            if (a % 100 == 0) {
                System.out.println(a);
            }
            BitSet aSet = quinList.get(a).getValue();
            for (int b = a + 1; b < entries.size(); b++) {
                BitSet bSet = entries.get(b).getValue();
                if (aSet.intersects(bSet)) {
                    continue;
                }
                for (int c = b + 1; c < entries.size(); c++) {
                    BitSet cSet = entries.get(c).getValue();
                    if (cSet.intersects(aSet) || cSet.intersects(bSet)) {
                        continue;
                    }
                    System.out.println(quinList.get(a) + " " + entries.get(b) + " " + entries.get(c));
                }
            }
        }
    }

    @Test
    public void findQuadruples1() {
        int size = 81;
        Map<Quadruple, BitSet> sets = getQuadruples(size);
        List<Map.Entry<Quadruple, BitSet>> entries = new ArrayList<>(sets.entrySet());
        for (int a = 0; a < entries.size(); a++) {
            if (a % 1000 == 0) {
                System.out.println(a);
            }
            BitSet aSet = entries.get(a).getValue();
            for (int b = a + 1; b < entries.size(); b++) {
                BitSet bSet = entries.get(b).getValue();
                if (aSet.intersects(bSet)) {
                    continue;
                }
                for (int c = b + 1; c < entries.size(); c++) {
                    BitSet cSet = entries.get(c).getValue();
                    if (cSet.intersects(aSet) || cSet.intersects(bSet)) {
                        continue;
                    }
                    for (int d = c + 1; d < entries.size(); d++) {
                        BitSet dSet = entries.get(d).getValue();
                        if (dSet.intersects(cSet) || dSet.intersects(bSet) || dSet.intersects(aSet)) {
                            continue;
                        }
                        System.out.println(entries.get(a) + " " + entries.get(b) + " " + entries.get(c) + " " + entries.get(d));
                    }
                }
            }
        }
    }

    private static Map<Quadruple, BitSet> getQuadruples(int size) {
        Map<Quadruple, BitSet> sets = new HashMap<>();
        for (int i = 1; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                for (int k = j + 1; k < size; k++) {
                    for (int l = k + 1; l < size; l++) {
                        BitSet bs = new BitSet();
                        bs.set(diff(0, i, size));
                        bs.set(diff(0, j, size));
                        bs.set(diff(0, k, size));
                        bs.set(diff(0, l, size));
                        bs.set(diff(i, k, size));
                        bs.set(diff(i, j, size));
                        bs.set(diff(j, k, size));
                        bs.set(diff(i, l, size));
                        bs.set(diff(j, l, size));
                        bs.set(diff(k, l, size));
                        if (bs.cardinality() == 10) {
                            sets.put(new Quadruple(i, j, k, l), bs);
                        }
                    }
                }
            }
        }
        System.out.println("Quadruples for size " + size + " count " + sets.size());
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
        System.out.println("Triples for size " + size + " count " + sets.size());
        return sets;
    }

    @Test
    public void findQuintuples3() {
        int size = 61;
        Map<Quintuple, BitSet> sets = getQuintuples(size);
        List<Map.Entry<Quintuple, BitSet>> entries = new ArrayList<>(sets.entrySet());
        for (int a = 0; a < entries.size(); a++) {
            if (a % 1000 == 0) {
                System.out.println(a);
            }
            BitSet aSet = entries.get(a).getValue();
            for (int b = a + 1; b < entries.size(); b++) {
                BitSet bSet = entries.get(b).getValue();
                if (aSet.intersects(bSet)) {
                    continue;
                }
                System.out.println(entries.get(a) + " " + entries.get(b));
            }
        }
    }

    @Test
    public void findSextuple() {
        int size = 85;
        Map<Sextuple, BitSet> sets = getSextuples(size);
        List<Map.Entry<Sextuple, BitSet>> entries = new ArrayList<>(sets.entrySet());
        for (int a = 0; a < entries.size(); a++) {
            if (a % 100 == 0) {
                System.out.println(a);
            }
            BitSet aSet = entries.get(a).getValue();
            for (int b = a + 1; b < entries.size(); b++) {
                BitSet bSet = entries.get(b).getValue();
                if (aSet.intersects(bSet)) {
                    continue;
                }
                System.out.println(entries.get(a) + " " + entries.get(b));
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
                            if (bs.cardinality() == 15) {
                                sets.put(new Quintuple(i, j, k, l, m), bs);
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Quintuples for size " + size + " count " + sets.size());
        return sets;
    }

    @Test
    public void findMutant() {
        int size = 73;
        Map<Quintuple, BitSet> quins = getQuintuples(size);
        Map<Sextuple, BitSet> six = getSextuples(size);
        List<Map.Entry<Quintuple, BitSet>> quinList = new ArrayList<>(quins.entrySet());
        List<Map.Entry<Sextuple, BitSet>> sixList = new ArrayList<>(six.entrySet());
        for (Map.Entry<Quintuple, BitSet> a1 : quinList) {
            for (Map.Entry<Sextuple, BitSet> a2 : sixList) {
                if (a1.getValue().intersects(a2.getValue())) {
                    continue;
                }
                System.out.println(a1 + " " + a2);
            }
        }
    }

    private static Map<Tuple, BitSet> getTuples(int size) {
        Map<Tuple, BitSet> sets = new HashMap<>();
        for (int i = 1; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                BitSet bs = new BitSet();
                bs.set(diff(0, i, size));
                bs.set(diff(0, j, size));
                bs.set(diff(i, j, size));
                if (bs.cardinality() == 3) {
                    sets.put(new Tuple(i, j), bs);
                }
            }
        }
        System.out.println("Tuples for size " + size + " count " + sets.size());
        return sets;
    }

    private static Map<Sextuple, BitSet> getSextuples(int size) {
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
                                if (bs.cardinality() == 21) {
                                    sets.put(new Sextuple(i, j, k, l, m, n), bs);
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Sextuples for size " + size + " count " + sets.size());
        return sets;
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
