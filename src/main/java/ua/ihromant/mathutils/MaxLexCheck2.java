package ua.ihromant.mathutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;

/**
 * Lex-max (canonicity) test for 0/1 incidence matrices with at most 64 columns, version 2.
 * Rows = lines as bitmasks, bit i = column i. The matrix is lex-max if no row/column permutation gives a
 * lexicographically larger row-major string (bit 0 of row 0 first).
 *
 * <h2>How it works</h2>
 * <b>1. Row-by-row search with an ordered column partition.</b> Rows are placed one at a time and must
 * reproduce the target (the matrix itself). Columns are not fixed; they form ordered cells of still-interchangeable
 * columns. A row's best appearance puts its ones first in every cell, and placing it splits the cells. If any row can
 * look larger than the target row at that depth, the matrix is not lex-max. Tied rows are branched on.
 *
 * <p><b>2. Pencil runs, now including the shared point.</b> Tied rows that agree outside one cell C and, inside C,
 * pairwise meet in the same set cc (e.g. all lines through one point, cc = {point}) give the same row strings in every
 * order. Each such pencil is placed in one step. Inside C its columns become [cc][group of interchangeable blocks]
 * [rest], and later rows sort the blocks for themselves. Version 1 needed cc = ∅, so a pencil could only start
 * after its first line had been chosen separately. That cost one branch per line of the pencil, and the whole
 * second-pencil search was repeated under each. Now the whole pencil, first line included, is one node.
 *
 * <p>The search stays exact. A row z interleaving after the first line x of a pencil is the start of another pencil
 * (the one through the point x and z share), which is explored as its own class, so it does not invalidate the run.
 * Any other row that could tie in between makes that pencil fall back to ordinary branching.
 *
 * <p><b>3. One base, many candidate lines ({@link #extender}).</b> The base is searched once, with automorphism
 * pruning (nauty-style: orbits plus backjumping), and the tree is recorded. A candidate L is tested on that tree.
 * <ul>
 *   <li>Every node of the full tree is an automorphic image of a recorded node, so L's whole orbit under the
 *       recorded automorphisms is tested on the recorded tree. Orbit elements that tie at a node and are
 *       swapped by an automorphism fixing the node get only one sub-search.</li>
 *   <li>Cells only get finer deeper in the tree, so a row's best appearance can only decrease. Once an element's best
 *       appearance at a node is below L, nothing below can tie or win, and the element is dropped there.</li>
 *   <li>Fresh sub-searches run only where the candidate itself ties, joins a pencil (the extended pencil is analysed
 *       once), or interleaves into a pencil without being covered by another one.</li>
 *   <li>If L is accepted, the other members of its orbit give isomorphic matrices, so they are rejected immediately.</li>
 * </ul>
 */
public final class MaxLexCheck2 {
    private static final int FAIL = -1;
    private static final int NONE = Integer.MAX_VALUE;
    private static final int MAX_AUTS = 5000;
    private static final int MAX_ORBIT = 4096;

    // ================================================================== public API

    /** Is the whole incidence matrix lex-max? */
    public static boolean isMaxLex(LInc inc) {
        return isMaxLex(inc.lines(), inc.v());
    }

    /**
     * Is base + line lex-max? Assumes base is lex-max (orderly generation). For many candidates against the same
     * base, {@link #extender} is faster.
     */
    public static boolean isMaxLex(LInc base, long line) {
        long[] lines = base.lines();
        int n = lines.length, v = base.v();
        if (n > 0 && cmp(line, lines[n - 1]) > 0) return false;
        long[] cells = {allPts(v)};
        for (long l : lines) cells = refinePlain(l, cells);
        if (maxRowPlain(line, cells) != line) return false;
        long[] all = Arrays.copyOf(lines, n + 1);
        all[n] = line;
        return new MaxLexCheck2(all, all, v, true).run();
    }

    /**
     * Prepared test for many candidate lines against one lex-max base; the base is analysed once (on the first
     * candidate that passes the cheap tests). Not thread-safe: one per base, per thread.
     */
    public static LongPredicate extender(LInc base) {
        return new Prepared(base.lines(), base.v())::test;
    }

    public static long toMask(int[] line) {
        long m = 0;
        for (int p : line) m |= 1L << p;
        return m;
    }

    static boolean isMaxLex(long[] lines, int v) {
        if (lines.length == 0) return true;
        long[] cells = {allPts(v)};
        for (int r = 0; r < lines.length; r++) {
            long line = lines[r];
            if (r > 0 && cmp(line, lines[r - 1]) > 0) return false;
            if (maxRowPlain(line, cells) != line) return false;
            cells = refinePlain(line, cells);
        }
        return new MaxLexCheck2(lines, lines, v, true).run();
    }

    // ================================================================== structures

