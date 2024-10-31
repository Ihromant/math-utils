package ua.ihromant.mathutils;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;

public class FuzzyLiner {
    private int pointCount;
    private FixBS s;
    private FixBS d;
    private FixBS l;
    private FixBS t;

    public FuzzyLiner(int pointCount, int[][] lines) {
//        this.relation = new int[pointCount][pointCount][pointCount];
//        this.distinction = new int[pointCount][pointCount];
//        for (int i = 0; i < pointCount; i++) {
//            Arrays.fill(distinction[i], -1);
//            distinction[i][i] = 1;
//        }
//        for (int[] line : lines) {
//            for (int i = 0; i < lines.length; i++) {
//                for (int j = i; j < lines.length; j++) {
//                    for (int k = j; k < lines.length; k++) {
//                        relation[line[i]][line[j]][line[k]] = 1;
//                        relation[line[i]][line[k]][line[j]] = 1;
//                        relation[line[j]][line[i]][line[k]] = 1;
//                        relation[line[j]][line[k]][line[i]] = 1;
//                        relation[line[k]][line[i]][line[j]] = 1;
//                        relation[line[k]][line[j]][line[i]] = 1;
//                    }
//                }
//            }
//        }
    }

//    public int collinear(int a, int b, int c) {
//        return relation[a][b][c];
//    }

//    public int intersection(Pair fst, Pair snd) {
//        int[] l1 = relation[fst.f()][fst.s()];
//        int[] l2 = relation[snd.f()][snd.s()];
//        for (int i = 0; i < relation.length; i++) {
//            if (l1[i] == 1 && l2[i] == 1) {
//                return i;
//            }
//        }
//        return -1;
//    }
}
