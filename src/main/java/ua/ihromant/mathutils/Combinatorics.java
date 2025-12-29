package ua.ihromant.mathutils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Combinatorics {
    public static Stream<int[]> permutations(int[] base) {
        int cnt = base.length;
        if (cnt < 2) {
            return Stream.of(base);
        }
        return Stream.iterate(base, Objects::nonNull, Combinatorics::nextPermutation);
    }

    public static int[] nextPermutation(int[] prev) {
        int[] next = prev.clone();
        int last = next.length - 2;
        while (last >= 0) {
            if (next[last] < next[last + 1]) {
                break;
            }
            last--;
        }
        if (last < 0) {
            return null;
        }

        int nextGreater = next.length - 1;
        for (int i = next.length - 1; i > last; i--) {
            if (next[i] > next[last]) {
                nextGreater = i;
                break;
            }
        }

        int temp = next[nextGreater];
        next[nextGreater] = next[last];
        next[last] = temp;

        int left = last + 1;
        int right = next.length - 1;
        while (left < right) {
            int temp1 = next[left];
            next[left++] = next[right];
            next[right--] = temp1;
        }

        return next;
    }

    public static Stream<int[]> choices(int n, int k) {
        return Stream.iterate(IntStream.range(0, k).toArray(), Objects::nonNull, prev -> nextChoice(n, prev));
    }

    public static int[] nextChoice(int cap, int[] prev) {
        int[] next = prev.clone();
        int last = next.length - 1;
        int max = cap;
        while (last >= 0) {
            if (max - next[last] != 1) {
                break;
            }
            max = next[last];
            last--;
        }
        if (last < 0) {
            return null;
        }

        max = next[last];
        int cnt = next.length - last;
        for (int i = 0; i < cnt; i++) {
            next[last + i] = max + i + 1;
        }

        return next;
    }

    public static long combinations(int n, int k) {
        if (n < 2 * k) {
            return combinations(n, n - k);
        }
        if (k == 0) {
            return 1;
        }
        long result = 1;
        for (int i = 1; i <= k; i++) {
            result = result * (n - i + 1) / i;
        }
        return result;
    }

    public static boolean admissible(int t, int v, int k) {
        for (int s = 0; s < t; s++) {
            long left = combinations(v - s, t - s);
            long right = combinations(k - s, t - s);
            if (left % right != 0) {
                return false;
            }
        }
        return true;
    }

    public static int parity(int[] arr) {
        int cnt = 0;
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                if (arr[i] > arr[j]) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    public static int compareArr(int[] fst, int[] snd) {
        int len = Math.min(fst.length, snd.length);
        for (int i = 0; i < len; i++) {
            int cmp = fst[i] - snd[i];
            if (cmp != 0) {
                return cmp;
            }
        }
        return fst.length - snd.length;
    }

    public static <T> int compareArr(T[] fst, T[] snd, Comparator<T> comp) {
        int len = Math.min(fst.length, snd.length);
        for (int i = 0; i < len; i++) {
            int cmp = comp.compare(fst[i], snd[i]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return fst.length - snd.length;
    }

    public static boolean isPrime(int val) {
        if (val < 2) {
            return false;
        }
        long cap = Math.round(Math.sqrt(val));
        for (int i = 2; i <= cap; i++) {
            if (val % i == 0) {
                return false;
            }
        }
        return true;
    }

    public static int[] factorize(int base) {
        List<Integer> result = new ArrayList<>();
        int from = 2;
        while (base != 1) {
            int factor = factor(from, base);
            from = factor;
            base = base / factor;
            result.add(factor);
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int factor(int from, int base) {
        int sqrt = (int) Math.ceil(Math.sqrt(base + 1));
        for (int i = from; i <= sqrt; i++) {
            if (base % i == 0) {
                return i;
            }
        }
        return base;
    }

    public static long[] factorize(long base) {
        List<Long> result = new ArrayList<>();
        long from = 2;
        while (base != 1) {
            long factor = factor(from, base);
            from = factor;
            base = base / factor;
            result.add(factor);
        }
        return result.stream().mapToLong(Long::longValue).toArray();
    }

    private static long factor(long from, long base) {
        long sqrt = (long) Math.ceil(Math.sqrt(base + 1));
        for (long i = from; i <= sqrt; i++) {
            if (base % i == 0) {
                return i;
            }
        }
        return base;
    }

    public static int gcd(int a, int b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }

    public static int euler(int base) {
        int[] factors = factorize(base);
        if (factors.length == 0) {
            return 0;
        }
        int result = 1;
        int idx = 0;
        while (idx < factors.length) {
            int curr = factors[idx];
            result = result * (curr - 1);
            while (++idx < factors.length && factors[idx] == curr) {
                result = result * curr;
            }
        }
        return result;
    }

    public static int[] multipliers(int v) {
        return IntStream.range(1, v).filter(m -> gcd(m, v) == 1).toArray();
    }
}
