package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApplicatorTest {
    private static class SubGroupApplicator {
        private final Group gr;
        private final FixBS[] cosets;
        private final int[] idx;

        public SubGroupApplicator(SubGroup sg) {
            this.gr = sg.group();
            Set<FixBS> set = new HashSet<>();
            for (int i = 0; i < gr.order(); i++) {
                FixBS coset = new FixBS(gr.order());
                for (int el : sg.arr()) {
                    coset.set(gr.op(i, el));
                }
                set.add(coset);
            }
            this.cosets = set.toArray(FixBS[]::new);
            Arrays.sort(cosets);
            this.idx = new int[gr.order()];
            for (int i = 0; i < cosets.length; i++) {
                FixBS coset = cosets[i];
                for (int el = coset.nextSetBit(0); el >= 0; el = coset.nextSetBit(el + 1)) {
                    idx[el] = i;
                }
            }
        }

        private int apply(int g, int x) {
            int min = cosets[x].nextSetBit(0);
            return idx[gr.op(g, min)];
        }
    }

    private static class OrbitApplicator {
        private final SubGroupApplicator[] subs;
        private final int[] oBeg;
        private final int v;

        public OrbitApplicator(SubGroup[] subs) {
            this.subs = new SubGroupApplicator[subs.length];
            this.oBeg = new int[subs.length];
            int min = 0;
            for (int i = 0; i < subs.length; i++) {
                oBeg[i] = min;
                SubGroupApplicator conf = new SubGroupApplicator(subs[i]);
                this.subs[i] = conf;
                min = min + conf.cosets.length;
            }
            this.v = min;
        }

        public int apply(int g, int x) {
            int idx = Arrays.binarySearch(oBeg, x);
            if (idx < 0) {
                idx = -idx - 2;
            }
            int min = oBeg[idx];
            SubGroupApplicator conf = subs[idx];
            return conf.apply(g, x - min) + min;
        }
    }

    private record State(FixBS block, FixBS stabilizer, FixBS filter, FixBS selfDiff, int size) {
        private State acceptElem(Group group, FixBS globalFilter, int val, int v, int k) {
            FixBS newBlock = block.copy();
            FixBS queue = new FixBS(v);
            queue.set(val);
            int sz = size;
            FixBS newSelfDiff = selfDiff.copy();
            FixBS newStabilizer = stabilizer.copy();
            FixBS newFilter = filter.copy();
            while (!queue.isEmpty()) {
                if (++sz > k) {
                    return null;
                }
                int x = queue.nextSetBit(0);
                if (x < val) {
                    return null;
                }
                FixBS stabExt = new FixBS(v);
                FixBS selfDiffExt = new FixBS(v);
                for (int b = newBlock.nextSetBit(0); b >= 0; b = newBlock.nextSetBit(b + 1)) {
                    int bInv = group.inv(b);
                    int xInv = group.inv(x);
                    int xb = group.op(x, bInv);
                    selfDiffExt.set(xb);
                    if (newSelfDiff.get(xb) || newBlock.get(group.op(xb, x))) {
                        stabExt.set(xb);
                    }
                    int bx = group.op(b, xInv);
                    if (newSelfDiff.get(bx)) {
                        stabExt.set(bx);
                    }
                    selfDiffExt.set(bx);
                    int diff = group.op(bInv, x);
                    if (globalFilter.get(diff)) {
                        return null;
                    }
                    int outDiff = group.op(xInv, b);
                    newFilter.set(diff);
                    if (globalFilter.get(outDiff)) {
                        return null;
                    }
                    newFilter.set(outDiff);
                }
                newBlock.set(x);
                stabExt.andNot(newStabilizer);
                for (int st = newStabilizer.nextSetBit(1); st >= 0; st = newStabilizer.nextSetBit(st + 1)) {
                    queue.set(group.op(st, x));
                }
                for (int st = stabExt.nextSetBit(1); st >= 0; st = stabExt.nextSetBit(st + 1)) {
                    for (int b = newBlock.nextSetBit(0); b >= 0; b = newBlock.nextSetBit(b + 1)) {
                        queue.set(group.op(st, b));
                    }
                }
                newStabilizer.or(stabExt);
                newSelfDiff.or(selfDiffExt);
                queue.andNot(newBlock);
            }
            return new State(newBlock, newStabilizer, newFilter, newSelfDiff, sz);
        }

        private State acceptElem(SubGroupApplicator app, FixBS globalFilter, int val, int v, int k) {
            FixBS newBlock = block.copy();
            FixBS queue = new FixBS(v);
            queue.set(val);
            int sz = size;
            FixBS newSelfDiff = selfDiff.copy();
            FixBS newStabilizer = stabilizer.copy();
            FixBS newFilter = filter.copy();
            while (!queue.isEmpty()) {
                if (++sz > k) {
                    return null;
                }
                int x = queue.nextSetBit(0);
                if (x < val) {
                    return null;
                }
                FixBS stabExt = new FixBS(v);
                FixBS selfDiffExt = new FixBS(v);
//                for (int b = newBlock.nextSetBit(0); b >= 0; b = newBlock.nextSetBit(b + 1)) {
//                    int bInv = group.inv(b);
//                    int xInv = group.inv(x);
//                    int xb = group.op(x, bInv);
//                    selfDiffExt.set(xb);
//                    if (newSelfDiff.get(xb) || newBlock.get(group.op(xb, x))) {
//                        stabExt.set(xb);
//                    }
//                    int bx = group.op(b, xInv);
//                    if (newSelfDiff.get(bx)) {
//                        stabExt.set(bx);
//                    }
//                    selfDiffExt.set(bx);
//                    int diff = group.op(bInv, x);
//                    if (globalFilter.get(diff)) {
//                        return null;
//                    }
//                    int outDiff = group.op(xInv, b);
//                    newFilter.set(diff);
//                    if (globalFilter.get(outDiff)) {
//                        return null;
//                    }
//                    newFilter.set(outDiff);
//                }
//                newBlock.set(x);
//                stabExt.andNot(newStabilizer);
//                for (int st = newStabilizer.nextSetBit(1); st >= 0; st = newStabilizer.nextSetBit(st + 1)) {
//                    queue.set(group.op(st, x));
//                }
//                for (int st = stabExt.nextSetBit(1); st >= 0; st = stabExt.nextSetBit(st + 1)) {
//                    for (int b = newBlock.nextSetBit(0); b >= 0; b = newBlock.nextSetBit(b + 1)) {
//                        queue.set(group.op(st, b));
//                    }
//                }
                newStabilizer.or(stabExt);
                newSelfDiff.or(selfDiffExt);
                queue.andNot(newBlock);
            }
            return new State(newBlock, newStabilizer, newFilter, newSelfDiff, sz);
        }
    }

    @Test
    public void testApplicator() {
        Group g = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        SubGroup tr = new SubGroup(g, FixBS.of(g.order(), 0));
        SubGroup small = new SubGroup(g, FixBS.of(g.order(), 0, 1, 2));
        FixBS f = new FixBS(g.order());
        f.set(0, g.order());
        SubGroup fix = new SubGroup(g, f);
        OrbitApplicator applicator = new OrbitApplicator(new SubGroup[]{tr, small, small, fix});
        assertEquals(66, applicator.v);
        for (int x = 0; x < applicator.v; x++) {
            for (int j = 0; j < g.order(); j++) {
                System.out.println(j + "*" + x + "=" + applicator.apply(j, x));
            }
        }
    }
}
