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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
        List<SLiner> basePlanes = readSLiners(28, 4);
        Map<FixBS, LinerInfo> liners = new HashMap<>();
        List<LinerInfo> stack = new ArrayList<>();
        for (int i = 0; i < basePlanes.size(); i++) {
            SLiner lnr = basePlanes.get(i);
            FixBS canon = lnr.smallCanon();
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
            SLiner lnr = info.getLiner();
            List<SLiner> para = lnr.paraModificationsAlt();
            Map<FixBS, SLiner> unique = new ConcurrentHashMap<>();
            para.stream().parallel().forEach(l -> unique.putIfAbsent(l.smallCanon(), l));
            for (Map.Entry<FixBS, SLiner> e : unique.entrySet()) {
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
        int v = 65;
        int k = 5;
        List<SLiner> basePlanes = readSLiners(v, k);
        FixBS done = readDone(basePlanes.size(), v, k);
        Map<FixBS, LinerInfo> liners = new HashMap<>();
        List<LinerInfo> stack = new ArrayList<>();
        for (int i = 0; i < basePlanes.size(); i++) {
            SLiner lnr = basePlanes.get(i);
            FixBS canon = lnr.smallCanon();
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
                SLiner lnr = info.getLiner();
                List<SLiner> para = lnr.paraModificationsAlt();
                Map<FixBS, SLiner> unique = new ConcurrentHashMap<>();
                para.stream().parallel().forEach(l -> unique.putIfAbsent(l.smallCanon(), l));
                for (Map.Entry<FixBS, SLiner> e : unique.entrySet()) {
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
                Files.readAllLines(f.toPath()).forEach(l -> result.set(Integer.parseInt(l.substring(0, l.indexOf(' ')))));
            }
            if (f.getName().contains("graph")) {
                Files.readAllLines(f.toPath()).stream().filter(l -> l.indexOf('(') >= 0)
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
            liners.append(i).append(" ").append(Arrays.deepToString(info.liner.lines())).append(System.lineSeparator());
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
        private SLiner liner;
        private Integer baseIdx;
        private Integer graphIdx;
        private boolean processed;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    private static class SLinerInfo {
        private long[] canon;
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

    private static List<SLiner> readSLiners(int v, int k) throws IOException {
        ObjectMapper om = new ObjectMapper();
        String content = Files.readString(Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/" + v + "-" + k + "final.txt"));
        return content.lines().map(l -> {
            short[][] lines = om.readValue(l.substring(l.indexOf("[[")), short[][].class);
            return new SLiner(lines);
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
        int counter = 200000;
        ObjectMapper om = new ObjectMapper();
        String content = Files.readString(Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/base.txt"));
        List<SLiner> basePlanes = content.lines().map(l -> {
            short[][] lines = om.readValue(l.substring(l.indexOf("[[")), short[][].class);
            return new SLiner(lines);
        }).toList();
        Map<FixBS, SLinerInfo> syncLiners = new ConcurrentHashMap<>();
        IntStream.range(0, basePlanes.size()).parallel().forEach(i -> {
            SLiner lnr = basePlanes.get(i);
            FixBS canon = lnr.canonByLines();
            SLinerInfo info = new SLinerInfo().setBaseIdx(i).setCanon(canon.words());
            syncLiners.put(canon, info);
        });
        Path grPath = Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/graph.txt");
        //List<String> lns = Files.readAllLines(grPath);
        SparseGraph graph = new SparseGraph();
//        for (String ln : lns) {
//            int ix = ln.indexOf(" (");
//            if (ix < 0) {
//                ix = ln.indexOf(':');
//            }
//            int from = Integer.parseInt(ln.substring(0, ix));
//            int[] to = om.readValue(ln.substring(ln.indexOf(':') + 1), int[].class);
//            for (int t : to) {
//                graph.connect(from, t);
//            }
//        }
        int cnt = lineCount(grPath); // lns.size();
        int processedCnt = cnt;
        Path stPath = Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/stack.txt");
        Stream<String> reached = Files.lines(stPath);
        List<SLinerInfo> stack = new ArrayList<>();
        AtomicInteger ai = new AtomicInteger();
        reached.forEach(l -> {
            int i = ai.getAndIncrement();
            short[][] lines = om.readValue(l.substring(l.indexOf("[[")), short[][].class);
            SLiner lnr = new SLiner(lines);
            FixBS canon = lnr.canonByLines();
            SLinerInfo info = syncLiners.computeIfAbsent(canon, ky -> new SLinerInfo().setCanon(ky.words()));
            info.setGraphIdx(i);
            info.setProcessed(i < cnt);
            if (i >= cnt) {
                stack.add(info);
            }
        });
        graph.connect(ai.get() - 1, ai.get() - 1);
        Map<FixBS, SLinerInfo> liners = new HashMap<>(syncLiners);
        syncLiners.clear();
        reached.close();
        //lns.clear();
        content = null;
        System.gc();
        while (!stack.isEmpty() && counter > 0) {
            SLinerInfo info = stack.removeFirst();
            if (info.isProcessed()) {
                continue;
            }
            graph.connect(info.getGraphIdx(), info.getGraphIdx());
            SLiner lnr = SLiner.bySmallCanon(info.getCanon(), v, k);
            List<SLiner> para = lnr.paraModificationsAlt();
            Map<FixBS, SLiner> unique = new ConcurrentHashMap<>();
            para.stream().parallel().forEach(l -> {
                FixBS canon = l.smallCanon();
                unique.putIfAbsent(canon, SLiner.bySmallCanon(canon.words(), v, k));
            });
            for (Map.Entry<FixBS, SLiner> e : unique.entrySet()) {
                SLinerInfo parInfo = liners.computeIfAbsent(e.getKey(), ky -> new SLinerInfo().setCanon(ky.words()));
                if (parInfo.getGraphIdx() == null) {
                    parInfo.setGraphIdx(graph.size());
                    Files.writeString(stPath, graph.size() + " " + Arrays.deepToString(e.getValue().lines()) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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

    private int lineCount(Path path) throws IOException {
        Stream<String> lines = Files.lines(path);
        int result = lines.mapToInt(_ -> 1).sum();
        lines.close();
        return result;
    }

    @Test
    public void integrityCheck() throws IOException {
        int k = 5;
        int v = 65;
        int b = v * (v - 1) / k / (k - 1);
        ObjectMapper om = new ObjectMapper();
        Path stPath = Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/stack.txt");
        Stream<String> reached = Files.lines(stPath);
        Set<FixBS> un = ConcurrentHashMap.newKeySet();
        List<SLinerInfo> stack = new ArrayList<>();
        reached.forEach(l -> {
            short[][] lines = om.readValue(l.substring(l.indexOf("[[")), short[][].class);
            SLiner lnr = new SLiner(lines);
            FixBS canon = lnr.canonByLines();
            SLinerInfo info = new SLinerInfo().setCanon(canon.words());
            stack.add(info);
        });
        System.out.println("Begin");
        AtomicInteger ai = new AtomicInteger();
        Map<Long, Integer> aut = new ConcurrentHashMap<>();
        IntStream.range(0, stack.size()).parallel().forEach(idx -> {
            long[] canon = stack.get(idx).getCanon();
            SLiner byLines = SLiner.bySmallCanon(canon, v, k);
            int[][] lines = new int[b][k];
            for (int i = 0; i < b; i++) {
                for (int j = 0; j < k; j++) {
                    lines[i][j] = byLines.line(i)[j];
                }
            }
            Liner lnr = new Liner(lines);
            SLiner byCanon = SLiner.byCanon(lnr.graphData().canonical(), v, k);
            un.add(byCanon.canonByLines());
            assertEquals(byLines.smallCanon(), byCanon.smallCanon());
            Map<Integer, Long> map = lnr.hyperbolicFreq();
            if (map.size() < 3) {
                System.out.println(idx + " " + map);
            }
            long autCnt = lnr.graphData().autCount();
            if (autCnt > 2) {
                System.out.println(idx + " " + autCnt);
            }
            aut.compute(autCnt, (_, vl) -> vl == null ? 1 : vl + 1);
            int val = ai.incrementAndGet();
            if (val % 10000 == 0) {
                System.out.println(val);
            }
        });
        reached.close();
        System.out.println(un.size() + " " + ai.get() + " " + aut);
    }

    @Test
    public void checkGraph() throws IOException {
        int v = 65;
        int k = 5;
        ObjectMapper om = new ObjectMapper();
        Path grPath = Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/graph.txt");
        Path stPath = Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/stack.txt");
        List<String> graphLines = Files.readAllLines(grPath);
        Map<FixBS, Integer> idxes = new HashMap<>();
        List<FixBS> canons = new ArrayList<>();
        AtomicInteger ai = new AtomicInteger();
        Stream<String> lns = Files.lines(stPath);
        lns.forEach(ln -> {
            short[][] arr = om.readValue(ln.substring(ln.indexOf("[[")), short[][].class);
            SLiner sl = new SLiner(arr);
            FixBS canon = sl.canonByLines();
            canons.add(canon);
            idxes.putIfAbsent(canon, ai.getAndIncrement());
        });
        lns.close();
        int tried = 10000;
        System.out.println("Begin");
        Set<Integer> incorrect = ConcurrentHashMap.newKeySet();
        IntStream.range(0, tried).parallel().forEach(_ -> {
            int idx = ThreadLocalRandom.current().nextInt(canons.size());
            SLiner lnr = SLiner.bySmallCanon(canons.get(idx).words(), v, k);
            List<SLiner> para = lnr.paraModificationsAlt();
            Set<FixBS> unique = ConcurrentHashMap.newKeySet();
            para.stream().parallel().forEach(l -> {
                FixBS canon = l.smallCanon();
                unique.add(canon);
            });
            int[] arr = unique.stream().mapToInt(idxes::get).filter(i -> i != idx).sorted().toArray();
            String fl = graphLines.get(idx);
            int[] inFile = om.readValue(fl.substring(fl.indexOf('[')), int[].class);
            if (!Arrays.equals(arr, inFile)) {
                System.out.println(idx + " " + Arrays.toString(arr) + " " + Arrays.toString(inFile));
                incorrect.add(idx);
            }
        });
        int[] wholeArr = incorrect.stream().mapToInt(Integer::intValue).sorted().toArray();
        if (wholeArr.length == 0) {
            System.out.println("Correct");
        } else {
            System.out.println(wholeArr[0] + " " + wholeArr[wholeArr.length - 1]);
        }
    }

    @Test
    public void storeGraphPart() throws IOException {
        int v = 65;
        int k = 5;
        ObjectMapper om = new ObjectMapper();
        Path stPath = Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/stack.txt");
        Map<FixBS, Integer> idxes = new HashMap<>();
        List<FixBS> canons = new ArrayList<>();
        AtomicInteger ai = new AtomicInteger();
        Stream<String> lns = Files.lines(stPath);
        lns.forEach(ln -> {
            short[][] arr = om.readValue(ln.substring(ln.indexOf("[[")), short[][].class);
            SLiner sl = new SLiner(arr);
            FixBS canon = sl.canonByLines();
            canons.add(canon);
            idxes.putIfAbsent(canon, ai.getAndIncrement());
        });
        lns.close();
        int from = 8200000;
        int cnt = 200000;
        Path partPath = Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/gfix.txt");
        ai.set(0);
        IntStream.range(from, from + cnt).parallel().forEach(idx -> {
            SLiner lnr = SLiner.bySmallCanon(canons.get(idx).words(), v, k);
            List<SLiner> para = lnr.paraModificationsAlt();
            Set<FixBS> unique = ConcurrentHashMap.newKeySet();
            para.stream().parallel().forEach(l -> {
                FixBS canon = l.smallCanon();
                unique.add(canon);
            });
            int[] arr = unique.stream().mapToInt(idxes::get).filter(i -> i != idx).sorted().toArray();
            try {
                Files.writeString(partPath, idx + ": " + Arrays.toString(arr)
                                + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int val = ai.incrementAndGet();
            if (val % 100 == 0) {
                System.out.println(val);
            }
        });
    }

    @Test
    public void merge() throws IOException {
        int v = 65;
        int k = 5;
        Path from = Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/graph.txt");
        Path with = Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/gfix.txt");
        Path to = Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/graph1.txt");
        List<String> lns = Files.readAllLines(from);
        List<String> withLns = Files.readAllLines(with);
        Map<Integer, String> map = withLns.stream().collect(Collectors.toMap(l -> Integer.parseInt(l.substring(0, l.indexOf(':'))), Function.identity()));
        for (int i = 0; i < lns.size(); i++) {
            if (map.containsKey(i)) {
                Files.writeString(to, map.get(i)
                        + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(to, lns.get(i)
                        + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        }
    }

    @Test
    public void convert() throws IOException {
        int v = 65;
        int k = 5;
        String fName = "stack";
        ObjectMapper om = new ObjectMapper();
        List<String> content = Files.readAllLines(Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/" + fName + ".txt"));
        FixBS[] converted = new FixBS[content.size()];
        IntStream.range(0, content.size()).parallel().forEach(idx -> {
            String l = content.get(idx);
            int[][] lines = om.readValue(l.substring(l.indexOf("[[")), int[][].class);
            converted[idx] = SLiner.byCanon(new Liner(lines).graphData().canonical(), v, k).canonByLines();
        });
        Path target = Path.of("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph/large/" + fName + "Conv.txt");
        for (int i = 0; i < content.size(); i++) {
            Files.writeString(target,
                    i + " " + Arrays.deepToString(SLiner.bySmallCanon(converted[i].words(), v, k).lines()) + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    @Test
    public void inspectGraph() throws IOException {
        int v = 65;
        int k = 5;
        ObjectMapper om = new ObjectMapper();
        Map<Long, Integer> freq = new ConcurrentHashMap<>();
        Map<Long, Integer> thrMap = new ConcurrentHashMap<>();
        for (File f : Objects.requireNonNull(new File("/home/ihromant/maths/g-spaces/final/" + k + "-" + v + "/graph").listFiles())) {
            if (f.getName().contains("graph") || f.getName().contains("base")) {
                continue;
            }
            boolean three = f.getName().equals("3liners.txt");
            Stream<String> str = Files.lines(f.toPath());
            str.parallel().forEach(l -> {
                int[][] lns = om.readValue(l.substring(l.indexOf("[[")), int[][].class);
                Liner lnr = new Liner(lns);
                long cnt = lnr.graphData().autCount();
                freq.compute(cnt, (_, val) -> val == null ? 1 : val + 1);
                if (three) {
                    thrMap.compute(cnt, (_, val) -> val == null ? 1 : val + 1);
                }
            });
            str.close();
        }
        System.out.println(freq);
        System.out.println(thrMap);
    }
}
