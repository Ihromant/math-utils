package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Graph;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.PartialLiner;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Finder1Test {
    @Test
    public void generateCom() {
        String prefix = "com";
        int v = 15;
        int k = 3;
        DumpConfig conf = readLast(prefix, v, k, () -> {throw new IllegalArgumentException();});
        int left = conf.left;
        List<PartialLiner> liners = Arrays.stream(conf.partials()).map(PartialLiner::new).toList();
        Map<FixBS, Liner> unique = new HashMap<>();
        for (PartialLiner l : liners) {
            List<int[]> possible = l.possibleBlocks();
            Graph g = new Graph(possible.size());
            for (int i = 0; i < possible.size(); i++) {
                int[] fst = possible.get(i);
                for (int j = i + 1; j < possible.size(); j++) {
                    int[] snd = possible.get(j);
                    if (intersectionCount(v, fst, snd) < 2) {
                        g.connect(i, j);
                    }
                }
            }
            g.bronKerbPivot((arr, sz) -> {
                if (sz == left) {
                    int[][] lines = Stream.concat(Arrays.stream(l.lines()), Arrays.stream(arr.toArray()).mapToObj(possible::get))
                                    .toArray(int[][]::new);
                    Liner full = new Liner(v, lines);
                    if (unique.putIfAbsent(l.getCanonical(), full) == null) {
                        System.out.println(Arrays.deepToString(full.lines()));
                    }
                }
            });
            System.out.println(possible.size());
        }
    }

    private int intersectionCount(int v, int[] fst, int[] snd) {
        return FixBS.of(v, fst).intersection(FixBS.of(v, snd)).cardinality();
    }

    private static DumpConfig readLast(String prefix, int v, int k, Supplier<DumpConfig> fallback) {
        try (FileInputStream fis = new FileInputStream("/home/ihromant/maths/partials/bases/" + prefix + "-" + v + "-" + k + ".txt");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            int left = Integer.MIN_VALUE;
            int lineCount = v * (v - 1) / k / (k - 1);
            int[][][] partials = null;
            while ((line = br.readLine()) != null) {
                left = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                line = br.readLine();
                int partialsCount = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                int partialSize = lineCount - left;
                partials = new int[partialsCount][partialSize][k];
                for (int i = 0; i < partialsCount; i++) {
                    int[][] partial = partials[i];
                    for (int j = 0; j < partialSize; j++) {
                        String[] pts = br.readLine().split(" ");
                        for (int l = 0; l < k; l++) {
                            partial[j][l] = Integer.parseInt(pts[l]);
                        }
                    }
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

    private record DumpConfig(int v, int k, int left, int[][][] partials) {}
}
