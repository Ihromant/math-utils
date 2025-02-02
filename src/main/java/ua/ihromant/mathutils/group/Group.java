package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.QuickFind;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.IntStream;

public interface Group {
    int op(int a, int b);

    int inv(int a);

    int order();

    default int order(int a) {
        int pow = 0;
        int counter = 0;
        do {
            counter++;
            pow = op(a, pow);
        } while (pow != 0);
        return counter;
    }

    default int expOrder(int a) {
        int eul = euler(order());
        int res = a;
        for (int i = 1; i < eul + 1; i++) {
            res = mul(res, a);
            if (res == a) {
                return i;
            }
        }
        return -1;
    }

    String name();

    String elementName(int a);

    int[][] auth();

    default int mul(int a, int cff) {
        int result = 0;
        for (int i = 0; i < cff; i++) {
            result = op(a, result);
        }
        return result;
    }

    default int exponent(int base, int power) {
        int result = 1;
        while (power > 0) {
            if (power % 2 == 1) {
                result = mul(result, base);
            }
            base = mul(base, base);
            power = power / 2;
        }
        return result;
    }

    default int[] squareRoots(int from) {
        return IntStream.range(0, order()).filter(i -> op(i, i) == from).toArray();
    }

    default IntStream elements() {
        return IntStream.range(0, order());
    }

    default Group asTable() {
        int order = order();
        int[][] table = new int[order][order];
        for (int i = 0; i < order; i++) {
            for (int j = 0; j < order; j++) {
                table[i][j] = op(i, j);
            }
        }
        return new TableGroup(table);
    }

    default BitSet difference(int[] basic) {
        return Arrays.stream(basic).flatMap(i -> Arrays.stream(basic).filter(j -> i != j).map(j -> op(i, inv(j))))
                .collect(BitSet::new, BitSet::set, BitSet::or);
    }

    static int[] factorize(int base) {
        List<Integer> result = new ArrayList<>();
        int from = 2;
        while (base != 1) {
            int factor = factor(from, base);
            from = factor;
            base = base / factor;
            result.add(factor);
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int factor(int from, int base) {
        int sqrt = (int) Math.ceil(Math.sqrt(base + 1));
        for (int i = from; i <= sqrt; i++) {
            if (base % i == 0) {
                return i;
            }
        }
        return base;
    }

    static long[] factorize(long base) {
        List<Long> result = new ArrayList<>();
        long from = 2;
        while (base != 1) {
            long factor = factor(from, base);
            from = factor;
            base = base / factor;
            result.add(factor);
        }
        return result.stream().mapToLong(Long::longValue).toArray();
    }

    private static long factor(long from, long base) {
        long sqrt = (long) Math.ceil(Math.sqrt(base + 1));
        for (long i = from; i <= sqrt; i++) {
            if (base % i == 0) {
                return i;
            }
        }
        return base;
    }

    static int gcd(int a, int b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }

    static int euler(int base) {
        int[] factors = factorize(base);
        if (factors.length == 0) {
            return 0;
        }
        int result = 1;
        int idx = 0;
        while (idx < factors.length) {
            int curr = factors[idx];
            result = result * (curr - 1);
            while (++idx < factors.length && factors[idx] == curr) {
                result = result * curr;
            }
        }
        return result;
    }

    default int conjugate(int fst, int snd) {
        return op(op(snd, fst), inv(snd));
    }

    default List<FixBS> conjugationClasses() {
        int order = order();
        QuickFind qf = new QuickFind(order);
        for (int g = 0; g < order; g++) {
            for (int x = 0; x < order; x++) {
                qf.union(x, op(inv(g), op(x, g)));
            }
        }
        return qf.components();
    }

    default boolean isCommutative() {
        return IntStream.range(1, order()).allMatch(i -> IntStream.range(1, order()).allMatch(j -> op(i, j) == op(j, i)));
    }

    default List<SubGroup> subGroups() {
        List<SubGroup> result = new ArrayList<>();
        int order = order();
        FixBS all = new FixBS(order);
        all.set(0, order);
        FixBS init = new FixBS(order);
        init.set(0);
        result.add(new SubGroup(this, init));
        find(result, init, 0, order);
        return result;
    }

    private void find(List<SubGroup> result, FixBS currGroup, int prev, int order) {
        ex: for (int gen = currGroup.nextClearBit(prev + 1); gen >= 0 && gen < order; gen = currGroup.nextClearBit(gen + 1)) {
            FixBS nextGroup = currGroup.copy();
            FixBS additional = new FixBS(order);
            additional.set(gen);
            do {
                if (additional.nextSetBit(0) < gen) {
                    continue ex;
                }
                nextGroup.or(additional);
            } while (!(additional = additional(nextGroup, additional, order)).isEmpty());
            result.add(new SubGroup(this, nextGroup));
            find(result, nextGroup, gen, order);
        }
    }

    private FixBS additional(FixBS currGroup, FixBS addition, int order) {
        FixBS result = new FixBS(order);
        for (int x = currGroup.nextSetBit(0); x >= 0; x = currGroup.nextSetBit(x + 1)) {
            for (int y = addition.nextSetBit(0); y >= 0; y = addition.nextSetBit(y + 1)) {
                result.set(op(x, y));
            }
        }
        result.andNot(currGroup);
        return result;
    }

    Group trivial = new Group() {
        @Override
        public int op(int a, int b) {
            return 0;
        }

        @Override
        public int inv(int a) {
            return 0;
        }

        @Override
        public int order() {
            return 1;
        }

        @Override
        public String name() {
            return "TR";
        }

        @Override
        public String elementName(int a) {
            return "0";
        }

        @Override
        public int[][] auth() {
            return new int[][]{{0}};
        }
    };
}
