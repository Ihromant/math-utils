package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupProduct;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BibdFinder1CyclicTest {
    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(Group group, int prev, int needed,
                                                                BitSet filter, BitSet blackList, BitSet tuple) {
        int tLength = tuple.length();
        return IntStream.range(tLength == 1 ? prev : tLength, group.order())
                .filter(idx -> !blackList.get(idx))
                .boxed().mapMulti((idx, sink) -> {
                    BitSet nextTuple = (BitSet) tuple.clone();
                    nextTuple.set(idx);
                    if (needed == 1) {
                        sink.accept(Map.entry(diff(nextTuple, group), nextTuple));
                        return;
                    }
                    BitSet newFilter = (BitSet) filter.clone();
                    BitSet newBlackList = (BitSet) blackList.clone();
                    for (int val = tuple.nextSetBit(0); val >= 0; val = tuple.nextSetBit(val + 1)) {
                        int mid = group.squareRoot(group.op(val, idx));
                        newBlackList.set(mid);
                        int diff = group.op(val, group.inv(idx));
                        int inv = group.inv(diff);
                        newFilter.set(diff);
                        newFilter.set(inv);
                        for (int nv = nextTuple.nextSetBit(0); nv >= 0; nv = nextTuple.nextSetBit(nv + 1)) {
                            newBlackList.set(group.op(nv, diff));
                            newBlackList.set(group.op(nv, inv));
                        }
                    }
                    for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                        newBlackList.set(group.op(idx, diff));
                    }
                    calcCycles(group, prev, needed - 1, newFilter, newBlackList, nextTuple).forEach(sink);
                    if (tLength == 1 && filter.cardinality() <= needed) {
                        System.out.println(idx);
                    }
                });
    }

    private static BitSet diff(BitSet block, Group group) {
        BitSet result = new BitSet();
        for (int i = block.nextSetBit(0); i >= 0; i = block.nextSetBit(i + 1)) {
            for (int j = block.nextSetBit(i + 1); j >= 0; j = block.nextSetBit(j + 1)) {
                result.set(group.op(i, group.inv(j)));
                result.set(group.op(group.inv(i), j));
            }
        }
        return result;
    }

    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(Group group, int size, int prev, BitSet filter) {
        Set<BitSet> dedup = ConcurrentHashMap.newKeySet();
        BitSet blackList = (BitSet) filter.clone();
        return calcCycles(group, prev, size - 1, filter, blackList, of(0)).filter(e -> dedup.add(e.getKey()));
    }

    @Test
    public void testDiffFamilies() {
        Group g = new GroupProduct(5, 5);
        int k = 4;
        System.out.println(g + " " + k);
        BitSet filter = g.order() % k == 0 ? IntStream.range(0, g.order()).filter(e -> g.order(e) == k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(g.order());
        SequencedMap<BitSet, BitSet> curr = new LinkedHashMap<>();
        long time = System.currentTimeMillis();
        Set<Set<BitSet>> dedup = ConcurrentHashMap.newKeySet();
        System.out.println(allDifferenceSets(g, k, curr, g.order() / k / (k - 1), filter).filter(res -> dedup.add(res.keySet()))
                .peek(System.out::println)
                .count() + " " + (System.currentTimeMillis() - time));
    }

    private static Stream<Map<BitSet, BitSet>> allDifferenceSets(Group group, int k, SequencedMap<BitSet, BitSet> curr, int needed, BitSet filter) {
        int prev = curr.isEmpty() ? 1 : IntStream.range(curr.lastEntry().getValue().nextSetBit(1) + 1, group.order())
                .filter(i -> !filter.get(i)).findFirst().orElse(group.order());
        return (needed > 1 ?
                calcCycles(group, k, prev, filter).parallel()
                : calcCycles(group, k, prev, filter)).mapMulti((pair, sink) -> {
            SequencedMap<BitSet, BitSet> nextCurr = new LinkedHashMap<>(curr);
            nextCurr.put(pair.getKey(), pair.getValue());
            if (needed == 1) {
                sink.accept(nextCurr);
                return;
            }
            BitSet nextFilter = (BitSet) filter.clone();
            nextFilter.or(pair.getKey());
            allDifferenceSets(group, k, nextCurr, needed - 1, nextFilter).forEach(sink);
        });
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet(values[values.length - 1] + 1);
        for (int v : values) {
            bs.set(v);
        }
        return bs;
    }
}
