package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BibdFinder7Test {
    private record State(FixBS block, FixBS inv, FixBS halves, FixBS selfSum, FixBS filter, FixBS whiteList, int size) {
        private static State forBlock(int v, FixBS baseFilter, FixBS block) {
            State state = next(baseFilter, v);
            for (int el = block.nextSetBit(2); el >= 0; el = block.nextSetBit(el + 1)) {
                state = state.acceptElem(el, v);
            }
            return state;
        }

        private static State next(FixBS filter, int v) {
            FixBS base = FixBS.of(v, 0);
            FixBS whiteList = filter.copy();
            whiteList.flip(1, v);
            return new State(base, base, base, base, filter, whiteList, 1).acceptElem(filter.nextClearBit(1), v);
        }

        private State acceptElem(int el, int v) {
            int invEl = v - el;
            FixBS nextBlock = block.copy();
            nextBlock.set(el);
            FixBS nextInv = inv.copy();
            nextInv.set(invEl);
            FixBS nextSelfSum = selfSum.copy();
            nextSelfSum.orModuleShifted(nextBlock, v, invEl);
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            newFilter.orModuleShifted(block, v, el);
            newFilter.orModuleShifted(inv, v, invEl);
            newWhiteList.diffModuleShifted(nextSelfSum, v, el);
            FixBS newHalves = halves.copy();
            newHalves.set((el >>> 1) + (((el & 1) != 0) ? (v >>> 1) + 1 : 0));
            newWhiteList.diffModuleShifted(halves, v, v - (el >>> 1) - (((el & 1) != 0) ? v % 2 == 0 ? 0 : v / 2 + 1 : v % 2 == 0 ? v / 2 : 0));
            newWhiteList.diffModuleShifted(newFilter, v, invEl);
            return new State(nextBlock, nextInv, newHalves, nextSelfSum, newFilter, newWhiteList, size + 1);
        }
    }

    private static void calcCycles(int v, int k, State[] design, State state, Predicate<State[]> sink) {
        FixBS whiteList = state.whiteList();
        FixBS currBlock = state.block();
        int idx = state.size();
        int lastVal = currBlock.previousSetBit(v);
        boolean first = idx == 2;
        int midCnt = k - idx - 1;
        int from = 0;
        int minMidSpace = 0;
        int fst = currBlock.nextSetBit(1);
        while (--midCnt >= 0) {
            from = state.filter.nextClearBit(from + 1);
            minMidSpace = minMidSpace + from;
        }
        int max = first ? (v + fst - minMidSpace + 1) / 2 : v - currBlock.nextSetBit(fst + 1) + fst - minMidSpace;
        for (int el = whiteList.nextSetBit(lastVal); el >= 0 && el < max; el = whiteList.nextSetBit(el + 1)) {
            State next = state.acceptElem(el, v);
            if (next.size() == k) {
                State[] nextDesign = Arrays.copyOf(design, design.length + 1);
                nextDesign[design.length] = next;
                if (sink.test(nextDesign)) {
                    return;
                }
                next = State.next(next.filter(), v);
                calcCycles(v, k, nextDesign, next, sink);
            } else {
                calcCycles(v, k, design, next, sink);
            }
        }
    }

    private static FixBS baseFilter(int v, int k) {
        FixBS filter = new FixBS(v);
        int rest = v % (k * (k - 1));
        if (rest == k) {
            for (int i = 1; i < v; i++) {
                if (i * k % v == 0) {
                    filter.set(i);
                }
            }
        }
        if (rest == (k - 1)) {
            for (int i = 1; i < v; i++) {
                if (i * (k - 1) % v == 0) {
                    filter.set(i);
                }
            }
        }
        return filter;
    }

    @Test
    public void toConsole() throws IOException {
        Group gr = new CyclicGroup(91);
        int v = gr.order();
        int k = 6;
        File beg = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + "beg.txt");
        try (FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr)) {
            FixBS[] arr = allBr.lines().map(l -> readFirst(l, v)).toArray(FixBS[]::new);
            logResultsDepth(System.out, v, k, arr);
        }
    }

    @Test
    public void toFile() throws IOException {
        Group gr = new CyclicGroup(156);
        int v = gr.order();
        int k = 6;
        File f = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + ".txt");
        File beg = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + "beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr);
             FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            Set<FixBS> set = allBr.lines().map(l -> readFirst(l, v)).collect(Collectors.toSet());
            br.lines().forEach(l -> {
                if (l.contains("[[")) {
                    System.out.println(l);
                } else {
                    set.remove(readFirst(l, v));
                }
            });
            logResultsDepth(ps, v, k, set.toArray(FixBS[]::new));
        }
    }

    private static FixBS readFirst(String line, int v) {
        return FixBS.of(v, Arrays.stream(line.substring(1, line.length() - 1).split(", ")).mapToInt(Integer::parseInt).toArray());
    }

    private static void logResultsDepth(PrintStream destination, int v, int k, FixBS[] unProcessed) {
        System.out.println(v + " " + k);
        System.out.println("Initial size " + unProcessed.length);
        int blocksNeeded = v / k / (k - 1);
        FixBS baseFilter = baseFilter(v, k);
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Predicate<State[]> designConsumer = design -> {
            if (design.length < blocksNeeded) {
                return false;
            }
            counter.incrementAndGet();
            destination.println(Arrays.deepToString(Arrays.stream(design).map(State::block).toArray(FixBS[]::new)));
            destination.flush();
            if (destination != System.out) {
                System.out.println(Arrays.deepToString(design));
            }
            return true;
        };
        AtomicInteger cnt = new AtomicInteger();
        Arrays.stream(unProcessed).parallel().forEach(init -> {
            State fst = State.forBlock(v, baseFilter, init);
            State[] design = new State[]{fst};
            State initial = State.next(fst.filter(), v);
            calcCycles(v, k, design, initial, designConsumer);
            if (destination != System.out) {
                destination.println(init);
                destination.flush();
            }
            int val = cnt.incrementAndGet();
            if (val % 1000 == 0) {
                System.out.println(val);
            }
        });
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }

    @Test
    public void logAllCycles() {
        Group group = new CyclicGroup(91);
        int k = 6;
        logAllCycles(group, k);
    }

    private static void logAllCycles(Group group, int k) {
        System.out.println(group.name() + " " + k);
        int v = group.order();
        FixBS filter = baseFilter(v, k);
        int blocksNeeded = v / k / (k - 1);
        State[] base = new State[0];
        FixBS whiteList = filter.copy();
        whiteList.flip(1, v);
        State initial = State.next(filter, v);
        calcCycles(v, k, base, initial, design -> {
            if (design.length < blocksNeeded) {
                return false;
            }
            System.out.println(Arrays.deepToString(Arrays.stream(design).map(State::block).toArray(FixBS[]::new)));
            System.out.flush();
            return true;
        });
    }
}
