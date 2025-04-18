package ua.ihromant.mathutils;

import ua.ihromant.mathutils.nauty.AutomorphismConsumerNew;
import ua.ihromant.mathutils.nauty.CanonicalConsumer;
import ua.ihromant.mathutils.nauty.GraphWrapper;
import ua.ihromant.mathutils.nauty.NautyAlgo;
import ua.ihromant.mathutils.nauty.NautyAlgoNew;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class PartialLiner {
    private final int pointCount;
    private final int ll;
    private final int[][] lines;
    private final boolean[][] flags;
    private final int[] beamCounts; // number of lines in beam
    private final int[] beamLengths; // distribution by beam count
    private final int[][] lookup;
    private final int[][] beams;
    private final int[][] beamDist; // points corresponding to specific beam count
    private final int[][] intersections;
    private final int[] lineInter; // number of other lines intersections
    private final int[] lineFreq; // distribution by line intersections count
    private int[] pointOrder;
    private FixBS canonical;

    public PartialLiner(int pointCount, int ll) {
        this.pointCount = pointCount;
        this.ll = ll;
        this.lines = new int[0][];
        this.flags = new boolean[lines.length][pointCount];
        this.beamCounts = new int[pointCount];
        int bmc = (pointCount - 1) / (ll - 1);
        int[] beamLengths = new int[bmc + 1];
        for (int beamCount : beamCounts) {
            beamLengths[beamCount]++;
        }
        this.beamLengths = beamLengths.clone();
        this.beams = new int[pointCount][];
        this.beamDist = new int[beamLengths.length][0];
        for (int pt = 0; pt < pointCount; pt++) {
            int bc = beamCounts[pt];
            if (beamDist[bc].length == 0) {
                beamDist[bc] = new int[beamLengths[bc]];
            }
            beamDist[bc][beamDist[bc].length - beamLengths[bc]--] = pt;
            beams[pt] = new int[bc];
        }
        this.lookup = generateLookup();
        this.intersections = new int[this.lines.length][this.lines.length];
        this.lineInter = new int[this.lines.length];
        this.lineFreq = new int[Math.min(lines.length, bmc * ll) + 1];
    }

    public PartialLiner(int[][] lines) {
        this(Arrays.stream(lines).mapToInt(arr -> arr[arr.length - 1]).max().orElseThrow() + 1, lines);
    }

    public PartialLiner(int pointCount, int[][] lines) {
        this.pointCount = pointCount;
        this.lines = lines;
        this.flags = new boolean[lines.length][pointCount];
        this.beamCounts = new int[pointCount];
        this.ll = lines[0].length;
        for (int i = 0; i < lines.length; i++) {
            int[] line = lines[i];
            for (int pt : line) {
                flags[i][pt] = true;
                this.beamCounts[pt]++;
            }
        }
        int bmc = (pointCount - 1) / (ll - 1);
        int[] beamLengths = new int[bmc + 1];
        for (int beamCount : beamCounts) {
            beamLengths[beamCount]++;
        }
        this.beamLengths = beamLengths.clone();
        this.beams = new int[pointCount][];
        this.beamDist = new int[beamLengths.length][0];
        for (int pt = 0; pt < pointCount; pt++) {
            int bc = beamCounts[pt];
            if (beamDist[bc].length == 0) {
                beamDist[bc] = new int[beamLengths[bc]];
            }
            beamDist[bc][beamDist[bc].length - beamLengths[bc]--] = pt;
            beams[pt] = new int[bc];
            int idx = 0;
            for (int ln = 0; ln < lines.length; ln++) {
                if (flags[ln][pt]) {
                    beams[pt][idx++] = ln;
                }
            }
        }
        this.lookup = generateLookup();
        this.intersections = new int[this.lines.length][this.lines.length];
        for (int[] arr : this.intersections) {
            Arrays.fill(arr, -1);
        }
        this.lineInter = new int[this.lines.length];
        for (int p = 0; p < this.pointCount; p++) {
            int[] beam = beams[p];
            for (int i = 0; i < beam.length; i++) {
                int l1 = beam[i];
                for (int j = i + 1; j < beam.length; j++) {
                    int l2 = beam[j];
                    this.intersections[l1][l2] = p;
                    this.intersections[l2][l1] = p;
                    lineInter[l1]++;
                    lineInter[l2]++;
                }
            }
        }
        this.lineFreq = new int[Math.min(lines.length, bmc * ll) + 1];
        for (int f : lineInter) {
            this.lineFreq[f]++;
        }
    }

    public PartialLiner(PartialLiner prev, int[] newLine) {
        this.pointCount = prev.pointCount;
        this.ll = prev.ll;
        int pll = prev.lines.length;
        this.lines = new int[pll + 1][];
        System.arraycopy(prev.lines, 0, this.lines, 0, pll);
        this.lines[pll] = newLine;
        this.flags = new boolean[pll + 1][];
        System.arraycopy(prev.flags, 0, this.flags, 0, pll);
        this.flags[pll] = new boolean[pointCount];
        for (int p : newLine) {
            flags[pll][p] = true;
        }
        this.lookup = prev.lookup.clone();
        this.beams = prev.beams.clone();
        this.beamCounts = prev.beamCounts.clone();
        this.beamLengths = prev.beamLengths.clone();
        this.lineInter = new int[pll + 1];
        int bl = beamLengths.length;
        this.lineFreq = new int[Math.min(lines.length, (bl - 1) * newLine.length) + 1];
        System.arraycopy(prev.lineInter, 0, this.lineInter, 0, pll);
        System.arraycopy(prev.lineFreq, 0, this.lineFreq, 0, prev.lineFreq.length);
        int ni = 0;
        this.intersections = new int[pll + 1][pll + 1];
        for (int i = 0; i < pll; i++) {
            System.arraycopy(prev.intersections[i], 0, this.intersections[i], 0, pll);
            intersections[i][pll] = -1;
        }
        Arrays.fill(intersections[pll], -1);
        for (int p : newLine) {
            int[] nl = this.lookup[p].clone(); // lookup
            for (int p1 : newLine) {
                if (p1 == p) {
                    continue;
                }
                nl[p1] = pll;
            }
            this.lookup[p] = nl;

            int[] ob = this.beams[p]; // beams
            int[] nb = new int[ob.length + 1];
            System.arraycopy(ob, 0, nb, 0, ob.length);
            nb[ob.length] = pll;
            this.beams[p] = nb;

            int pbc = beamCounts[p]; // beamcounts-beamlengths
            beamLengths[pbc++]--;
            beamCounts[p] = pbc;
            beamLengths[pbc]++;

            int[] beam = prev.beams[p];
            for (int l : beam) {
                int prInter = lineInter[l]; // lineinter-linefreq
                lineFreq[prInter++]--;
                lineInter[l] = prInter;
                lineFreq[prInter]++;

                intersections[l][pll] = p; // intersections
                intersections[pll][l] = p;
            }
            ni = ni + beam.length;
        }
        lineInter[pll] = ni;
        lineFreq[ni]++;

        this.beamDist = new int[bl][0];
        int[] beamLengths = this.beamLengths.clone();
        for (int pt = 0; pt < pointCount; pt++) {
            int bc = beamCounts[pt];
            if (beamDist[bc].length == 0) {
                beamDist[bc] = new int[beamLengths[bc]];
            }
            beamDist[bc][beamDist[bc].length - beamLengths[bc]--] = pt;
        }
    }

    public PartialLiner(Inc inc) {
        this(inc.v(), lines(inc));
    }

    private static int[][] lines(Inc inc) {
        int k = 0;
        for (int i = 0; i < inc.v(); i++) {
            if (inc.inc(0, i)) {
                k++;
            }
        }
        int[][] lines = new int[inc.b()][k];
        for (int l = 0; l < inc.b(); l++) {
            int[] newLine = new int[k];
            int idx = 0;
            for (int p = 0; p < inc.v(); p++) {
                if (inc.inc(l, p)) {
                    newLine[idx++] = p;
                }
            }
            lines[l] = newLine;
        }
        return lines;
    }

    private int[][] generateLookup() {
        int[][] result = new int[pointCount][pointCount];
        for (int[] p : result) {
            Arrays.fill(p, -1);
        }
        for (int l = 0; l < lines.length; l++) {
            int[] line = lines[l];
            for (int i = 0; i < line.length; i++) {
                int p1 = line[i];
                for (int j = i + 1; j < line.length; j++) {
                    int p2 = line[j];
                    result[p1][p2] = l;
                    result[p2][p1] = l;
                }
            }
        }
        return result;
    }

    public int pointCount() {
        return pointCount;
    }

    public int lineCount() {
        return lines.length;
    }

    public int[][] lines() {
        return lines;
    }

    public int[] beamCounts() {
        return beamCounts;
    }

    public int[] beamLengths() {
        return beamLengths;
    }

    public int[][] lookup() {
        return lookup;
    }

    public int[][] beams() {
        return beams;
    }

    public int[][] beamDist() {
        return beamDist;
    }

    public int[][] intersections() {
        return intersections;
    }

    public int[] lineInter() {
        return lineInter;
    }

    public int[] lineFreq() {
        return lineFreq;
    }

    public Inc toInc() {
        int b = lines.length;
        Inc res = Inc.empty(pointCount, b);
        for (int l = 0; l < b; l++) {
            boolean[] row = flags[l];
            for (int pt = 0; pt < pointCount; pt++) {
                if (row[pt]) {
                    res.set(l, pt);
                }
            }
        }
        return res;
    }

    public boolean flag(int line, int point) {
        return flags[line][point];
    }

    private void initOrder() {
        if (pointOrder != null) {
            return;
        }
        int[] freq = new int[pointCount];
        for (int[] pts : beamDist) {
            for (int pt : pts) {
                freq[pt] = pts.length;
            }
        }
        Integer[] arr = new Integer[pointCount];
        for (int i = 0; i < pointCount; i++) {
            arr[i] = i;
        }
        Arrays.sort(arr, Comparator.<Integer>comparingInt(pt -> freq[pt])
                .thenComparingInt(pt -> -beams[pt].length));
        pointOrder = new int[pointCount];
        for (int i = 0; i < pointCount; i++) {
            pointOrder[i] = arr[i];
        }
    }

    public int[] lookup(int pt) {
        return lookup[pt];
    }

    public int[] line(int line) {
        return lines[line];
    }

    public int line(int p1, int p2) {
        return lookup[p1][p2];
    }

    public int[] lines(int point) {
        return beams[point];
    }

    public int[] point(int point) {
        return beams[point];
    }

    public int intersection(int l1, int l2) {
        return intersections[l1][l2];
    }

    public int[] points(int line) {
        return lines[line];
    }

    public boolean isomorphic(PartialLiner second) {
        if (!Arrays.equals(lineFreq, second.lineFreq)) {
            return false;
        }
        int[] partialPoints = new int[pointCount];
        int[] partialLines = new int[lines.length];
        Arrays.fill(partialPoints, -1);
        Arrays.fill(partialLines, -1);
        int[] perLineUnAss = new int[lines.length];
        Arrays.fill(perLineUnAss, lines[0].length);
        int[] perPointUnAss = beamCounts.clone();
        initOrder();
        return isomorphic(0, 0, second, partialPoints, new boolean[pointCount], perPointUnAss, partialLines, new boolean[lines.length], perLineUnAss);
    }

    public boolean isomorphicL(PartialLiner second) {
        if (!Arrays.equals(lineFreq, second.lineFreq)) {
            return false;
        }
        int[] partialPoints = new int[pointCount];
        int[] partialLines = new int[lines.length];
        Arrays.fill(partialPoints, -1);
        Arrays.fill(partialLines, -1);
        int[] perLineUnAss = new int[lines.length];
        Arrays.fill(perLineUnAss, lines[0].length);
        int[] perPointUnAss = beamCounts.clone();
        return isomorphicL(second, 0, partialPoints, new boolean[pointCount], perPointUnAss, partialLines, new boolean[lines.length], perLineUnAss);
    }

    private boolean isomorphicL(PartialLiner second, int mapped, int[] pointsMap, boolean[] ptMapped, int[] perPointUnAss, int[] linesMap, boolean[] lnMapped, int[] perLineUnAss) {
        int from = -1;
        boolean foundNotCrossing = false;
        ex: for (int l = 0; l < linesMap.length; l++) {
            if (linesMap[l] >= 0) {
                continue;
            }
            for (int p : lines[l]) {
                if (pointsMap[p] >= 0) {
                    continue ex;
                }
            }
            foundNotCrossing = true;
            from = l;
            break;
        }
        if (!foundNotCrossing) {
            for (int i = 0; i < lines.length; i++) {
                if (linesMap[i] < 0) {
                    from = i;
                    break;
                }
            }
        }
        BitSet toFilter = new BitSet();
        if (foundNotCrossing) {
            for (int p : pointsMap) {
                if (p < 0) {
                    continue;
                }
                for (int l : second.beams[p]) {
                    toFilter.set(l);
                }
            }
        } else {
            for (int l : linesMap) {
                if (l < 0) {
                    continue;
                }
                toFilter.set(l);
            }
        }
        for (int to = toFilter.nextClearBit(0); to < linesMap.length; to = toFilter.nextClearBit(to + 1)) {
            if (lnMapped[to] || second.lineInter[to] != lineInter[from]) {
                continue;
            }
            int[] newPointsMap = pointsMap.clone();
            int[] newLinesMap = linesMap.clone();
            boolean[] newPtMapped = ptMapped.clone();
            boolean[] newLnMapped = lnMapped.clone();
            int[] newPerPointUnAss = perPointUnAss.clone();
            int[] newPerLineUnAss = perLineUnAss.clone();
            int added = mapLine(second, from, to, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
            if (added < 0) {
                continue;
            }
            int newMapped = mapped + added;
            if (newMapped == lines.length) {
                return true;
            }
            if (isomorphicL(second, newMapped, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss)) {
                return true;
            }
        }
        return false;
    }

    private static int findMinIdx(int[] freq, int[] arr, int[] map) {
        int idx = -1;
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < arr.length; i++) {
            int minC;
            if (map[i] < 0 && (minC = freq[arr[i]]) < min) {
                idx = i;
                min = minC;
            }
        }
        return idx;
    }

    private static int getUnMapped(int[] arr, boolean[] map) {
        int result = 0;
        for (int el : arr) {
            if (!map[el]) {
                result++;
            }
        }
        return result;
    }

    private boolean byPt(PartialLiner second, int from, int mapped, int[] pointsMap, boolean[] ptMapped, int[] perPointUnAss, int[] linesMap, boolean[] lnMapped, int[] perLineUnAss) {
        for (int to : second.beamDist[beams[from].length]) {
            if (ptMapped[to] || getUnMapped(second.beams[to], lnMapped) != perPointUnAss[from]) {
                continue;
            }
            int[] newPointsMap = pointsMap.clone();
            int[] newLinesMap = linesMap.clone();
            boolean[] newPtMapped = ptMapped.clone();
            boolean[] newLnMapped = lnMapped.clone();
            int[] newPerPointUnAss = perPointUnAss.clone();
            int[] newPerLineUnAss = perLineUnAss.clone();
            int added = mapPoint(second, from, to, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
            if (added < 0) {
                continue;
            }
            int newMapped = mapped + added;
            if (newMapped == lines.length) {
                return true;
            }
            if (isomorphicSel(second, newMapped, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss)) {
                return true;
            }
        }
        return false;
    }

    private boolean byLine(PartialLiner second, int from, int mapped, int[] pointsMap, boolean[] ptMapped, int[] perPointUnAss, int[] linesMap, boolean[] lnMapped, int[] perLineUnAss) {
        for (int to = 0; to < lines.length; to++) {
            if (lnMapped[to] || second.lineInter[to] != lineInter[from] || getUnMapped(second.lines[to], ptMapped) != perLineUnAss[from]) {
                continue;
            }
            int[] newPointsMap = pointsMap.clone();
            int[] newLinesMap = linesMap.clone();
            boolean[] newPtMapped = ptMapped.clone();
            boolean[] newLnMapped = lnMapped.clone();
            int[] newPerPointUnAss = perPointUnAss.clone();
            int[] newPerLineUnAss = perLineUnAss.clone();
            int added = mapLine(second, from, to, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
            if (added < 0) {
                continue;
            }
            int newMapped = mapped + added;
            if (newMapped == lines.length) {
                return true;
            }
            if (isomorphicSel(second, newMapped, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss)) {
                return true;
            }
        }
        return false;
    }

    public boolean isomorphicSel(PartialLiner second) {
        if (!Arrays.equals(lineFreq, second.lineFreq)) {
            return false;
        }
        int[] partialPoints = new int[pointCount];
        int[] partialLines = new int[lines.length];
        Arrays.fill(partialPoints, -1);
        Arrays.fill(partialLines, -1);
        int[] perLineUnAss = new int[lines.length];
        Arrays.fill(perLineUnAss, lines[0].length);
        int[] perPointUnAss = beamCounts.clone();
        return isomorphicSel(second, 0, partialPoints, new boolean[pointCount], perPointUnAss, partialLines, new boolean[lines.length], perLineUnAss);
    }

    private boolean isomorphicSel(PartialLiner second, int mapped, int[] pointsMap, boolean[] ptMapped, int[] perPointUnAss, int[] linesMap, boolean[] lnMapped, int[] perLineUnAss) {
        int[] ptFreq = new int[beams[0].length + 1];
        for (int i = 0; i < perPointUnAss.length; i++) {
            if (pointsMap[i] >= 0) {
                continue;
            }
            ptFreq[perPointUnAss[i]]++;
        }
        int[] lineFr = new int[lines[0].length + 1];
        for (int i = 0; i < perLineUnAss.length; i++) {
            if (linesMap[i] >= 0) {
                continue;
            }
            lineFr[perLineUnAss[i]]++;
        }
        int minPtIdx = findMinIdx(ptFreq, perPointUnAss, pointsMap);
        int minLineIdx = findMinIdx(lineFr, perLineUnAss, linesMap);
        return ptFreq[perPointUnAss[minPtIdx]] < lineFr[perLineUnAss[minLineIdx]]
                ? byPt(second, minPtIdx, mapped, pointsMap, ptMapped, perPointUnAss, linesMap, lnMapped, perLineUnAss)
                : byLine(second, minLineIdx, mapped, pointsMap, ptMapped, perPointUnAss, linesMap, lnMapped, perLineUnAss);
    }

    private boolean isomorphic(int mapped, int fromIdx, PartialLiner second, int[] pointsMap, boolean[] ptMapped, int[] perPointUnAss, int[] linesMap, boolean[] lnMapped, int[] perLineUnAss) {
        int from = pointOrder[fromIdx];
        for (int to : second.beamDist[beams[from].length]) {
            if (ptMapped[to]) {
                continue;
            }
            int[] newPointsMap = pointsMap.clone();
            int[] newLinesMap = linesMap.clone();
            boolean[] newPtMapped = ptMapped.clone();
            boolean[] newLnMapped = lnMapped.clone();
            int[] newPerPointUnAss = perPointUnAss.clone();
            int[] newPerLineUnAss = perLineUnAss.clone();
            int added = mapPoint(second, from, to, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
            if (added < 0) {
                continue;
            }
            int newMapped = mapped + added;
            if (newMapped == lines.length) {
                return true;
            }
            int newFrom = fromIdx + 1;
            while (newPointsMap[pointOrder[newFrom]] >= 0) {
                newFrom++;
            }
            if (isomorphic(newMapped, newFrom, second, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss)) {
                return true;
            }
        }
        return false;
    }

    private int mapPoint(PartialLiner second, int from, int to, int[] newPointsMap, boolean[] newPtMapped, int[] newPerPointUnAss, int[] newLinesMap, boolean[] newLnMapped, int[] newPerLineUnAss) {
        if (from < 0) {
            return to >= 0 ? -1 : 0;
        } else {
            if (to < 0) {
                return -1;
            }
        }
        int oldPoint = newPointsMap[from];
        if (oldPoint >= 0) {
            return oldPoint != to ? -1 : 0;
        }
        if (newPtMapped[to]) {
            return -1;
        }
        newPointsMap[from] = to;
        newPtMapped[to] = true;
        int result = 0;
        for (int line : beams[from]) {
            newPerLineUnAss[line]--;
            for (int p : lines[line]) {
                int pMap = newPointsMap[p];
                if (p == from || pMap < 0) {
                    continue;
                }
                int lineTo = second.lookup[to][pMap];
                int added = mapLine(second, line, lineTo, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
                if (added < 0) {
                    return -1;
                }
                result = result + added;
                if (newPerLineUnAss[line] == 1) {
                    int ptFrom = -1;
                    int ptTo = -1;
                    for (int p1 : lines[line]) {
                        if (newPointsMap[p1] < 0) {
                            ptFrom = p1;
                            break;
                        }
                    }
                    for (int p1 : second.lines[lineTo]) {
                        if (!newPtMapped[p1]) {
                            ptTo = p1;
                            break;
                        }
                    }
                    added = mapPoint(second, ptFrom, ptTo, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
                    if (added < 0) {
                        return -1;
                    }
                    result = result + added;
                }
                break;
            }
        }
        return result;
    }

    private int mapLine(PartialLiner second, int from, int to, int[] newPointsMap, boolean[] newPtMapped, int[] newPerPointUnAss, int[] newLinesMap, boolean[] newLnMapped, int[] newPerLineUnAss) {
        if (from < 0) {
            return to >= 0 ? -1 : 0;
        } else {
            if (to < 0) {
                return -1;
            }
        }
        int oldLine = newLinesMap[from];
        if (oldLine >= 0) {
            return oldLine != to ? -1 : 0;
        }
        if (newLnMapped[to]) {
            return -1;
        }
        newLinesMap[from] = to;
        newLnMapped[to] = true;
        int result = 1;
        for (int pt : lines[from]) {
            newPerPointUnAss[pt]--;
            for (int line : beams[pt]) {
                int lineMap = newLinesMap[line];
                if (line == from || lineMap < 0) {
                    continue;
                }
                int ptTo = second.intersections[to][lineMap];
                int added = mapPoint(second, pt, ptTo, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
                if (added < 0) {
                    return -1;
                }
                result = result + added;
                if (newPerPointUnAss[pt] == 1) {
                    int lineFrom = -1;
                    int lineTo = -1;
                    for (int l1 : beams[pt]) {
                        if (newLinesMap[l1] < 0) {
                            lineFrom = l1;
                            break;
                        }
                    }
                    for (int l1 : second.beams[ptTo]) {
                        if (!newLnMapped[l1]) {
                            lineTo = l1;
                            break;
                        }
                    }
                    added = mapLine(second, lineFrom, lineTo, newPointsMap, newPtMapped, newPerPointUnAss, newLinesMap, newLnMapped, newPerLineUnAss);
                    if (added < 0) {
                        return -1;
                    }
                    result = result + added;
                }
                break;
            }
        }
        return result;
    }

    public boolean checkAP() {
        int last = lines.length - 1;
        int[] lLine = lines[last];
        int ll = lLine.length;
        for (int p : lLine) {
            int[] beam = beams[p];
            for (int ol : beam) {
                if (ol == last) {
                    continue;
                }
                int[] oLine = lines[ol];
                for (int a = 0; a < ll; a++) {
                    int pl1 = lLine[a];
                    if (pl1 == p) {
                        continue;
                    }
                    for (int b = a + 1; b < ll; b++) {
                        int pl2 = lLine[b];
                        if (pl2 == p) {
                            continue;
                        }
                        for (int c = 0; c < ll; c++) {
                            int po1 = oLine[c];
                            if (po1 == p) {
                                continue;
                            }
                            int l1 = lookup[pl1][po1];
                            int l2 = lookup[pl2][po1];
                            if (l1 < 0 && l2 < 0) {
                                continue;
                            }
                            for (int d = c + 1; d < ll; d++) {
                                int po2 = oLine[d];
                                if (po2 == p) {
                                    continue;
                                }
                                int l4 = lookup[pl2][po2];
                                if (l1 >= 0 && l4 >= 0 && intersections[l1][l4] >= 0) {
                                    return false;
                                }
                                int l3 = lookup[pl1][po2];
                                if (l2 >= 0 && l3 >= 0 && intersections[l2][l3] >= 0) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean hullsUnderCap(int cap) {
        int[] ll = lines[lines.length - 1];
        BitSet last = new BitSet(pointCount);
        for (int point : ll) {
            last.set(point);
        }
        for (int pt = 0; pt < pointCount; pt++) {
            if (last.get(pt)) {
                continue;
            }
            BitSet base = (BitSet) last.clone();
            base.set(pt);
            BitSet additional = new BitSet(pointCount);
            additional.set(pt);
            while (!(additional = additional(base, additional)).isEmpty()) {
                base.or(additional);
                if (base.cardinality() > cap) {
                    return false;
                }
            }
        }
        return true;
    }

    private BitSet additional(BitSet first, BitSet second) {
        BitSet result = new BitSet(pointCount);
        for (int x = first.nextSetBit(0); x >= 0; x = first.nextSetBit(x + 1)) {
            for (int y = second.nextSetBit(0); y >= 0; y = second.nextSetBit(y + 1)) {
                int line = lookup[x][y];
                if (line < 0) {
                    continue;
                }
                for (int p : lines[line]) {
                    if (!first.get(p) && !second.get(p)) {
                        result.set(p);
                    }
                }
            }
        }
        return result;
    }

    public boolean hasNext(int depth) {
        if (depth == 0) {
            return true;
        }
        for (int[] block : blocks()) {
            PartialLiner part = new PartialLiner(this, block);
            if (part.hasNext(depth - 1)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNext(Function<PartialLiner, Iterable<int[]>> blocks, int depth) {
        if (depth == 0) {
            return true;
        }
        for (int[] block : blocks.apply(this)) {
            PartialLiner part = new PartialLiner(this, block);
            if (part.hasNext(blocks, depth - 1)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNext(Predicate<PartialLiner> test, int depth) {
        if (!test.test(this)) {
            return false;
        }
        if (depth == 0) {
            return true;
        }
        for (int[] block : blocks()) {
            PartialLiner part = new PartialLiner(this, block);
            if (part.hasNext(test, depth - 1)) {
                return true;
            }
        }
        return false;
    }

    public Iterable<int[]> blocks() {
        return () -> new BlocksIterator(false);
    }

    public Iterable<int[]> blocks(boolean minimal) {
        return () -> new BlocksIterator(minimal);
    }

    private class BlocksIterator implements Iterator<int[]> {
        private final int[] block;
        private boolean hasNext;

        public BlocksIterator(boolean minimal) {
            int fst;
            int snd;
            this.block = new int[ll];
            if (minimal) {
                fst = pointCount;
                snd = pointCount;
                ex: for (int i = 0; i < pointCount; i++) {
                    for (int j = i + 1; j < pointCount; j++) {
                        if (line(i, j) < 0) {
                            fst = i;
                            snd = j;
                            break ex;
                        }
                    }
                }
            } else {
                int[] prev = lines[lines.length - 1];
                fst = prev[0];
                int[] look;
                do {
                    look = lookup[fst];
                    snd = getUnassigned(look, fst);
                } while (snd < 0 && ++fst < pointCount);
            }
            block[0] = fst;
            block[1] = snd;
            for (int i = 2; i < ll; i++) {
                block[i] = snd + i - 1;
            }
            this.hasNext = fst < pointCount && findNext(ll - 2);
        }

        private static int getUnassigned(int[] look, int pt) {
            for (int i = pt + 1; i < look.length; i++) {
                if (look[i] < 0) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        private boolean findNext(int moreNeeded) {
            int len = block.length - moreNeeded;
            ex: for (int p = Math.max(block[len - 1] + 1, block[len]); p < pointCount - moreNeeded + 1; p++) {
                int[] look = lookup[p];
                for (int i = 0; i < len; i++) {
                    if (look[block[i]] >= 0) {
                        continue ex;
                    }
                }
                block[len] = p;
                if (moreNeeded == 1 || findNext(moreNeeded - 1)) {
                    return true;
                }
            }
            int base = ++block[len - 1] - len + 1;
            for (int i = len; i < block.length; i++) {
                block[i] = base + i;
            }
            return false;
        }

        @Override
        public int[] next() {
            int[] res = block.clone();
            block[block.length - 1]++;
            this.hasNext = findNext(block.length - 2);
            return res;
        }
    }

    public Iterable<int[]> blocksResolvable() {
        return BlocksResIterator::new;
    }

    private class BlocksResIterator implements Iterator<int[]> {
        private final int[] block;
        private final int desired;
        private boolean hasNext;

        public BlocksResIterator() {
            int ll = lines[0].length;
            this.desired = lines.length * ll / pointCount + 1;
            this.block = new int[ll];
            int fst = 0;
            while (beamCounts[fst] == desired) {
                fst++;
            }
            block[0] = fst;
            for (int i = 1; i < ll; i++) {
                block[i] = fst + i;
            }
            this.hasNext = fst < pointCount && findNext(ll - 1);
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        private boolean findNext(int moreNeeded) {
            int len = block.length - moreNeeded;
            ex: for (int p = Math.max(block[len - 1] + 1, block[len]); p < pointCount - moreNeeded + 1; p++) {
                if (beamCounts[p] == desired) {
                    continue;
                }
                int[] look = lookup[p];
                for (int i = 0; i < len; i++) {
                    if (look[block[i]] >= 0) {
                        continue ex;
                    }
                }
                block[len] = p;
                if (moreNeeded == 1 || findNext(moreNeeded - 1)) {
                    return true;
                }
            }
            int base = ++block[len - 1] - len + 1;
            for (int i = len; i < block.length; i++) {
                block[i] = base + i;
            }
            return false;
        }

        @Override
        public int[] next() {
            int[] res = block.clone();
            block[block.length - 1]++;
            this.hasNext = findNext(block.length - 1);
            return res;
        }
    }

    private class AltBlocksIterator implements Iterator<int[]> {
        private final int[] block;
        private final BiPredicate<PartialLiner, int[]> pred;
        private boolean hasNext;

        public AltBlocksIterator(BiPredicate<PartialLiner, int[]> pred, boolean minimal) {
            this.pred = pred;
            int ll = lines[0].length;
            this.block = new int[ll];
            int pair = findFirstPair(pred);
            if (pair < 0) {
                return;
            }
            if (minimal) {
                pair = findMinimalPair();
            }
            block[0] = pair / pointCount;
            block[1] = pair % pointCount;
            for (int i = 2; i < ll; i++) {
                block[i] = i - 2;
            }
            this.hasNext = findNext(ll - 2);
        }

        private int findMinimalPair() {
            for (int i = 0; i < 2 * pointCount - 2; i++) {
                int hf = (i + 1) / 2;
                for (int j = Math.max(0, i - pointCount + 1); j < hf; j++) {
                    int k = i - j;
                    if (lookup[j][k] < 0) {
                        return j * pointCount + k;
                    }
                }
            }
            return -1;
        }

        private int findFirstPair(BiPredicate<PartialLiner, int[]> pred) {
            int result = -1;
            int minAv = Integer.MAX_VALUE;
            int[][] available = availableLines(pred);
            for (int i = 0; i < pointCount; i++) {
                int[] look = lookup[i];
                for (int j = i + 1; j < pointCount; j++) {
                    if (look[j] >= 0) {
                        continue;
                    }
                    int av = available[i][j];
                    if (minAv > av) {
                        if (av == 0) {
                            return -1;
                        }
                        minAv = av;
                        result = i * pointCount + j;
                    }
                }
            }
            return result;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        private boolean findNext(int moreNeeded) {
            int len = block.length - moreNeeded;
            ex: for (int p = Math.max(len == 2 ? 0 : block[len - 1] + 1, block[len]); p < pointCount - moreNeeded + 1; p++) {
                if (p == block[0] || p == block[1]) {
                    continue;
                }
                int[] look = lookup[p];
                for (int i = 0; i < len; i++) {
                    if (look[block[i]] >= 0) {
                        continue ex;
                    }
                }
                block[len] = p;
                if (moreNeeded == 1) {
                    if (pred.test(PartialLiner.this, block)) {
                        return true;
                    }
                } else if (findNext(moreNeeded - 1)) {
                    return true;
                }
            }
            int base = ++block[len - 1] - len + 1;
            for (int i = len; i < block.length; i++) {
                block[i] = base + i;
            }
            return false;
        }

        @Override
        public int[] next() {
            int[] res = block.clone();
            Arrays.sort(res);
            block[block.length - 1]++;
            this.hasNext = findNext(block.length - 2);
            return res;
        }
    }

    private class TriangleBlocksIterator implements Iterator<int[]> {
        private final int[] block;
        private final BiPredicate<PartialLiner, int[]> pred;
        private boolean hasNext;

        public TriangleBlocksIterator(BiPredicate<PartialLiner, int[]> pred) {
            this.pred = pred;
            int ll = lines[0].length;
            int r = (pointCount - 1) / (ll - 1);
            this.block = new int[ll];
            ex: for (int i = 1; i < r; i++) {
                int[] lineB = lines[i];
                for (int j = 0; j < i; j++) {
                    int[] lineA = lines[j];
                    for (int a = 1; a < ll; a++) {
                        int p1 = lineA[a];
                        for (int b = 1; b < ll; b++) {
                            int p2 = lineB[b];
                            if (line(p1, p2) < 0) {
                                block[0] = Math.min(p1, p2);
                                block[1] = Math.max(p1, p2);
                                break ex;
                            }
                        }
                    }
                }
            }
            for (int i = 2; i < ll; i++) {
                block[i] = i - 2;
            }
            this.hasNext = findNext(ll - 2);
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        private boolean findNext(int moreNeeded) {
            int len = block.length - moreNeeded;
            ex: for (int p = Math.max(len == 2 ? 0 : block[len - 1] + 1, block[len]); p < pointCount - moreNeeded + 1; p++) {
                if (p == block[0] || p == block[1]) {
                    continue;
                }
                int[] look = lookup[p];
                for (int i = 0; i < len; i++) {
                    if (look[block[i]] >= 0) {
                        continue ex;
                    }
                }
                block[len] = p;
                if (moreNeeded == 1) {
                    if (pred.test(PartialLiner.this, block)) {
                        return true;
                    }
                } else if (findNext(moreNeeded - 1)) {
                    return true;
                }
            }
            int base = ++block[len - 1] - len + 1;
            for (int i = len; i < block.length; i++) {
                block[i] = base + i;
            }
            return false;
        }

        @Override
        public int[] next() {
            int[] res = block.clone();
            Arrays.sort(res);
            block[block.length - 1]++;
            this.hasNext = findNext(block.length - 2);
            return res;
        }
    }

    public Iterable<int[]> altBlocks(BiPredicate<PartialLiner, int[]> pred, boolean minimal) {
        return () -> new AltBlocksIterator(pred, minimal);
    }

    public Iterable<int[]> altBlocks(BiPredicate<PartialLiner, int[]> pred) {
        return () -> new AltBlocksIterator(pred, false);
    }

    public Iterable<int[]> trBlocks(BiPredicate<PartialLiner, int[]> pred) {
        return () -> new TriangleBlocksIterator(pred);
    }

    private static int min = Integer.MAX_VALUE;

    public int designs(int needed, Function<PartialLiner, Iterable<int[]>> gen, Consumer<PartialLiner> cons) {
        int res = needed;
        for (int[] block : gen.apply(this)) {
            PartialLiner nextPartial = new PartialLiner(this, block);
            if (needed == 1) {
                cons.accept(nextPartial);
                res = 0;
            } else {
                int next = nextPartial.designs(needed - 1, gen, cons);
                if (next < res) {
                    res = next;
                    if (next < min) {
                        min = next;
                        System.out.println(min);
                    }
                }
            }
        }
        return res;
    }

    public FixBS getCanonical() {
        if (canonical == null) {
            GraphWrapper graph = GraphWrapper.forPartial(this);
            CanonicalConsumer cons = new CanonicalConsumer(graph);
            NautyAlgo.search(graph, cons);
            canonical = cons.canonicalForm();
        }
        return canonical;
    }

    public long autCount() {
        AtomicLong counter = new AtomicLong();
        Consumer<int[]> cons = arr -> counter.incrementAndGet();
        GraphWrapper wrap = GraphWrapper.forPartial(this);
        AutomorphismConsumerNew aut = new AutomorphismConsumerNew(wrap, cons);
        NautyAlgoNew.search(wrap, aut);
        return counter.get();
    }

    public int[][] automorphisms() {
        List<int[]> res = new ArrayList<>();
        Consumer<int[]> cons = res::add;
        GraphWrapper wrap = GraphWrapper.forPartial(this);
        AutomorphismConsumerNew aut = new AutomorphismConsumerNew(wrap, cons);
        NautyAlgoNew.search(wrap, aut);
        return res.toArray(int[][]::new);
    }

    public int[][] availableLines(BiPredicate<PartialLiner, int[]> pred) {
        int[][] result = new int[pointCount][pointCount];
        int ll = lines[0].length;
        int[] curr = new int[ll];
        availableLines(result, curr, ll, pred);
        return result;
    }

    private void availableLines(int[][] dist, int[] arr, int needed, BiPredicate<PartialLiner, int[]> pred) {
        int ll = arr.length;
        int len = ll - needed;
        ex: for (int p = len == 0 ? 0 : arr[len - 1] + 1; p < pointCount; p++) {
            int[] look = lookup[p];
            for (int i = 0; i < len; i++) {
                if (look[arr[i]] >= 0) {
                    continue ex;
                }
            }
            arr[len] = p;
            if (needed == 1) {
                if (pred.test(this, arr)) {
                    for (int i = 0; i < ll; i++) {
                        int a = arr[i];
                        for (int j = i + 1; j < ll; j++) {
                            int b = arr[j];
                            dist[a][b]++;
                            dist[b][a]++;
                        }
                    }
                }
            } else {
                availableLines(dist, arr, needed - 1, pred);
            }
        }
    }

    public List<int[]> availableRegular() {
        List<int[]> min = null;
        int k = lines[0].length;
        for (int l1 = 0; l1 < lines.length; l1++) {
            for (int l2 = l1 + 1; l2 < lines.length; l2++) {
                int inter = intersection(l1, l2);
                if (inter < 0) {
                    continue;
                }
                FixBS needed = new FixBS(pointCount);
                needed.set(0, pointCount);
                int counter = k * k - 1;
                for (int p1 : lines[l1]) {
                    for (int p2 : lines[l2]) {
                        int ln = line(p1, p2);
                        if (ln < 0) {
                            continue;
                        }
                        counter--;
                        for (int pt : lines[ln]) {
                            needed.clear(pt);
                        }
                    }
                }
                int crd = needed.cardinality();
                if (crd == 0) {
                    continue;
                }
                if (counter * (k - 2) < crd) {
                    return List.of();
                }
                for (int pt = needed.nextSetBit(0); pt >= 0; pt = needed.nextSetBit(pt + 1)) {
                    List<int[]> lns = new ArrayList<>();
                    for (int p1 : lines[l1]) {
                        if (p1 == inter || line(pt, p1) >= 0) {
                            continue;
                        }
                        for (int p2 : lines[l2]) {
                            if (p2 == inter || line(pt, p2) >= 0 || line(p1, p2) >= 0) {
                                continue;
                            }
                            int[] line = new int[k];
                            line[0] = pt;
                            line[1] = p1;
                            line[2] = p2;
                            regBlocks(line, k - 3, lns::add);
                        }
                    }
                    if (min == null || min.size() > lns.size()) {
                        min = lns;
                    }
                }
            }
        }
        if (min != null) {
            return min;
        } else {
            List<int[]> res = new ArrayList<>();
            for (int[] bl : altBlocks((l, b) -> true)) {
                res.add(bl);
            }
            return res;
        }
    }

    private void regBlocks(int[] curr, int moreNeeded, Consumer<int[]> cons) {
        int len = curr.length - moreNeeded;
        ex: for (int p = len == 3 ? 0 : curr[len - 1] + 1; p < pointCount; p++) {
            if (p == curr[0] || p == curr[1] || p == curr[2]) {
                continue;
            }
            int[] look = lookup[p];
            for (int i = 0; i < len; i++) {
                if (look[curr[i]] >= 0) {
                    continue ex;
                }
            }
            curr[len] = p;
            if (moreNeeded == 1) {
                int[] res = curr.clone();
                Arrays.sort(res);
                cons.accept(res);
            } else {
                regBlocks(curr, moreNeeded - 1, cons);
            }
        }
    }

    public boolean checkAP(int[] line) {
        int ll = line.length;
        for (int p : line) {
            for (int a = 0; a < ll; a++) {
                int pl1 = line[a];
                if (pl1 == p) {
                    continue;
                }
                for (int b = a + 1; b < ll; b++) {
                    int pl2 = line[b];
                    if (pl2 == p) {
                        continue;
                    }
                    for (int ol : beams[p]) {
                        int[] oLine = lines[ol];
                        for (int c = 0; c < ll; c++) {
                            int po1 = oLine[c];
                            if (po1 == p) {
                                continue;
                            }
                            int[] lk = lookup[po1];
                            int l1 = lk[pl1];
                            int l2 = lk[pl2];
                            if (l1 < 0 && l2 < 0) {
                                continue;
                            }
                            for (int d = c + 1; d < ll; d++) {
                                int po2 = oLine[d];
                                if (po2 == p) {
                                    continue;
                                }
                                int[] lk1 = lookup[po2];
                                int l4 = lk1[pl2];
                                if (l1 >= 0 && l4 >= 0 && intersections[l1][l4] >= 0) {
                                    return false;
                                }
                                int l3 = lk1[pl1];
                                if (l2 >= 0 && l3 >= 0 && intersections[l2][l3] >= 0) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean hullsUnderCap(int[] line, int cap) {
        BitSet last = new BitSet(pointCount);
        for (int point : line) {
            last.set(point);
        }
        for (int pt = 0; pt < pointCount; pt++) {
            if (last.get(pt)) {
                continue;
            }
            BitSet base = (BitSet) last.clone();
            base.set(pt);
            BitSet additional = new BitSet(pointCount);
            additional.set(pt);
            while (!(additional = additional(base, additional)).isEmpty()) {
                base.or(additional);
                if (base.cardinality() > cap) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<int[]> possibleBlocks() {
        List<int[]> res = new ArrayList<>();
        int k = lines[0].length;
        for (int i = 0; i < pointCount; i++) {
            int[] bl = new int[k];
            bl[0] = i;
            possibleBlocks(bl, k - 1, res::add);
        }
        return res;
    }

    private void possibleBlocks(int[] curr, int needed, Consumer<int[]> cons) {
        if (needed == 0) {
            cons.accept(curr.clone());
            return;
        }
        int sz = curr.length - needed;
        int last = curr[sz - 1];
        ex: for (int p = last + 1; p < pointCount; p++) {
            for (int i = 0; i < sz; i++) {
                if (line(curr[i], p) >= 0) {
                    continue ex;
                }
            }
            curr[sz] = p;
            possibleBlocks(curr, needed - 1, cons);
        }
    }
}
