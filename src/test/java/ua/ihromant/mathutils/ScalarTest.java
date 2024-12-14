package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.plane.NumeratedAffinePlane;
import ua.ihromant.mathutils.util.FixBS;
import ua.ihromant.mathutils.vector.TranslationPlaneTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
}
