package ua.ihromant.mathutils.group;

import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GapInteractor {
    public static final String ANSI_RED = "\u001B[31m";
    private final Process process;
    private final ObjectMapper om = new ObjectMapper();

    public GapInteractor() throws IOException {
        this.process = new ProcessBuilder().directory(new File("/home/ihromant/maths/gap-4.14.0")).command("./gap").start();
    }

    public Group smallGroup(int order, int index) throws IOException {
        process.outputWriter().write("LoadPackage(\"LOOPS\");\n");
        process.outputWriter().write("IsPackageLoaded(\"LOOPS\");\n");
        process.outputWriter().write("CayleyTable(IntoLoop(SmallGroup(" + order + "," + index + ")));\n");
        process.outputWriter().write("quit;\n");
        process.outputWriter().flush();
        String joined = process.inputReader().lines().dropWhile(l -> !l.contains("true")).skip(2).map(l -> {
            int idx = l.lastIndexOf(ANSI_RED);
            return idx < 0 ? l : l.substring(idx + ANSI_RED.length());
        }).collect(Collectors.joining());
        int[][] arr = om.readValue(joined, int[][].class);
        int[][] table = new int[order][order];
        for (int i = 0; i < order; i++) {
            for (int j = 0; j < order; j++) {
                table[i][j] = arr[i][j] - 1;
            }
        }
        process.destroyForcibly();
        return new TableGroup("SG(" + order + "," + index + ")", table);
    }

    public String identifyGroup(Group g) throws IOException {
        process.outputWriter().write("LoadPackage(\"LOOPS\");\n");
        process.outputWriter().write("IsPackageLoaded(\"LOOPS\");\n");
        String repr = Arrays.deepToString(g.asTable().table());
        process.outputWriter().write("StructureDescription(IntoGroup(LoopByCayleyTable(" + repr + ")));\n");
        process.outputWriter().write("quit;\n");
        process.outputWriter().flush();
        String joined = process.inputReader().lines().dropWhile(l -> !l.contains("true")).skip(2).map(l -> {
            int idx = l.lastIndexOf(ANSI_RED);
            return idx < 0 ? l : l.substring(idx + ANSI_RED.length());
        }).collect(Collectors.joining());
        process.destroyForcibly();
        return joined;
    }

    public int groupId(Group g) throws IOException {
        process.outputWriter().write("LoadPackage(\"LOOPS\");\n");
        process.outputWriter().write("IsPackageLoaded(\"LOOPS\");\n");
        String repr = Arrays.deepToString(g.asTable().table());
        process.outputWriter().write("IdGroup(IntoGroup(LoopByCayleyTable(" + repr + ")));\n");
        process.outputWriter().write("quit;\n");
        process.outputWriter().flush();
        String joined = process.inputReader().lines().dropWhile(l -> !l.contains("true")).skip(2).map(l -> {
            int idx = l.lastIndexOf(ANSI_RED);
            return idx < 0 ? l : l.substring(idx + ANSI_RED.length());
        }).collect(Collectors.joining());
        process.destroyForcibly();
        return om.readValue(joined, int[].class)[1];
    }

    public int groupCount(int order) throws IOException {
        process.outputWriter().write("IsPackageLoaded(\"LOOPS\");\n");
        process.outputWriter().write("NumberSmallGroups(" + order + ");\n");
        process.outputWriter().write("quit;\n");
        process.outputWriter().flush();
        int result = Integer.parseInt(process.inputReader().lines().dropWhile(l -> !l.contains("false")).skip(1).map(l -> {
            int idx = l.lastIndexOf(ANSI_RED);
            return idx < 0 ? l : l.substring(idx + ANSI_RED.length());
        }).findFirst().orElseThrow());
        process.destroyForcibly();
        return result;
    }

    public List<List<List<Integer>>> gapCycles(int order, int index) throws IOException {
        process.outputWriter().write("IsPackageLoaded(\"LOOPS\");\n");
        process.outputWriter().write("Print(PrintString(GeneratorsOfGroup(Image(IsomorphismPermGroup(SmallGroup(" + order + "," + index + "))))));\n");
        process.outputWriter().write("quit;\n");
        process.outputWriter().flush();
        String line = process.inputReader().lines().dropWhile(l -> !l.contains("false")).skip(1).map(l -> {
            int idx = l.lastIndexOf(ANSI_RED);
            String ln = idx < 0 ? l : l.substring(idx + ANSI_RED.length());
            return ln.replace(" ", "").replace("\\", "");
        }).collect(Collectors.joining());
        process.destroyForcibly();
        List<List<List<Integer>>> cycles = new ArrayList<>();
        cycles.add(new ArrayList<>());
        cycles.getLast().add(new ArrayList<>());
        int from = findNext(line, 0);
        int to = findNext(line, from);
        char c;
        while ((c = line.charAt(to)) != ']') {
            cycles.getLast().getLast().add(Integer.parseInt(line.substring(from + 1, to)));
            from = to;
            to = findNext(line, from);
            if (c == ')') {
                char next = line.charAt(to);
                if (next == ',') {
                    cycles.add(new ArrayList<>());
                    cycles.getLast().add(new ArrayList<>());
                    from = to;
                    to = findNext(line, from);
                    from = to;
                    to = findNext(line, from);
                }
                if (next == '(') {
                    cycles.getLast().add(new ArrayList<>());
                    from = to;
                    to = findNext(line, from);
                }
            }
        }
        return cycles;
    }

    private static int findNext(String s, int from) {
        for (int i = from + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isDigit(c)) {
                return i;
            }
        }
        return s.length();
    }

    public String identifyByCycles(List<List<List<Integer>>> cycles) throws IOException {
        process.outputWriter().write("IsPackageLoaded(\"LOOPS\");\n");
        String repr = cycles.stream().map(base -> {
            return base.stream().map(l -> l.stream().map(Object::toString)
                    .collect(Collectors.joining(",", "(", ")"))).collect(Collectors.joining());
        }).collect(Collectors.joining(",", "[", "]"));
        process.outputWriter().write("StructureDescription(Group(" + repr + "));\n");
        process.outputWriter().write("quit;\n");
        process.outputWriter().flush();
        String joined = process.inputReader().lines().dropWhile(l -> !l.contains("false")).skip(1).map(l -> {
            int idx = l.lastIndexOf(ANSI_RED);
            return idx < 0 ? l : l.substring(idx + ANSI_RED.length());
        }).collect(Collectors.joining());
        process.destroyForcibly();
        return joined;
    }
}
