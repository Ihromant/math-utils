package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record VeblenPoint(String l, int cff) {
    public static final List<VeblenPoint> POINTS = Stream.of("A", "B", "C", "D", "E", "F", "G")
            .flatMap(l -> IntStream.range(0, 13).mapToObj(cff -> new VeblenPoint(l, cff))).toList();

    private static final Set<VeblenPoint> s1 = Stream.of("A0", "A1", "A3", "A9", "B0", "C0", "D0", "E0", "F0", "G0")
            .map(VeblenPoint::parse).collect(Collectors.toSet());
    private static final Set<VeblenPoint> s2 = Stream.of("A0", "B1", "B8", "D3", "D11", "E2", "E5", "E6", "G7", "G9")
            .map(VeblenPoint::parse).collect(Collectors.toSet());
    private static final Set<VeblenPoint> s3 = Stream.of("A0", "C1", "C8", "E7", "E9", "F3", "F11", "G2", "G5", "G6")
            .map(VeblenPoint::parse).collect(Collectors.toSet());
    private static final Set<VeblenPoint> s4 = Stream.of("A0", "B7", "B9", "D1", "D8", "F2", "F5", "F6", "G3", "G11")
            .map(VeblenPoint::parse).collect(Collectors.toSet());
    private static final Set<VeblenPoint> s5 = Stream.of("A0", "B2", "B5", "B6", "C3", "C11", "E1", "E8", "F7", "F9")
            .map(VeblenPoint::parse).collect(Collectors.toSet());
    private static final Set<VeblenPoint> s6 = Stream.of("A0", "C7", "C9", "D2", "D5", "D6", "E3", "E11", "F1", "F8")
            .map(VeblenPoint::parse).collect(Collectors.toSet());
    private static final Set<VeblenPoint> s7 = Stream.of("A0", "B3", "B11", "C2", "C5", "C6", "D7", "D9", "G1", "G8")
            .map(VeblenPoint::parse).collect(Collectors.toSet());

    public static final List<Set<VeblenPoint>> LINES = Stream.of(s1, s2, s3, s4, s5, s6, s7)
            .flatMap(s -> IntStream.range(0, 13).mapToObj(i -> s.stream().map(vp -> vp.next(i))
                    .collect(Collectors.toSet()))).toList();

    public static final Map<VeblenPoint, Map<VeblenPoint, Set<VeblenPoint>>> LOOKUP = generateLookup();

    private static Map<VeblenPoint, Map<VeblenPoint, Set<VeblenPoint>>> generateLookup() {
        Map<VeblenPoint, Map<VeblenPoint, Set<VeblenPoint>>> result = new HashMap<>();
        for (Set<VeblenPoint> line : LINES) {
            for (VeblenPoint p1 : line) {
                if (!result.containsKey(p1)) {
                    result.put(p1, new HashMap<>());
                }
                Map<VeblenPoint, Set<VeblenPoint>> map = result.get(p1);
                for (VeblenPoint p2 : line) {
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

    public static final Map<VeblenPoint, Set<Set<VeblenPoint>>> BEAMS = generateBeams();

    private static Map<VeblenPoint, Set<Set<VeblenPoint>>> generateBeams() {
        Map<VeblenPoint, Set<Set<VeblenPoint>>> result = new HashMap<>();
        for (VeblenPoint p1 : POINTS) {
            Set<Set<VeblenPoint>> beam = new HashSet<>();
            result.put(p1, beam);
            for (VeblenPoint p2 : POINTS) {
                if (p1.equals(p2)) {
                    continue;
                }
                beam.add(line(p1, p2));
            }
        }
        return result;
    }

    public static Set<VeblenPoint> line(VeblenPoint p1, VeblenPoint p2) {
        return LOOKUP.get(p1).get(p2);
    }

    public static boolean collinear(VeblenPoint... points) {
        if (points.length <= 2) {
            return true;
        }
        Set<VeblenPoint> line = LOOKUP.get(points[0]).get(points[1]);
        return Arrays.stream(points, 2, points.length).allMatch(line::contains);
    }


    public static VeblenPoint parse(String from) {
        return new VeblenPoint(from.substring(0, 1), Integer.parseInt(from.substring(1)));
    }

    public VeblenPoint next(int add) {
        return new VeblenPoint(l, (cff + add) % 13);
    }

    @Override
    public String toString() {
        return l + cff;
    }
}
