package ua.ihromant.mathutils.mini;

public final class SearchResult {
    public final int[] rowPerm;   // rowPerm[i] = original row placed at position i
    public final int[] colPerm;   // colPerm[j] = original column placed at position j
    public final String value;    // the maximal matrix, row-major
    public final long nodes;      // search-tree nodes visited
    public final int automorphisms;

    SearchResult(int[] r, int[] c, String v, long nodes, int auts) {
        rowPerm = r;
        colPerm = c;
        value = v;
        this.nodes = nodes;
        automorphisms = auts;
    }
}
