package ua.ihromant.mathutils.gomoku;

import java.util.Arrays;

public record Frame(Boolean[][] frame) {
    @Override
    public int hashCode() {
        return Arrays.deepHashCode(frame);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Frame frame = (Frame) o;
        return Arrays.deepEquals(this.frame, frame.frame);
    }
}
