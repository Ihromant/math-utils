package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.IntList;
import ua.ihromant.mathutils.QuickFind;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GSpace {
    private final Group group;
    private final GCosets[] cosets;
    private final int[] oBeg;
    private final int[] orbIdx;
    private final int v;
    private final FixBS empty;
    private final FixBS emptyFilter;
    private final int k;
    private final int[][] cayley;
    private final List<Relation> relations;
    private final int[] diffMap;
    private final List<Map<Integer, FixBS>> preImages;
    private final State[][] statesCache;

    private final int[][] auths;

    public GSpace(int k, Group group, boolean genAuths, int... comps) {
        this.k = k;
        Group table = group.asTable();
        this.group = table;
        List<SubGroup> subGroups = table.subGroups();
        SubGroup[] subs = Arrays.stream(comps).mapToObj(sz -> subGroups.stream().filter(sg -> sg.order() == sz).findAny().orElseThrow()).toArray(SubGroup[]::new);
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
        this.empty = new FixBS(v);
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
        this.preImages = IntStream.range(0, v * v).<Map<Integer, FixBS>>mapToObj(i -> new HashMap<>()).toList();
        for (int x1 = 0; x1 < v; x1++) {
            for (int x2 = 0; x2 < v; x2++) {
                int pair = x1 * v + x2;
                for (int g = 0; g < gOrd; g++) {
                    int gx1 = cayley[g][x1];
                    int gx2 = cayley[g][x2];
                    int gPair = gx1 * v + gx2;
                    qf.union(pair, gPair);
                    preImages.get(gPair).computeIfAbsent(pair, key -> new FixBS(gOrd)).set(g);
                }
            }
        }
        FixBS diagonal = FixBS.of(v * v, IntStream.range(0, v).map(i -> i * v + i).toArray());
        this.relations = qf.components().stream().filter(c -> !c.intersects(diagonal)).map(c -> new Relation(v, c)).toList();
        this.diffMap = new int[v * v];
        int sz = relations.size();
        for (int i = 0; i < sz; i++) {
            FixBS comp = difference(i);
            for (int val = comp.nextSetBit(0); val >= 0; val = comp.nextSetBit(val + 1)) {
                diffMap[val] = i;
            }
        }
        for (int i = 0; i < v; i++) {
            diffMap[i * v + i] = -1;
        }

        this.statesCache = new State[v][v];
        this.emptyFilter = new FixBS(v * v);
        for (int i = 0; i < v; i++) {
            emptyFilter.set(i * v + i);
        }
        for (int f = 0; f < v; f++) {
            for (int s = f + 1; s < v; s++) {
                statesCache[f][s] = new State(FixBS.of(v, f), FixBS.of(gOrd, 0), new FixBS(sz), new IntList[sz], 1)
                        .acceptElem(this, emptyFilter, s);
            }
        }

        if (genAuths) {
            Set<int[]> sortedAuths = Collections.synchronizedSet(new TreeSet<>(Combinatorics::compareArr));
            PartialMap emMap = new PartialMap(empty, empty, new int[v]);
            IntStream.range(0, v).parallel().forEach(fst -> {
                PartialMap init = emMap.copy();
                init.set(0, fst);
                find(sortedAuths, init);
            });
            this.auths = sortedAuths.toArray(int[][]::new);
        } else {
            this.auths = new int[0][];
        }
    }

    private void find(Set<int[]> auths, PartialMap currMap) {
        FixBS keys = currMap.keys();
        int nextKey = keys.nextClearBit(0);
        if (nextKey == v) {
            if (autToDiffAut(currMap.map) != null) {
                auths.add(currMap.map);
            }
            return;
        }
        FixBS possVals = empty.copy();
        possVals.set(0, v);
        possVals.andNot(currMap.values);
        ex: for (int a = keys.nextSetBit(0); a >= 0; a = keys.nextSetBit(a + 1)) {
            int aVal = currMap.val(a);
            for (int b = keys.nextSetBit(a + 1); b >= 0; b = keys.nextSetBit(b + 1)) {
                int bVal = currMap.val(b);
                Relation rel = relation(a, b);
                Relation relVal = relation(aVal, bVal);
                FixBS secondKeys = forFirst(rel, nextKey).intersection(keys);
                for (int sndKey = secondKeys.nextSetBit(0); sndKey >= 0; sndKey = secondKeys.nextSetBit(sndKey + 1)) {
                    int sndVal = currMap.val(sndKey);
                    possVals.and(forSecond(relVal, sndVal));
                }
                FixBS firstKeys = forSecond(rel, nextKey).intersection(keys);
                for (int fstKey = firstKeys.nextSetBit(0); fstKey >= 0; fstKey = firstKeys.nextSetBit(fstKey + 1)) {
                    int fstVal = currMap.val(fstKey);
                    possVals.and(forFirst(relVal, fstVal));
                }
                int card = possVals.cardinality();
                if (card <= 1) {
                    break ex;
                }
            }
        }
        for (int nextVal = possVals.nextSetBit(0); nextVal >= 0; nextVal = possVals.nextSetBit(nextVal + 1)) {
            PartialMap nextMap = currMap.copy();
            nextMap.set(nextKey, nextVal);
            find(auths, nextMap);
        }
    }

    private FixBS forFirst(Relation rel, int fst) {
        FixBS vals = rel.secondFor(fst);
        return vals == null ? empty : vals;
    }

    private FixBS forSecond(Relation rel, int snd) {
        FixBS vals = rel.firstFor(snd);
        return vals == null ? empty : vals;
    }

    private int[] autToDiffAut(int[] auth) {
        int[] diffAut = new int[diffLength()];
        Arrays.fill(diffAut, -1);
        for (int x : oBeg) {
            for (int y = x + 1; y < v; y++) {
                int basePair = x * v + y;
                int mapPair = auth[x] * v + auth[y];
                if (connect(diffAut, diffMap[basePair], diffMap[mapPair])) {
                    return null;
                }
                int inv = y * v + x;
                int invMap = auth[y] * v + auth[x];
                if (connect(diffAut, diffMap[inv], diffMap[invMap])) {
                    return null;
                }
            }
        }
        return diffAut;
    }

    private boolean connect(int[] arr, int from, int to) {
        int prev = arr[from];
        if (prev >= 0) {
            return prev != to;
        } else {
            arr[from] = to;
            return false;
        }
    }

    private record PartialMap(FixBS keys, FixBS values, int[] map) {
        private PartialMap copy() {
            return new PartialMap(keys.copy(), values.copy(), map.clone());
        }

        private void set(int k, int v) {
            if (keys.get(k)) {
                if (map[k] != v) {
                    throw new IllegalStateException();
                }
            } else {
                keys.set(k);
                values.set(v);
                map[k] = v;
            }
        }

        private int val(int key) {
            return map[key];
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

    public int diffLength() {
        return relations.size();
    }

    public FixBS difference(int idx) {
        return relations.get(idx).diffs();
    }

    public Relation relation(int a, int b) {
        return relations.get(diffMap[a * v + b]);
    }

    public int diffIdx(int xy) {
        return diffMap[xy];
    }

    public FixBS preImage(int from, int to) {
        return preImages.get(to).get(from);
    }

    public State forInitial(int fst, int snd) {
        return statesCache[fst][snd];
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

    public FixBS emptyFilter() {
        return emptyFilter;
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
        for (int[] auth : auths) {
            FixBS alt = new FixBS(v);
            for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                alt.set(auth[el]);
            }
            if (alt.compareTo(block) < 0) {
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

    public boolean minimalTwo(State[] states) {
        for (int[] auth : auths) {
            FixBS sndMapped = new FixBS(v);
            FixBS fstBlock = states[0].block();
            FixBS sndBlock = states[1].block();
            for (int el = sndBlock.nextSetBit(0); el >= 0; el = sndBlock.nextSetBit(el + 1)) {
                sndMapped.set(auth[el]);
            }
            int cmpToFst = sndMapped.compareTo(fstBlock);
            if (cmpToFst < 0) {
                return false;
            }
            int cmpToSnd = sndMapped.compareTo(sndBlock);
            if (cmpToSnd < 0) {
                FixBS fstMapped = new FixBS(v);
                for (int el = fstBlock.nextSetBit(0); el >= 0; el = fstBlock.nextSetBit(el + 1)) {
                    fstMapped.set(auth[el]);
                }
                if ((cmpToFst == 0 && fstMapped.compareTo(sndBlock) < 0) || fstMapped.equals(fstBlock)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean minimal(State[] states) {
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
}
