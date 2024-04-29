package ua.ihromant.mathutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.stream.IntStream;
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
        BitSet oldPointsAssigned = new BitSet();
        BitSet oldLinesAssigned = new BitSet();
        BitSet pointValues = new BitSet();
        for (int i = 0; i < partialLines.length; i++) {
            int ln = partialLines[i];
            if (ln >= 0) {
                oldLinesAssigned.set(i);
            }
        }
        for (int i = 0; i < partialPoints.length; i++) {
            int pt = partialPoints[i];
            if (pt >= 0) {
                oldPointsAssigned.set(i);
                pointValues.set(pt);
            }
        }
        OptionalInt nextLine = IntStream.range(0, partialLines.length)
                .filter(l -> !oldLinesAssigned.get(l) && !liner.line(l).intersects(oldPointsAssigned)).findAny();
        int from = nextLine.orElseGet(() -> oldLinesAssigned.nextClearBit(0));
        BitSet toFilter = new BitSet();
        if (nextLine.isPresent()) {
            for (int i = 0; i < partialLines.length; i++) {
                if (liner.line(i).intersects(pointValues)) {
                    toFilter.set(i);
                }
            }
        } else {
            for (int i = oldLinesAssigned.nextSetBit(0); i >= 0; i = oldLinesAssigned.nextSetBit(i + 1)) {
                toFilter.set(partialLines[i]);
            }
        }
        br: for (int to = toFilter.nextClearBit(0); to < partialLines.length; to = toFilter.nextClearBit(to + 1)) {
            int[] nextPartialLines = partialLines.clone();
            int[] nextPartialPoints = partialPoints.clone();
            nextPartialLines[from] = to;
            BitSet linesAssigned = new BitSet();
            linesAssigned.set(from);
            while (!linesAssigned.isEmpty()) {
                BitSet pointsAssigned = new BitSet();
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
                linesAssigned.clear();
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
