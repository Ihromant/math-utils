package ua.ihromant.mathutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;
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
            //BitSet newVals = new BitSet(); // this is very rare case, use it when error only because it slows down calculation
            for (int i = 0; i < newArr.length; i++) {
                int newVal = newArr[i];
                if (newVal >= 0) {
                    oldArr[i] = newVal;
                    oldKeys.set(i);
//                    if (newVals.get(newVal)) {
//                        return null;
//                    }
//                    newVals.set(newVal);
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

    public static long autCount(Liner liner) {
        CountingConsumer cnt = new CountingConsumer();
        automorphisms(liner, cnt);
        return cnt.count();
    }

    public static int[][] autArray(Liner liner) {
        CollectingConsumer coll = new CollectingConsumer();
        automorphisms(liner, coll);
        return coll.array();
    }

    public static long autCount(Liner liner, int[] partialPoints, int[] partialLines) {
        CountingConsumer cnt = new CountingConsumer();
        automorphisms(liner, partialPoints, partialLines, cnt);
        return cnt.count();
    }

    public static int[][] autArray(Liner liner, int[] partialPoints, int[] partialLines) {
        CollectingConsumer coll = new CollectingConsumer();
        automorphisms(liner, partialPoints, partialLines, coll);
        return coll.array();
    }

    private static void automorphisms(Liner liner, Consumer<int[]> sink) {
        int[] partialPoints = new int[liner.pointCount()];
        int[] partialLines = new int[liner.lineCount()];
        Arrays.fill(partialPoints, -1);
        Arrays.fill(partialLines, -1);
        automorphisms(liner, partialPoints, partialLines, sink);
    }

    private static void automorphisms(Liner liner, int[] partialPoints, int[] partialLines, Consumer<int[]> sink) {
        BitSet fromBanned = new BitSet();
        BitSet toBanned = new BitSet();
        int len = liner.pointCount();
        for (int i = 0; i < partialLines.length; i++) {
            int ln = partialLines[i];
            if (ln >= 0) {
                fromBanned.or(liner.line(i));
                toBanned.or(liner.line(ln));
            }
        }
        if (fromBanned.cardinality() == len) {
            if (toBanned.cardinality() != len) {
                return;
            }
            fromBanned.clear();
            toBanned.clear();
        }
        for (int i = 0; i < partialPoints.length; i++) {
            int pt = partialPoints[i];
            if (pt >= 0) {
                fromBanned.set(i);
                toBanned.set(pt);
            }
        }
        int from = fromBanned.nextClearBit(0);
        br: for (int to = toBanned.nextClearBit(0); to < partialPoints.length; to = toBanned.nextClearBit(to + 1)) {
            int[] nextPartialPoints = partialPoints.clone();
            int[] nextPartialLines = partialLines.clone();
            nextPartialPoints[from] = to;
            BitSet pointsAssigned = new BitSet();
            pointsAssigned.set(from);
            while (!pointsAssigned.isEmpty()) {
                BitSet linesAssigned = new BitSet();
                for (int p1 = 0; p1 < nextPartialPoints.length; p1++) {
                    int p1To = nextPartialPoints[p1];
                    if (p1To < 0 || pointsAssigned.get(p1)) {
                        continue;
                    }
                    for (int p2 = pointsAssigned.nextSetBit(0); p2 >= 0; p2 = pointsAssigned.nextSetBit(p2 + 1)) {
                        int lineFrom = liner.line(p1, p2);
                        int lineTo = liner.line(p1To, nextPartialPoints[p2]);
                        int oldLine = nextPartialLines[lineFrom];
                        if (oldLine >= 0) {
                            if (oldLine != lineTo) {
                                continue br;
                            }
                            continue;
                        }
                        nextPartialLines[lineFrom] = lineTo;
                        linesAssigned.set(lineFrom);
                    }
                }
                pointsAssigned.clear();
                for (int l1 = 0; l1 < nextPartialLines.length; l1++) {
                    int lineTo = nextPartialLines[l1];
                    if (nextPartialLines[l1] < 0) {
                        continue;
                    }
                    for (int l2 = linesAssigned.nextSetBit(0); l2 >= 0; l2 = linesAssigned.nextSetBit(l2 + 1)) {
                        int intFrom = liner.intersection(l1, l2);
                        int intTo = liner.intersection(lineTo, nextPartialLines[l2]);
                        if (intFrom == -1 && intTo == -1) {
                            continue;
                        }
                        if (intFrom == -1 || intTo == -1) {
                            continue br;
                        }
                        int oldPoint = nextPartialPoints[intFrom];
                        if (oldPoint >= 0) {
                            if (oldPoint != intTo) {
                                continue br;
                            }
                            continue;
                        }
                        nextPartialPoints[intFrom] = intTo;
                        pointsAssigned.set(intFrom);
                    }
                }
            }
            if (Arrays.stream(nextPartialPoints).allMatch(p -> p >= 0)) {
                sink.accept(nextPartialPoints);
                continue;
            }
            automorphisms(liner, nextPartialPoints, nextPartialLines, sink);
        }
    }

    private static class CollectingConsumer implements Consumer<int[]> {
        private final List<int[]> list = new ArrayList<>();

        @Override
        public void accept(int[] ints) {
            list.add(ints);
        }

        public int[][] array() {
            return list.toArray(int[][]::new);
        }

        public Stream<int[]> stream() {
            return list.stream();
        }
    }

    private static class CountingConsumer implements Consumer<int[]> {
        private long count;

        @Override
        public void accept(int[] ints) {
            count++;
        }

        public long count() {
            return count;
        }
    }
}
