package ua.ihromant.mathutils.nauty;

import ua.ihromant.mathutils.util.FixBS;

import java.util.List;

public interface NodeChecker {
    boolean check(Partition partition, List<FixBS> path);
}
