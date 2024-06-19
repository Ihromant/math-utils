package ua.ihromant.mathutils.saucy;

public class SaucyAlgo {
    public static int minIdx(int[] arr, int from, int n) {
        int res = 0;
        int min = arr[from];
        for (int i = from + 1; i < from + n; i++) {
            int cand = arr[i];
            if (cand < min) {
                min = cand;
                res = i;
            }
        }
        return res - from;
    }

    public static void swap(int[] arr, int x, int y) {
        int tmp = arr[x];
        arr[x] = arr[y];
        arr[y] = tmp;
    }

    private static void siftUp(int[] arr, int k) {
        do {
            int p = k / 2;
            if (arr[k] <= arr[p]) {
                return;
            } else {
                swap(arr, k, p);
                k = p;
            }
        } while (k > 1);
    }

    private static void siftDown(int[] arr, int n) {
        int p = 1;
        int k = 2;
        while (k <= n) {
            if (k < n && arr[k] < arr[k + 1]) {
                k++;
            }
            if (arr[p] < arr[k]) {
                swap(arr, p, k);
                p = k;
                k = 2 * p;
            } else {
                return;
            }
        }
    }

    private static int partition(int[] arr, int n, int m) {
        int f = 0;
        int b = n;
        for (;;) {
            while (arr[f] <= m) {
                ++f;
            }
            do {
                --b;
            } while (m <= arr[b]);
            if (f < b) {
                swap(arr, f, b);
                ++f;
            } else {
                break;
            }
        }
        return f;
    }

    private static int lobBase2(int n) {
        int k = 0;
        while (n > 1) {
            ++k;
            n >>= 1;
        }
        return k;
    }

    static int median(int a, int b, int c) {
        if (a <= b) {
            if (b <= c) {
                return b;
            }
            if (a <= c) {
                return c;
            } else {
                return a;
            }
        } else {
            if (a <= c) {
                return a;
            }
            if (b <= c) {
                return c;
            } else {
                return b;
            }
        }
    }

    public static void arrayIndirectSort(int[] a, int[] b, int n) {
        int j = n / 3;
        int h = 1;
        do {
            h = 3 * h + 1;
        } while (h < j);
        do {
            for (int i = h; i < n; i++) {
                int k = a[i];
                for (j = i; b[a[j - h]] > b[k]; ) {
                    a[j] = a[j - h];
                    if ((j -= h) < h) {
                        break;
                    }
                }
                a[j] = k;
            }
            h /= 3;
        } while (h > 0);
    }
}
