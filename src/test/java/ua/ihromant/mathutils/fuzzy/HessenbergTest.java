package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HessenbergTest {
    @Test
    public void test() {
        int[][] desargues = {
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {1, 3, 7},
                {1, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9},
                {4, 6, 9}
        };
        LinerHistory initial = FuzzyLiner.of(desargues, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        Map<Rel, Update> updates = new HashMap<>(initial.updates());
        FuzzyLiner base = initial.liner();
        Function<FuzzyLiner, LinerHistory> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processP));
        base.printChars();
        LinerHistory afterIntersect = base.intersectLines();
        afterIntersect.updates().forEach(updates::putIfAbsent);
        base = afterIntersect.liner();
        base.printChars();
        try {
            op.apply(base);
        } catch (ContradictionException e) {
            e.updates().forEach(updates::putIfAbsent);
            Rel rel = e.rel();
            Rel opposite = switch (rel) {
                case Dist(int a, int b) -> new Same(a, b);
                case Same(int a, int b) -> new Dist(a, b);
                case Col(int a, int b, int c) -> new Trg(a, b, c);
                case Trg(int a, int b, int c) -> new Col(a, b, c);
            };
            System.out.println("From one side: ");
            SequencedMap<Rel, Update> stack = new LinkedHashMap<>();
            reconstruct(rel, updates, stack);
            for (Update u : stack.reversed().values()) {
                System.out.println(u.base().ordered() + " follows from " + u.reasonName() + " due to "
                        + Arrays.stream(u.reasons()).map(r -> r.ordered().toString()).collect(Collectors.joining(" ")));
            }
            System.out.println("But from the other side: ");
            stack = new LinkedHashMap<>();
            reconstruct(opposite, updates, stack);
            for (Update u : stack.reversed().values()) {
                System.out.println(u.base().ordered() + " follows from " + u.reasonName() + " due to "
                        + Arrays.stream(u.reasons()).map(r -> r.ordered().toString()).collect(Collectors.joining(" ")));
            }
            System.out.println("Contradiction");
        }
    }

    private static void reconstruct(Rel rel, Map<Rel, Update> updates, SequencedMap<Rel, Update> stack) {
        rel = rel.ordered();
        if (stack.containsKey(rel)) {
            return;
        }
        Update u = updates.get(rel);
        stack.put(rel, u);
        for (Rel r : u.reasons()) {
            reconstruct(r, updates, stack);
        }
    }
}
