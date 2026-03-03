package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.jnauty.JNauty;
import ua.ihromant.mathutils.Graph;
import ua.ihromant.mathutils.Inc;
import ua.ihromant.mathutils.g.GSpace;
import ua.ihromant.mathutils.g.State;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IncOrbitFinderTest {
    @Test
    public void generateCom() {
        int v = 25;
        int k = 4;
        int r = (v - 1) / (k - 1);
        Group g = new CyclicGroup(1);
        GSpace sp = new GSpace(k, g, false, IntStream.concat(IntStream.empty(), IntStream.range(0, v).map(_ -> 1)).toArray());
        List<State> singles = Collections.synchronizedList(new ArrayList<>());
        IntStream.of(sp.oBeg()).parallel().forEach(fst -> {
            IntStream.range(fst + 1, sp.v()).parallel().forEach(snd -> {
                IntStream.range(snd + 1, sp.v()).forEach(trd -> {
                    State state = sp.forInitial(fst, snd, trd);
                    if (state == null) {
                        return;
                    }
                    searchDesignsFirst(sp, state, trd, st -> {
                        State min = st.minimizeBlock(sp);
                        if (min.block().equals(st.block())) {
                            singles.add(st);
                        }
                    });
                });
            });
        });
        DumpConfig conf = IncFinderTest.readLast("bases/com", v, k, () -> {throw new RuntimeException();});
        Map<FixBS, Inc> un = new ConcurrentHashMap<>();
        for (Inc inc : conf.partials()) {
            List<State> beamPartial = Arrays.stream(inc.lines()).map(l -> State.fromBlock(sp, l)).toList();
            List<State> noBeam = singles.stream().filter(st -> beamPartial.stream().noneMatch(b -> b.diffSet().intersects(st.diffSet()))).toList();
            List<State> fstStage = noBeam.stream().filter(st -> {
                for (int i = st.diffSet().nextSetBit(0); i >= 0; i = st.diffSet().nextSetBit(i + 1)) {
                    if (sp.difference(i).nextSetBit(0) < 3 * v) {
                        return true;
                    }
                }
                return false;
            }).toList();
            Graph gr = new Graph(fstStage.size());
            for (int i = 0; i < fstStage.size(); i++) {
                for (int j = i + 1; j < fstStage.size(); j++) {
                    if (!fstStage.get(i).diffSet().intersects(fstStage.get(j).diffSet())) {
                        gr.connect(i, j);
                    }
                }
            }
            System.out.println(singles.size() + " " + noBeam.size() + " " + fstStage.size());
            gr.bronKerbPivotPar((fbs, sz) -> {
                if (sz != r - 1) {
                    return;
                }
                int[] idx = fbs.toArray();
                Inc fi = new Inc(Stream.concat(Arrays.stream(inc.lines()),
                        Arrays.stream(idx).mapToObj(i -> fstStage.get(i).block())).toArray(FixBS[]::new), v);
                long[] canon = JNauty.instance().traces(fi).canonical();
                un.putIfAbsent(new FixBS(canon), fi);
            });
            System.out.println(un.size());
        }

        //System.out.println(un.size());
    }

    private record Lns(FixBS diffSet, State[] states) {}

    private static void searchDesignsFirst(GSpace space, State state, int prev, Consumer<State> cons) {
        int v = space.v();
        if (state.size() == space.k()) {
            cons.accept(state);
        } else {
            for (int el = prev + 1; el < v; el++) {
                State nextState = state.acceptElem(space, space.emptyFilter(), el);
                if (nextState != null && space.minimal(nextState.block())) {
                    searchDesignsFirst(space, nextState, el, cons);
                }
            }
        }
    }
}
