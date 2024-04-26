package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Stream;

public class Automorphisms {
    public static Stream<int[]> automorphisms(Liner liner) {
        int[] partial = new int[liner.pointCount()];
        Arrays.fill(partial, -1);
        return automorphisms(liner, partial);
    }

    public static Stream<int[]> automorphisms(Liner liner, int[] partial) {
        BitSet assigned = new BitSet();
        BitSet fromPossible = new BitSet();
        BitSet toPossible = new BitSet();
        fromPossible.set(0, partial.length);
        toPossible.set(0, partial.length);
        for (int i = 0; i < partial.length; i++) {
            if (partial[i] < 0) {
                continue;
            }
            assigned.set(i);
            fromPossible.set(i, false);
            toPossible.set(partial[i], false);
            for (int j = i + 1; j < partial.length; j++) {
                if (partial[j] < 0) {
                    continue;
                }
                liner.line(liner.line(i, j)).stream().forEach(p -> fromPossible.set(p, false));
                liner.line(liner.line(partial[i], partial[j])).stream().forEach(p -> toPossible.set(p, false));
            }
        }
        return automorphisms(liner, partial, assigned, fromPossible, toPossible);
    }

    public static Stream<int[]> automorphisms(Liner liner, int[] partial, BitSet assigned, BitSet fromPossible, BitSet toPossible) {
        int fromNotSet = fromPossible.nextSetBit(0);
        if (fromNotSet == -1) {
            if (toPossible.nextSetBit(0) >= 0) {
                return Stream.empty();
            }
            BitSet otherPossible = new BitSet();
            toPossible = new BitSet();
            otherPossible.set(0, partial.length);
            toPossible.set(0, partial.length);
            for (int i = assigned.nextSetBit(0); i >= 0; i = assigned.nextSetBit(i + 1)) {
                otherPossible.set(i, false);
                toPossible.set(partial[i], false);
            }
            fromNotSet = otherPossible.nextSetBit(0);
        }
        int fromNotSetF = fromNotSet;
        BitSet toPossibleF = toPossible;
        return toPossibleF.stream().boxed().mapMulti((toNotSet, sink) -> {
            BitSet newAssigned = (BitSet) assigned.clone();
            int[] newPartial = intersectionClosure(liner, partial, assigned, fromNotSetF, toNotSet);
            if (newPartial == null) {
                return;
            }
            if (Arrays.stream(newPartial).noneMatch(i -> i < 0)) {
                sink.accept(newPartial);
                return;
            }
            BitSet newFromPossible = (BitSet) fromPossible.clone();
            BitSet newToPossible = (BitSet) toPossibleF.clone();
            for (int i = assigned.nextSetBit(0); i >= 0; i = assigned.nextSetBit(i + 1)) {
                liner.line(liner.line(i, fromNotSetF)).stream().forEach(p -> newFromPossible.set(p, false));
                liner.line(liner.line(partial[i], toNotSet)).stream().forEach(p -> newToPossible.set(p, false));
            }
            newFromPossible.set(fromNotSetF, false);
            newToPossible.set(toNotSet, false);
            newAssigned.set(fromNotSetF);
            automorphisms(liner, newPartial, newAssigned, newFromPossible, newToPossible).forEach(sink);
        });
    }

    private static int[] intersectionClosure(Liner liner, int[] partial, BitSet assigned, int from, Integer to) {
        int[] oldArr = partial.clone();
        int[] newArr = oldArr.clone();
        BitSet oldKeys = (BitSet) assigned.clone();
        Arrays.fill(newArr, -1);
        newArr[from] = to;
        while (Arrays.stream(newArr).anyMatch(i -> i >= 0)) {
            for (int i = 0; i < newArr.length; i++) {
                if (newArr[i] >= 0) {
                    oldArr[i] = newArr[i];
                    oldKeys.set(i);
                }
            }
            newArr = new int[partial.length];
            Arrays.fill(newArr, -1);
            for (int a = oldKeys.nextSetBit(0); a >= 0; a = oldKeys.nextSetBit(a + 1)) {
                for (int b = oldKeys.nextSetBit(a + 1); b >= 0; b = oldKeys.nextSetBit(b + 1)) {
                    for (int c = oldKeys.nextSetBit(a + 1); c >= 0; c = oldKeys.nextSetBit(c + 1)) {
                        if (c == b) {
                            continue;
                        }
                        for (int d = oldKeys.nextSetBit(c + 1); d >= 0; d = oldKeys.nextSetBit(d + 1)) {
                            if (d == b) {
                                continue;
                            }
                            int fInt = liner.intersection(liner.line(a, b), liner.line(c, d));
                            int tInt = liner.intersection(liner.line(oldArr[a], oldArr[b]), liner.line(oldArr[c], oldArr[d]));
                            if (fInt == -1 && tInt == -1) {
                                continue;
                            }
                            if (fInt == -1 || tInt == -1) {
                                return null;
                            }
                            int oldVal = oldArr[fInt];
                            if (oldVal >= 0) {
                                if (oldVal != tInt) {
                                    return null;
                                }
                                continue;
                            }
                            int newVal = newArr[fInt];
                            if (newVal >= 0) {
                                if (newVal != tInt) {
                                    return null;
                                }
                                continue;
                            }
                            newArr[fInt] = tInt;
                        }
                    }
                }
            }
        }
        return oldArr;
    }
}
