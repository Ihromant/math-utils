package ua.ihromant.mathutils.nauty;

import ua.ihromant.mathutils.Liner;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record NautyWrapper(Liner liner) implements Graph {
    @Override
    public List<NautyNode> nodes() {
        return Stream.concat(IntStream.range(0, liner.pointCount()).mapToObj(i -> new NautyNode(this, false, i)),
                IntStream.range(0, liner.lineCount()).mapToObj(i -> new NautyNode(this, true, i))).toList();
    }

    @Override
    public int size() {
        return liner.pointCount() + liner.lineCount();
    }

}
