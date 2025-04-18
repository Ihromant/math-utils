package ua.ihromant.mathutils.auto;

import ua.ihromant.mathutils.Liner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;

public class Automorphisms {
    public static long autCount(Liner liner) {
        CountingConsumer cont = new CountingConsumer();
        liner.automorphisms(cont);
        return cont.count();
    }

    public static int[][] autArray(Liner liner) {
        CollectingConsumer coll = new CollectingConsumer();
        liner.automorphisms(coll);
        return coll.array();
    }

    public static long autCount(Liner liner, int[] partialPoints, int[] partialLines) {
        CountingConsumer cnt = new CountingConsumer();
        BitSet pointsAssigned = new BitSet();
        BitSet linesAssigned = new BitSet();
        for (int i = 0; i < partialLines.length; i++) {
            int ln = partialLines[i];
            if (ln >= 0) {
                linesAssigned.set(i);
            }
        }
        for (int i = 0; i < partialPoints.length; i++) {
            int pt = partialPoints[i];
            if (pt >= 0) {
                pointsAssigned.set(i);
            }
        }
        automorphisms(liner, partialPoints, pointsAssigned, partialLines, linesAssigned, cnt);
        return cnt.count();
    }

    public static int[][] autArray(Liner liner, int[] partialPoints, int[] partialLines) {
        CollectingConsumer coll = new CollectingConsumer();
        BitSet pointsAssigned = new BitSet();
        BitSet linesAssigned = new BitSet();
        for (int i = 0; i < partialLines.length; i++) {
            int ln = partialLines[i];
            if (ln >= 0) {
                linesAssigned.set(i);
            }
        }
        for (int i = 0; i < partialPoints.length; i++) {
            int pt = partialPoints[i];
            if (pt >= 0) {
                pointsAssigned.set(i);
            }
        }
        automorphisms(liner, partialPoints, pointsAssigned, partialLines, linesAssigned, coll);
        return coll.array();
    }

