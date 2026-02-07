package ua.ihromant.mathutils;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import ua.ihromant.jnauty.GraphData;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GraphTest {
    @Test
    public void testBronKerbosh() {
        for (int n = 4; n < 10; n++) {
            Graph g = new Graph(n);
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    g.connect(i, j);
                }
            }
            List<FixBS> lst = new ArrayList<>();
            g.bronKerbPivot(lst::add);
            assertEquals(List.of(FixBS.of(n, IntStream.range(0, n).toArray())), lst);
            Graph g1 = new Graph(n);
            for (int i = 0; i < n; i++) {
                g1.connect(i, (i + 1) % n);
            }
            lst.clear();
            g1.bronKerbPivot(lst::add);
            assertEquals(n, lst.size());
        }
        FixBS[] arr = new FixBS[] {
                FixBS.of(12, 1, 2, 3),
                FixBS.of(12, 0, 2, 3, 4),
                FixBS.of(12, 0, 1, 3, 4),
                FixBS.of(12, 0, 1, 2, 5),
                FixBS.of(12, 1, 2, 6),
                FixBS.of(12, 3, 6, 7),
                FixBS.of(12, 4, 5, 7, 8),
                FixBS.of(12, 5, 6, 8),
                FixBS.of(12, 6, 7, 9),
                FixBS.of(12, 8, 10, 11),
                FixBS.of(12, 9, 11),
                FixBS.of(12, 9, 10),
        };
        Graph g = new Graph(arr);
        List<FixBS> lst = new ArrayList<>();
        g.bronKerbPivot(lst::add);
        System.out.println(lst);

        Graph g2 = new Graph(6);
        g2.connect(0, 1);
        g2.connect(0, 4);
        g2.connect(1, 4);
        g2.connect(1, 2);
        g2.connect(3, 4);
        g2.connect(2, 3);
        g2.connect(3, 5);
        g2.bronKerb(a -> System.out.println(Arrays.toString(a.toArray())));
        System.out.println();

        g2.bronKerbPivot(a -> System.out.println(Arrays.toString(a.toArray())));
        System.out.println();
    }

    @Test
    public void buildGraph() throws IOException {
        Graph graph = new Graph(300000);
        List<Liner> basePlanes = BatchLinerTest.readPlanes(28, 4);
        Map<FixBS, LinerInfo> liners = new HashMap<>();
        List<LinerInfo> stack = new ArrayList<>();
        basePlanes.parallelStream().forEach(Liner::graphData);
        for (int i = 0; i < basePlanes.size(); i++) {
            Liner lnr = basePlanes.get(i);
            FixBS canon = new FixBS(lnr.graphData().canonical());
            LinerInfo info = new LinerInfo().setLiner(lnr).setGraphIdx(i);
            liners.put(canon, info);
            stack.add(info);
        }
        System.out.println("Base size " + stack.size());
        int graphSize = stack.size();
        while (!stack.isEmpty()) {
            LinerInfo info = stack.removeLast();
            if (info.isProcessed()) {
                continue;
            }
            Liner lnr = info.getLiner();
            List<Liner> para = lnr.paraModifications();
            Map<FixBS, Liner> unique = new ConcurrentHashMap<>();
            para.stream().parallel().forEach(l -> {
                GraphData gd = l.graphData();
                unique.putIfAbsent(new FixBS(gd.canonical()), l);
            });
            for (Map.Entry<FixBS, Liner> e : unique.entrySet()) {
                LinerInfo parInfo = liners.get(e.getKey());
                if (parInfo == null) {
                    parInfo = new LinerInfo().setLiner(lnr).setGraphIdx(graphSize++);
                    liners.put(e.getKey(), parInfo);
                    continue;
                }
                graph.connect(info.getGraphIdx(), parInfo.getGraphIdx());
                stack.add(parInfo);
            }
            info.setProcessed(true);
            System.out.println(stack.size() + " " + graphSize);
        }
        System.out.println(graphSize);
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    private static class LinerInfo {
        private Liner liner;
        private Integer graphIdx;
        private boolean processed;
    }

    private static List<Liner> readLiners(int v, int k) throws IOException {
        ObjectMapper om = new ObjectMapper();
        String content = Files.readString(Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/" + v + "-" + k + "final.txt"));
        return content.lines().map(l -> {
            int[][] lines = om.readValue(l.substring(l.indexOf("[[")), int[][].class);
            return new Liner(lines);
        }).collect(Collectors.toList());
    }
}