    /** Interchangeable blocks: blocks[j][t] = t-th internal cell of block j; cell sizes equal across blocks. */
    private record Group(long[][] blocks, int[] owners, int width, long[] blockMask) {
        Group(long[][] blocks, int[] owners, int width) {
            this(blocks, owners, width, masksOf(blocks));
        }

        private static long[] masksOf(long[][] blocks) {
            long[] bm = new long[blocks.length];
            for (int j = 0; j < blocks.length; j++) for (long c : blocks[j]) bm[j] |= c;
            return bm;
        }
    }

    /** Ordered items. groups[i] == null: masks[i] is a plain cell; otherwise masks[i] is the union of the group. */
    private record State(long[] masks, Group[] groups, int[] start) {
        State(long[] masks, Group[] groups) {
            this(masks, groups, starts(masks));
        }

        /** start[i] = first position of item i; start[length] = total width. */
        private static int[] starts(long[] masks) {
            int[] st = new int[masks.length + 1];
            for (int i = 0; i < masks.length; i++) st[i + 1] = st[i] + Long.bitCount(masks[i]);
            return st;
        }
    }

    private record Aut(int[] rows, int[] cols) {}

    private record ClsKey(int item, long cc, long outside) {}

    /** Pencil class at a node: rows identical outside plain cell {@code cell}, pairwise meeting in cc inside it. */
    private static final class Cls {
        int item;
        long cell, cc, outside, unionA;
        int[] members;
        boolean[] mem;
        Run run;       // its analysis at this node
        Node child;    // recorded subtree after the run (null if none / dead)
    }

    private static final class Run {
        int[] members;
        int posC, ccSize, w, matched;
        long cell, cc;
        State stA;          // refined by the members' common part outside the cell
        State after;        // state after the whole run; null if it dies early
        long outside;       // members' string outside the cell
        boolean fallback;   // some row could tie in between and is not covered: branch on members instead
    }

    private static final class Node {
        int depth;
        State st;
        boolean[] used;
        final List<Cls> classes = new ArrayList<>();
        final List<Node> singleChild = new ArrayList<>();
        int[][] fixCols;
        int[][] classesOf;
    }

    @FunctionalInterface
    private interface Cover {
        boolean covered(int m, int z);
    }

    // ================================================================== search state

    private final long[] lines;     // rows to arrange
    private final long[] targets;   // row strings to reproduce
    private final int b, v;
    private final long all;
    private final int[] rows;
    private final boolean[] used;
    private final boolean useAuts;
    private final List<Aut> auts = new ArrayList<>();
    private List<Aut> baseAuts;
    private final int[] blockPos;
    private final int[] runStart;
    private final int[][] runMembers;
    private int runCount;
    private final int[] choice, span, firstChoice, firstRows, firstCols;
    private boolean haveFirst;
    private final long[] scratchS = new long[64];
    private final int[] cntScratch;
    private final long[] nm = new long[64];
    private final Group[] ng = new Group[64];

    private MaxLexCheck2(long[] lines, long[] targets, int v, boolean useAuts) {
        this.lines = lines;
        this.targets = targets;
        this.b = lines.length;
        this.v = v;
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
        this.firstCols = new int[v];
        this.cntScratch = new int[b + 1];
    }

