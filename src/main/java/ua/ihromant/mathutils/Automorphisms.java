package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Automorphisms {
    public static Stream<int[]> automorphisms(Liner liner) {
        int[] base = new int[liner.pointCount()];
        Arrays.fill(base, -1);
        return automorphisms(liner, base);
    }

    private static Stream<int[]> automorphisms(Liner liner, int[] base) {
        BitSet fromLines = new BitSet();
        BitSet toLines = new BitSet();
        for (int i = 0; i < base.length; i++) {
            int toi = base[i];
            if (toi == -1) {
                continue;
            }
            fromLines.set(i);
            toLines.set(toi);
            for (int j = i + 1; j < base.length; j++) {
                int toj = base[j];
                if (toj == -1) {
                    continue;
                }
                fromLines.or(liner.line(liner.line(i, j)));
                toLines.or(liner.line(liner.line(toi, toj)));
            }
        }
        int fromNotSet = fromLines.nextClearBit(0);
        if (fromNotSet == liner.pointCount()) {
            if (toLines.nextSetBit(0) != liner.pointCount()) {
                return Stream.empty();
            }
            fromLines = IntStream.range(0, base.length).filter(i -> base[i] >= 0).collect(BitSet::new, BitSet::set, BitSet::or);
            toLines = IntStream.of(base).filter(i -> i >= 0).collect(BitSet::new, BitSet::set, BitSet::or);
            fromNotSet = fromLines.nextSetBit(0);
        }
        int fromNotSetF = fromNotSet;
        BitSet toLinesF = toLines;
        return IntStream.range(0, liner.pointCount()).filter(i -> !toLinesF.get(i)).boxed().mapMulti((toNotSet, sink) -> {
            int[] newArr = intersectionClosure(liner, base, fromNotSetF, toNotSet);
            if (newArr == null) {
                return;
            }
            if (Arrays.stream(newArr).noneMatch(i -> i < 0)) {
                sink.accept(newArr);
                return;
            }
            automorphisms(liner, newArr).forEach(sink);
        });
    }

    private static int[] intersectionClosure(Liner liner, int[] base, int from, Integer to) {
        Map<Integer, Integer> old = new HashMap<>();
        for (int i = 0; i < base.length; i++) {
            if (base[i] == -1) {
                continue;
            }
            old.put(i, base[i]);
        }
        Map<Integer, Integer> next = new HashMap<>();
        next.put(from, to);
        while (!next.isEmpty()) {
            old.putAll(next);
            next = new HashMap<>();
            for (Map.Entry<Integer, Integer> a : old.entrySet()) {
                int af = a.getKey();
                int at = a.getValue();
                for (Map.Entry<Integer, Integer> b : old.entrySet()) {
                    int bf = b.getKey();
                    int bt = b.getValue();
                    if (af >= bf) {
                        continue;
                    }
                    for (Map.Entry<Integer, Integer> c : old.entrySet()) {
                        int cf = c.getKey();
                        int ct = c.getValue();
                        if (cf == af || cf == bf) {
                            continue;
                        }
                        for (Map.Entry<Integer, Integer> d : old.entrySet()) {
                            int df = d.getKey();
                            int dt = d.getValue();
                            if (df == af || df == bf || cf >= df) {
                                continue;
                            }
                            int fInt = liner.intersection(liner.line(af, bf), liner.line(cf, df));
                            int tInt = liner.intersection(liner.line(at, bt), liner.line(ct, dt));
                            if (fInt == -1 && tInt == -1) {
                                continue;
                            }
                            if (fInt == -1 || tInt == -1) {
                                return null;
                            }
                            Integer oldVal = old.get(fInt);
                            if (oldVal != null) {
                                if (oldVal != tInt) {
                                    return null;
                                }
                                continue;
                            }
                            Integer newVal = next.get(fInt);
                            if (newVal != null) {
                                if (newVal != tInt) {
                                    continue;
                                }
                            }
                            next.put(fInt, tInt);
                        }
                    }
                }
            }
        }
        return IntStream.range(0, base.length).map(i -> old.getOrDefault(i, -1)).toArray();
    }
}
