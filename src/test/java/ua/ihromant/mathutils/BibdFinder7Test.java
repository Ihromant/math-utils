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
    private static final int v = 91;
    private static final int k = 6;
    private static final int blocksNeeded = v / k / (k - 1);
    private static final FixBS baseFilter = baseFilter(v, k);
    private static final int HALVES_EVEN_OFFSET = 0;
    private static final int HALVES_ODD_OFFSET = v / 2 + 1;
    private static final int HALVES_EVEN_SHIFT = v % 2 == 0 ? (v / 2) : 0;
    private static final int HALVES_ODD_SHIFT = v % 2 == 0 ? 0 : (v / 2 + 1);
    private static final int diff_even = v - HALVES_EVEN_SHIFT;
    private static final int diff_odd = v - HALVES_ODD_SHIFT;

    private record State(FixBS block, FixBS inv, FixBS halves, FixBS selfSum, FixBS filter, FixBS whiteList, int size) {
        private static State forBlock(FixBS block) {
            State state = next(baseFilter);
            for (int el = block.nextSetBit(2); el >= 0; el = block.nextSetBit(el + 1)) {
                state = state.acceptElem(el);
            }
            return state;
        }

        private static State next(FixBS filter) {
            FixBS base = FixBS.of(v, 0);
            FixBS whiteList = filter.copy();
            whiteList.flip(1, v);
            return new State(base, base, base, base, filter, whiteList, 1).acceptElem(filter.nextClearBit(1));
        }

        private State acceptElem(int el) {
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
            newHalves.set((el >>> 1) + (((el & 1) != 0) ? HALVES_ODD_OFFSET : HALVES_EVEN_OFFSET));
            newWhiteList.diffModuleShifted(halves, v, (((el & 1) != 0) ? diff_odd : diff_even) - (el >>> 1));
            newWhiteList.diffModuleShifted(newFilter, v, invEl);
            return new State(nextBlock, nextInv, newHalves, nextSelfSum, newFilter, newWhiteList, size + 1);
        }
    }

    private static void calcCycles(FixBS[] design, State state, Predicate<FixBS[]> sink) {
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
            State next = state.acceptElem(el);
            if (next.size() == k) {
                FixBS[] nextDesign = Arrays.copyOf(design, design.length + 1);
                nextDesign[design.length] = next.block();
                if (sink.test(nextDesign)) {
                    return;
                }
                next = State.next(next.filter());
                calcCycles(nextDesign, next, sink);
            } else {
                calcCycles(design, next, sink);
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
        Group gr = new CyclicGroup(v);
        File beg = new File("/home/ihromant/maths/diffSets/nbeg", k + "-" + gr.name() + "beg.txt");
        try (FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr)) {
            FixBS[] arr = allBr.lines().map(BibdFinder7Test::readFirst).toArray(FixBS[]::new);
            logResultsDepth(System.out, arr);
        }
    }

    @Test
    public void toFile() throws IOException {
        Group gr = new CyclicGroup(v);
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
            Set<FixBS> set = allBr.lines().map(BibdFinder7Test::readFirst).collect(Collectors.toSet());
            br.lines().forEach(l -> {
                if (l.contains("[[")) {
                    System.out.println(l);
                } else {
                    set.remove(readFirst(l));
                }
            });
            logResultsDepth(ps, set.toArray(FixBS[]::new));
        }
    }

    private static FixBS readFirst(String line) {
        return FixBS.of(v, Arrays.stream(line.substring(1, line.length() - 1).split(", ")).mapToInt(Integer::parseInt).toArray());
    }

    private static void logResultsDepth(PrintStream destination, FixBS[] unProcessed) {
        System.out.println(v + " " + k);
        System.out.println("Initial size " + unProcessed.length);
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Predicate<FixBS[]> designConsumer = design -> {
            if (design.length < blocksNeeded) {
                return false;
            }
            counter.incrementAndGet();
            destination.println(Arrays.deepToString(design));
            destination.flush();
            if (destination != System.out) {
                System.out.println(Arrays.deepToString(design));
            }
            return true;
        };
        AtomicInteger cnt = new AtomicInteger();
        Arrays.stream(unProcessed).parallel().forEach(init -> {
            State fst = State.forBlock(init);
            FixBS[] design = new FixBS[]{init};
            State initial = State.next(fst.filter());
            calcCycles(design, initial, designConsumer);
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
        Group group = new CyclicGroup(v);
        logAllCycles(group);
    }

    private static void logAllCycles(Group group) {
        System.out.println(group.name() + " " + k);
        FixBS[] base = new FixBS[0];
        FixBS whiteList = baseFilter.copy();
        whiteList.flip(1, v);
        State initial = State.next(baseFilter);
        calcCycles(base, initial, design -> {
            if (design.length < blocksNeeded) {
                return false;
            }
            System.out.println(Arrays.deepToString(design));
            System.out.flush();
            return true;
        });
    }
}
