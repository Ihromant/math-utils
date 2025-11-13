package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;

public record StateQuad(FixBS block, FixBS stabilizer, FixBS diffSet, int[][] diffs, int size) {
    private static final int[][] perms = Combinatorics.permutations(new int[]{0, 1, 2, 3}).toArray(int[][]::new);

    public StateQuad acceptElem(GSpaceQuad gSpace, FixBS globalFilter, int val) {
        int v = gSpace.v();
        int k = gSpace.k();
        FixBS newBlock = block.copy();
        FixBS queue = new FixBS(v);
        queue.set(val);
        int sz = size;
        FixBS newStabilizer = stabilizer.copy();
        FixBS newDiffSet = diffSet.copy();
        int[][] newDiffs = diffs.clone();
        while (!queue.isEmpty()) {
            if (++sz > k) {
                return null;
            }
            int x = queue.nextSetBit(0);
            if (x < val) {
                return null;
            }
            FixBS stabExt = new FixBS(gSpace.gOrd());
            for (int a = block.nextSetBit(0); a >= 0; a = newBlock.nextSetBit(a + 1)) {
                for (int b = newBlock.nextSetBit(a + 1); b >= 0; b = newBlock.nextSetBit(b + 1)) {
                    for (int c = newBlock.nextSetBit(b + 1); c >= 0; c = newBlock.nextSetBit(c + 1)) {
                        stabExt.or(gSpace.preImage(a, a).intersection(gSpace.preImage(b, b)).intersection(gSpace.preImage(c, c))
                                .intersection(gSpace.preImage(x, x)));
                        int[] arr = new int[]{a, b, c, x};
                        for (int[] perm : perms) {
                            int fst = arr[perm[0]];
                            int snd = arr[perm[1]];
                            int trd = arr[perm[2]];
                            int four = arr[perm[3]];
                            int cmb = gSpace.to(fst, snd, trd, four);
                            if (globalFilter.get(cmb)) {
                                return null;
                            }
                            int compComb = gSpace.diffIdx(cmb);
                            int[] exComp = newDiffs[compComb];
                            if (exComp != null) {
                                for (int diff : exComp) {
                                    int dFst = diff / v / v / v;
                                    int dSnd = diff / v / v % v;
                                    int dTrd = diff / v % v;
                                    int dFour = diff % v;
                                    stabExt.or(gSpace.preImage(fst, dFst).intersection(gSpace.preImage(snd, dSnd))
                                            .intersection(gSpace.preImage(trd, dTrd)).intersection(gSpace.preImage(four, dFour)));
                                }
                                int[] newExDiffs = Arrays.copyOf(exComp, exComp.length + 1);
                                newExDiffs[exComp.length] = cmb;
                                newDiffs[compComb] = newExDiffs;
                            } else {
                                newDiffs[compComb] = new int[]{cmb};
                                newDiffSet.set(compComb);
                            }
                        }
                    }
                }
            }
            newBlock.set(x);
            stabExt.andNot(newStabilizer);
            for (int st = newStabilizer.nextSetBit(1); st >= 0; st = newStabilizer.nextSetBit(st + 1)) {
                queue.set(gSpace.apply(st, x));
            }
            for (int st = stabExt.nextSetBit(1); st >= 0; st = stabExt.nextSetBit(st + 1)) {
                for (int b = newBlock.nextSetBit(0); b >= 0; b = newBlock.nextSetBit(b + 1)) {
                    queue.set(gSpace.apply(st, b));
                }
            }
            newStabilizer.or(stabExt);
            queue.andNot(newBlock);
        }
        return new StateQuad(newBlock, newStabilizer, newDiffSet, newDiffs, sz);
    }

    public void updateFilter(FixBS filter, GSpaceQuad space) {
        for (int i = diffSet.nextSetBit(0); i >= 0; i = diffSet.nextSetBit(i + 1)) {
            filter.or(space.difference(i));
        }
    }
}
