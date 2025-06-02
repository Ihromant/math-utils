package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.stream.IntStream;

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

    public IntList(int[] arr, int size) {
        this.arr = arr;
        this.size = size;
    }

    public int size() {
        return size;
    }

    // IMPORTANT: unsafe, so use read-only or if you know what are you doing
    public int[] arr() {
        return arr;
    }

    public void add(int v) {
        arr[size++] = v;
    }

    public void addToSorted(int el) {
        int pos = size;
        while (pos > 0) {
            int diff = arr[pos - 1] - el;
            if (diff > 0) {
                pos--;
            } else if (diff == 0) {
                return;
            } else {
                break;
            }
        }
        int sh = size - pos;
        if (sh > 0) {
            System.arraycopy(arr, pos, arr, pos + 1, sh);
        }
        arr[pos] = el;
        size++;
    }

    public void addToSortedDesc(int el) {
        int pos = size;
        while (pos > 0) {
            int diff = arr[pos - 1] - el;
            if (diff < 0) {
                pos--;
            } else if (diff == 0) {
                return;
            } else {
                break;
            }
        }
        int sh = size - pos;
        if (sh > 0) {
            System.arraycopy(arr, pos, arr, pos + 1, sh);
        }
        arr[pos] = el;
        size++;
    }

    public int remove() {
        return arr[--size];
    }
    
    public boolean contains(int el) {
        for (int i = 0; i < size; i++) {
            if (arr[i] == el) {
                return true;
            }
        }
        return false;
    }

    public int get(int idx) {
        return arr[idx];
    }

    public int getLast() {
        return arr[size - 1];
    }

    public boolean isEmpty() {
        return size == 0;
    }
    
    public int[] toArray() {
        return Arrays.copyOf(arr, size);
    }

    public IntStream stream() {
        return Arrays.stream(arr, 0, size);
    }

    public IntList copy() {
        return new IntList(arr.clone(), size);
    }

    @Override
    public String toString() {
        return Arrays.toString(Arrays.copyOf(arr, size));
    }
}
