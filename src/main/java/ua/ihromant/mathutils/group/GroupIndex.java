package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.QuickFind;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GroupIndex {
    private static final Map<Integer, Map<Fingerprint, Group>> index = new HashMap<>();

    static {
        register(new CyclicGroup(2)); // 2
        register(new CyclicGroup(3)); // 3
        register(new CyclicGroup(4)); // 4
        register(new CyclicProduct(2, 2)); // 4
        register(new CyclicGroup(5)); // 5
        register(new CyclicGroup(6)); // 6
        register(new SemiDirectProduct(new CyclicGroup(3), new CyclicGroup(2))); // 6
        register(new CyclicGroup(7)); // 7
        register(new CyclicGroup(8)); // 8
        register(new CyclicProduct(2, 4)); // 8
        register(new CyclicProduct(2, 2, 2)); // 8
        register(new SemiDirectProduct(new CyclicProduct(4), new CyclicGroup(2))); // 8
        register(new QuaternionGroup()); // 8
        register(new CyclicGroup(9)); // 9
        register(new CyclicProduct(3, 3)); // 9
        register(new CyclicGroup(10)); // 10
        register(new SemiDirectProduct(new CyclicGroup(5), new CyclicGroup(2))); // 10
        register(new CyclicGroup(11)); // 11
        register(new CyclicGroup(12)); // 12
        register(new CyclicProduct(2, 2, 3)); // 12
        register(new SemiDirectProduct(new CyclicGroup(3), new CyclicGroup(4))); // 12
        register(new SemiDirectProduct(new CyclicGroup(6), new CyclicGroup(2))); // 12
        register(new PermutationGroup(4, true)); // 12
        register(new CyclicGroup(13)); // 13
        register(new CyclicGroup(14)); // 14
        register(new SemiDirectProduct(new CyclicGroup(7), new CyclicGroup(2))); // 14
        register(new CyclicGroup(15)); // 15
        register(new CyclicGroup(16)); // 16 TODO add all (14 together)
        register(new CyclicProduct(2, 8)); // 16
        register(new CyclicProduct(4, 4)); // 16
        register(new CyclicProduct(2, 2, 4)); // 16
        register(new CyclicProduct(2, 2, 2, 2)); // 16
        register(new SemiDirectProduct(new CyclicGroup(4), new CyclicGroup(4))); // 16
        register(new SemiDirectProduct(new CyclicGroup(8), new CyclicGroup(2))); // 16
        register(new CyclicGroup(17)); // 17
        register(new CyclicGroup(18)); // 18
        register(new CyclicProduct(2, 3, 3)); // 18
        register(new GroupProduct(new SemiDirectProduct(new CyclicGroup(3), new CyclicGroup(2)), new CyclicGroup(3))); // 18
        register(new SemiDirectProduct(new CyclicGroup(9), new CyclicGroup(2))); // 18
        register(new SemiDirectProduct(new CyclicProduct(3, 3), new CyclicGroup(2), 6, false)); // 18
        register(new CyclicGroup(19)); // 19
        register(new CyclicGroup(20)); // 20
        register(new CyclicProduct(2, 2, 5)); // 20
        register(new SemiDirectProduct(new CyclicGroup(5), new CyclicGroup(4))); // 20
        register(new SemiDirectProduct(new CyclicGroup(5), new CyclicGroup(4), 2)); // 20
        register(new SemiDirectProduct(new CyclicGroup(10), new CyclicGroup(2))); // 20
        register(new CyclicGroup(21)); // 21
        register(new SemiDirectProduct(new CyclicGroup(7), new CyclicGroup(3))); // 21
        register(new CyclicGroup(22)); // 22
        register(new SemiDirectProduct(new CyclicGroup(11), new CyclicGroup(2))); // 22
        register(new CyclicGroup(23)); // 23
        register(new CyclicGroup(24)); // 24 TODO add all (15 together)
        register(new CyclicProduct(2, 4, 3)); // 24
        register(new CyclicProduct(2, 2, 2, 3)); // 24
        register(new SemiDirectProduct(new CyclicGroup(3), new CyclicGroup(8))); // 24
        register(new PermutationGroup(4, false)); // 24
        register(new CyclicGroup(25)); // 25
        register(new CyclicProduct(5, 5)); // 25
        register(new CyclicGroup(26)); // 26
        register(new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(2))); // 26
        register(new CyclicGroup(27)); // 27
        register(new CyclicProduct(3, 9)); // 27
        register(new CyclicProduct(3, 3, 3)); // 27
        register(new SemiDirectProduct(new CyclicGroup(9), new CyclicGroup(3))); // 27
        register(new SemiDirectProduct(new CyclicProduct(3, 3), new CyclicGroup(3))); // 27
        register(new CyclicGroup(28)); // 28
        register(new CyclicProduct(2, 2, 7)); // 28
        register(new SemiDirectProduct(new CyclicGroup(14), new CyclicGroup(2))); // 28
        register(new SemiDirectProduct(new CyclicGroup(7), new CyclicGroup(4))); // 28
        register(new CyclicGroup(29)); // 29
        register(new CyclicGroup(30)); // 30
        register(new SemiDirectProduct(new CyclicGroup(15), new CyclicGroup(2), 0, false), new GroupProduct(new SemiDirectProduct(new CyclicGroup(5), new CyclicGroup(2)), new CyclicGroup(3))); // 30
        register(new GroupProduct(new SemiDirectProduct(new CyclicGroup(3), new CyclicGroup(2)), new CyclicGroup(5)), new GroupProduct(new SemiDirectProduct(new CyclicGroup(3), new CyclicGroup(2)), new CyclicGroup(5))); // 30
        register(new SemiDirectProduct(new CyclicGroup(15), new CyclicGroup(2), 2, false), new DihedralGroup(15)); // 30
        register(new CyclicGroup(31)); // 31
    }

    public static Group group(int order, int index) throws IOException {
        System.out.println("Reading SmallGroup(" + order + "," + index + ")");
        return new GapInteractor().smallGroup(order, index);
    }

    public static String identify(Group g) {
        int ord = g.order();
        if (ord == 1) {
            return "Trivial";
        }
        Fingerprint fp = fingerprint(g);
        Group gr = index.getOrDefault(ord, Map.of()).get(fp);
        if (gr != null) {
            return gr.name();
        } else {
            return "Unknown " + ord + " " + fp;
        }
    }

    private static void register(Group g) {
        Fingerprint fp = fingerprint(g);
        Group prev = index.computeIfAbsent(g.order(), k -> new HashMap<>()).put(fp, g);
        if (prev != null) {
            throw new IllegalStateException("Duplicate groups " + prev.name() + " and " + g.name());
        }
    }

    private static void register(Group g, Group alias) {
        Fingerprint fp = fingerprint(g);
        Group prev = index.computeIfAbsent(g.order(), k -> new HashMap<>()).put(fp, alias);
        if (prev != null) {
            throw new IllegalStateException("Duplicate groups " + prev.name() + " and " + alias.name());
        }
    }

    private static Fingerprint fingerprint(Group gr) {
        int order = gr.order();
        Map<Integer, Integer> orders = new HashMap<>();
        QuickFind qf = new QuickFind(order);
        for (int g = 0; g < order; g++) {
            orders.compute(gr.order(g), (k, v) -> v == null ? 1 : v + 1);
            for (int h = 0; h < order; h++) {
                qf.union(g, gr.op(gr.inv(h), gr.op(g, h)));
            }
        }
        Map<Integer, Integer> conjugations = new HashMap<>();
        for (int i = 0; i < order; i++) {
            if (qf.root(i) == i) {
                conjugations.compute(qf.size(i), (k, v) -> v == null ? 1 : v + 1);
            }
        }
        return new GroupIndex.Fingerprint(orders, conjugations);
    }

    private record Fingerprint(Map<Integer, Integer> orders, Map<Integer, Integer> conjugations) {
        @Override
        public String toString() {
            return "FP(" + orders + ", " + conjugations + ")";
        }
    }
}
