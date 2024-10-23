package ua.ihromant.mathutils;

public class RelationLiner {
    private final boolean[][][] relation;

    public RelationLiner(int pointCount, int[][] lines) {
        this.relation = new boolean[pointCount][pointCount][pointCount];
        for (int[] line : lines) {
            for (int i = 0; i < lines.length; i++) {
                for (int j = i; j < lines.length; j++) {
                    for (int k = j; k < lines.length; k++) {
                        relation[line[i]][line[j]][line[k]] = true;
                        relation[line[i]][line[k]][line[j]] = true;
                        relation[line[j]][line[i]][line[k]] = true;
                        relation[line[j]][line[k]][line[i]] = true;
                        relation[line[k]][line[i]][line[j]] = true;
                        relation[line[k]][line[j]][line[i]] = true;
                    }
                }
            }
        }
    }

    public boolean collinear(int... pts) {
        if (pts.length < 3) {
            return true;
        }
        int fst = pts[0];
        for (int i = 1; i < pts.length; i++) {
            int snd = pts[i];
            if (fst != snd) {
                boolean[] line = relation[fst][snd];
                for (int j = i + 1; j < pts.length; j++) {
                    if (!line[pts[j]]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return true;
    }

    public int intersection(Pair fst, Pair snd) {
        boolean[] l1 = relation[fst.f()][fst.s()];
        boolean[] l2 = relation[snd.f()][snd.s()];
        for (int i = 0; i < relation.length; i++) {
            if (l1[i] && l2[i]) {
                return i;
            }
        }
        return -1;
    }
}
