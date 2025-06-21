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

    private static void calcCycles(FixBS[] design, FixBS block, FixBS inv, FixBS halves, FixBS selfSum, FixBS filter, FixBS whiteList, int size, Predicate<FixBS[]> sink) {
        int lastVal = block.previousSetBit(v);
        boolean first = size == 2;
        int midCnt = k - size - 1;
        int from = 0;
        int minMidSpace = 0;
        int fst = block.nextSetBit(1);
        while (--midCnt >= 0) {
            from = filter.nextClearBit(from + 1);
            minMidSpace = minMidSpace + from;
        }
        int max = first ? (v + fst - minMidSpace + 1) / 2 : v - block.nextSetBit(fst + 1) + fst - minMidSpace;
        int newSize = size + 1;
        for (int el = whiteList.nextSetBit(lastVal); el >= 0 && el < max; el = whiteList.nextSetBit(el + 1)) {
            int invEl = v - el;
            FixBS nextBlock = block.copy();
            nextBlock.set(el);
            FixBS nextInv = inv.copy();
            nextInv.set(invEl);
            FixBS nextFilter = filter.copy();
            nextFilter.orModuleShifted(block, v, el);
            nextFilter.orModuleShifted(inv, v, invEl);
            if (newSize == k) {
                FixBS[] nextDesign = Arrays.copyOf(design, design.length + 1);
                nextDesign[design.length] = nextBlock;
                if (sink.test(nextDesign)) {
                    return;
                }
                int nextClear = nextFilter.nextClearBit(1);
                int nextClearInv = v - nextClear;
                nextBlock = FixBS.of(v, 0, nextClear);
                nextInv = FixBS.of(v, 0, nextClearInv);
                nextFilter.set(nextClear);
                nextFilter.set(nextClearInv);
                FixBS nextHalves = FixBS.of(v, 0, (nextClear >>> 1) + (((nextClear & 1) != 0) ? HALVES_ODD_OFFSET : HALVES_EVEN_OFFSET));
                FixBS nextSelfSum = FixBS.of(v, 0, nextClear, 2 * nextClear);
                FixBS nextWhiteList = nextFilter.copy();
                nextWhiteList.flip(1, v);
                if (nextClearInv % 2 == 0) {
                    nextWhiteList.clear(nextClear + nextClearInv / 2);
                }
                nextWhiteList.clear(2 * nextClear);
                nextWhiteList.diffModuleShifted(nextFilter, v, nextClearInv);
                calcCycles(nextDesign, nextBlock, nextInv, nextHalves, nextSelfSum, nextFilter, nextWhiteList, 2, sink);
            } else {
                FixBS nextSelfSum = selfSum.copy();
                nextSelfSum.orModuleShifted(nextBlock, v, invEl);
                FixBS nextWhiteList = whiteList.copy();
                nextWhiteList.diffModuleShifted(nextSelfSum, v, el);
                FixBS nextHalves = halves.copy();
                nextHalves.set((el >>> 1) + (((el & 1) != 0) ? HALVES_ODD_OFFSET : HALVES_EVEN_OFFSET));
                nextWhiteList.diffModuleShifted(halves, v, (((el & 1) != 0) ? diff_odd : diff_even) - (el >>> 1));
                nextWhiteList.diffModuleShifted(nextFilter, v, invEl);
                calcCycles(design, nextBlock, nextInv, nextHalves, nextSelfSum, nextFilter, nextWhiteList, newSize, sink);
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

    private static FixBS filter(FixBS block) {
        FixBS result = baseFilter.copy();
        for (int i = block.nextSetBit(0); i >= 0; i = block.nextSetBit(i + 1)) {
            for (int j = block.nextSetBit(i + 1); j >= 0; j = block.nextSetBit(j + 1)) {
                result.set(j - i);
                result.set(v + i - j);
            }
        }
        return result;
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
            FixBS[] design = new FixBS[]{init};
            FixBS nextFilter = filter(init);
            int nextClear = nextFilter.nextClearBit(1);
            int nextClearInv = v - nextClear;
            FixBS nextBlock = FixBS.of(v, 0, nextClear);
            FixBS nextInv = FixBS.of(v, 0, nextClearInv);
            nextFilter.set(nextClear);
            nextFilter.set(nextClearInv);
            FixBS nextHalves = FixBS.of(v, 0, (nextClear >>> 1) + (((nextClear & 1) != 0) ? HALVES_ODD_OFFSET : HALVES_EVEN_OFFSET));
            FixBS nextSelfSum = FixBS.of(v, 0, nextClear, 2 * nextClear);
            FixBS nextWhiteList = nextFilter.copy();
            nextWhiteList.flip(1, v);
            if (nextClearInv % 2 == 0) {
                nextWhiteList.clear(nextClear + nextClearInv / 2);
            }
            nextWhiteList.clear(2 * nextClear);
            nextWhiteList.diffModuleShifted(nextFilter, v, nextClearInv);
            calcCycles(design, nextBlock, nextInv, nextHalves, nextSelfSum, nextFilter, nextWhiteList, 2, designConsumer);
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
        FixBS nextFilter = baseFilter.copy();
        int nextClear = nextFilter.nextClearBit(1);
        int nextClearInv = v - nextClear;
        FixBS nextBlock = FixBS.of(v, 0, nextClear);
        FixBS nextInv = FixBS.of(v, 0, nextClearInv);
        nextFilter.set(nextClear);
        nextFilter.set(nextClearInv);
        FixBS nextHalves = FixBS.of(v, 0, (nextClear >>> 1) + (((nextClear & 1) != 0) ? HALVES_ODD_OFFSET : HALVES_EVEN_OFFSET));
        FixBS nextSelfSum = FixBS.of(v, 0, nextClear, 2 * nextClear);
        FixBS nextWhiteList = nextFilter.copy();
        nextWhiteList.flip(1, v);
        if (nextClearInv % 2 == 0) {
            nextWhiteList.clear(nextClear + nextClearInv / 2);
        }
        nextWhiteList.clear(2 * nextClear);
        nextWhiteList.diffModuleShifted(nextFilter, v, nextClearInv);
        calcCycles(base, nextBlock, nextInv, nextHalves, nextSelfSum, nextFilter, nextWhiteList, 2, design -> {
            if (design.length < blocksNeeded) {
                return false;
            }
            System.out.println(Arrays.deepToString(design));
            System.out.flush();
            return true;
        });
    }
}
