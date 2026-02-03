package ua.ihromant.mathutils.nauty;

public interface GraphWrapper {
    int size();

    int color(int idx);

    boolean edge(int a, int b);
}
