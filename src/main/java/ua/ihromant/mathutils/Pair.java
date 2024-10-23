package ua.ihromant.mathutils;

public record Pair(int f, int s) {
    public Pair {
        if (f > s) {
            int min = s;
            s = f;
            f = min;
        }
    }

    public static void main(String[] args) {
        Pair p = new Pair(5, 4);
        System.out.println(p);
    }
}