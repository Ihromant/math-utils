package ua.ihromant.mathutils;

import lombok.Getter;
import ua.ihromant.mathutils.plane.Quad;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Getter
public class FuzzyLiner {
    private final int pc;
    private final Set<Pair> d;
    private final Set<Triple> l;
    private final Set<Triple> t;

    public FuzzyLiner(int pc) {
        this.pc = pc;
        this.d = new HashSet<>();
        this.l = new HashSet<>();
        this.t = new HashSet<>();
    }

    public FuzzyLiner(int[][] lines) {
        this(Arrays.stream(lines).mapToInt(l -> Arrays.stream(l).max().orElseThrow()).max().orElseThrow() + 1);
        for (int[] line : lines) {
            for (int i = 0; i < line.length; i++) {
                int p1 = line[i];
                for (int j = i + 1; j < line.length; j++) {
                    int p2 = line[j];
                    for (int k = 0; k < pc; k++) {
                        if (k == p1 || k == p2) {
                            continue;
                        }
                        if (Arrays.binarySearch(line, k) >= 0) {
                            colline(p1, p2, k);
                        } else {
                            triangule(p1, p2, k);
                        }
                    }
                }
            }
        }
    }

    public boolean distinguish(int i, int j) {
        if (i == j) {
            throw new IllegalArgumentException(i + " " + j);
        }
        return d.add(new Pair(i, j));
    }

    public boolean colline(int a, int b, int c) {
        if (a == b || a == c || b == c || triangle(a, b, c)) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        return l.add(new Triple(a, b, c));
    }

    public boolean triangule(int a, int b, int c) {
        if (a == b || a == c || b == c || collinear(a, b, c)) {
            throw new IllegalArgumentException(a + " " + b + " " + c);
        }
        return t.add(new Triple(a, b, c));
    }

    public boolean distinct(int a, int b) {
        return d.contains(new Pair(a, b));
    }

    public boolean collinear(int a, int b, int c) {
        return l.contains(new Triple(a, b, c));
    }

    public boolean triangle(int a, int b, int c) {
        return t.contains(new Triple(a, b, c));
    }

    public int intersection(Pair fst, Pair snd) {
        for (int i = 0; i < pc; i++) {
            if (collinear(fst.f(), fst.s(), i) && collinear(snd.f(), snd.s(), i)) {
                return i;
            }
        }
        return -1;
    }

    public void update() {
        int counter = 0;
        while (updateStep()) {
            System.out.println("Updating... " + counter);
        }
    }

    private boolean updateStep() {
        boolean result = false;
        for (int x = 0; x < pc; x++) {
            for (int y = x + 1; y < pc; y++) {
                for (int z = y + 1; z < pc; z++) {
                    boolean cxyz = collinear(x, y, z);
                    if (triangle(x, y, z)) {
                        result = result | distinguish(x, y) | distinguish(y, z) | distinguish(x, z);
                    }
                    if (!cxyz) {
                        continue;
                    }
                    result = result | distinguish(x, y) | distinguish(y, z) | distinguish(x, z);
                    for (int w = z + 1; w < pc; w++) {
                        if (distinct(z, w) && collinear(x, y, w)) {
                            result = result | colline(z, w, x) | colline(z, w, y);
                        }
                        if (distinct(y, w) && collinear(x, z, w)) {
                            result = result | colline(y, w, x) | colline(y, w, z);
                        }
                        if (distinct(x, w) && collinear(z, y, w)) {
                            result = result | colline(x, w, y) | colline(x, w, z);
                        }
                        if (triangle(x, y, w)) {
                            result = result | triangule(x, z, w) | triangule(y, z, w);
                        }
                        if (triangle(x, z, w)) {
                            result = result | triangule(x, y, w) | triangule(y, z, w);
                        }
                        if (triangle(y, z, w)) {
                            result = result | triangule(x, z, w) | triangule(x, y, w);
                        }
                    }
                }
            }
        }
        return result;
    }

    public Quad quad(int desiredCount) {
        for (int a = 0; a < pc; a++) {
            for (int b = a + 1; b < pc; b++) {
                if (!distinct(a, b)) {
                    continue;
                }
                for (int c = b + 1; c < pc; c++) {
                    if (!triangle(a, b, c)) {
                        continue;
                    }
                    for (int d = c + 1; d < pc; d++) {
                        if (!triangle(a, c, d) || !triangle(a, b, d) || !triangle(b, c, d)) {
                            continue;
                        }
                        int cnt = 0;
                        if (intersection(new Pair(a, b), new Pair(c, d)) >= 0) {
                            cnt++;
                        }
                        if (intersection(new Pair(a, c), new Pair(b, d)) >= 0) {
                            cnt++;
                        }
                        if (intersection(new Pair(a, d), new Pair(b, c)) >= 0) {
                            cnt++;
                        }
                        if (cnt == desiredCount) {
                            return new Quad(a, b, c, d);
                        }
                    }
                }
            }
        }
        return null;
    }
}
