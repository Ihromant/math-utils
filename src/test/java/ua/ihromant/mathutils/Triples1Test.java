package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.g.GSpaceTr;
import ua.ihromant.mathutils.g.StateTr;
import ua.ihromant.mathutils.group.GapGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Triples1Test {
    @Test
    public void tst() throws IOException {
        int gs = 72;
        int k = 6;
        int[] orbits = new int[]{18, 24};
        int c = GroupIndex.groupCount(gs);
        System.out.println(c);
        for (int j = 1; j <= c; j++) {
            System.out.println("Reading SmallGroup(" + gs + "," + j + ")");
            GapGroup gg = GroupIndex.gapGroup(gs, j);
            Group group = gg.group();
            List<int[][]> configs = ApplicatorTest.configs(group, orbits);
            for (int[][] config : configs) {
                GSpaceTr space;
                try {
                    space = new GSpaceTr(k, group, false, config);
                } catch (IllegalArgumentException e) {
                    System.out.println("Not empty");
                    continue;
                }
                int v = space.v();
                FixBS evenDiffs = space.evenDiffs();
                StateTr[] stab = space.getStabilized();
                FixBS notExcluded = evenDiffs.copy();
                Arrays.stream(stab).forEach(st -> notExcluded.andNot(st.diffSet()));
                System.out.println(GroupIndex.identify(gg) + " " + v + " " + k + " configs: "
                        + Arrays.deepToString(config) + " stab: " + stab.length + " diffs: " + evenDiffs.cardinality());
            }
        }
    }
}
