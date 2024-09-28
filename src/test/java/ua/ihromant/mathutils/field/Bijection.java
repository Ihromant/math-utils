package ua.ihromant.mathutils.field;

import java.util.Arrays;

public record Bijection(int[] map) {
    @Override
    public String toString() {
        return Arrays.toString(map);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bijection bijection = (Bijection) o;
        return Arrays.equals(map, bijection.map);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(map);
    }
}
