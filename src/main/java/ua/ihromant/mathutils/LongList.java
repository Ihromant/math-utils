package ua.ihromant.mathutils;

import java.util.Arrays;

public class LongList {
    private final long[] arr;
    private int size;

    public LongList(int size) {
        this.arr = new long[size];
    }

    public LongList(long[] arr) {
        this.arr = arr;
        this.size = arr.length;
    }

    public LongList(long[] arr, int size) {
        this.arr = arr;
        this.size = size;
    }

    public int size() {
        return size;
    }

    // IMPORTANT: unsafe, so use read-only or if you know what are you doing
    public long[] arr() {
        return arr;
    }

    public void add(int v) {
        arr[size++] = v;
    }

    public long get(int idx) {
        return arr[idx];
    }

    public long getLast() {
        return arr[size - 1];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public long[] toArray() {
        return Arrays.copyOf(arr, size);
    }

    @Override
    public String toString() {
        return Arrays.toString(Arrays.copyOf(arr, size));
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof LongList lList)) return false;

        return size == lList.size && Arrays.equals(arr, 0, size, lList.arr, 0, size);
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (int i = 0; i < size; i++) {
            result = 31 * result + Long.hashCode(arr[i]);
        }
        return result;
    }
}
