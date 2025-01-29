package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.QuickFind;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    // TODO this is not true for even orders
    default int squareRoot(int from) {
        return mul(from, (order() + 1) / 2);
    }

    default int[] squareRoots(int from) {
        return IntStream.range(0, order()).filter(i -> op(i, i) == from).toArray();
    }

    default IntStream elements() {
        return IntStream.range(0, order());
    }

    default Group asTable() {
        return new TableGroup(
                elements().mapToObj(i -> elements().map(j -> op(i, j)).toArray()).toArray(int[][]::new),
                elements().map(this::inv).toArray(),
                elements().mapToObj(this::squareRoots).toArray(int[][]::new));
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
        Set<FixBS> found = new HashSet<>();
        int order = order();
        FixBS all = new FixBS(order);
        all.set(0, order);
        found.add(all);
        FixBS init = new FixBS(order);
        init.set(0);
        result.add(new SubGroup(this, init));
        find(result, found, init, order);
        result.add(new SubGroup(this, all));
        return result;
    }

    private void find(List<SubGroup> result, Set<FixBS> found, FixBS currGroup, int order) {
        for (int gen = currGroup.nextClearBit(0); gen >= 0 && gen < order; gen = currGroup.nextClearBit(gen + 1)) {
            FixBS nextGroup = currGroup.copy();
            nextGroup.set(gen);
            FixBS additional = new FixBS(order);
            additional.set(gen);
            do {
                nextGroup.or(additional);
            } while (!(additional = additional(nextGroup, additional, order)).isEmpty());
            if (!found.add(nextGroup)) {
                continue;
            }
            result.add(new SubGroup(this, nextGroup));
            find(result, found, nextGroup, order);
        }
    }

    private FixBS additional(FixBS first, FixBS second, int order) {
        FixBS result = new FixBS(order);
        for (int x = first.nextSetBit(0); x >= 0; x = first.nextSetBit(x + 1)) {
            for (int y = second.nextSetBit(0); y >= 0; y = second.nextSetBit(y + 1)) {
                result.set(op(x, y));
            }
        }
        FixBS removal = new FixBS(order);
        removal.or(first);
        removal.or(second);
        result.xor(removal);
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
