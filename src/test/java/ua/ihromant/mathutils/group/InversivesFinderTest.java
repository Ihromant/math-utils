package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.Inc;
import ua.ihromant.mathutils.InversivePlane;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InversivesFinderTest {
    @Test
    public void generateInversives() throws IOException {
        String prefix = "inv";
        int v = 14;
        int k = 4;
        int dp = 4;
        int b = v * (v - 1) * (v - 2) / k / (k - 1) / (k - 2);
        int r = (v - 2) / (k - 2);
        DumpConfig conf = readLast(prefix, v, k, () -> new DumpConfig(v, k, b - r, new Inc[]{(beamBlocks(v, k))}));
        List<Inc> liners = Arrays.asList(conf.partials());
        int left = conf.left();
        long time = System.currentTimeMillis();
        System.out.println("Started generation for v = " + v + ", k = " + k + ", blocks left " + left + ", base size " + liners.size() + ", depth " + dp);
        while (left > 0 && !liners.isEmpty()) {
            AtomicLong cnt = new AtomicLong();
            liners = nextStageAlt(liners, cnt);
            left--;
            IncFinderTest.dump(prefix, v, k, left, liners);
            System.out.println(left + " " + liners.size() + " " + cnt.get());
        }
        System.out.println(System.currentTimeMillis() - time);
    }

    public static Inc beamBlocks(int v, int k) {
        int r = (v - 2) / (k - 2);
        Inc res = Inc.empty(v, r);
        for (int l = 0; l < r; l++) {
            res.set(l, 0);
            res.set(l, 1);
            for (int j = 0; j < k - 2; j++) {
                res.set(l, l * (k - 2) + j + 2);
            }
        }
        return res;
    }

    private static List<Inc> nextStageAlt(List<Inc> partials, AtomicLong cnt) {
        Map<FixBS, Inc> nonIsomorphic = new HashMap<>();
        for (Inc inc : partials) {
            InversivePlane partial = new InversivePlane(inc);
            for (int[] block : partial.blocks()) {
                Inc next = inc.addLine(block);
                cnt.incrementAndGet();
                nonIsomorphic.putIfAbsent(next.removeTwins().getCanonicalOld(), next);
            }
        }
        return new ArrayList<>(nonIsomorphic.values());
    }

    private static List<Inc> nextStageAltConc(List<Inc> partials, AtomicLong cnt) {
        Map<FixBS, Inc> nonIsomorphic = new ConcurrentHashMap<>();
        partials.stream().parallel().forEach(inc -> {
            InversivePlane partial = new InversivePlane(inc);
            for (int[] block : partial.blocks()) {
                Inc next = inc.addLine(block);
                cnt.incrementAndGet();
                nonIsomorphic.putIfAbsent(next.removeTwins().getCanonicalOld(), next);
            }
        });
        return new ArrayList<>(nonIsomorphic.values());
    }

    @Test
    public void testInversives() throws IOException {
        String prefix = "inv";
        int v = 14;
        int k = 4;
        int b = v * (v - 1) * (v - 2) / k / (k - 1) / (k - 2);
        int r = (v - 2) / (k - 2);
        DumpConfig conf = readLast(prefix, v, k, () -> new DumpConfig(v, k, b - r - 1, new Inc[]{(beamBlocks(v, k))}));
        List<Inc> liners = Arrays.asList(conf.partials());
        for (Inc liner : liners) {
            System.out.println(new InversivePlane(liner).autCount());
        }
    }

    @Test
    public void printAdmissible() {
        int t = 3;
        int k = 7;
        System.out.println(t + " " + k);
        System.out.println(IntStream.range(0, 400).filter(v -> Combinatorics.admissible(t, v, k)).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
    }

    private static DumpConfig readLast(String prefix, int v, int k, Supplier<DumpConfig> fallback) {
        try (FileInputStream fis = new FileInputStream("/home/ihromant/maths/partials/" + prefix + "-" + v + "-" + k + ".txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            int left = Integer.MIN_VALUE;
            int lineCount = v * (v - 1) * (v - 2) / k / (k - 1) / (k - 2);
            Inc[] partials = null;
            while ((line = br.readLine()) != null) {
                left = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                line = br.readLine();
                int partialsCount = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                int partialSize = lineCount - left;
                partials = new Inc[partialsCount];
                for (int i = 0; i < partialsCount; i++) {
                    Inc partial = Inc.empty(v, partialSize);
                    for (int j = 0; j < partialSize; j++) {
                        String[] pts = br.readLine().split(" ");
                        for (int l = 0; l < k; l++) {
                            partial.set(j, Integer.parseInt(pts[l]));
                        }
                    }
                    partials[i] = partial;
                    br.readLine();
                }
            }
            return new DumpConfig(v, k, left, partials);
        } catch (FileNotFoundException e) {
            return fallback.get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
