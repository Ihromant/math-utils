package ua.ihromant.mathutils.g;

import ua.ihromant.mathutils.util.FixBS;

public record OrbitFilter(FixBS[] filters) {
    public int currOrbit(int v) {
        int fl = filters.length;
        for (int i = 0; i < fl; i++) {
            if (!filters[i].isFull(v)) {
                return i;
            }
        }
        return fl;
    }

    public OrbitFilter copy() {
        int fl = filters.length;
        FixBS[] nextFilters = new FixBS[fl];
        for (int i = 0; i < fl; i++) {
            nextFilters[i] = filters[i].copy();
        }
        return new OrbitFilter(nextFilters);
    }

    public void or(OrbitFilter of) {
        int fl = filters.length;
        for (int i = 0; i < fl; i++) {
            filters[i].or(of.filters[i]);
        }
    }

    public boolean intersects(OrbitFilter of) {
        int fl = filters.length;
        for (int i = 0; i < fl; i++) {
            if (filters[i].intersects(of.filters[i])) {
                return true;
            }
        }
        return false;
    }
}
