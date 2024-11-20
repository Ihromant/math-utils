package ua.ihromant.mathutils;

public record Triangle(int o, int u, int w) {
    @Override
    public String toString() {
        return "T(" + o + " " + u + " " + w + ")";
    }
}