    private static void automorphisms(Liner liner, int[] partialPoints, BitSet oldPointsAssigned, int[] partialLines, BitSet oldLinesAssigned, Consumer<int[]> sink) {
        int from = -1;
        boolean foundNotCrossing = false;
        ex: for (int l = 0; l < partialLines.length; l++) {
            if (oldLinesAssigned.get(l)) {
                continue;
            }
            for (int p : liner.line(l)) {
                if (oldPointsAssigned.get(p)) {
                    continue ex;
                }
            }
            foundNotCrossing = true;
            from = l;
            break;
        }
        if (!foundNotCrossing) {
            from = oldLinesAssigned.nextClearBit(0);
        }
        BitSet toFilter = new BitSet();
        if (foundNotCrossing) {
            for (int p : partialPoints) {
                if (p < 0) {
                    continue;
                }
                for (int l : liner.point(p)) {
                    toFilter.set(l);
                }
            }
        } else {
            for (int l : partialLines) {
                if (l < 0) {
                    continue;
                }
                toFilter.set(l);
            }
        }
        br: for (int to = toFilter.nextClearBit(0); to < partialLines.length; to = toFilter.nextClearBit(to + 1)) {
            int[] nextPartialLines = partialLines.clone();
            int[] nextPartialPoints = partialPoints.clone();
            BitSet nextPointsAssigned = (BitSet) oldPointsAssigned.clone();
            BitSet nextLinesAssigned = (BitSet) oldLinesAssigned.clone();
            nextPartialLines[from] = to;
            BitSet linesAssigned = new BitSet();
            linesAssigned.set(from);
            while (!linesAssigned.isEmpty()) {
                nextLinesAssigned.or(linesAssigned);
                BitSet pointsAssigned = new BitSet();
                for (int l1 = nextLinesAssigned.nextSetBit(0); l1 >= 0; l1 = nextLinesAssigned.nextSetBit(l1 + 1)) {
                    int l1To = nextPartialLines[l1];
                    for (int l2 = linesAssigned.nextSetBit(0); l2 >= 0; l2 = linesAssigned.nextSetBit(l2 + 1)) {
                        int intFrom = liner.intersection(l1, l2);
                        int intTo = liner.intersection(l1To, nextPartialLines[l2]);
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
                nextPointsAssigned.or(pointsAssigned);
                for (int p1 = nextPointsAssigned.nextSetBit(0); p1 >= 0; p1 = nextPointsAssigned.nextSetBit(p1 + 1)) {
                    if (pointsAssigned.get(p1)) {
                        continue;
                    }
                    int p1To = nextPartialPoints[p1];
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
            if (nextPointsAssigned.nextClearBit(0) == liner.pointCount()) {
                sink.accept(nextPartialPoints);
                continue;
            }
            automorphisms(liner, nextPartialPoints, nextPointsAssigned, nextPartialLines, nextLinesAssigned, sink);
        }
    }

    public static long autCountOld(Liner liner) {
        int[] partialPoints = new int[liner.pointCount()];
        int[] partialLines = new int[liner.lineCount()];
        Arrays.fill(partialPoints, -1);
        Arrays.fill(partialLines, -1);
        CountingConsumer cnt = new CountingConsumer();
        automorphismsOld(liner, partialPoints, new BitSet(), partialLines, new BitSet(), cnt);
        return cnt.count();
    }

    public static int[][] autArrayOld(Liner liner) {
        int[] partialPoints = new int[liner.pointCount()];
        int[] partialLines = new int[liner.lineCount()];
        Arrays.fill(partialPoints, -1);
        Arrays.fill(partialLines, -1);
        CollectingConsumer coll = new CollectingConsumer();
        automorphismsOld(liner, partialPoints, new BitSet(), partialLines, new BitSet(), coll);
        return coll.array();
    }

    public static long autCountOld(Liner liner, int[] partialPoints, int[] partialLines) {
        CountingConsumer cnt = new CountingConsumer();
        BitSet pointsAssigned = new BitSet();
        BitSet linesAssigned = new BitSet();
        for (int i = 0; i < partialLines.length; i++) {
            int ln = partialLines[i];
            if (ln >= 0) {
                linesAssigned.set(i);
            }
        }
        for (int i = 0; i < partialPoints.length; i++) {
            int pt = partialPoints[i];
            if (pt >= 0) {
                pointsAssigned.set(i);
            }
        }
        automorphismsOld(liner, partialPoints, pointsAssigned, partialLines, linesAssigned, cnt);
        return cnt.count();
    }

    public static int[][] autArrayOld(Liner liner, int[] partialPoints, int[] partialLines) {
        CollectingConsumer coll = new CollectingConsumer();
        BitSet pointsAssigned = new BitSet();
        BitSet linesAssigned = new BitSet();
        for (int i = 0; i < partialLines.length; i++) {
            int ln = partialLines[i];
            if (ln >= 0) {
                linesAssigned.set(i);
            }
        }
        for (int i = 0; i < partialPoints.length; i++) {
            int pt = partialPoints[i];
            if (pt >= 0) {
                pointsAssigned.set(i);
            }
        }
        automorphismsOld(liner, partialPoints, pointsAssigned, partialLines, linesAssigned, coll);
        return coll.array();
    }

    private static void automorphismsOld(Liner liner, int[] partialPoints, BitSet pointsAssignedOld, int[] partialLines, BitSet linesAssignedOld, Consumer<int[]> sink) {
        BitSet fromBanned = new BitSet();
        BitSet toBanned = new BitSet();
        for (int i = linesAssignedOld.nextSetBit(0); i >= 0; i = linesAssignedOld.nextSetBit(i + 1)) {
            Arrays.stream(liner.line(i)).forEach(fromBanned::set);
            Arrays.stream(liner.line(partialLines[i])).forEach(toBanned::set);
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
        for (int i = pointsAssignedOld.nextSetBit(0); i >= 0; i = pointsAssignedOld.nextSetBit(i+1)) {
            fromBanned.set(i);
            toBanned.set(partialPoints[i]);
        }
        int from = fromBanned.nextClearBit(0);
        br: for (int to = toBanned.nextClearBit(0); to < partialPoints.length; to = toBanned.nextClearBit(to + 1)) {
            int[] nextPartialPoints = partialPoints.clone();
            int[] nextPartialLines = partialLines.clone();
            BitSet nextPointsAssigned = (BitSet) pointsAssignedOld.clone();
            BitSet nextLinesAssigned = (BitSet) linesAssignedOld.clone();
            nextPartialPoints[from] = to;
            BitSet pointsAssigned = new BitSet();
            pointsAssigned.set(from);
            while (!pointsAssigned.isEmpty()) {
                nextPointsAssigned.or(pointsAssigned);
                BitSet linesAssigned = new BitSet();
                for (int p1 = nextPointsAssigned.nextSetBit(0); p1 >= 0; p1 = nextPointsAssigned.nextSetBit(p1 + 1)) {
                    if (pointsAssigned.get(p1)) {
                        continue;
                    }
                    int p1To = nextPartialPoints[p1];
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
                nextLinesAssigned.or(linesAssigned);
                for (int l1 = nextLinesAssigned.nextSetBit(0); l1 >= 0; l1 = nextLinesAssigned.nextSetBit(l1 + 1)) {
                    int l1To = nextPartialLines[l1];
                    for (int l2 = linesAssigned.nextSetBit(0); l2 >= 0; l2 = linesAssigned.nextSetBit(l2 + 1)) {
                        int intFrom = liner.intersection(l1, l2);
                        int intTo = liner.intersection(l1To, nextPartialLines[l2]);
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
            if (nextPointsAssigned.nextClearBit(0) == liner.pointCount()) {
                sink.accept(nextPartialPoints);
                continue;
            }
            automorphismsOld(liner, nextPartialPoints, nextPointsAssigned, nextPartialLines, nextLinesAssigned, sink);
        }
    }

    public static int[] isomorphism(Liner first, Liner second) {
//        if (first.pointCount() != second.pointCount() || first.lineCount() != second.lineCount()
//            || !first.beamFrequencies().equals(second.beamFrequencies())) {
//            return null;
//        }
        int[] partialPoints = new int[first.pointCount()];
        int[] partialLines = new int[first.lineCount()];
        Arrays.fill(partialPoints, -1);
        Arrays.fill(partialLines, -1);
        return isomorphism(first, second, partialPoints, new BitSet(), partialLines, new BitSet());
    }

    private static int[] isomorphism(Liner first, Liner second, int[] partialPoints, BitSet oldPointsAssigned, int[] partialLines, BitSet oldLinesAssigned) {
        int from = -1;
        boolean foundNotCrossing = false;
        ex: for (int l = 0; l < partialLines.length; l++) {
            if (oldLinesAssigned.get(l)) {
                continue;
            }
            for (int p : first.line(l)) {
                if (oldPointsAssigned.get(p)) {
                    continue ex;
                }
            }
            foundNotCrossing = true;
            from = l;
            break;
        }
        if (!foundNotCrossing) {
            from = oldLinesAssigned.nextClearBit(0);
        }
        BitSet toFilter = new BitSet();
        if (foundNotCrossing) {
            for (int p : partialPoints) {
                if (p < 0) {
                    continue;
                }
                for (int l : second.point(p)) {
                    toFilter.set(l);
                }
            }
        } else {
            for (int l : partialLines) {
                if (l < 0) {
                    continue;
                }
                toFilter.set(l);
            }
        }
        br: for (int to = toFilter.nextClearBit(0); to < partialLines.length; to = toFilter.nextClearBit(to + 1)) {
            int[] nextPartialLines = partialLines.clone();
            int[] nextPartialPoints = partialPoints.clone();
            BitSet nextPointsAssigned = (BitSet) oldPointsAssigned.clone();
            BitSet nextLinesAssigned = (BitSet) oldLinesAssigned.clone();
            nextPartialLines[from] = to;
            BitSet linesAssigned = new BitSet();
            linesAssigned.set(from);
            while (!linesAssigned.isEmpty()) {
                nextLinesAssigned.or(linesAssigned);
                BitSet pointsAssigned = new BitSet();
                for (int l1 = nextLinesAssigned.nextSetBit(0); l1 >= 0; l1 = nextLinesAssigned.nextSetBit(l1 + 1)) {
                    int l1To = nextPartialLines[l1];
                    for (int l2 = linesAssigned.nextSetBit(0); l2 >= 0; l2 = linesAssigned.nextSetBit(l2 + 1)) {
                        int intFrom = first.intersection(l1, l2);
                        int intTo = second.intersection(l1To, nextPartialLines[l2]);
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
                nextPointsAssigned.or(pointsAssigned);
                for (int p1 = nextPointsAssigned.nextSetBit(0); p1 >= 0; p1 = nextPointsAssigned.nextSetBit(p1 + 1)) {
                    if (pointsAssigned.get(p1)) {
                        continue;
                    }
                    int p1To = nextPartialPoints[p1];
                    for (int p2 = pointsAssigned.nextSetBit(0); p2 >= 0; p2 = pointsAssigned.nextSetBit(p2 + 1)) {
                        int lineFrom = first.line(p1, p2);
                        int lineTo = second.line(p1To, nextPartialPoints[p2]);
                        if (lineFrom == -1 && lineTo == -1) {
                            continue;
                        }
                        if (lineFrom == -1 || lineTo == -1) {
                            continue br;
                        }
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
            if (nextLinesAssigned.nextClearBit(0) == first.lineCount()) {
                return nextPartialPoints;
            }
            int[] candidate = isomorphism(first, second, nextPartialPoints, nextPointsAssigned, nextPartialLines, nextLinesAssigned);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean enhanceFailed(Liner first, Liner second, BitSet newStepPoints, int[] newPointsMap, BitSet newPoints, int[] newLinesMap, BitSet newLines) {
        while (!newStepPoints.isEmpty()) {
            newPoints.or(newStepPoints);
            BitSet linesAssigned = new BitSet();
            for (int p1 = newPoints.nextSetBit(0); p1 >= 0; p1 = newPoints.nextSetBit(p1 + 1)) {
                if (newStepPoints.get(p1)) {
                    continue;
                }
                int p1To = newPointsMap[p1];
                for (int p2 = newStepPoints.nextSetBit(0); p2 >= 0; p2 = newStepPoints.nextSetBit(p2 + 1)) {
                    int lineFrom = first.line(p1, p2);
                    int lineTo = second.line(p1To, newPointsMap[p2]);
                    if (lineFrom < 0) {
                        if (lineTo < 0) {
                            continue;
                        } else {
                            return true;
                        }
                    } else {
                        if (lineTo < 0) {
                            return true;
                        }
                    }
                    int oldLine = newLinesMap[lineFrom];
                    if (oldLine >= 0) {
                        if (oldLine != lineTo) {
                            return true;
                        }
                        continue;
                    }
                    newLinesMap[lineFrom] = lineTo;
                    linesAssigned.set(lineFrom);
                }
            }
            newStepPoints.clear();
            newLines.or(linesAssigned);
            for (int l1 = newLines.nextSetBit(0); l1 >= 0; l1 = newLines.nextSetBit(l1 + 1)) {
                int l1To = newLinesMap[l1];
                for (int l2 = linesAssigned.nextSetBit(0); l2 >= 0; l2 = linesAssigned.nextSetBit(l2 + 1)) {
                    int ptFrom = first.intersection(l1, l2);
                    int ptTo = second.intersection(l1To, newLinesMap[l2]);
                    if (ptFrom < 0) {
                        if (ptTo < 0) {
                            continue;
                        } else {
                            return true;
                        }
                    } else {
                        if (ptTo < 0) {
                            return true;
                        }
                    }
                    int oldPoint = newPointsMap[ptFrom];
                    if (oldPoint >= 0) {
                        if (oldPoint != ptTo) {
                            return true;
                        }
                        continue;
                    }
                    newPointsMap[ptFrom] = ptTo;
                    newStepPoints.set(ptFrom);
                }
            }
        }
        return false;
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
