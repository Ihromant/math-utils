package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.plane.NumeratedAffinePlane;
import ua.ihromant.mathutils.util.FixBS;
import ua.ihromant.mathutils.vector.TranslationPlaneTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;

public class ScalarTest {
    @Test
    public void testAffineScalars() throws IOException {
        String name = "dhall9";
        int order = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + order + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            int line = TranslationPlaneTest.findTranslationLine(proj);
            if (line < 0) {
                throw new IllegalArgumentException("Not translation");
            } else {
                System.out.println(name + " dropped line " + line);
            }
            NumeratedAffinePlane aff = new NumeratedAffinePlane(proj, line);
            int lc = aff.lineCount();
            int[][] triples = aff.triples();
            QuickFind find = new QuickFind(triples.length * lc);
            for (int l1 = 0; l1 < lc; l1++) {
                for (int l2 = l1 + 1; l2 < lc; l2++) {
                    int inter = aff.intersection(l1, l2);
                    if (inter < 0) {
                        continue;
                    }
                    int[] fPts = aff.line(l1);
                    int[] sPts = aff.line(l2);
                    int fst = fPts[0] == inter ? fPts[1] : fPts[0];
                    for (int i = 0; i < order; i++) {
                        int snd = sPts[i];
                        if (snd == inter) {
                            continue;
                        }
                        int[] map = new int[order];
                        int par = aff.line(fst, snd);
                        for (int j = 0; j < order; j++) {
                            int aSnd = sPts[j];
                            if (aSnd == inter) {
                                map[j] = aff.idxOf(l1, inter);
                                continue;
                            }
                            int parInter = aff.intersection(l1, aff.parallel(par, aSnd));
                            map[j] = aff.idxOf(l1, parInter);
                        }
                        for (int[] tr : triples) {
                            int p = l2 * triples.length + aff.trIdx(tr[0], tr[1], tr[2]);
                            int q = l1 * triples.length + aff.trIdx(map[tr[0]], map[tr[1]], map[tr[2]]);
                            find.union(p, q);
                        }
                    }
                }
            }
            System.out.println(find.components().stream().map(FixBS::cardinality).toList());
        }
    }

    @Test
    public void testProjectiveScalars() throws IOException {
        String name = "dhall9";
        int order = 9;
        int[][] quads = new int[(order + 1) * order * (order - 1) * (order - 2)][4];
        int[][][][] quadRev = new int[order + 1][order + 1][order + 1][order + 1];
        int idx = 0;
        for (int i = 0; i <= order; i++) {
            for (int j = 0; j <= order; j++) {
                if (i == j) {
                    continue;
                }
                for (int k = 0; k <= order; k++) {
                    if (i == k || j == k) {
                        continue;
                    }
                    for (int l = 0; l <= order; l++) {
                        if (i == l || j == l || k == l) {
                            continue;
                        }
                        quads[idx][0] = i;
                        quads[idx][1] = j;
                        quads[idx][2] = k;
                        quads[idx][3] = l;
                        quadRev[i][j][k][l] = idx++;
                    }
                }
            }
        }
        try (InputStream is = getClass().getResourceAsStream("/proj" + order + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            Liner proj = BatchAffineTest.readProj(br);
            int lc = proj.lineCount();
            int[][] ptIdxes = new int[lc][lc];
            for (int l = 0; l < lc; l++) {
                Arrays.fill(ptIdxes[l], -1);
                int[] pts = proj.line(l);
                for (int i = 0; i < pts.length; i++) {
                    ptIdxes[l][pts[i]] = i;
                }
            }
            QuickFind find = new QuickFind(quads.length * lc);
            for (int l1 = 0; l1 < lc; l1++) {
                for (int l2 = l1 + 1; l2 < lc; l2++) {
                    for (int pt = 0; pt < lc; pt++) {
                        if (proj.flag(l1, pt) || proj.flag(l2, pt)) {
                            continue;
                        }
                        int[] sPts = proj.line(l2);
                        int[] map = new int[order + 1];
                        for (int i = 0; i <= order; i++) {
                            int snd = sPts[i];
                            int fst = proj.intersection(l1, proj.line(pt, snd));
                            map[i] = ptIdxes[l1][fst];
                        }
                        for (int[] quad : quads) {
                            int p = l2 * quads.length + quadRev[quad[0]][quad[1]][quad[2]][quad[3]];
                            int q = l1 * quads.length + quadRev[map[quad[0]]][map[quad[1]]][map[quad[2]]][map[quad[3]]];
                            find.union(p, q);
                        }
                    }
                }
            }
            System.out.println(find.components().stream().map(FixBS::cardinality).toList());
        }
    }
}
