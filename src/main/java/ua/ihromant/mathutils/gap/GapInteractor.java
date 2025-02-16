package ua.ihromant.mathutils.gap;

import com.fasterxml.jackson.databind.ObjectMapper;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.TableGroup;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

public class GapInteractor {
    public static final String ANSI_RED = "\u001B[31m";
    private final Process process;
    private final ObjectMapper om = new ObjectMapper();

    public GapInteractor() throws IOException {
        this.process = new ProcessBuilder().directory(new File("/home/ihromant/maths/gap-4.14.0")).command("./gap").start();
    }

    public Group smallGroup(int order, int index) throws IOException {
        GapInteractor inter = new GapInteractor();
        inter.process.outputWriter().write("LoadPackage(\"LOOPS\");\n");
        inter.process.outputWriter().write("IsPackageLoaded(\"LOOPS\");\n");
        inter.process.outputWriter().write("CayleyTable(IntoLoop(SmallGroup(" + order + "," + index + ")));\n");
        inter.process.outputWriter().write("quit;\n");
        inter.process.outputWriter().flush();
        String joined = inter.process.inputReader().lines().dropWhile(l -> !l.contains("true")).skip(2).map(l -> {
            int idx = l.indexOf(ANSI_RED);
            return idx < 0 ? l : l.substring(idx + ANSI_RED.length());
        }).collect(Collectors.joining());
        int[][] arr = om.readValue(joined, int[][].class);
        int[][] table = new int[order][order];
        for (int i = 0; i < order; i++) {
            for (int j = 0; j < order; j++) {
                table[i][j] = arr[i][j] - 1;
            }
        }
        inter.process.destroyForcibly();
        return new TableGroup(table);
    }

    public static Group group(int order, int index) throws IOException {
        System.out.println("Reading SmallGroup(" + order + "," + index + ")");
        return new GapInteractor().smallGroup(order, index);
    }
}
