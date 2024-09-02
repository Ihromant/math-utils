package ua.ihromant.mathutils.plane;

public sealed interface MoultonLine permits VerticalLine, PositiveLine, NegativeLine {
    MoultonPoint intersection(MoultonLine other);

    boolean contains(MoultonPoint point);

    boolean isParallel(MoultonLine other);

    MoultonLine parallelThrough(MoultonPoint point);
}
