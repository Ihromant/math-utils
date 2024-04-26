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
        BitSet fromBanned = new BitSet();
        BitSet toBanned = new BitSet();
        for (int i = 0; i < partial.length; i++) {
            if (partial[i] < 0) {
                continue;
            }
            assigned.set(i);
            fromBanned.set(i);
            toBanned.set(partial[i]);
            for (int j = i + 1; j < partial.length; j++) {
                if (partial[j] < 0) {
                    continue;
                }
                fromBanned.or(liner.line(liner.line(i, j)));
                toBanned.or(liner.line(liner.line(partial[i], partial[j])));
            }
        }
        return automorphisms(liner, partial, assigned, fromBanned, toBanned);
    }

    public static Stream<int[]> automorphisms(Liner liner, int[] partial, BitSet assigned, BitSet fromBanned, BitSet toBanned) {
        int fromNotSet = fromBanned.nextClearBit(0);
        if (fromNotSet == partial.length) {
            if (toBanned.nextClearBit(0) != partial.length) {
                return Stream.empty();
            }
            BitSet otherBanned = new BitSet();
            toBanned = new BitSet();
            for (int i = assigned.nextSetBit(0); i >= 0; i = assigned.nextSetBit(i + 1)) {
                otherBanned.set(i);
                toBanned.set(partial[i]);
            }
            fromNotSet = otherBanned.nextClearBit(0);
        }
        int fromNotSetF = fromNotSet;
        BitSet toBannedF = toBanned;
        BitSet allowed = (BitSet) toBannedF.clone();
        allowed.flip(0, partial.length);
        return allowed.stream().boxed().mapMulti((toNotSet, sink) -> {
            BitSet newAssigned = (BitSet) assigned.clone();
            int[] newPartial = intersectionClosure(liner, partial, assigned, fromNotSetF, toNotSet);
            if (newPartial == null) {
                return;
            }
            if (Arrays.stream(newPartial).noneMatch(i -> i < 0)) {
                sink.accept(newPartial);
                return;
            }
            BitSet newFromBanned = (BitSet) fromBanned.clone();
            BitSet newToBanned = (BitSet) toBannedF.clone();
            for (int i = assigned.nextSetBit(0); i >= 0; i = assigned.nextSetBit(i + 1)) {
                newFromBanned.or(liner.line(liner.line(i, fromNotSetF)));
                newToBanned.or(liner.line(liner.line(partial[i], toNotSet)));
            }
            newFromBanned.set(fromNotSetF);
            newToBanned.set(toNotSet);
            newAssigned.set(fromNotSetF);
            automorphisms(liner, newPartial, newAssigned, newFromBanned, newToBanned).forEach(sink);
        });
    }

    private static int[] intersectionClosure(Liner liner, int[] partial, BitSet assigned, int from, Integer to) {
        int[] oldArr = partial.clone();
        BitSet oldKeys = (BitSet) assigned.clone();
        int[] newArr = new int[oldArr.length];
        Arrays.fill(newArr, -1);
        newArr[from] = to;
        while (Arrays.stream(newArr).anyMatch(i -> i >= 0)) {
            for (int i = 0; i < newArr.length; i++) {
                if (newArr[i] >= 0) {
                    oldArr[i] = newArr[i];
                    oldKeys.set(i);
                }
            }
            if (Arrays.stream(oldArr).allMatch(i -> i >= 0)) {
                for (int line : liner.lines()) {
                    if (!liner.collinear(liner.line(line).stream().map(f -> oldArr[f]).toArray())) {
                        return null;
                    }
                }
                return oldArr;
            }
            newArr = new int[partial.length];
            Arrays.fill(newArr, -1);
            for (int a = oldKeys.nextSetBit(0); a >= 0; a = oldKeys.nextSetBit(a + 1)) {
                for (int b = oldKeys.nextSetBit(a + 1); b >= 0; b = oldKeys.nextSetBit(b + 1)) {
                    for (int c = oldKeys.nextSetBit(b + 1); c >= 0; c = oldKeys.nextSetBit(c + 1)) {
                        for (int d = oldKeys.nextSetBit(c + 1); d >= 0; d = oldKeys.nextSetBit(d + 1)) {
                            if (failed(liner, a, b, c, d, oldArr, newArr) || failed(liner, a, c, b, d, oldArr, newArr)
                                    || failed(liner, a, d, b, c, oldArr, newArr)) {
                                return null;
                            }
                        }
                    }
                }
            }
        }
        return oldArr;
    }

    private static boolean failed(Liner liner, int a, int b, int c, int d, int[] oldArr, int[] newArr) {
        int fInt = liner.intersection(liner.line(a, b), liner.line(c, d));
        int tInt = liner.intersection(liner.line(oldArr[a], oldArr[b]), liner.line(oldArr[c], oldArr[d]));
        if (fInt == -1 && tInt == -1) {
            return false;
        }
        if (fInt == -1 || tInt == -1) {
            return true;
        }
        int oldVal = oldArr[fInt];
        if (oldVal >= 0) {
            return oldVal != tInt;
        }
        int newVal = newArr[fInt];
        if (newVal >= 0) {
            return newVal != tInt;
        }
        newArr[fInt] = tInt;
        return false;
    }
}
