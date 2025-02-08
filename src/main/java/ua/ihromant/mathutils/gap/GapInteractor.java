package ua.ihromant.mathutils.gap;

import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class GapInteractor {
    public static final String ANSI_RED = "\u001B[31m";
    private final Process process;

    public GapInteractor() throws IOException {
        this.process = new ProcessBuilder().directory(new File("/home/ihromant/maths/gap-4.14.0")).command("./gap").start();
    }

    public static void main(String[] args) throws IOException {
        GapInteractor inter = new GapInteractor();
        Group g = new CyclicProduct(3, 3);
        inter.process.outputWriter().write("LoadPackage(\"LOOPS\");\n");
        inter.process.outputWriter().write("IsPackageLoaded(\"LOOPS\");\n");
        inter.process.outputWriter().write("g := AutomorphismGroup(IntoGroup(LoopByCayleyTable(" + Arrays.deepToString(g.asTable().table()) + ")));\n");
        inter.process.outputWriter().write("CayleyTable(IntoLoop(g));\n");
        inter.process.outputWriter().write("quit;\n");
        inter.process.outputWriter().flush();
        inter.process.inputReader().lines().dropWhile(l -> !l.contains("true")).forEach(l -> {
            System.out.println(l);
//            int idx = l.indexOf(ANSI_RED);
//            if (idx < 0) {
//                return;
//            }
//            System.out.println(l.substring(idx + ANSI_RED.length()));
        });
        System.out.println(inter.process.isAlive());
        inter.process.destroyForcibly();
        System.out.println(inter.process.isAlive());
    }
}
