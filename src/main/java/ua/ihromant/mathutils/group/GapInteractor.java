package ua.ihromant.mathutils.group;

import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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
}
