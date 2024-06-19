package ua.ihromant.mathutils.saucy;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class Saucy {
    private int n;
    private int[] adj;
    private int[] edg;

    private Coloring left;
    private Coloring right;
    private int[] nextNon;
    private int[] prevNon;

    private int[] indMark;
    private int[] nInduce;
    private int[] sInduce;
    private int nnInduce;
    private int nsInduce;

    private int[] cList;
    private int cSize;

    private int[] stuff;
    private int[] cCount;
    private int[] bucket;
    private int[] count;
    private int[] junk;
    private int[] gamma;
    private int[] connCnts;

    private int lev;
    private int anc;
    private int[] ancTar;
    private int kAncTar;
    private int[] start;
    private int indMin;
    private int match;

    private int[] splitWho;
    private int[] splitFrom;
    private int[] splitLev;
    private int nSplits;

    private int[] diffMark;
    private int[] diffs;
    private int[] diffLev;
    private int nDiffs;
    private int[] unDiffLev;
    private int nUndiffs;
    private int[] unSupp;
    private int[] specMin;
    private int[] pairs;
    private int[] unPairs;
    private int nPairs;
    private int[] diffNons;
    private int[] unDiffNons;
    private int nDiffNons;

    private Split split;
    private Refiner refSingleton;
    private Refiner refNonSingle;

    private SaucyStats stats;

    public int findMin(int t) {
        return right.doFindMin(t);
    }

    public void moveToBack(Coloring c, int k) {
        int cf = c.getCFront()[k];
        int cb = cf + c.getCLen()[cf];
        int offset = connCnts[cf]++;
        c.swapLabels(cb - offset, c.getUnLab()[k]);
        if (offset == 0) {
            cList[cSize++] = cf;
        }
    }

    public void dataMark(Coloring c, int k) {
        int cf = c.getCFront()[k];
        if (c.getCLen()[cf] > 0) {
            moveToBack(c, k);
        }
    }

    public void dataCount(Coloring c, int k) {
        int cf = c.getCFront()[k];
        if (c.getCLen()[cf] > 0 && cCount[k]++ == 0) {
            moveToBack(c, k);
        }
    }

    public int checkMapping(int[] adj, int[] edg, int k) {
        int ret = 1;
        for (int i = adj[k]; i != adj[k + 1]; ++i) {
            stuff[gamma[edg[i]]] = 1;
        }
        int gk = gamma[k];
        for (int i = adj[gk]; ret != 0 && i != adj[gk + 1]; ++i) {
            ret = stuff[edg[i]];
        }
        for (int i = adj[k]; i != adj[k + 1]; ++i) {
            stuff[gamma[edg[i]]] = 0;
        }
        return ret;
    }

    public int isUndirectedAutomorphism() {
        for (int i = 0; i < nDiffs; ++i) {
            int j = unSupp[i];
            if (checkMapping(adj, edg, j) == 0) {
                return 0;
            }
        }
        return 1;
    }

    public void addInduce(Coloring c, int who) {
        if (c.getCLen()[who] == 0) {
             sInduce[nsInduce++] = who;
        } else {
            nInduce[nnInduce++] = who;
        }
        indMark[who] = 1;
    }

    public boolean atTerminal() {
        return nSplits == n;
    }

    public void addDiffNon(int k) {
        if (nDiffNons == -1) {
            return;
        }
        unDiffNons[k] = nDiffNons;
        diffNons[nDiffNons++] = k;
    }

    public void removeDiffNon(int k) {
        if (unDiffNons[k] == -1) {
            return;
        }
        int j = diffNons[--nDiffNons];
        diffNons[unDiffNons[k]] = j;
        unDiffNons[j] = unDiffNons[k];
        unDiffNons[k] = -1;
    }

    public void addDiff(int k) {
        if (diffMark[k] == 0) {
            diffMark[k] = 1;
            diffs[nDiffs++] = k;
            addDiffNon(k);
        }
    }

    public boolean isAPair(int k) {
        return unPairs[k] != -1;
    }

    public void addPair(int k) {
        if (nPairs != -1) {
            unPairs[k] = nPairs;
            pairs[nPairs++] = k;
        }
    }

    public void eatPair(int k) {
        int j = pairs[--nPairs];
        pairs[unPairs[k]] = j;
        unPairs[j] = unPairs[k];
        unPairs[k] = -1;
    }

    public void pickAllThePairs() {
        for (int i = 0; i < nPairs; ++i) {
            unPairs[pairs[i]] = -1;
        }
        nPairs = 0;
    }

    public void clearUnDiffNons() {
        for (int i = 0; i < nDiffNons; ++i) {
            unDiffNons[diffNons[i]] = -1;
        }
    }

    public void fixDiffSingleton(int cf) {
        int r = right.getLab()[cf];
        int l = left.getLab()[cf];
        if (right.getCLen()[cf] == 0 && r != l) {
            addDiff(r);
            nUndiffs++;
            removeDiffNon(r);
            int rcfl = right.getCFront()[l];
            if (right.getCLen()[rcfl] != 0) {
                addDiff(l);
                if (right.inCellRange(left.getUnLab()[r], rcfl)) {
                    addPair(l);
                }
            } else if (isAPair(r)) {
                eatPair(r);
            }
        }
    }

    public void fixDiffSubtract(int cf, int[] a, int[] b) {
        int cb = cf + right.getCLen()[cf];
        for (int i = cf; i <= cb; ++i) {
            stuff[a[i]] = 1;
        }
        for (int i = cf; i <= cb; ++i) {
            int k = b[i];
            if (stuff[k] == 0) {
                addDiff(k);
            }
        }
        for (int i = cf; i <= cb; ++i) {
            stuff[a[i]] = 0;
        }
    }

    public void fixDiffs(int cf, int ff) {
        fixDiffSingleton(cf);
        fixDiffSingleton(ff);
        if (right.getCLen()[cf] != 0 && right.getCLen()[ff] != 0) {
            int min = right.getCLen()[cf] < right.getCLen()[ff] ? cf : ff;
            fixDiffSubtract(min, left.getLab(), right.getLab());
            fixDiffSubtract(min, right.getLab(), left.getLab());
        }
    }

    public void splitCommon(Coloring c, int cf, int ff) {
        c.splitColor(cf, ff);
        if (indMark[cf] != 0 || c.getCLen()[ff] < c.getCLen()[cf]) {
            addInduce(c, ff);
        } else {
            addInduce(c, cf);
        }
    }

    public int splitLeft(Coloring c, int cf, int ff) {
        splitWho[nSplits] = ff;
        splitFrom[nSplits] = cf;
        nSplits++;
        splitCommon(c, cf, ff);
        return 1;
    }

    public int splitInit(Coloring c, int cf, int ff) {
        splitLeft(c, cf, ff);
        if (c.getCLen()[ff] != 0) {
            prevNon[nextNon[cf + 1]] = ff;
            nextNon[ff + 1] = nextNon[cf + 1];
            prevNon[ff] = cf;
            nextNon[cf + 1] = ff;
        }
        if (c.getCLen()[cf] == 0) {
            nextNon[prevNon[cf] + 1] = nextNon[cf + 1];
            prevNon[nextNon[cf + 1]] = prevNon[cf];
        }
        return 1;
    }

    public int splitOther(Coloring c, int cf, int ff) {
        int k = nSplits;

        if (splitWho[k] != ff || splitFrom[k] != cf || k >= splitLev[lev]) {
            return 0;
        }
        nSplits++;
        splitCommon(c, cf, ff);
        fixDiffs(cf, ff);
        return 1;
    }

    public interface Refiner {
        int refine(Saucy saucy, Coloring c, int cf);
    }

    public interface Split {
        int split(Saucy saucy, Coloring c, int cf, int ff);
    }

    public int refineCell(Coloring c, Refiner refine) {
        int ret = 1;
        if (lev > 1) {
            Arrays.sort(cList, 0, cSize);
        }
        for (int i = 0; ret != 0 && i < cSize; ++i) {
            int cf = cList[i];
            ret = refine.refine(this, c, cf);
        }
        for (int i = 0; i < cSize; ++i) {
            int cf = cList[i];
            connCnts[cf] = 0;
        }
        cSize = 0;
        return ret;
    }

    public int maybeSplit(Coloring c, int cf, int ff) {
        return cf == ff ? 1 : split.split(this, c, cf, ff);
    }

    public int refSingleCell(Coloring c, int cf) {
        int zCnt = c.getCLen()[cf] + 1 - connCnts[cf];
        return maybeSplit(c, cf, cf + zCnt);
    }

    public int refSingleton(Coloring c, int[] adj, int[] edg, int cf) {
        int k = c.getLab()[cf];
        for (int i = adj[k]; i != adj[k + 1]; ++i) {
            dataMark(c, edg[i]);
        }
        return refineCell(c, Saucy::refSingleCell);
    }

    public int refSingletonUndirected(Coloring c, int cf) {
        return refSingleton(c, adj, edg, cf);
    }

    public int refNonSingleCell(Coloring c, int cf) {
        int cb = cf + c.getCLen()[cf];
        int nzf = cb - connCnts[cf] + 1;
        int ff = nzf;
        int cnt = cCount[c.getLab()[ff]];
        count[ff] = cnt;
        int bMin = cnt;
        int bMax = cnt;
        bucket[cnt] = 1;

        while (++ff <= cb) {
            cnt = cCount[c.getLab()[ff]];
            while (bMin > cnt) {
                bucket[--bMin] = 0;
            }
            while (bMax < cnt) {
                bucket[++bMax] = 0;
            }
            ++bucket[cnt];
            count[ff] = cnt;
        }
        ff = nzf;
        int fb = nzf;

        for (int i = bMin; i <= bMax; ++i, ff = fb) {
            if (bucket[i] == 0) {
                continue;
            }
            fb = ff + bucket[i];
            bucket[i] = fb;
        }

        for (int i = nzf; i <= cb; ++i) {
            junk[--bucket[count[i]]] = c.getLab()[i];
        }
        for (int i = nzf; i <= cb; ++i) {
            c.setLabel(i, junk[i]);
        }
        for (int i = bMax; i > bMin; --i) {
            ff = bucket[i];
            if (ff != 0 && split.split(this, c, cf, ff) == 0) {
                return 0;
            }
        }
        return maybeSplit(c, cf, bucket[bMin]);
    }

    public int refNonSingle(Coloring c, int[] adj, int[] edg, int cf) {
        int cb = cf + c.getCLen()[cf];
        int size = cb - cf + 1;

        if (cf == cb) {
            return refSingleton(c, adj, edg, cf);
        }
        System.arraycopy(c.getLab(), cf, junk, 0, size);
        for (int i = 0; i < size; ++i) {
            int k = junk[i];
            for (int j = adj[k]; j != adj[k + 1]; ++j) {
                dataCount(c, edg[j]);
            }
        }

        int ret = refineCell(c, Saucy::refNonSingleCell);

        for (int i = cf; i <= cb; ++i) {
            int k = c.getLab()[i];
            for (int j = adj[k]; j != adj[k + 1]; ++j) {
                cCount[edg[j]] = 0;
            }
        }

        return ret;
    }

    public int refNonSingleUndirected(Coloring c, int cf) {
        return refNonSingle(c, adj, edg, cf);
    }

    public void clearRefine() {
        for (int i = 0; i < nnInduce; ++i) {
            indMark[nInduce[i]] = 0;
        }
        for (int i = 0; i < nsInduce; ++i) {
            indMark[sInduce[i]] = 0;
        }
        nnInduce = 0;
        nsInduce = 0;
    }

    public int refine(Coloring c) {
        while (true) {
            if (atTerminal()) {
                clearRefine();
                return 1;
            }
            if (nsInduce != 0) {
                int front = sInduce[--nsInduce];
                indMark[front] = 0;
                if (refSingleton.refine(this, c, front) == 0) {
                    break;
                }
            } else if (nnInduce != 0) {
                int front = nInduce[--nnInduce];
                indMark[front] = 0;
                if (refNonSingle.refine(this, c, front) == 0) {
                    break;
                }
            } else {
                return 1;
            }
        }
        clearRefine();
        return 0;
    }

    public int descend(Coloring c, int target, int min) {
        int back = target + c.getCLen()[target];
        stats.setNodes(stats.getNodes() + 1);
        c.swapLabels(min, back);
        diffLev[lev] = nDiffs;
        unDiffLev[lev] = nUndiffs;
        lev++;
        split.split(this, c, target, back);

        int ret = refine(c);

        if (c == right && ret != 0) {
            for (int i = nSplits - 1; i > splitLev[lev - 1]; --i) {
                int v = c.getLab()[splitWho[i]];
                int sum1 = 0;
                int xor1 = 0;
                for (int j = adj[v]; j < adj[v + 1]; j++) {
                    sum1 += c.getCFront()[edg[j]];
                    xor1 ^= c.getCFront()[edg[j]];
                }
                v = left.getLab()[splitWho[i]];
                int sum2 = 0;
                int xor2 = 0;
                for (int j = adj[v]; j < adj[v + 1]; j++) {
                    sum2 += left.getCFront()[edg[j]];
                    xor2 ^= left.getCFront()[edg[j]];
                }
                if (sum1 != sum2 || xor1 != xor2) {
                    ret = 0;
                    break;
                }
                v = c.getLab()[splitFrom[i]];
                sum1 = 0;
                xor1 = 0;
                for (int j = adj[v]; j < adj[v + 1]; j++) {
                    sum1 += c.getCFront()[edg[j]];
                    xor1 ^= c.getCFront()[edg[j]];
                }
                v = left.getLab()[splitFrom[i]];
                sum2 = 0;
                xor2 = 0;
                for (int j = adj[v]; j < adj[v + 1]; j++) {
                    sum2 += left.getCFront()[edg[j]];
                    xor2 ^= left.getCFront()[edg[j]];
                }
                if (sum1 != sum2 || xor1 != xor2) {
                    ret = 0;
                    break;
                }
            }
        }
        return ret;
    }

    public int descendLeftmost() {
        while (!atTerminal()) {
            int target = nextNon[0];
            start[lev] = target;
            splitLev[lev] = nSplits;
            if (descend(left, target, target) == 0) {
                return 0;
            }
        }
        splitLev[lev] = n;
        return 1;
    }

    public boolean zetaFixed() {
        return nDiffs == nUndiffs;
    }

    public void selectDecomposition(int target, int lMin, int rMin) {
        int[] cLen = left.getCLen();
        for (int i = 0; i < nPairs; ++i) {
            int k = pairs[i];
            target = right.getCFront()[k];
            lMin = left.getUnLab()[right.getLab()[left.getUnLab()[k]]];
            rMin = right.getUnLab()[k];
            if (cLen[target] != 0 && left.inCellRange(lMin, target) && right.inCellRange(rMin, target)) {
                return;
            }
        }

        if (nDiffNons != -1) {
            // TODO check, some pointer magic here
        }
    }
}
