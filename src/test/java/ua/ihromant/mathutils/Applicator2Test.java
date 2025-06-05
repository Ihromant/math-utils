package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import ua.ihromant.mathutils.g.OrbitConfig;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Applicator2Test {
    @Test
    public void findPossible() {
        OrbitConfig conf = new OrbitConfig(28, 4, 0, true, 3);
        System.out.println(conf + " " + conf.innerFilter() + " " + conf.outerFilter());
        Map<int[], List<int[][]>> res = conf.groupedSuitable();
        for (Map.Entry<int[], List<int[][]>> e : res.entrySet()) {
            System.out.println(Arrays.toString(e.getKey()) + " " + e.getValue().stream().map(Arrays::deepToString).collect(Collectors.joining(", ", "[", "]")));
        }
    }

    @Test
    public void calculateFile() throws IOException {
        OrbitConfig conf = new OrbitConfig(96, 6, 6);
        ObjectMapper om = new ObjectMapper();
        File f = new File("/home/ihromant/maths/g-spaces/chunks", conf + "all.txt");
        File beg = new File("/home/ihromant/maths/g-spaces/chunks", conf + ".txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr);
             FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            Set<ArrWrap> set = allBr.lines().map(s -> new ArrWrap(om.readValue(s, int[][].class))).collect(Collectors.toSet());
            br.lines().forEach(l -> {
                if (l.contains("[[[")) {
                    int[][][] design = om.readValue(l, int[][][].class);
                    Liner liner = conf.fromChunks(design);
                    System.out.println(liner.hyperbolicFreq() + " " + Arrays.deepToString(design));
                } else {
                    set.remove(new ArrWrap(om.readValue(l, int[][].class)));
                }
            });
            AtomicInteger ai = new AtomicInteger();
            ChunkCallback cb = new ChunkCallback() {
                @Override
                public void onDesign(int[][][] design) {
                    Liner liner = conf.fromChunks(design);
                    System.out.println(liner.hyperbolicFreq() + " " + Arrays.deepToString(design));
                    ps.println(Arrays.deepToString(design));
                    ps.flush();
                }

                @Override
                public void onFinish(int[][] chunk) {
                    ps.println(Arrays.deepToString(chunk));
                    ps.flush();
                    int val = ai.incrementAndGet();
                    if (val % 100 == 0) {
                        System.out.println(val);
                    }
                }
            };
            calculate(set.stream().map(ArrWrap::arr).collect(Collectors.toList()), conf, cb);
        }
    }

    private static void calculate(List<int[][]> lefts, OrbitConfig conf, ChunkCallback cb) {
        System.out.println("Lefts size: " + lefts.size() + " for conf " + conf);
        lefts.stream().parallel().forEach(left -> {
            int ll = left.length;
//            Predicate<RightState[]> cons = arr -> {
//                if (arr[ll - 1] == null) {
//                    return false;
//                }
//                int[][][] res = IntStream.range(0, ll).mapToObj(i -> new int[][]{left[i], arr[i].block.toArray()}).toArray(int[][][]::new);
//                cb.onDesign(res);
//                return true;
//            };
//            Applicator1Test.LeftCalc[] calcs = Arrays.stream(left).map(arr -> fromBlock(arr, conf.orbitSize())).toArray(Applicator1Test.LeftCalc[]::new);
//            Applicator1Test.LeftCalc fstLeft = calcs[0];
//            Applicator1Test.RightState[] rights = new Applicator1Test.RightState[ll];
//            FixBS whiteList = new FixBS(conf.orbitSize());
//            whiteList.set(0, conf.orbitSize());
//            FixBS outerFilter = conf.outerFilter();
//            for (int el : fstLeft.block()) {
//                whiteList.diffModuleShifted(outerFilter, conf.orbitSize(), conf.orbitSize() - el);
//            }
//            Applicator1Test.RightState state = new Applicator1Test.RightState(new IntList(conf.k()), conf.innerFilter(), outerFilter, whiteList, 0);
//            if (outerFilter.isEmpty()) {
//                state = state.acceptElem(0, fstLeft, conf.orbitSize());
//            }
//            find(calcs, rights, state, conf, cons);
//            cb.onFinish(left);
        });
    }

    private record State(List<List<SubBlock>> finished, IntList currBlock, FixBS[][] filters, FixBS whiteList, int idx) {

    }

    private record SubBlock(int[] block, FixBS inv, FixBS diff, int len) {}

    private interface ChunkCallback {
        void onDesign(int[][][] design);
        void onFinish(int[][] chunk);
    }

    private record ArrWrap(int[][] arr) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ArrWrap(int[][] arr1))) return false;

            return Arrays.deepEquals(arr, arr1);
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(arr);
        }
    }
}
