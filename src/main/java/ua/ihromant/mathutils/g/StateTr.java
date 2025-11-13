package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.Objects;

public record StateTr(FixBS block, FixBS stabilizer, FixBS diffSet, int[][] diffs, int size) {
    public static StateTr fromBlock(GSpaceTr space, FixBS block) {
        int fst = block.nextSetBit(0);
        int snd = block.nextSetBit(fst + 1);
        int trd = block.nextSetBit(snd + 1);
        StateTr result = space.forInitial(fst, snd, trd);
        for (int el = block.nextSetBit(trd + 1); el >= 0; el = block.nextSetBit(el + 1)) {
            if (result.block().get(el)) {
                continue;
            }
            result = Objects.requireNonNull(result.acceptElem(space, space.emptyFilter(), el));
        }
        return result;
    }

    public StateTr acceptElem(GSpaceTr gSpace, FixBS globalFilter, int val) {
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
                    int abx = gSpace.to(a, b, x);
                    int bax = gSpace.to(b, a, x);
                    int axb = gSpace.to(a, x, b);
                    int bxa = gSpace.to(b, x, a);
                    int xab = gSpace.to(x, a, b);
                    int xba = gSpace.to(x, b, a);
                    if (globalFilter.get(abx) || globalFilter.get(bax) || globalFilter.get(axb) || globalFilter.get(bxa)
                            || globalFilter.get(xab) || globalFilter.get(xba)) {
                        return null;
                    }
                    stabExt.or(gSpace.preImage(a, a).intersection(gSpace.preImage(b, b)).intersection(gSpace.preImage(x, x)));

                    int compAbx = gSpace.diffIdx(abx);
                    int[] exAbx = newDiffs[compAbx];
                    if (exAbx != null) {
                        for (int diff : exAbx) {
                            int fst = diff / v / v;
                            int snd = diff / v % v;
                            int trd = diff % v;
                            stabExt.or(gSpace.preImage(a, fst).intersection(gSpace.preImage(b, snd)).intersection(gSpace.preImage(x, trd)));
                        }
                        int[] newExDiffs = Arrays.copyOf(exAbx, exAbx.length + 1);
                        newExDiffs[exAbx.length] = abx;
                        newDiffs[compAbx] = newExDiffs;
                    } else {
                        newDiffs[compAbx] = new int[]{abx};
                        newDiffSet.set(compAbx);
                    }

                    int compBax = gSpace.diffIdx(bax);
                    int[] exBax = newDiffs[compBax];
                    if (exBax != null) {
                        for (int diff : exBax) {
                            int fst = diff / v / v;
                            int snd = diff / v % v;
                            int trd = diff % v;
                            stabExt.or(gSpace.preImage(b, fst).intersection(gSpace.preImage(a, snd)).intersection(gSpace.preImage(x, trd)));
                        }
                        int[] newExDiffs = Arrays.copyOf(exBax, exBax.length + 1);
                        newExDiffs[exBax.length] = bax;
                        newDiffs[compBax] = newExDiffs;
                    } else {
                        newDiffs[compBax] = new int[]{bax};
                        newDiffSet.set(compBax);
                    }

                    int compAxb = gSpace.diffIdx(axb);
                    int[] exAxb = newDiffs[compAxb];
                    if (exAxb != null) {
                        for (int diff : exAxb) {
                            int fst = diff / v / v;
                            int snd = diff / v % v;
                            int trd = diff % v;
                            stabExt.or(gSpace.preImage(a, fst).intersection(gSpace.preImage(x, snd)).intersection(gSpace.preImage(b, trd)));
                        }
                        int[] newExDiffs = Arrays.copyOf(exAxb, exAxb.length + 1);
                        newExDiffs[exAxb.length] = axb;
                        newDiffs[compAxb] = newExDiffs;
                    } else {
                        newDiffs[compAxb] = new int[]{axb};
                        newDiffSet.set(compAxb);
                    }

                    int compBxa = gSpace.diffIdx(bxa);
                    int[] exBxa = newDiffs[compBxa];
                    if (exBxa != null) {
                        for (int diff : exBxa) {
                            int fst = diff / v / v;
                            int snd = diff / v % v;
                            int trd = diff % v;
                            stabExt.or(gSpace.preImage(b, fst).intersection(gSpace.preImage(x, snd)).intersection(gSpace.preImage(a, trd)));
                        }
                        int[] newExDiffs = Arrays.copyOf(exBxa, exBxa.length + 1);
                        newExDiffs[exBxa.length] = bxa;
                        newDiffs[compBxa] = newExDiffs;
                    } else {
                        newDiffs[compBxa] = new int[]{bxa};
                        newDiffSet.set(compBxa);
                    }

                    int compXab = gSpace.diffIdx(xab);
                    int[] exXab = newDiffs[compXab];
                    if (exXab != null) {
                        for (int diff : exXab) {
                            int fst = diff / v / v;
                            int snd = diff / v % v;
                            int trd = diff % v;
                            stabExt.or(gSpace.preImage(x, fst).intersection(gSpace.preImage(a, snd)).intersection(gSpace.preImage(b, trd)));
                        }
                        int[] newExDiffs = Arrays.copyOf(exXab, exXab.length + 1);
                        newExDiffs[exXab.length] = xab;
                        newDiffs[compXab] = newExDiffs;
                    } else {
                        newDiffs[compXab] = new int[]{xab};
                        newDiffSet.set(compXab);
                    }

                    int compXba = gSpace.diffIdx(xba);
                    int[] exXba = newDiffs[compXba];
                    if (exXba != null) {
                        for (int diff : exXba) {
                            int fst = diff / v / v;
                            int snd = diff / v % v;
                            int trd = diff % v;
                            stabExt.or(gSpace.preImage(x, fst).intersection(gSpace.preImage(b, snd)).intersection(gSpace.preImage(a, trd)));
                        }
                        int[] newExDiffs = Arrays.copyOf(exXba, exXba.length + 1);
                        newExDiffs[exXba.length] = xba;
                        newDiffs[compXba] = newExDiffs;
                    } else {
                        newDiffs[compXba] = new int[]{xba};
                        newDiffSet.set(compXba);
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
        return new StateTr(newBlock, newStabilizer, newDiffSet, newDiffs, sz);
    }

    public void updateFilter(FixBS filter, GSpaceTr space) {
        for (int i = diffSet.nextSetBit(0); i >= 0; i = diffSet.nextSetBit(i + 1)) {
            filter.or(space.difference(i));
        }
    }
}
