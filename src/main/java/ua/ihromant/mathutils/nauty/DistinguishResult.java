package ua.ihromant.mathutils.nauty;

import java.util.BitSet;

public record DistinguishResult(int[][] elms, int largest, BitSet singulars) {
}
