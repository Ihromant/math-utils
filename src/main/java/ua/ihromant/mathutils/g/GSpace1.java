package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.IntList;
import ua.ihromant.mathutils.QuickFind;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GSpace1 {
    private final Group group;
    private final GCosets[] cosets;
    private final int[] oBeg;
    private final int[] orbIdx;
    private final int v;
    private final OrbitFilter[] projections;
    private final int k;
    private final int[][] cayley;
    private final FixBS[] differences;
    private final int[] diffMap;
    private final FixBS[][] prImages;
    private final State1[][] statesCache;

    private final int[][] auths;

    public GSpace1(int k, Group group, boolean genAuths, int[][] comps) {
        this(k, group.asTable(), genAuths, sgs(group, comps));
    }

    public GSpace1(int k, Group group, boolean genAuths, int... comps) {
        this(k, group.asTable(), genAuths, sgs(group, comps));
    }

    private static SubGroup[] sgs(Group gr, int[][] comps) {
        Map<Integer, List<SubGroup>> subGroups = gr.asTable().groupedSubGroups();
        //System.out.println(subGroups.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())));
        return Arrays.stream(comps).map(pr -> subGroups.get(pr[0]).get(pr[1])).toArray(SubGroup[]::new);
    }

    private static SubGroup[] sgs(Group gr, int... comps) {
        List<SubGroup> subGroups = gr.asTable().subGroups();
        return Arrays.stream(comps).mapToObj(sz -> subGroups.stream().filter(sg -> sg.order() == sz).findAny().orElseThrow()).toArray(SubGroup[]::new);
    }

    public GSpace1(int k, Group table, boolean genAuths, SubGroup... subs) {
        this.group = table;
        this.k = k;
        this.cosets = new GCosets[subs.length];
        this.oBeg = new int[subs.length];
        int min = 0;
        for (int i = 0; i < subs.length; i++) {
            oBeg[i] = min;
            GCosets conf = new GCosets(subs[i]);
            this.cosets[i] = conf;
            min = min + conf.cosetCount();
        }
        this.v = min;
        this.orbIdx = new int[v];
        for (int i = 0; i < oBeg.length; i++) {
            int beg = oBeg[i];
            int sz = (i == oBeg.length - 1 ? v : oBeg[i + 1]) - beg;
            for (int j = 0; j < sz; j++) {
                orbIdx[beg + j] = i;
            }
        }
        int gOrd = table.order();
        FixBS inter = new FixBS(gOrd);
        inter.set(0, gOrd);
        for (SubGroup sub : subs) {
            for (int x = 0; x < gOrd; x++) {
                FixBS conj = new FixBS(gOrd);
                for (int h : sub.arr()) {
                    conj.set(table.op(table.inv(x), table.op(h, x)));
                }
                inter.and(conj);
            }
        }
        if (inter.cardinality() != 1) {
            throw new IllegalArgumentException("Non empty intersection " + inter);
        }
        this.cayley = new int[gOrd][v];
        for (int g = 0; g < gOrd; g++) {
            for (int x = 0; x < v; x++) {
                cayley[g][x] = applyByDef(g, x);
            }
        }
        QuickFind qf = new QuickFind(v * v);
        this.prImages = new FixBS[v][v];
        for (int x1 = 0; x1 < v; x1++) {
            for (int g = 0; g < gOrd; g++) {
                int gx1 = cayley[g][x1];
                for (int x2 = 0; x2 < v; x2++) {
                    int pair = x1 * v + x2;
                    int gx2 = cayley[g][x2];
                    int gPair = gx1 * v + gx2;
                    qf.union(pair, gPair);
                }
                if (prImages[gx1][x1] == null) {
                    prImages[gx1][x1] = new FixBS(gOrd);
                }
                prImages[gx1][x1].set(g);
            }
        }
        FixBS diagonal = FixBS.of(v * v, IntStream.range(0, v).map(i -> i * v + i).toArray());
        this.differences = qf.components().stream().filter(c -> !c.intersects(diagonal)).toArray(FixBS[]::new);
        this.diffMap = new int[v * v];
        int sz = differences.length;
        this.projections = IntStream.range(0, sz).mapToObj(i -> emptyOf()).toArray(OrbitFilter[]::new);
        for (int i = 0; i < sz; i++) {
            FixBS comp = differences[i];
            for (int val = comp.nextSetBit(0); val >= 0; val = comp.nextSetBit(val + 1)) {
                diffMap[val] = i;
            }
            for (int oi = 0; oi < oBeg.length; oi++) {
                int beg = oBeg[oi];
                for (int pr = comp.nextSetBit(beg * v); pr >= 0 && pr < (beg + 1) * v; pr = comp.nextSetBit(pr + 1)) {
                    projections[i].filters()[oi].set(pr % v);
                }
                projections[i].filters()[oi].clear(beg);
            }
        }
        for (int i = 0; i < v; i++) {
            diffMap[i * v + i] = -1;
        }

        this.statesCache = new State1[v][v];
        OrbitFilter empty = emptyOf();
        for (int f = 0; f < v; f++) {
            for (int s = f + 1; s < v; s++) {
                statesCache[f][s] = new State1(FixBS.of(v, f), FixBS.of(gOrd, 0), new FixBS(sz), new IntList[sz], 1)
                        .acceptElem(this, empty, s);
            }
        }

        if (genAuths) {
            this.auths = genAuthsNew();
        } else {
            this.auths = new int[0][];
        }
    }

    private int[][] genAuthsNew() {
        Set<int[]> sortedAuths = new TreeSet<>(Combinatorics::compareArr);
        int[][] grAuths = group.auth();
        int[][] baseMappings = generateBaseMappings();
        for (int[] auth : grAuths) {
            ex: for (int[] baseMapping : baseMappings) {
                int[] mapping = baseMapping.clone();
                for (int g = 0; g < group.order(); g++) {
                    for (int t : oBeg) {
                        int from = apply(g, t);
                        int to = apply(auth[g], baseMapping[t]);
                        int prev = mapping[from];
                        if (prev < 0) {
                            mapping[from] = to;
                        } else {
                            if (prev != to) {
                                continue ex;
                            }
                        }
                    }
                }
                sortedAuths.add(mapping);
            }
        }
        return sortedAuths.toArray(int[][]::new);
    }

    private int[][] generateBaseMappings() {
        List<int[]> result = new ArrayList<>();
        FixBS availableOrbits = new FixBS(cosets.length);
        availableOrbits.set(0, cosets.length);
        int[] base = new int[v];
        Arrays.fill(base, -1);
        recur(base, 0, availableOrbits, result::add);
        return result.toArray(int[][]::new);
    }

    private void recur(int[] curr, int orbit, FixBS availableOrbits, Consumer<int[]> cons) {
        if (orbit == cosets.length) {
            cons.accept(curr);
            return;
        }
        GCosets from = cosets[orbit];
        int oLen = from.cosetCount();
        int start = oBeg[orbit];
        for (int orb = availableOrbits.nextSetBit(0); orb >= 0; orb = availableOrbits.nextSetBit(orb + 1)) {
            GCosets coset = cosets[orb];
            if (coset.cosetCount() != oLen) {
                continue;
            }
            FixBS nextAvailable = availableOrbits.copy();
            nextAvailable.clear(orb);
            for (int i = 0; i < oLen; i++) {
                int[] nextCurr = curr.clone();
                nextCurr[start] = oBeg[orb] + i;
                recur(nextCurr, orbit + 1, nextAvailable, cons);
            }
        }
    }

    public int v() {
        return v;
    }

    public int k() {
        return k;
    }

    public int gOrd() {
        return group.order();
    }

    public int authLength() {
        return auths.length;
    }

    public int[] auth(int i) {
        return auths[i];
    }

    public OrbitFilter projection(int i) {
        return projections[i];
    }

    public FixBS[] differences() {
        return differences;
    }

    public FixBS difference(int idx) {
        return differences[idx];
    }

    public int diffIdx(int xy) {
        return diffMap[xy];
    }

    public FixBS prImage(int from, int to) {
        return prImages[to][from];
    }

    public State1 forInitial(int fst, int snd) {
        return statesCache[fst][snd];
    }

    public int oBeg(int oIdx) {
        return oBeg[oIdx];
    }

    public int orbitCount() {
        return oBeg.length;
    }

    private int orbIdx(int x) {
        return orbIdx[x];
    }

    private int applyByDef(int g, int x) {
        int idx = orbIdx(x);
        int g1 = xToG(x);
        return gToX(group.op(g, g1), idx);
    }

    private int xToG(int x) {
        int idx = orbIdx[x];
        int xCos = x - oBeg[idx];
        return cosets[idx].xToG(xCos);
    }

    private int gToX(int g, int orbIdx) {
        return cosets[orbIdx].gToX(g) + oBeg[orbIdx];
    }

    public OrbitFilter emptyOf() {
        int ol = oBeg.length;
        FixBS[] arr = new FixBS[ol];
        for (int i = 0; i < ol; i++) {
            arr[i] = FixBS.of(v, oBeg[i]);
        }
        return new OrbitFilter(arr);
    }

    public int apply(int g, int x) {
        return cayley[g][x];
    }

    public Stream<int[]> blocks(FixBS block) {
        Set<FixBS> set = new HashSet<>(2 * group.order());
        List<int[]> res = new ArrayList<>();
        for (int g = 0; g < group.order(); g++) {
            FixBS fbs = new FixBS(v);
            for (int x = block.nextSetBit(0); x >= 0; x = block.nextSetBit(x + 1)) {
                fbs.set(apply(g, x));
            }
            if (set.add(fbs)) {
                res.add(fbs.toArray());
            }
        }
        return res.stream();
    }

    public boolean minimal(FixBS block) {
        return Arrays.stream(auths).parallel().allMatch(auth -> {
            FixBS alt = new FixBS(v);
            for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                alt.set(auth[el]);
            }
            return alt.compareTo(block) >= 0;
        });
    }

    public boolean twoMinimal(State1[] states) {
        for (int[] auth : auths) {
            FixBS[] mapped = new FixBS[states.length];
            for (int i = 0; i < states.length; i++) {
                FixBS bl = states[i].block();
                FixBS block = new FixBS(v);
                for (int el = bl.nextSetBit(0); el >= 0; el = bl.nextSetBit(el + 1)) {
                    block.set(auth[el]);
                }
                mapped[i] = block;
            }
            if (mapped[0].compareTo(mapped[1]) > 0) {
                FixBS tmp = mapped[1];
                mapped[1] = mapped[0];
                mapped[0] = tmp;
            }
            int cmp = mapped[0].compareTo(states[0].block());
            if (cmp < 0 || cmp == 0 && mapped[1].compareTo(states[1].block()) < 0) {
                return false;
            }
        }
        return true;
    }

    public FixBS minimalBlock(FixBS block) {
        FixBS result = block;
        for (int[] auth : auths) {
            FixBS alt = new FixBS(v);
            for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                alt.set(auth[el]);
            }
            if (alt.compareTo(result) < 0) {
                result = alt;
            }
        }
        return result;
    }

    public FixBS[] minimalBlocks(FixBS[] blocks) {
        FixBS[] result = blocks;
        ex: for (int[] auth : auths) {
            FixBS[] altBlocks = new FixBS[blocks.length];
            for (int i = 0; i < blocks.length; i++) {
                FixBS block = blocks[i];
                FixBS alt = new FixBS(v);
                for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                    alt.set(auth[el]);
                }
                altBlocks[i] = alt;
            }
            Arrays.sort(altBlocks);
            for (int i = 0; i < blocks.length; i++) {
                int cmp = altBlocks[i].compareTo(result[i]);
                if (cmp < 0) {
                    result = altBlocks;
                    break;
                }
                if (cmp > 0) {
                    continue ex;
                }
            }
        }
        return result;
    }

    public boolean minimal(State1[] states) {
        FixBS[] blocks = new FixBS[states.length];
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = states[i].block();
        }
        ex: for (int[] auth : auths) {
            FixBS[] altBlocks = new FixBS[blocks.length];
            for (int i = 0; i < blocks.length; i++) {
                FixBS block = blocks[i];
                FixBS alt = new FixBS(v);
                for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                    alt.set(auth[el]);
                }
                altBlocks[i] = alt;
            }
            Arrays.sort(altBlocks);
            for (int i = 0; i < blocks.length; i++) {
                int cmp = altBlocks[i].compareTo(blocks[i]);
                if (cmp < 0) {
                    return false;
                }
                if (cmp > 0) {
                    continue ex;
                }
            }
        }
        return true;
    }

    public boolean parMinimal(State1[] states) {
        FixBS[] blocks = new FixBS[states.length];
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = states[i].block();
        }
        return Arrays.stream(auths).parallel().allMatch(auth -> {
            FixBS[] altBlocks = new FixBS[blocks.length];
            for (int i = 0; i < blocks.length; i++) {
                FixBS block = blocks[i];
                FixBS alt = new FixBS(v);
                for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                    alt.set(auth[el]);
                }
                altBlocks[i] = alt;
            }
            Arrays.sort(altBlocks);
            for (int i = 0; i < blocks.length; i++) {
                int cmp = altBlocks[i].compareTo(blocks[i]);
                if (cmp < 0) {
                    return false;
                }
                if (cmp > 0) {
                    return true;
                }
            }
            return true;
        });
    }
}
