package ua.ihromant.mathutils.nautyx;

import java.util.Arrays;
import java.util.List;

public record NautyNode(NautyWrapper wrap, boolean line, int idx) {
    public List<NautyNode> neighbors() {
        if (line) {
            int[] pts = wrap.liner().points(idx);
            return Arrays.stream(pts).mapToObj(i -> new NautyNode(wrap, false, i)).toList();
        } else {
            int[] lines = wrap.liner().lines(idx);
            return Arrays.stream(lines).mapToObj(i -> new NautyNode(wrap, true, i)).toList();
        }
    }

    public boolean connected(NautyNode that) {
        if (this.line && that.line || !this.line && !that.line) {
            return false;
        }
        return line ? wrap.liner().flag(this.idx, that.idx) : wrap.liner().flag(that.idx, this.idx);
    }

    public int index() {
        return line ? idx + wrap.liner().pointCount() : idx;
    }
}
