package ua.ihromant.mathutils;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import ua.ihromant.jnauty.GraphData;
import ua.ihromant.mathutils.util.FixBS;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
            g.bronKerbPivot((el, _) -> lst.add(el.copy()));
            assertEquals(List.of(FixBS.of(n, IntStream.range(0, n).toArray())), lst);
            Graph g1 = new Graph(n);
            for (int i = 0; i < n; i++) {
                g1.connect(i, (i + 1) % n);
            }
            lst.clear();
            g1.bronKerbPivot((el, _) -> lst.add(el.copy()));
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
        g.bronKerbPivot((el, _) -> lst.add(el.copy()));
        System.out.println(lst);

        Graph g2 = new Graph(6);
        g2.connect(0, 1);
        g2.connect(0, 4);
        g2.connect(1, 4);
        g2.connect(1, 2);
        g2.connect(3, 4);
        g2.connect(2, 3);
        g2.connect(3, 5);
        g2.bronKerb((a, _) -> System.out.println(Arrays.toString(a.toArray())));
        System.out.println();

        g2.bronKerbPivot((a, _) -> System.out.println(Arrays.toString(a.toArray())));
        System.out.println();
    }

    @Test
    public void buildGraph() throws IOException {
        SparseGraph graph = new SparseGraph();
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
        int processed = 0;
        while (!stack.isEmpty()) {
            LinerInfo info = stack.removeLast();
            if (info.isProcessed()) {
                continue;
            }
            graph.connect(info.getGraphIdx(), info.getGraphIdx());
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
                    parInfo = new LinerInfo().setLiner(e.getValue()).setGraphIdx(graphSize++);
                    liners.put(e.getKey(), parInfo);
                }
                graph.connect(info.getGraphIdx(), parInfo.getGraphIdx());
                stack.add(parInfo);
            }
            info.setProcessed(true);
            System.out.println(++processed + " " + stack.size() + " " + graphSize);
        }
        System.out.println(graphSize);
        List<FixBS> comps = graph.components();
        System.out.println(graph.size() + " " + comps.size() + " " + comps);
    }

    @Test
    public void buildByComponent() throws IOException {
        int v = 28;
        int k = 4;
        List<Liner> basePlanes = BatchLinerTest.readPlanes(v, k);
        FixBS done = readDone(basePlanes.size(), v, k);
        Map<FixBS, LinerInfo> liners = new HashMap<>();
        List<LinerInfo> stack = new ArrayList<>();
        basePlanes.parallelStream().forEach(Liner::graphData);
        for (int i = 0; i < basePlanes.size(); i++) {
            Liner lnr = basePlanes.get(i);
            FixBS canon = new FixBS(lnr.graphData().canonical());
            LinerInfo info = new LinerInfo().setLiner(lnr).setBaseIdx(i).setProcessed(done.get(i));
            liners.put(canon, info);
            stack.add(info);
        }
        System.out.println("Base size " + stack.size() + " " + liners.size());
        while (!stack.isEmpty()) {
            LinerInfo baseInfo = stack.removeLast();
            if (baseInfo.isProcessed()) {
                continue;
            }
            SparseGraph graph = new SparseGraph();
            baseInfo.setGraphIdx(0);
            List<LinerInfo> componentStack = new ArrayList<>();
            componentStack.add(baseInfo);
            System.out.println("Processing component for " + baseInfo.getBaseIdx());
            List<LinerInfo> processed = new ArrayList<>();
            while (!componentStack.isEmpty()) {
                LinerInfo info = componentStack.removeLast();
                if (info.isProcessed()) {
                    continue;
                }
                graph.connect(info.getGraphIdx(), info.getGraphIdx());
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
                        parInfo = new LinerInfo().setLiner(e.getValue());
                        liners.put(e.getKey(), parInfo);
                    }
                    if (parInfo.getGraphIdx() == null) {
                        parInfo.setGraphIdx(graph.size());
                    }
                    graph.connect(info.getGraphIdx(), parInfo.getGraphIdx());
                    componentStack.add(parInfo);
                }
                info.setProcessed(true);
                processed.add(info);
                System.out.println(processed.size() + " " + componentStack.size() + " " + graph.size());
            }
            dumpGraph(graph, processed, v, k);
            System.out.println("Component size " + graph.size());
        }
    }

    private static FixBS readDone(int sz, int v, int k) throws IOException {
        FixBS result = new FixBS(sz);
        for (File f : Objects.requireNonNull(new File("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph").listFiles())) {
            if (f.getName().contains("single")) {
                Files.readString(f.toPath()).lines().forEach(l -> result.set(Integer.parseInt(l.substring(0, l.indexOf(' ')))));
            }
            if (f.getName().contains("graph")) {
                Files.readString(f.toPath()).lines().filter(l -> l.indexOf('(') >= 0)
                        .forEach(l -> result.set(Integer.parseInt(l.substring(l.indexOf('(') + 1, l.indexOf(')')))));
            }
        }
        return result;
    }

    private static void dumpGraph(SparseGraph g, List<LinerInfo> processed, int v, int k) throws IOException {
        for (int i = 0; i < g.size(); i++) {
            g.disconnect(i, i);
        }
        LinerInfo fst = processed.getFirst();
        int baseIdx = fst.getBaseIdx();
        if (g.size() == 1) {
            Files.writeString(Path.of("/home/ihromant/maths/g-spaces/final/"+ k + "-" + v + "/graph/single.txt"),
                    baseIdx + " " + Arrays.deepToString(fst.liner.lines()) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return;
        }
        StringBuilder graph = new StringBuilder();
        StringBuilder liners = new StringBuilder();
        for (int i = 0; i < g.size(); i++) {
            LinerInfo info = processed.get(i);
            liners.append(i).append(" ").append(Arrays.deepToString(fst.liner.lines())).append(System.lineSeparator());
            graph.append(i);
            if (info.getBaseIdx() != null) {
                graph.append(" (").append(info.getBaseIdx()).append(")");
            }
            graph.append(": ").append(Arrays.toString(g.adjacent(i))).append(System.lineSeparator());
        }
        Files.writeString(Path.of("/home/ihromant/maths/g-spaces/final/"+ k + "-" + v + "/graph/" + baseIdx + "liners.txt"),
                liners, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        Files.writeString(Path.of("/home/ihromant/maths/g-spaces/final/"+ k + "-" + v + "/graph/" + baseIdx + "graph.txt"),
                graph, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    private static class LinerInfo {
        private Liner liner;
        private Integer baseIdx;
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

    @Test
    public void generateFirstLayer() throws IOException {
        List<Liner> basePlanes = readLiners(175, 7);
        for (int i = 0; i < basePlanes.size(); i++) {
            Liner lnr = basePlanes.get(i);
            List<Liner> para = lnr.paraModifications();
            Map<FixBS, Liner> unique = new ConcurrentHashMap<>();
            para.stream().parallel().forEach(l -> {
                GraphData gd = l.graphData();
                unique.putIfAbsent(new FixBS(gd.canonical()), l);
            });
            System.out.println("Generate for " + i);
            System.out.println(lnr.graphData().autCount() + " " + lnr.hyperbolicFreq() + " " + Arrays.deepToString(lnr.lines()));
            System.out.println("Paramodifications: ");
            for (Map.Entry<FixBS, Liner> e : unique.entrySet()) {
                System.out.println(e.getValue().graphData().autCount() + " " + e.getValue().hyperbolicFreq() + " " + Arrays.deepToString(e.getValue().lines()));
            }
            System.out.println();
        }
    }

    @Test
    public void generateLargeComponent() throws IOException {
        int v = 65;
        int k = 5;
        int counter = 20000;
        ObjectMapper om = new ObjectMapper();
        String content = Files.readString(Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/base.txt"));
        List<Liner> basePlanes = content.lines().map(l -> {
            int[][] lines = om.readValue(l.substring(l.indexOf("[[")), int[][].class);
            return new Liner(lines);
        }).toList();
        Map<FixBS, LinerInfo> syncLiners = new ConcurrentHashMap<>();
        basePlanes.parallelStream().forEach(Liner::graphData);
        IntStream.range(0, basePlanes.size()).parallel().forEach(i -> {
            Liner lnr = basePlanes.get(i);
            FixBS canon = new FixBS(lnr.graphData().canonical());
            LinerInfo info = new LinerInfo().setLiner(lnr).setBaseIdx(i);
            syncLiners.put(canon, info);
        });
        Path grPath = Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/graph.txt");
        content = Files.readString(grPath);
        String[] lns = content.lines().toArray(String[]::new);
        SparseGraph graph = new SparseGraph();
        for (String ln : lns) {
            int ix = ln.indexOf(" (");
            if (ix < 0) {
                ix = ln.indexOf(':');
            }
            int from = Integer.parseInt(ln.substring(0, ix));
            int[] to = om.readValue(ln.substring(ln.indexOf(':') + 1), int[].class);
            for (int t : to) {
                graph.connect(from, t);
            }
        }
        Path stPath = Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/stack.txt");
        List<String> reached = Files.readAllLines(stPath);
        LinerInfo[] compLiners = new LinerInfo[reached.size()];
        IntStream.range(0, reached.size()).parallel().forEach(i -> {
            String l = reached.get(i);
            int[][] lines = om.readValue(l.substring(l.indexOf("[[")), int[][].class);
            Liner lnr = new Liner(lines);
            LinerInfo info = syncLiners.computeIfAbsent(new FixBS(lnr.graphData().canonical()), _ -> new LinerInfo().setLiner(lnr));
            info.setGraphIdx(i);
            info.setProcessed(i < lns.length);
            compLiners[i] = info;
        });
        List<LinerInfo> stack = Arrays.stream(compLiners, lns.length, compLiners.length).collect(Collectors.toList());
        int processedCnt = lns.length;
        Map<FixBS, LinerInfo> liners = new HashMap<>(syncLiners);
        syncLiners.clear();
        reached.clear();
        content = null;
        while (!stack.isEmpty() && counter > 0) {
            LinerInfo info = stack.removeFirst();
            if (info.isProcessed()) {
                continue;
            }
            graph.connect(info.getGraphIdx(), info.getGraphIdx());
            Liner lnr = info.getLiner();
            List<Liner> para = lnr.paraModificationsAlt();
            Map<FixBS, Liner> unique = new ConcurrentHashMap<>();
            para.stream().parallel().forEach(l -> {
                GraphData gd = l.graphData();
                unique.putIfAbsent(new FixBS(gd.canonical()), l);
            });
            for (Map.Entry<FixBS, Liner> e : unique.entrySet()) {
                LinerInfo parInfo = liners.get(e.getKey());
                if (parInfo == null) {
                    parInfo = new LinerInfo().setLiner(e.getValue());
                    liners.put(e.getKey(), parInfo);
                }
                if (parInfo.getGraphIdx() == null) {
                    parInfo.setGraphIdx(graph.size());
                    Files.writeString(stPath, graph.size() + " " + Arrays.deepToString(parInfo.liner.lines()) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
                graph.connect(info.getGraphIdx(), parInfo.getGraphIdx());
                stack.add(parInfo);
            }
            info.setProcessed(true);
            graph.disconnect(info.getGraphIdx(), info.getGraphIdx());
            Files.writeString(grPath,
                    info.getGraphIdx() + (info.getBaseIdx() != null ? " (" + info.getBaseIdx() + ")" : "") + ": " + Arrays.toString(graph.adjacent(info.getGraphIdx()))
                            + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println(++processedCnt + " " + stack.size() + " " + graph.size() + " " + --counter);
        }
        System.out.println(content);
    }
}
