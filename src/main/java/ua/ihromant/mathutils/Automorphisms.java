package ua.ihromant.mathutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Automorphisms {
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
        for (int i = 0; i < partialLines.length; i++) {
            int ln = partialLines[i];
            if (ln >= 0) {
                fromBanned.or(liner.line(i));
                toBanned.or(liner.line(ln));
            }
        }
        fromBanned.flip(0, partialPoints.length);
        if (fromBanned.isEmpty()) {
            toBanned.flip(0, partialPoints.length);
            if (!toBanned.isEmpty()) {
                return;
            }
        } else {
            fromBanned.flip(0, partialPoints.length);
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
