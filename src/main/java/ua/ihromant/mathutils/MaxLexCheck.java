package ua.ihromant.mathutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Checks whether a 0/1 matrix (rows = lines as bitmasks, bit i = column i, at most 64 columns) is
 * lexicographically maximal (row-major, bit 0 first) under all row and column permutations.
 *
 * <p>Search: rows are placed one at a time and must reproduce the target (the matrix itself) row by row.
 * Columns are kept as an ordered partition. Each item is either a plain cell (columns that are interchangeable)
 * or a group of interchangeable blocks (see below). A row's best appearance is computed by sorting
 * inside each item, and placing it refines the items.
 *
 * <p>Parallel-class runs: suppose several tied candidate rows are identical outside one plain cell C, and inside C
 * they occupy pairwise disjoint blocks of equal size (e.g. the lines through a point, the "pencil"). Then every
 * order of these rows gives the same row strings; the order only fixes the order of their blocks. Instead of
 * branching over all s! orders, the whole class is placed in one step and its blocks become a group of
 * interchangeable blocks. Later rows sort the blocks in their own best order. Interleavings, where a non-class row
 * is placed in between, are tested exactly beforehand. If one could tie, the search falls back to ordinary
 * branching for that class, so the result is always exact.
 *
 * <p>Automorphisms found at leaves prune orbits (nauty-style, with backjumping to the first leaf's path).
 *
 * <p>{@link Prepared}: for orderly generation, where one lex-max base is tested against many candidate lines.
 * The base's tie-tree is built once. Each candidate is then checked by walking that tree with a single
 * {@code maxRow} per node, and a real sub-search runs only where the candidate itself ties.
 */
final class MaxLexCheck {
    private static final int FAIL = -1;
    private static final int NONE = Integer.MAX_VALUE;
    private static final int FALLBACK = Integer.MAX_VALUE - 1;
    private static final int MAX_AUTS = 5000;
    /** Prepared base trees larger than this are not cached; candidates then use the plain check. */
    private static final int MAX_CACHED_NODES = 200_000;

    /** Interchangeable blocks: blocks[j][t] = t-th internal cell of block j. Cell sizes are equal across blocks. */
    private record Group(long[][] blocks, int[] owners, int width) {}

    /** Ordered items. groups[i] == null means masks[i] is a plain cell; otherwise masks[i] = union of the group. */
    private record State(long[] masks, Group[] groups) {}

    /** Result of analysing a parallel-class run at some node. */
    private static final class Run {
        int[] members;
        long cell;
        int posC, w, matched;
        State stA;     // state refined by the class's common part
        State after;   // state after the full run (null if the run dies early)
    }

    private final long[] lines;
    private final int b;
    private final long all;
    private final int[] rows;
    private final boolean[] used;
    private final boolean useAuts;
    private final List<int[]> auts = new ArrayList<>();
    private final int[] blockPos;
    private final int[] runStart;
    private final int[][] runMembers;
    private int runCount;
    private final int[] choice, span, firstChoice, firstRows;
    private boolean haveFirst;
    // scratch
    private final long[] scratchS = new long[64];
    private final long[] nm = new long[64];
    private final Group[] ng = new Group[64];

    private MaxLexCheck(long[] lines, int v, boolean useAuts) {
        this.lines = lines;
        this.b = lines.length;
        this.all = allPts(v);
        this.useAuts = useAuts;
        this.rows = new int[b];
        this.used = new boolean[b];
        this.blockPos = new int[b];
        this.runStart = new int[b];
        this.runMembers = new int[b][];
        this.choice = new int[b];
        this.span = new int[b];
        this.firstChoice = new int[b];
        this.firstRows = new int[b];
    }

    // ================================================================== entry points

    /** Full check of the whole matrix. */
    static boolean isMaxLex(long[] lines, int v) {
        if (lines.length == 0) return true;
        if (!cheapCheckAll(lines, v)) return false;
        return new MaxLexCheck(lines, v, true).run();
    }

    /** Checks base + line, assuming {@code base} is already lex-max. */
    static boolean isMaxLexWith(long[] base, int v, long line) {
        if (!cheapCheckLast(base, finalCells(base, v), line)) return false;
        long[] all = Arrays.copyOf(base, base.length + 1);
        all[base.length] = line;
        return new MaxLexCheck(all, v, true).run();
    }

    static Prepared prepare(long[] base, int v) {
        return new Prepared(base, v);
    }

    private static boolean cheapCheckAll(long[] lines, int v) {
        long[] cells = {allPts(v)};
        for (int r = 0; r < lines.length; r++) {
            long line = lines[r];
            if (r > 0 && cmp(line, lines[r - 1]) > 0) return false;
            if (maxRowPlain(line, cells) != line) return false;
            cells = refinePlain(line, cells);
        }
        return true;
    }

    private static long[] finalCells(long[] base, int v) {
        long[] cells = {allPts(v)};
        for (long l : base) cells = refinePlain(l, cells);
        return cells;
    }

    private static boolean cheapCheckLast(long[] base, long[] cells, long line) {
        int n = base.length;
        if (n > 0 && cmp(line, base[n - 1]) > 0) return false;
        return maxRowPlain(line, cells) == line;
    }

    private boolean run() {
        seedDuplicateRows();
        return dfs(0, initialState()) != FAIL;
    }

    private State initialState() {
        return new State(new long[]{all}, new Group[1]);
    }

    private void seedDuplicateRows() {
        if (!useAuts) return;
        for (int r = 1; r < b; r++) {
            if (lines[r] == lines[r - 1]) {
                int[] g = identityPerm(b);
                g[r] = r - 1;
                g[r - 1] = r;
                auts.add(g);
            }
        }
    }

    private static long allPts(int v) {
        return v == 64 ? -1L : (1L << v) - 1L;
    }

    // ================================================================== search

    private int dfs(int depth, State st) {
        if (depth == b) return leaf(st);
        long target = lines[depth];

        int[] cand = new int[b - depth];
        int cc = 0;
        for (int r = 0; r < b; r++) {
            if (used[r]) continue;
            int c = cmp(maxRow(lines[r], st), target);
            if (c > 0) return FAIL;          // a strictly larger row exists here
            if (c == 0) cand[cc++] = r;
        }
        if (cc == 0) return NONE;            // every continuation is smaller: prune

        int[][] classes = parallelClasses(st, cand, cc);
        int[] done = new int[cc];
        int doneCount = 0;
        boolean[] inRun = new boolean[b];
        for (int[] cls : classes) {
            Run run = analyzeRun(depth, st, cls[0], Arrays.copyOfRange(cls, 1, cls.length));
            if (run == null) return FAIL;
            if (run.members == null) continue;                  // FALLBACK: branch on members individually
            for (int m : run.members) { inRun[m] = true; done[doneCount++] = m; }
            if (run.after == null) continue;                     // run dies before completing
            int jump = descendRun(depth, run);
            if (jump < depth) return jump;
        }

        int[] parent = null;
        int parentAuts = -1;
        for (int i = 0; i < cc; i++) {
            int r = cand[i];
            if (inRun[r]) continue;
            if (useAuts && doneCount > 0 && !auts.isEmpty()) {
                if (parentAuts != auts.size()) {
                    parent = orbits();
                    parentAuts = auts.size();
                }
                if (parent != null && sameOrbit(parent, r, done, doneCount)) continue;
            }
            done[doneCount++] = r;
            int jump = descendSingle(depth, st, r);
            if (jump < depth) return jump;
        }
        return NONE;
    }

    private int descendSingle(int depth, State st, int r) {
        State next = refine(lines[r], st);
        rows[depth] = r;
        used[r] = true;
        choice[depth] = r;
        span[depth] = 1;
        int jump = dfs(depth + 1, next);
        used[r] = false;
        return jump;
    }

    private int descendRun(int depth, Run run) {
        int[] members = run.members;
        for (int m : members) used[m] = true;
        runStart[runCount] = depth;
        runMembers[runCount++] = members;
        choice[depth] = -2 - members[0];
        span[depth] = members.length;
        int jump = dfs(depth + members.length, run.after);
        runCount--;
        for (int m : members) used[m] = false;
        return jump;
    }

    /**
     * Parallel classes among tied candidates: rows identical outside one plain cell and pairwise disjoint inside
     * it. Each returned array is {cellIndex, member...} with at least two members.
     */
    private int[][] parallelClasses(State st, int[] cand, int cc) {
        if (cc < 2) return new int[0][];
        int[] classOf = new int[cc];
        Arrays.fill(classOf, -1);
        int[] classCell = new int[cc];
        int[] classSize = new int[cc];
        int classes = 0;
        for (int i = 0; i < cc; i++) {
            if (classOf[i] >= 0) continue;
            long li = lines[cand[i]];
            for (int j = i + 1; j < cc; j++) {
                if (classOf[j] >= 0) continue;
                long lj = lines[cand[j]];
                int item = plainItemContaining(st, li ^ lj);
                if (item < 0 || (li & lj & st.masks[item]) != 0) continue;
                if (classOf[i] < 0) {
                    classOf[i] = classes;
                    classCell[classes] = item;
                    classSize[classes++] = 1;
                } else if (classCell[classOf[i]] != item) {
                    continue;
                }
                classOf[j] = classOf[i];
                classSize[classOf[i]]++;
            }
        }
        int[][] res = new int[classes][];
        int valid = 0;
        for (int k = 0; k < classes; k++) {
            int[] cls = new int[classSize[k] + 1];
            cls[0] = classCell[k];
            for (int i = 0, o = 1; i < cc; i++) if (classOf[i] == k) cls[o++] = cand[i];
            long seen = 0, cell = st.masks[classCell[k]];
            boolean ok = true;
            for (int t = 1; t < cls.length && ok; t++) {
                long part = lines[cls[t]] & cell;
                ok = (seen & part) == 0;
                seen |= part;
            }
            if (ok) res[valid++] = cls;
        }
        return valid == classes ? res : Arrays.copyOf(res, valid);
    }

    /**
     * Analyses placing the whole class at depths depth..depth+s-1. Returns null for FAIL (a larger matrix
     * exists), a Run with members == null for FALLBACK, otherwise the run (after == null if it dies early).
     * Every other unused row is tested as a possible interleaving row.
     */
    private Run analyzeRun(int depth, State st, int cellIdx, int[] members) {
        int s = members.length;
        long cell = st.masks[cellIdx];
        long x0 = lines[members[0]];
        long common = x0 & ~cell;
        Run run = new Run();
        run.cell = cell;
        run.w = Long.bitCount(x0 & cell);
        run.stA = refine(common, st);                 // the cell itself is untouched (common ∩ cell = ∅)
        int idxA = -1;
        for (int i = 0, pos = 0; i < run.stA.masks.length; i++) {
            if (run.stA.groups[i] == null && run.stA.masks[i] == cell) { idxA = i; run.posC = pos; break; }
            pos += Long.bitCount(run.stA.masks[i]);
        }
        long outside = maxRow(common, run.stA);
        long block = ones(run.w);
        for (int j = 0; j < s; j++) {
            int c = cmp(outside | (block << (run.posC + j * run.w)), lines[depth + j]);
            if (c > 0) return null;
            if (c < 0) break;
            run.matched++;
        }
        run.members = members;
        for (int z = 0; z < b; z++) {
            if (used[z] || contains(members, z)) continue;
            int v = interleave(run, depth, lines[z]);
            if (v == FAIL) return null;
            if (v == FALLBACK) { run.members = null; return run; }
        }
        if (run.matched < s) return run;

        long union = 0;
        long[][] blocks = new long[s][];
        for (int j = 0; j < s; j++) {
            long bl = lines[members[j]] & cell;
            blocks[j] = new long[]{bl};
            union |= bl;
        }
        long rest = cell & ~union;
        State stA = run.stA;
        int n = stA.masks.length + (rest != 0 ? 1 : 0);
        long[] masks = new long[n];
        Group[] groups = new Group[n];
        for (int i = 0, o = 0; i < stA.masks.length; i++) {
            if (i == idxA) {
                masks[o] = union;
                groups[o++] = new Group(blocks, members.clone(), run.w);
                if (rest != 0) masks[o++] = rest;
            } else {
                masks[o] = stA.masks[i];
                groups[o++] = stA.groups[i];
            }
        }
        run.after = new State(masks, groups);
        return run;
    }

    /**
     * Can row z, placed right after i (1..) members of the run, tie or beat the target?
     * Returns FAIL (beats), FALLBACK (ties) or NONE.
     */
    private int interleave(Run run, int depth, long lz) {
        int s = run.members.length;
        int maxI = Math.min(run.matched, s - 1);
        if (maxI < 1) return NONE;
        long cell = run.cell;
        int posC = run.posC, w = run.w;
        long outZ = maxRow(lz & ~cell, run.stA);
        int[] cnt = new int[s];
        for (int j = 0; j < s; j++) cnt[j] = Long.bitCount(lz & lines[run.members[j]] & cell);
        Arrays.sort(cnt);
        int total = Long.bitCount(lz & cell);
        int taken = 0;
        long slots = 0;
        for (int i = 1; i <= maxI; i++) {
            int top = cnt[s - i];
            slots |= ones(top) << (posC + (i - 1) * w);
            taken += top;
            long str = outZ | slots | (ones(total - taken) << (posC + i * w));
            int c = cmp(str, lines[depth + i]);
            if (c > 0) return FAIL;
            if (c == 0) return FALLBACK;
        }
        return NONE;
    }

    private int leaf(State st) {
        if (!useAuts) return NONE;
        if (runCount > 0) {
            for (int i = 0, pos = 0; i < st.masks.length; i++) {
                Group g = st.groups[i];
                if (g != null) {
                    for (int j = 0; j < g.owners.length; j++) blockPos[g.owners[j]] = pos + j * g.width;
                }
                pos += Long.bitCount(st.masks[i]);
            }
            for (int k = 0; k < runCount; k++) {
                int[] mem = runMembers[k].clone();
                for (int i = 1; i < mem.length; i++) {
                    int x = mem[i], j = i - 1;
                    while (j >= 0 && blockPos[mem[j]] > blockPos[x]) { mem[j + 1] = mem[j]; j--; }
                    mem[j + 1] = x;
                }
                System.arraycopy(mem, 0, rows, runStart[k], mem.length);
            }
        }
        if (!haveFirst) {
            haveFirst = true;
            System.arraycopy(rows, 0, firstRows, 0, b);
            System.arraycopy(choice, 0, firstChoice, 0, b);
            return NONE;
        }
        // same matrix as the first leaf -> automorphism firstRows[i] -> rows[i]
        if (auts.size() < MAX_AUTS) {
            int[] g = new int[b];
            for (int i = 0; i < b; i++) g[firstRows[i]] = rows[i];
            auts.add(g);
        }
        // backjump to the frame where this path left the first leaf's path
        for (int p = 0; p < b; p += span[p]) {
            if (choice[p] != firstChoice[p]) return p;
        }
        return NONE;
    }

    // ================================================================== prepared base (tree cache)

    /**
     * One lex-max base, many candidate lines. Use from a single thread; create one per base.
     */
    static final class Prepared {
        private final long[] base;
        private final int v;
        private final int n;
        private final long[] cells;
        private final long[] all;      // base + slot for the candidate
        private Node root;
        private boolean built, tooBig;
        private int nodeCount;

        private Prepared(long[] base, int v) {
            this.base = base;
            this.v = v;
            this.n = base.length;
            this.cells = finalCells(base, v);
            this.all = Arrays.copyOf(base, n + 1);
        }

        boolean test(long line) {
            if (!cheapCheckLast(base, cells, line)) return false;
            if (!built) build();
            all[n] = line;
            if (tooBig) return new MaxLexCheck(all.clone(), v, true).run();
            MaxLexCheck sub = new MaxLexCheck(all, v, false);
            return sub.visit(root, line, n) != FAIL;
        }

        private void build() {
            built = true;
            MaxLexCheck m = new MaxLexCheck(base, v, false);
            root = m.buildNode(0, m.initialState(), this);
            if (root == null) tooBig = true;
        }
    }

    /** Node of the base's tie-tree (no symmetry pruning: the candidate may break the base's symmetry). */
    private static final class Node {
        int depth;
        State st;
        boolean[] used;
        int[] singles;
        Node[] singleChild;
        Run[] runs;
        Node[] runChild;
    }

    /** Builds the tie-tree of this (base) matrix. Returns null if it exceeds the size cap. */
    private Node buildNode(int depth, State st, Prepared p) {
        if (++p.nodeCount > MAX_CACHED_NODES) return null;
        Node nd = new Node();
        nd.depth = depth;
        nd.st = st;
        nd.used = used.clone();
        if (depth == b) return nd;
        long target = lines[depth];
        int[] cand = new int[b - depth];
        int cc = 0;
        for (int r = 0; r < b; r++) {
            if (used[r]) continue;
            int c = cmp(maxRow(lines[r], st), target);
            if (c > 0) throw new IllegalStateException("base is not lex-max");
            if (c == 0) cand[cc++] = r;
        }
        boolean[] inRun = new boolean[b];
        List<Run> runs = new ArrayList<>();
        List<Node> runChild = new ArrayList<>();
        for (int[] cls : parallelClasses(st, cand, cc)) {
            Run run = analyzeRun(depth, st, cls[0], Arrays.copyOfRange(cls, 1, cls.length));
            if (run == null) throw new IllegalStateException("base is not lex-max");
            if (run.members == null) continue;
            for (int m : run.members) inRun[m] = true;
            Node child = null;
            if (run.after != null) {
                for (int m : run.members) used[m] = true;
                child = buildNode(depth + run.members.length, run.after, p);
                for (int m : run.members) used[m] = false;
                if (child == null) return null;
            }
            runs.add(run);
            runChild.add(child);
        }
        int singles = 0;
        for (int i = 0; i < cc; i++) if (!inRun[cand[i]]) singles++;
        nd.singles = new int[singles];
        nd.singleChild = new Node[singles];
        for (int i = 0, o = 0; i < cc; i++) {
            int r = cand[i];
            if (inRun[r]) continue;
            used[r] = true;
            Node child = buildNode(depth + 1, refine(lines[r], st), p);
            used[r] = false;
            if (child == null) return null;
            nd.singles[o] = r;
            nd.singleChild[o++] = child;
        }
        nd.runs = runs.toArray(new Run[0]);
        nd.runChild = runChild.toArray(new Node[0]);
        return nd;
    }

    /**
     * Walks the base's tie-tree for candidate {@code line} (row index {@code lr} in this matrix = base + line).
     * Every node of the tree is a tied prefix of base rows; the candidate can only change things where it ties
     * or beats the target itself, or interleaves into a run.
     */
    private int visit(Node nd, long line, int lr) {
        int depth = nd.depth;
        int c = cmp(maxRow(line, nd.st), lines[depth]);
        if (c > 0) return FAIL;
        if (depth == lr) return NONE;                 // all base rows placed; candidate is not larger than itself
        if (c == 0) {                                 // candidate ties here: explore it as the row at this depth
            if (subSearch(nd, lr) == FAIL) return FAIL;
        }
        for (int i = 0; i < nd.runs.length; i++) {
            Run run = nd.runs[i];
            int iv = interleave(run, depth, line);
            if (iv == FAIL) return FAIL;
            if (iv == FALLBACK) {                     // the run is not valid any more: branch on members
                for (int m : run.members) if (subSearch(nd, m) == FAIL) return FAIL;
            } else if (nd.runChild[i] != null) {
                if (visit(nd.runChild[i], line, lr) == FAIL) return FAIL;
            }
        }
        for (int i = 0; i < nd.singles.length; i++) {
            if (visit(nd.singleChild[i], line, lr) == FAIL) return FAIL;
        }
        return NONE;
    }

    /** Fresh search below node nd with row r placed at nd.depth (no symmetry pruning). */
    private int subSearch(Node nd, int r) {
        System.arraycopy(nd.used, 0, used, 0, nd.used.length);
        used[b - 1] = false;
        used[r] = true;
        int res = dfs(nd.depth + 1, refine(lines[r], nd.st));
        Arrays.fill(used, false);
        return res;
    }

    // ================================================================== rows and refinement

    /** Lexicographically largest image of {@code line} under the freedom left by the state. */
    private long maxRow(long line, State st) {
        long out = 0;
        int pos = 0;
        long[] masks = st.masks;
        Group[] groups = st.groups;
        for (int i = 0; i < masks.length; i++) {
            long m = masks[i];
            Group g = groups[i];
            if (g == null) {
                int k = Long.bitCount(m & line);
                if (k != 0) out |= ones(k) << pos;
                pos += Long.bitCount(m);
            } else {
                int q = g.blocks.length, wd = g.width;
                if ((m & line) != 0) {
                    long[] s = scratchS;
                    blockStrings(g, line, s);
                    sortDesc(s, q, null);
                    for (int j = 0; j < q && s[j] != 0; j++) out |= s[j] << (pos + j * wd);
                }
                pos += q * wd;
            }
        }
        return out;
    }

    private State refine(long line, State st) {
        long[] masks = st.masks;
        Group[] groups = st.groups;
        int o = 0, pos = 0;
        boolean changed = false;
        for (int i = 0; i < masks.length; i++) {
            long m = masks[i];
            Group g = groups[i];
            long on = m & line;
            if (g == null) {
                long off = m & ~line;
                if (on != 0) { nm[o] = on; ng[o++] = null; }
                if (off != 0) { nm[o] = off; ng[o++] = null; }
                if (on != 0 && off != 0) changed = true;
                pos += Long.bitCount(m);
                continue;
            }
            int q = g.blocks.length, wd = g.width;
            if (on == 0) {
                nm[o] = m;
                ng[o++] = g;
                pos += q * wd;
                continue;
            }
            changed = true;
            long[] s = new long[q];
            blockStrings(g, line, s);
            int[] idx = identityPerm(q);
            sortDesc(s, q, idx);
            int j = 0;
            while (j < q) {
                int e = j + 1;
                while (e < q && s[e] == s[j]) e++;
                if (e - j == 1) {
                    int bi = idx[j];
                    blockPos[g.owners[bi]] = pos + j * wd;
                    for (long c : g.blocks[bi]) {
                        long c1 = c & line, c0 = c & ~line;
                        if (c1 != 0) { nm[o] = c1; ng[o++] = null; }
                        if (c0 != 0) { nm[o] = c0; ng[o++] = null; }
                    }
                } else {
                    int cnt = e - j;
                    long[][] nb = new long[cnt][];
                    int[] own = new int[cnt];
                    long union = 0;
                    for (int t = 0; t < cnt; t++) {
                        int bi = idx[j + t];
                        own[t] = g.owners[bi];
                        long[] cells = g.blocks[bi];
                        long[] ncells = new long[cells.length * 2];
                        int nc = 0;
                        for (long c : cells) {
                            long c1 = c & line, c0 = c & ~line;
                            if (c1 != 0) ncells[nc++] = c1;
                            if (c0 != 0) ncells[nc++] = c0;
                            union |= c;
                        }
                        nb[t] = nc == ncells.length ? ncells : Arrays.copyOf(ncells, nc);
                    }
                    nm[o] = union;
                    ng[o++] = new Group(nb, own, wd);
                }
                j = e;
            }
            pos += q * wd;
        }
        if (!changed) return st;
        return new State(Arrays.copyOf(nm, o), Arrays.copyOf(ng, o));
    }

    private static void blockStrings(Group g, long line, long[] s) {
        long[][] blocks = g.blocks;
        for (int j = 0; j < blocks.length; j++) {
            long str = 0;
            int p = 0;
            for (long c : blocks[j]) {
                int k = Long.bitCount(c & line);
                if (k != 0) str |= ones(k) << p;
                p += Long.bitCount(c);
            }
            s[j] = str;
        }
    }

    /** Insertion sort of s[0..len), lexicographically descending, stable; permutes idx alongside if given. */
    private static void sortDesc(long[] s, int len, int[] idx) {
        for (int i = 1; i < len; i++) {
            long x = s[i];
            int xi = idx == null ? 0 : idx[i];
            int j = i - 1;
            while (j >= 0 && cmp(s[j], x) < 0) {
                s[j + 1] = s[j];
                if (idx != null) idx[j + 1] = idx[j];
                j--;
            }
            s[j + 1] = x;
            if (idx != null) idx[j + 1] = xi;
        }
    }

    /** Index of the plain-cell item containing all bits of {@code bits}, or -1. */
    private static int plainItemContaining(State st, long bits) {
        long low = bits & -bits;
        for (int i = 0; i < st.masks.length; i++) {
            long m = st.masks[i];
            if ((m & low) != 0) {
                return st.groups[i] == null && (bits & ~m) == 0 ? i : -1;
            }
        }
        return -1;
    }

    // ================================================================== automorphism orbits

    /** Union-find of rows under the known automorphisms that fix every placed row; null if none apply. */
    private int[] orbits() {
        int[] parent = null;
        ex:
        for (int[] g : auts) {
            for (int i = 0; i < b; i++) {
                if (used[i] && g[i] != i) continue ex;
            }
            if (parent == null) parent = identityPerm(b);
            for (int x = 0; x < b; x++) union(parent, x, g[x]);
        }
        return parent;
    }

    private static boolean sameOrbit(int[] parent, int r, int[] done, int doneCount) {
        int root = find(parent, r);
        for (int i = 0; i < doneCount; i++) if (find(parent, done[i]) == root) return true;
        return false;
    }

    // ================================================================== utilities

    /** Lexicographic comparison with bit 0 most significant: positive if a > b. */
    static int cmp(long a, long b) {
        return Long.compareUnsigned(Long.reverse(a), Long.reverse(b));
    }

    private static long ones(int k) {
        return k == 0 ? 0L : -1L >>> (64 - k);
    }

    private static long maxRowPlain(long line, long[] cells) {
        long out = 0L;
        int pos = 0;
        for (long cell : cells) {
            int k = Long.bitCount(cell & line);
            if (k != 0) out |= ones(k) << pos;
            pos += Long.bitCount(cell);
        }
        return out;
    }

    private static long[] refinePlain(long line, long[] cells) {
        int newLen = 0;
        for (long cell : cells) newLen += ((cell & line) != 0 && (cell & ~line) != 0) ? 2 : 1;
        if (newLen == cells.length) return cells;
        long[] next = new long[newLen];
        int idx = 0;
        for (long cell : cells) {
            long on = cell & line, off = cell & ~line;
            if (on != 0) next[idx++] = on;
            if (off != 0) next[idx++] = off;
        }
        return next;
    }

    private static boolean contains(int[] a, int x) {
        for (int y : a) if (y == x) return true;
        return false;
    }

    private static int[] identityPerm(int k) {
        int[] p = new int[k];
        for (int i = 0; i < k; i++) p[i] = i;
        return p;
    }

    private static int find(int[] p, int x) {
        while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; }
        return x;
    }

    private static void union(int[] p, int x, int y) {
        x = find(p, x);
        y = find(p, y);
        if (x != y) p[x] = y;
    }
}

