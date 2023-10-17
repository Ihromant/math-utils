package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record TriNearPoint(NearField x, NearField y, NearField z) {
    private static final NearField THETA = NearField.PL_J;

    public static final TriNearPoint[] POINTS = generatePoints();

    private static TriNearPoint[] generatePoints() {
        NearField[] vals = NearField.values();
        int size = vals.length;
        TriNearPoint[] points = new TriNearPoint[size * size + size + 1];
        for (int y = 0; y < size; y++) {
            for (int z = 0; z < size; z++) {
                points[y * size + z] = new TriNearPoint(NearField.PL_1, vals[y], vals[z]);
            }
        }
        for (int z = 0; z < size; z++) {
            points[size * size + z] = new TriNearPoint(NearField.ZERO, NearField.PL_1, vals[z]);
        }
        points[size * size + size] = new TriNearPoint(NearField.ZERO, NearField.ZERO, NearField.PL_1);
        return points;
    }

    public static final Map<TriNearPoint, Set<TriNearPoint>> LINES = createLines();

    private static Map<TriNearPoint, Set<TriNearPoint>> createLines() {
        Map<TriNearPoint, Set<TriNearPoint>> lines = new HashMap<>();
        NearField[] base = new NearField[] {NearField.ZERO, NearField.PL_1, NearField.MI_1};
        for (NearField a1 : base) {
            for (NearField b1 : base) {
                for (NearField c1 : base) {
                    for (NearField a2 : base) {
                        for (NearField b2 : base) {
                            for (NearField c2 : base) {
                                if (a1 == NearField.ZERO && b1 == NearField.ZERO && c1 == NearField.ZERO
                                        && a2 == NearField.ZERO && b2 == NearField.ZERO && c2 == NearField.ZERO) {
                                    continue;
                                }
                                TriNearPoint p = new TriNearPoint(a1.add(a2.mul(THETA)), b1.add(b2.mul(THETA)), c1.add(c2.mul(THETA))).normalize();
                                if (lines.containsKey(p)) {
                                    continue;
                                }
                                Set<TriNearPoint> line = new HashSet<>();
                                for (TriNearPoint lp : POINTS) {
                                    NearField fc = NearField.add(lp.x.mul(a1), lp.y.mul(b1), lp.z.mul(c1));
                                    NearField sc = NearField.add(lp.x.mul(a2), lp.y.mul(b2), lp.z.mul(c2));
                                    if (fc.add(sc.mul(THETA)) == NearField.ZERO) {
                                        line.add(lp);
                                    }
                                }
                                lines.put(p, line);
                            }
                        }
                    }
                }
            }
        }
        return lines;
    }

    public static final Map<TriNearPoint, Map<TriNearPoint, Set<TriNearPoint>>> LOOKUP = generateLookup();

    private static Map<TriNearPoint, Map<TriNearPoint, Set<TriNearPoint>>> generateLookup() {
        Map<TriNearPoint, Map<TriNearPoint, Set<TriNearPoint>>> result = new HashMap<>();
        for (Set<TriNearPoint> line : LINES.values()) {
            for (TriNearPoint p1 : line) {
                if (!result.containsKey(p1)) {
                    result.put(p1, new HashMap<>());
                }
                Map<TriNearPoint, Set<TriNearPoint>> map = result.get(p1);
                for (TriNearPoint p2 : line) {
                    if (map.containsKey(p2)) {
                        continue;
                    }
                    if (p1.equals(p2)) {
                        map.put(p2, Set.of(p2));
                    } else {
                        map.put(p2, line);
                    }
                }
            }
        }
        return result;
    }

    public static final Map<TriNearPoint, Set<Set<TriNearPoint>>> BEAMS = generateBeams();

    private static Map<TriNearPoint, Set<Set<TriNearPoint>>> generateBeams() {
        Map<TriNearPoint, Set<Set<TriNearPoint>>> result = new HashMap<>();
        for (TriNearPoint p1 : POINTS) {
            Set<Set<TriNearPoint>> beam = new HashSet<>();
            result.put(p1, beam);
            for (TriNearPoint p2 : POINTS) {
                if (p1.equals(p2)) {
                    continue;
                }
                beam.add(line(p1, p2));
            }
        }
        return result;
    }

    public static boolean collinear(TriNearPoint... points) {
        if (points.length <= 2) {
            return true;
        }
        Set<TriNearPoint> line = LOOKUP.get(points[0]).get(points[1]);
        return Arrays.stream(points, 2, points.length).allMatch(line::contains);
    }

    public static Set<TriNearPoint> line(TriNearPoint p1, TriNearPoint p2) {
        return LOOKUP.get(p1).get(p2);
    }

    public TriNearPoint normalize() {
        if (x != NearField.ZERO) {
            return this.mul(x.inv());
        }
        if (y != NearField.ZERO) {
            return this.mul(y.inv());
        }
        return this.mul(z.inv());
    }

    public TriNearPoint mul(NearField cff) {
        return new TriNearPoint(cff.mul(x), cff.mul(y), cff.mul(z));
    }
}
