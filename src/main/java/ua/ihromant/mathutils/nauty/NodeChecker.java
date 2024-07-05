package ua.ihromant.mathutils.nauty;

import java.util.BitSet;
import java.util.List;

public interface NodeChecker {
    boolean check(Partition partition, List<long[]> path);

    BitSet filter(int lvl, PartitionFragment[] arr);
}
