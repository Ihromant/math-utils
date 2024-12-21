package ua.ihromant.mathutils.vector;

import java.util.Arrays;

public class IntList {
    private final int[] arr;
    private int size = 0;

    public IntList(int size) {
        this.arr = new int[size];
    }

    public IntList(int[] arr) {
        this.arr = arr;
        this.size = arr.length;
    }

    public int size() {
        return size;
    }

    public void add(int v) {
        arr[size++] = v;
    }

    public int get(int idx) {
        return arr[idx];
    }

    public boolean isEmpty() {
        return size == 0;
    }
    
    public int[] toArray() {
        return Arrays.copyOf(arr, size);
    }
}
