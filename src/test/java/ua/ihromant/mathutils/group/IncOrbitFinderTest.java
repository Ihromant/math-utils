package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Inc;
import ua.ihromant.mathutils.g.GSpace;
import ua.ihromant.mathutils.g.State;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IncOrbitFinderTest {
    @Test
    public void generateCom() {
        Group g = new CyclicGroup(1);
        GSpace sp = new GSpace(4, g, false, IntStream.concat(IntStream.empty(), IntStream.range(0, 28).map(_ -> 1)).toArray());
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
        List<Lns> grouped = singles.stream().collect(Collectors.groupingBy(State::diffSet)).entrySet().stream()
                .map(e -> new Lns(e.getKey(), e.getValue().toArray(State[]::new))).sorted(Comparator.comparing(Lns::diffSet)).toList();
        Inc beamBlocks = IncFinderTest.beamBlocks(sp.v(), sp.k());

        Lns bs = grouped.getFirst();
        System.out.println(singles.size() + " " + grouped.size());
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