    private boolean run() {
        seedDuplicateRows();
        return dfs(0, initialState(), null) != FAIL;
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
                auts.add(new Aut(g, identityPerm(v)));
            }
        }
    }

    // ================================================================== search

    /** Search below a node; if {@code rec} is not null the explored tree is recorded into it. */
    private int dfs(int depth, State st, Node rec) {
        if (rec != null) {
            rec.depth = depth;
            rec.st = st;
            rec.used = used.clone();
        }
        if (depth == b) return leaf(st);
        long target = targets[depth];

        int[] cand = new int[b - depth];
        int cc = 0;
        for (int r = 0; r < b; r++) {
            if (used[r]) continue;
            int c = cmpMaxRow(lines[r], st, target);
            if (c > 0) return FAIL;
            if (c == 0) cand[cc++] = r;
        }
        if (cc == 0) return NONE;

        List<Cls> classes = findClasses(st, cand, cc);
        boolean[] inRun = new boolean[b];
        boolean[] single = new boolean[b];
        for (Cls k : classes) {
            Run run = analyzeRun(depth, st, k, k.members, (m, z) -> coveredByOther(classes, k, m, z));
            if (run == null) return FAIL;
            k.run = run;
            for (int m : k.members) {
                if (run.fallback) single[m] = true;
                else inRun[m] = true;
            }
        }
        if (rec != null) rec.classes.addAll(classes);

        int[] done = new int[cc];
        int doneCount = 0;
        for (int i = 0; i < cc; i++) if (inRun[cand[i]] && !single[cand[i]]) done[doneCount++] = cand[i];
        for (Cls k : classes) {
            if (k.run.fallback || k.run.after == null) continue;
            if (rec != null) k.child = new Node();
            int jump = descendRun(depth, k.run, k.child);
            if (jump < depth) return jump;
        }

        int[] parent = null;
        int parentAuts = -1;
        for (int i = 0; i < cc; i++) {
            int r = cand[i];
            if (inRun[r] && !single[r]) continue;
            if (useAuts && doneCount > 0 && !auts.isEmpty()) {
                if (parentAuts != auts.size()) {
                    parent = orbits();
                    parentAuts = auts.size();
                }
                if (parent != null && sameOrbit(parent, r, done, doneCount)) continue;
            }
            done[doneCount++] = r;
            Node child = rec != null ? new Node() : null;
            if (rec != null) rec.singleChild.add(child);
            State next = refine(lines[r], st);
            rows[depth] = r;
            used[r] = true;
            choice[depth] = r;
            span[depth] = 1;
            int jump = dfs(depth + 1, next, child);
            used[r] = false;
            if (jump < depth) return jump;
        }
        return NONE;
    }

    private static boolean coveredByOther(List<Cls> classes, Cls self, int m, int z) {
        for (Cls k : classes) if (k != self && k.mem[m] && k.mem[z]) return true;
        return false;
    }

    private int descendRun(int depth, Run run, Node child) {
        int[] members = run.members;
        for (int m : members) used[m] = true;
        runStart[runCount] = depth;
        runMembers[runCount++] = members;
        choice[depth] = -2 - members[0];
        span[depth] = members.length;
        int jump = dfs(depth + members.length, run.after, child);
        runCount--;
        for (int m : members) used[m] = false;
        return jump;
    }

    /** All pencil classes (size >= 2) among the tied candidates; a row may belong to several. */
    private List<Cls> findClasses(State st, int[] cand, int cc) {
        List<Cls> res = new ArrayList<>();
        if (cc < 2) return res;
        Map<ClsKey, boolean[]> map = new HashMap<>();
        for (int i = 0; i < cc; i++) {
            long li = lines[cand[i]];
            for (int j = i + 1; j < cc; j++) {
                long lj = lines[cand[j]];
                int item = plainItemContaining(st, li ^ lj);
                if (item < 0) continue;
                long cell = st.masks[item];
                boolean[] mem = map.computeIfAbsent(new ClsKey(item, li & lj & cell, li & ~cell), _k -> new boolean[b]);
                mem[cand[i]] = true;
                mem[cand[j]] = true;
            }
        }
        for (Map.Entry<ClsKey, boolean[]> e : map.entrySet()) {
            ClsKey key = e.getKey();
            boolean[] mem = e.getValue();
            long cell = st.masks[key.item()];
            int size = 0;
            long seen = 0;
            boolean ok = true;
            for (int r = 0; r < b && ok; r++) {
                if (!mem[r]) continue;
                long a = lines[r] & cell & ~key.cc();
                ok = (lines[r] & key.cc()) == key.cc() && (seen & a) == 0 && a != 0;
                seen |= a;
                size++;
            }
            if (!ok) continue;
            Cls k = new Cls();
            k.item = key.item();
            k.cell = cell;
            k.cc = key.cc();
            k.outside = key.outside();
            k.unionA = seen;
            k.mem = mem;
            k.members = new int[size];
            for (int r = 0, o = 0; r < b; r++) if (mem[r]) k.members[o++] = r;
            res.add(k);
        }
        res.sort((x, y) -> Integer.compare(x.members[0], y.members[0]));   // deterministic order
        return res;
    }

    /**
     * Placing all {@code members} (a pencil of class k) at depths depth..depth+s-1. Returns null for FAIL (a larger
     * matrix exists), otherwise the analysis (fallback flag, after == null if the run dies early).
     */
    private Run analyzeRun(int depth, State st, Cls k, int[] members, Cover cover) {
        int s = members.length;
        Run run = new Run();
        run.members = members;
        run.cell = k.cell;
        run.cc = k.cc;
        run.ccSize = Long.bitCount(k.cc);
        long x0 = lines[members[0]];
        long common = x0 & ~k.cell;
        run.w = Long.bitCount(x0 & k.cell) - run.ccSize;
        run.stA = refine(common, st);
        int idxA = -1;
        for (int i = 0, pos = 0; i < run.stA.masks.length; i++) {
            if (run.stA.groups[i] == null && run.stA.masks[i] == k.cell) { idxA = i; run.posC = pos; break; }
            pos += Long.bitCount(run.stA.masks[i]);
        }
        long outside = maxRow(common, run.stA);
        run.outside = outside;
        long head = ones(run.ccSize) << run.posC;
        long block = ones(run.w);
        for (int j = 0; j < s; j++) {
            long str = outside | head | (block << (run.posC + run.ccSize + j * run.w));
            int c = cmp(str, targets[depth + j]);
            if (c > 0) return null;
            if (c < 0) break;
            run.matched++;
        }
        for (int z = 0; z < b; z++) {
            if (used[z] || contains(members, z)) continue;
            int iv = interleave(run, depth, z, lines[z], cover);
            if (iv == FAIL) return null;
            if (iv == 0) { run.fallback = true; return run; }
        }
        if (run.matched < s) return run;

        long[][] blocks = new long[s][];
        long unionA = 0;
        for (int j = 0; j < s; j++) {
            blocks[j] = new long[]{lines[members[j]] & k.cell & ~k.cc};
            unionA |= blocks[j][0];
        }
        long rest = k.cell & ~k.cc & ~unionA;
        State stA = run.stA;
        int n = stA.masks.length + (run.ccSize > 0 ? 1 : 0) + (rest != 0 ? 1 : 0);
        long[] masks = new long[n];
        Group[] groups = new Group[n];
        for (int i = 0, o = 0; i < stA.masks.length; i++) {
            if (i == idxA) {
                if (run.ccSize > 0) masks[o++] = k.cc;
                masks[o] = k.cell & ~k.cc & ~rest;
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
     * Row z (bits lz), not a member, placed right after i (1..) members. FAIL if it beats the target,
     * 0 if it ties uncovered, NONE otherwise. A tie right after the first member m (i = 1) is covered when another
     * explored class contains both m and z.
     */
    private int interleave(Run run, int depth, int z, long lz, Cover cover) {
        int s = run.members.length;
        int maxI = Math.min(run.matched, s - 1);
        if (maxI < 1) return NONE;
        long cell = run.cell;
        int posC = run.posC, w = run.w, ccs = run.ccSize;
        if (run.matched == s) {
            // complete run: targets at depth+1.. equal the run's strings, which agree with run.outside before the cell
            int lc = cmpMaxRow(lz & ~cell, run.stA, run.outside, posC);
            if (lc < 0) return NONE;
            if (lc > 0) return FAIL;
        }
        long outZ = maxRow(lz & ~cell, run.stA);
        int zc = Long.bitCount(lz & run.cc);
        int total = Long.bitCount(lz & cell);
        int[] cnt = cntScratch;
        long t1 = targets[depth + 1];
        for (int j = 0; j < s; j++) {
            cnt[j] = Long.bitCount(lz & lines[run.members[j]] & cell & ~run.cc);
            long str = outZ | (ones(zc + cnt[j]) << posC) | (ones(total - zc - cnt[j]) << (posC + ccs + w));
            int c = cmp(str, t1);
            if (c > 0) return FAIL;
            if (c == 0 && !cover.covered(run.members[j], z)) return 0;
        }
        if (maxI < 2 || (run.matched == s && zc < ccs)) return NONE;
        sortSmall(cnt, s);
        long slots = 0;
        int taken = 0;
        long head = ones(zc) << posC;
        for (int i = 1; i <= maxI; i++) {
            int top = cnt[s - i];
            slots |= ones(top) << (posC + ccs + (i - 1) * w);
            taken += top;
            if (i < 2) continue;
            long str = outZ | head | slots | (ones(total - zc - taken) << (posC + ccs + i * w));
            int c = cmp(str, targets[depth + i]);
            if (c > 0) return FAIL;
            if (c == 0) return 0;
        }
        return NONE;
    }

    private int leaf(State st) {
        if (!useAuts) return NONE;
        int[] colAt = new int[v];
        int pos = 0;
        for (int i = 0; i < st.masks.length; i++) {
            Group g = st.groups[i];
            if (g == null) {
                pos = emitBits(st.masks[i], colAt, pos);
            } else {
                for (int j = 0; j < g.blocks.length; j++) {
                    blockPos[g.owners[j]] = pos;
                    for (long c : g.blocks[j]) pos = emitBits(c, colAt, pos);
                }
            }
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
        if (!haveFirst) {
            haveFirst = true;
            System.arraycopy(rows, 0, firstRows, 0, b);
            System.arraycopy(choice, 0, firstChoice, 0, b);
            System.arraycopy(colAt, 0, firstCols, 0, v);
            return NONE;
        }
        if (auts.size() < MAX_AUTS) {
            int[] g = new int[b];
            for (int i = 0; i < b; i++) g[firstRows[i]] = rows[i];
            int[] t = new int[v];
            for (int p = 0; p < v; p++) t[firstCols[p]] = colAt[p];
            auts.add(new Aut(g, t));
        }
        for (int p = 0; p < b; p += span[p]) {
            if (choice[p] != firstChoice[p]) return p;
        }
        return NONE;
    }

    private static int emitBits(long m, int[] colAt, int pos) {
        while (m != 0) {
            colAt[pos++] = Long.numberOfTrailingZeros(m);
            m &= m - 1;
        }
        return pos;
    }

    // ================================================================== prepared base

    private static final class Prepared {
        private final long[] base;
        private final int v, n;
        private final long[] cells;
        private Node root;
        private List<int[]> gens;
        private boolean built;
        private final LongSet rejected = new LongSet();
        private final long[] rowsArr, targArr;
        private MaxLexCheck2 sub;

        Prepared(long[] base, int v) {
            this.base = base;
            this.v = v;
            this.n = base.length;
            long[] c = {allPts(v)};
            for (long l : base) c = refinePlain(l, c);
            this.cells = c;
            this.rowsArr = Arrays.copyOf(base, n + 1);
            this.targArr = Arrays.copyOf(base, n + 1);
        }

        boolean test(long line) {
            if (n > 0 && cmp(line, base[n - 1]) > 0) return false;
            if (maxRowPlain(line, cells) != line) return false;
            if (rejected.contains(line)) return false;
            if (!built) build();
            long[] orbit = orbit(line);
            boolean ok;
            if (orbit == null) {
                long[] m = Arrays.copyOf(base, n + 1);
                m[n] = line;
                ok = new MaxLexCheck2(m, m, v, true).run();
            } else {
                targArr[n] = line;
                rowsArr[n] = line;
                ok = sub.visit(root, orbit, orbit.length, line, n) != FAIL;
                if (ok) for (long o : orbit) if (o != line) rejected.add(o);
            }
            return ok;
        }

        private void build() {
            built = true;
            MaxLexCheck2 m = new MaxLexCheck2(base, base, v, true);
            m.seedDuplicateRows();
            root = new Node();
            if (m.dfs(0, m.initialState(), root) == FAIL) throw new IllegalStateException("base is not lex-max");
            gens = new ArrayList<>();
            for (Aut a : m.auts) gens.add(a.cols);
            sub = new MaxLexCheck2(rowsArr, targArr, v, false);
            sub.baseAuts = m.auts;
        }

        private long[] orbit(long line) {
            LongSet seen = new LongSet();
            long[] queue = new long[16];
            int head = 0, tail = 0;
            seen.add(line);
            queue[tail++] = line;
            while (head < tail) {
                long x = queue[head++];
                for (int[] g : gens) {
                    long y = apply(g, x);
                    if (seen.add(y)) {
                        if (tail == MAX_ORBIT) return null;
                        if (tail == queue.length) queue = Arrays.copyOf(queue, tail * 2);
                        queue[tail++] = y;
                    }
                }
            }
            return Arrays.copyOf(queue, tail);
        }
    }

    // ================================================================== candidate test on the recorded tree

    /**
     * Tests orbit elements {@code live} at a recorded node. This instance has rows = base + slot n (set per element)
     * and targets = base + candidate.
     */
    private int visit(Node nd, long[] live, int liveCount, long cand, int n) {
        int depth = nd.depth;
        long target = targets[depth];
        long[] next = new long[liveCount];
        boolean[] tie = new boolean[liveCount];
        int nextCount = 0, tieCount = 0;
        boolean dupLast = n > 0 && cand == targets[n - 1];
        for (int i = 0; i < liveCount; i++) {
            long x = live[i];
            int cl = cmpMaxRow(x, nd.st, cand);
            if (cl < 0) continue;                                  // below the candidate here and everywhere below
            long mr = maxRow(x, nd.st);
            int c = cmp(mr, target);
            if (c > 0) return FAIL;
            if (depth == n) continue;
            if (c == 0 || cl > 0 || (cl == 0 && dupLast)) {     // otherwise nothing below can tie or win
                if (c == 0) tieCount++;
                tie[nextCount] = c == 0;
                next[nextCount++] = x;
            }
        }
        if (depth == n || nextCount == 0) return NONE;
        int kc = nd.classes.size();
        boolean[][] covers = tieCount > 0 ? new boolean[nextCount][] : null;
        for (int ki = 0; ki < kc; ki++) {
            Cls k = nd.classes.get(ki);
            if (k.run.fallback) continue;                          // its members are recorded as single branches
            long[] cont = new long[nextCount];
            int contCount = 0;
            for (int j = 0; j < nextCount; j++) {
                long x = next[j];
                int r;
                if (compatible(x, k)) {                            // x joins this pencil
                    r = extended(nd, k, x);
                    if (r == EXT_COVERS) {
                        if (covers[j] == null) covers[j] = new boolean[kc];
                        covers[j][ki] = true;
                    }
                } else {
                    r = interleaveRecorded(nd, k, x);
                }
                if (r == FAIL) return FAIL;
                if (r == NONE) cont[contCount++] = x;              // the recorded run stays valid for x
            }
            if (k.child != null && contCount > 0 && visit(k.child, cont, contCount, cand, n) == FAIL) return FAIL;
        }
        if (tieCount > 0) {
            for (int j : orbitReps(nd, next, tie, nextCount)) {
                if (firstRow(nd, next[j], covers[j]) == FAIL) return FAIL;
            }
        }
        for (Node child : nd.singleChild) {
            if (visit(child, next, nextCount, cand, n) == FAIL) return FAIL;
        }
        return NONE;
    }

    private static final int EXT_COVERS = Integer.MAX_VALUE - 1;    // extended pencil analysed (run or dies)
    private static final int EXT_SEARCHED = Integer.MAX_VALUE - 2;  // it fell back; members searched one by one

    /** Would x (row n) join class k at this node? */
    private boolean compatible(long x, Cls k) {
        long in = x & k.cell;
        return (x & ~k.cell) == k.outside && (in & k.cc) == k.cc && (in & ~k.cc & k.unionA) == 0
                && Long.bitCount(in) == Long.bitCount(lines[k.members[0]] & k.cell);
    }

    /** x joins pencil k: analyse the extended pencil once and search below it. */
    private int extended(Node nd, Cls k, long x) {
        int n = nd.used.length;
        lines[n] = x;
        System.arraycopy(nd.used, 0, used, 0, n);
        used[n] = false;
        int[] members = Arrays.copyOf(k.members, k.members.length + 1);
        members[k.members.length] = n;
        // (x, z) prefixes with z outside the pencil are handled by firstRow
        Run e = analyzeRun(nd.depth, nd.st, k, members, (m, z) -> m == n || coveredByOther(nd.classes, k, m, z));
        int res;
        if (e == null) {
            res = FAIL;
        } else if (e.fallback) {
            res = EXT_SEARCHED;
            for (int m : k.members) {
                if (subSearch(nd, m, x) == FAIL) { res = FAIL; break; }
            }
        } else if (e.after == null) {
            res = EXT_COVERS;
        } else {
            System.arraycopy(nd.used, 0, used, 0, n);
            used[n] = false;
            for (int m : members) used[m] = true;
            res = dfs(nd.depth + members.length, e.after, null) == FAIL ? FAIL : EXT_COVERS;
        }
        Arrays.fill(used, false);
        return res;
    }

    /**
     * x tied at this node: branches starting with x. A continuation (x, z) is already explored when z is in a pencil
     * that x extended (covers). A fresh search is needed only if some tying z is not covered.
     */
    private int firstRow(Node nd, long x, boolean[] covers) {
        int n = nd.used.length;
        lines[n] = x;
        State st1 = refine(x, nd.st);
        long t1 = targets[nd.depth + 1];
        int[][] classesOf = classesOf(nd);
        for (int z = 0; z < n; z++) {
            if (nd.used[z]) continue;
            int c = cmpMaxRow(lines[z], st1, t1);
            if (c > 0) return FAIL;
            if (c == 0) {
                boolean cov = false;
                if (covers != null) for (int ki : classesOf[z]) if (covers[ki]) { cov = true; break; }
                if (!cov) return subSearch(nd, -1, x);
            }
        }
        return NONE;
    }

    private static int[][] classesOf(Node nd) {
        if (nd.classesOf == null) {
            int n = nd.used.length;
            int[] cnt = new int[n];
            for (Cls k : nd.classes) for (int m : k.members) cnt[m]++;
            int[][] res = new int[n][];
            for (int r = 0; r < n; r++) res[r] = new int[cnt[r]];
            Arrays.fill(cnt, 0);
            for (int ki = 0; ki < nd.classes.size(); ki++) for (int m : nd.classes.get(ki).members) res[m][cnt[m]++] = ki;
            nd.classesOf = res;
        }
        return nd.classesOf;
    }

    /**
     * x does not join recorded pencil k: check x as an interleaving row. Returns FAIL, NONE (run still valid for x;
     * uncovered prefixes (m, x) were searched explicitly) or EXT_SEARCHED (members searched individually).
     */
    private int interleaveRecorded(Node nd, Cls k, long x) {
        Run run = k.run;
        int s = run.members.length;
        int maxI = Math.min(run.matched, s - 1);
        if (maxI < 1) return NONE;
        int depth = nd.depth;
        long cell = run.cell;
        int posC = run.posC, w = run.w, ccs = run.ccSize;
        if (run.matched == s) {
            // complete run: targets at depth+1.. equal the run's strings, which agree with run.outside before the cell
            int lc = cmpMaxRow(x & ~cell, run.stA, run.outside, posC);
            if (lc < 0) return NONE;
            if (lc > 0) return FAIL;
        }
        long outZ = maxRow(x & ~cell, run.stA);
        int zc = Long.bitCount(x & run.cc);
        int total = Long.bitCount(x & cell);
        int[] cnt = cntScratch;
        long t1 = targets[depth + 1];
        for (int j = 0; j < s; j++) {
            int m = run.members[j];
            cnt[j] = Long.bitCount(x & lines[m] & cell & ~run.cc);
            long str = outZ | (ones(zc + cnt[j]) << posC) | (ones(total - zc - cnt[j]) << (posC + ccs + w));
            int c = cmp(str, t1);
            if (c > 0) return FAIL;
            if (c == 0 && !coveredRecorded(nd, k, m, x) && subSearchPrefix(nd, m, x) == FAIL) return FAIL;
        }
        if (maxI < 2 || (run.matched == s && zc < ccs)) return NONE;
        sortSmall(cnt, s);
        long slots = 0;
        int taken = 0;
        long head = ones(zc) << posC;
        for (int i = 1; i <= maxI; i++) {
            int top = cnt[s - i];
            slots |= ones(top) << (posC + ccs + (i - 1) * w);
            taken += top;
            if (i < 2) continue;
            long str = outZ | head | slots | (ones(total - zc - taken) << (posC + ccs + i * w));
            int c = cmp(str, targets[depth + i]);
            if (c > 0) return FAIL;
            if (c == 0) {
                for (int m : run.members) if (subSearch(nd, m, x) == FAIL) return FAIL;
                return EXT_SEARCHED;
            }
        }
        return NONE;
    }

    /** Prefix (m, x) is explored elsewhere: by another recorded pencil that x joins, or by m's single branch. */
    private boolean coveredRecorded(Node nd, Cls self, int m, long x) {
        for (int ki : classesOf(nd)[m]) {
            Cls k = nd.classes.get(ki);
            if (k != self && (k.run.fallback || compatible(x, k))) return true;
        }
        return false;
    }

    /** Indices j (tie[j]) of one representative per orbit of tied elements under automorphisms fixing the node. */
    private int[] orbitReps(Node nd, long[] vals, boolean[] tie, int count) {
        int[] idx = new int[count];
        int tc = 0;
        for (int j = 0; j < count; j++) if (tie[j]) idx[tc++] = j;
        if (tc <= 1) return Arrays.copyOf(idx, tc);
        if (nd.fixCols == null) {
            List<int[]> fix = new ArrayList<>();
            ex:
            for (Aut a : baseAuts) {
                for (int r = 0; r < nd.used.length; r++) if (nd.used[r] && a.rows[r] != r) continue ex;
                fix.add(a.cols);
            }
            nd.fixCols = fix.toArray(new int[0][]);
        }
        if (nd.fixCols.length == 0) return Arrays.copyOf(idx, tc);
        long[] sorted = new long[tc];
        for (int i = 0; i < tc; i++) sorted[i] = vals[idx[i]];
        Arrays.sort(sorted);
        int[] pos = new int[tc];                       // sorted position -> original index
        for (int i = 0; i < tc; i++) pos[Arrays.binarySearch(sorted, vals[idx[i]])] = idx[i];
        int[] parent = identityPerm(tc);
        for (int[] g : nd.fixCols) {
            for (int i = 0; i < tc; i++) {
                int j = Arrays.binarySearch(sorted, apply(g, sorted[i]));
                if (j >= 0) union(parent, i, j);
            }
        }
        int[] reps = new int[tc];
        int rc = 0;
        for (int i = 0; i < tc; i++) if (find(parent, i) == i) reps[rc++] = pos[i];
        return Arrays.copyOf(reps, rc);
    }

    /** Search below nd with row r (or x itself if r < 0) placed at nd.depth; row n of the matrix is x. */
    private int subSearch(Node nd, int r, long x) {
        int n = nd.used.length;
        lines[n] = x;
        if (r < 0) r = n;
        System.arraycopy(nd.used, 0, used, 0, n);
        used[n] = false;
        used[r] = true;
        int res = dfs(nd.depth + 1, refine(lines[r], nd.st), null);
        Arrays.fill(used, false);
        return res;
    }

    /** Search below nd with base row m and then x placed (x ties at depth + 1 after m). */
    private int subSearchPrefix(Node nd, int m, long x) {
        int n = nd.used.length;
        lines[n] = x;
        System.arraycopy(nd.used, 0, used, 0, n);
        used[n] = true;
        used[m] = true;
        int res = dfs(nd.depth + 2, refine(x, refine(lines[m], nd.st)), null);
        Arrays.fill(used, false);
        return res;
    }

    // ================================================================== rows and refinement

    /**
     * Compares maxRow(line, st) with target lexicographically, but only on positions below {@code limit}, and stops
     * at the first item that decides it.
     */
    private int cmpMaxRow(long line, State st, long target, int limit) {
        long out = 0;
        long[] masks = st.masks;
        Group[] groups = st.groups;
        int[] start = st.start;
        for (int i = 0; i < masks.length && start[i] < limit; i++) {
            long on = masks[i] & line;
            if (on != 0) {
                Group g = groups[i];
                out |= g == null ? ones(Long.bitCount(on)) << start[i] : groupBits(g, line, start[i]);
            }
            long diff = (out ^ target) & ones(Math.min(start[i + 1], limit));
            if (diff != 0) return (out & diff & -diff) != 0 ? 1 : -1;
        }
        return 0;
    }

    private int cmpMaxRow(long line, State st, long target) {
        return cmpMaxRow(line, st, target, 64);
    }

    /** The bits a row contributes inside a group starting at {@code pos}: touched blocks sorted, best first. */
    private long groupBits(Group g, long line, int pos) {
        long[] s = scratchS;
        long[] bm = g.blockMask;
        int q = bm.length, wd = g.width, cnt = 0;
        for (int j = 0; j < q; j++) {
            if ((bm[j] & line) != 0) s[cnt++] = Long.reverse(blockString(g.blocks[j], line));
        }
        for (int a = 1; a < cnt; a++) {        // descending (reversed keys compare as unsigned)
            long x = s[a];
            int j = a - 1;
            while (j >= 0 && Long.compareUnsigned(s[j], x) < 0) { s[j + 1] = s[j]; j--; }
            s[j + 1] = x;
        }
        long out = 0;
        for (int j = 0; j < cnt; j++) out |= Long.reverse(s[j]) << (pos + j * wd);
        return out;
    }

    private long maxRow(long line, State st) {
        long out = 0;
        long[] masks = st.masks;
        Group[] groups = st.groups;
        int[] start = st.start;
        for (int i = 0; i < masks.length; i++) {
            long on = masks[i] & line;
            if (on == 0) continue;
            Group g = groups[i];
            out |= g == null ? ones(Long.bitCount(on)) << start[i] : groupBits(g, line, start[i]);
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
            for (int j = 0; j < q; j++) {
                s[j] = (g.blockMask[j] & line) == 0 ? 0 : Long.reverse(blockString(g.blocks[j], line));
            }
            int[] idx = identityPerm(q);
            sortDescRev(s, q, idx);
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

    /** Best string of one block: ones first inside each internal cell (bit 0 = first column of the block). */
    private static long blockString(long[] cells, long line) {
        if (cells.length == 1) return ones(Long.bitCount(cells[0] & line));
        long str = 0;
        int p = 0;
        for (long c : cells) {
            int k = Long.bitCount(c & line);
            if (k != 0) str |= ones(k) << p;
            p += Long.bitCount(c);
        }
        return str;
    }

    /** Stable insertion sort of bit-reversed strings, descending (= lexicographically descending); idx follows. */
    private static void sortDescRev(long[] s, int len, int[] idx) {
        for (int i = 1; i < len; i++) {
            long x = s[i];
            int xi = idx[i];
            int j = i - 1;
            while (j >= 0 && Long.compareUnsigned(s[j], x) < 0) {
                s[j + 1] = s[j];
                idx[j + 1] = idx[j];
                j--;
            }
            s[j + 1] = x;
            idx[j + 1] = xi;
        }
    }

    /** Insertion sort, ascending (small arrays). */
    private static void sortSmall(int[] a, int len) {
        for (int i = 1; i < len; i++) {
            int x = a[i], j = i - 1;
            while (j >= 0 && a[j] > x) { a[j + 1] = a[j]; j--; }
            a[j + 1] = x;
        }
    }

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

    // ================================================================== automorphism orbits (rows)

    private int[] orbits() {
        int[] parent = null;
        ex:
        for (Aut a : auts) {
            int[] g = a.rows;
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

    private static long apply(int[] perm, long x) {
        long y = 0;
        while (x != 0) {
            y |= 1L << perm[Long.numberOfTrailingZeros(x)];
            x &= x - 1;
        }
        return y;
    }

    private static long allPts(int v) {
        return v == 64 ? -1L : (1L << v) - 1L;
    }

    private static long ones(int k) {
        return k <= 0 ? 0L : -1L >>> (64 - k);
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

    private static final class LongSet {
        private long[] keys = new long[16];
        private boolean[] full = new boolean[16];
        private int size;

        boolean contains(long k) {
            int mask = keys.length - 1;
            for (int i = hash(k) & mask; full[i]; i = (i + 1) & mask) if (keys[i] == k) return true;
            return false;
        }

        boolean add(long k) {
            if ((size + 1) * 2 > keys.length) grow();
            int mask = keys.length - 1;
            int i = hash(k) & mask;
            for (; full[i]; i = (i + 1) & mask) if (keys[i] == k) return false;
            keys[i] = k;
            full[i] = true;
            size++;
            return true;
        }

        private void grow() {
            long[] ok = keys;
            boolean[] of = full;
            keys = new long[ok.length * 2];
            full = new boolean[ok.length * 2];
            size = 0;
            for (int i = 0; i < ok.length; i++) if (of[i]) add(ok[i]);
        }

        private static int hash(long k) {
            long h = k * 0x9E3779B97F4A7C15L;
            return (int) (h ^ (h >>> 32));
        }
    }
}
