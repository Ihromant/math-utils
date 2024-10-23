package ua.ihromant.mathutils;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;

public class SimpleLiner {
    private final int pointCount;
    private final FixBS[] lines;

    public SimpleLiner(int pointCount, int[][] lines) {
        this.pointCount = pointCount;
        this.lines = Arrays.stream(lines).map(l -> {
            FixBS bs = new FixBS(pointCount);
            for (int pt : l) {
                bs.set(pt);
            }
            return bs;
        }).toArray(FixBS[]::new);
        consistencyCheck();
    }

    private void consistencyCheck() {
        for (int i = 0; i < lines.length; i++) {
            FixBS l1 = lines[i];
            for (int j = i + 1; j < lines.length; j++) {
                FixBS l2 = lines[j];
                if (l1.intersects(l2) && !l1.singleIntersection(l2)) {
                    throw new IllegalStateException(i + ":" + l1 + ", " + j + ":" + l2);
                }
            }
        }
    }

    public int pointCount() {
        return pointCount;
    }

    public int lineCount() {
        return lines.length;
    }

    public int[] line(int line) {
        return lines[line].stream().toArray();
    }

    public int line(int p1, int p2) {
        for (int i = 0; i < lines.length; i++) {
            FixBS line = lines[i];
            if (line.get(p1) && line.get(p2)) {
                return i;
            }
        }
        return -1;
    }

    public int intersection(int l1, int l2) {
        FixBS line1 = lines[l1];
        FixBS line2 = lines[l2];
        for (int i = line1.nextSetBit(0); i >= 0; i = line1.nextSetBit(i + 1)) {
            if (line2.get(i)) {
                return i;
            }
        }
        return -1;
    }

    public int[] points(int line) {
        return lines[line].stream().toArray();
    }

    public FixBS[] lines() {
        return lines;
    }

    public boolean collinear(int... points) {
        if (points.length == 0) {
            return true;
        }
        int first = points[0];
        for (int i = 1; i < points.length; i++) {
            int second = points[i];
            if (first != second) {
                FixBS line = lines[line(first, second)];
                return Arrays.stream(points, i + 1, points.length).allMatch(line::get);
            }
        }
        return true;
    }

    public String lineToString(int line) {
        return lines[line].toString();
    }
}
