package ua.ihromant.mathutils.nauty;

import ua.ihromant.mathutils.Liner;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record NautyWrapper(Liner liner) implements Graph<Integer> {
    @Override
    public List<? extends Node<Integer>> nodes() {
        return Stream.concat(IntStream.range(0, liner.pointCount()).mapToObj(i -> new NautyNode(this, false, i)),
                IntStream.range(0, liner.lineCount()).mapToObj(i -> new NautyNode(this, true, i))).toList();
    }

    @Override
    public int size() {
        return liner.pointCount() + liner.lineCount();
    }

    private record NautyNode(NautyWrapper wrap, boolean line, int idx) implements Node<Integer> {
        @Override
        public Collection<? extends Node<Integer>> neighbors() {
            if (line) {
                int[] pts = wrap.liner.points(idx);
                return Arrays.stream(pts).mapToObj(i -> new NautyNode(wrap, false, i)).toList();
            } else {
                int[] lines = wrap.liner.lines(idx);
                return Arrays.stream(lines).mapToObj(i -> new NautyNode(wrap, true, i)).toList();
            }
        }

        @Override
        public Integer label() {
            return line ? 1 : 0;
        }

        @Override
        public boolean connected(Node<Integer> other) {
            if (!(other instanceof NautyNode that) || this.line && that.line || !this.line && !that.line) {
                return false;
            }
            return line ? wrap.liner.flag(this.idx, that.idx) : wrap.liner.flag(that.idx, this.idx);
        }

        @Override
        public int index() {
            return line ? idx + wrap.liner.pointCount() : idx;
        }
    }
}
