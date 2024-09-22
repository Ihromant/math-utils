package ua.ihromant.mathutils.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class FixBS implements Comparable<FixBS> {
    private final long[] words;

    private static final long WORD_MASK = 0xffffffffffffffffL;
    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;

    public FixBS(int n) {
        this.words = new long[len(n)];
    }

    public static int len(int n) {
        return wordIndex(n - 1) + 1;
    }

    public FixBS(long[] arr) {
        this.words = arr;
    }

    private static int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }

    public void set(int fromIndex, int toIndex) {
        if (fromIndex == toIndex)
            return;

        // Increase capacity if necessary
        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex = wordIndex(toIndex - 1);

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            words[startWordIndex] |= (firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            words[startWordIndex] |= firstWordMask;

            // Handle intermediate words, if any
            for (int i = startWordIndex + 1; i < endWordIndex; i++) {
                words[i] = WORD_MASK;
            }

            // Handle last word (restores invariants)
            words[endWordIndex] |= lastWordMask;
        }
    }

    public void clear(int fromIndex, int toIndex) {
        if (fromIndex == toIndex)
            return;

        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex = wordIndex(toIndex - 1);

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            words[startWordIndex] &= ~(firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            words[startWordIndex] &= ~firstWordMask;

            // Handle intermediate words, if any
            for (int i = startWordIndex + 1; i < endWordIndex; i++) {
                words[i] = 0;
            }

            // Handle last word
            words[endWordIndex] &= ~lastWordMask;
        }
    }

    public boolean get(int bitIndex) {
        int wordIndex = wordIndex(bitIndex);
        return (words[wordIndex] & (1L << bitIndex)) != 0;
    }

    public void set(int bitIndex) {
        int wordIndex = wordIndex(bitIndex);
        words[wordIndex] |= (1L << bitIndex); // Restores invariants
    }

    public void clear(int bitIndex) {
        int wordIndex = wordIndex(bitIndex);
        words[wordIndex] &= ~(1L << bitIndex);
    }

    public void set(int bitIndex, boolean value) {
        if (value) {
            set(bitIndex);
        } else {
            clear(bitIndex);
        }
    }

    public void flip(int bitIndex) {
        int wordIndex = wordIndex(bitIndex);
        words[wordIndex] ^= (1L << bitIndex);
    }

    public void flip(int fromIndex, int toIndex) {
        if (fromIndex == toIndex)
            return;

        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex   = wordIndex(toIndex - 1);

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask  = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            words[startWordIndex] ^= (firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            words[startWordIndex] ^= firstWordMask;

            // Handle intermediate words, if any
            for (int i = startWordIndex+1; i < endWordIndex; i++)
                words[i] ^= WORD_MASK;

            // Handle last word
            words[endWordIndex] ^= lastWordMask;
        }
    }

    public void or(FixBS set) {
        for (int i = 0; i < words.length; i++) {
            words[i] |= set.words[i];
        }
    }

    public void and(FixBS set) {
        for (int i = 0; i < words.length; i++) {
            words[i] &= set.words[i];
        }
    }

    public int nextSetBit(int fromIndex) {
        int u = wordIndex(fromIndex);
        if (u >= words.length) {
            return -1;
        }

        long word = words[u] & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == words.length)
                return -1;
            word = words[u];
        }
    }

    public int nextClearBit(int fromIndex) {
        int u = wordIndex(fromIndex);
        if (u >= words.length) {
            return fromIndex;
        }

        long word = ~words[u] & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == words.length)
                return words.length * BITS_PER_WORD;
            word = ~words[u];
        }
    }

    public int previousSetBit(int fromIndex) {
        if (fromIndex < 0) {
            return -1;
        }

        int u = wordIndex(fromIndex);
        if (u >= words.length) {
            return length() - 1;
        }

        long word = words[u] & (WORD_MASK >>> -(fromIndex + 1));

        while (true) {
            if (word != 0)
                return (u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
            if (u-- == 0)
                return -1;
            word = words[u];
        }
    }

    public boolean intersects(FixBS set) {
        for (int i = 0; i < words.length; i++) {
            if ((words[i] & set.words[i]) != 0) {
                return true;
            }
        }
        return false;
    }

    public int length() {
        int wiu = words.length;
        while (wiu > 0 && words[--wiu] == 0) {
        }
        if (wiu == 0) {
            return 0;
        }

        return BITS_PER_WORD * wiu + (BITS_PER_WORD - Long.numberOfLeadingZeros(words[wiu]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FixBS fixBS = (FixBS) o;
        return Arrays.equals(words, fixBS.words);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(words);
    }

    public int cardinality() {
        int sum = 0;
        for (long word : words) {
            sum += Long.bitCount(word);
        }
        return sum;
    }

    @Override
    public String toString() {
        final int MAX_INITIAL_CAPACITY = Integer.MAX_VALUE - 8;
        int numBits = (words.length > 128) ?
                cardinality() : words.length * BITS_PER_WORD;
        // Avoid overflow in the case of a humongous numBits
        int initialCapacity = (numBits <= (MAX_INITIAL_CAPACITY - 2) / 6) ?
                6 * numBits + 2 : MAX_INITIAL_CAPACITY;
        StringBuilder b = new StringBuilder(initialCapacity);
        b.append('{');

        int i = nextSetBit(0);
        if (i != -1) {
            b.append(i);
            while (true) {
                if (++i < 0) break;
                if ((i = nextSetBit(i)) < 0) break;
                int endOfRun = nextClearBit(i);
                do { b.append(", ").append(i); }
                while (++i != endOfRun);
            }
        }

        b.append('}');
        return b.toString();
    }

    public FixBS copy() {
        return new FixBS(words.clone());
    }

    /**
     * CUSTOM METHODS
     */
    public boolean singleIntersection(FixBS snd) {
        boolean fnd = false;
        for (int i = 0; i < words.length; i++) {
            long prd = words[i] & snd.words[i];
            boolean pow = (prd & (prd - 1)) == 0;
            if (!pow) {
                return false;
            }
            boolean nonZero = prd != 0;
            if (fnd) {
                if (nonZero) {
                    return false;
                }
            } else {
                fnd = nonZero;
            }
        }
        return fnd;
    }

    @Override
    public int compareTo(FixBS o) {
        for (int i = 0; i < words.length; i++) {
            int cmp = Long.compareUnsigned(Long.reverse(words[i]), Long.reverse(o.words[i]));
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    public void swap(int a, int b) {
        boolean tmp = get(a);
        set(a, get(b));
        set(b, tmp);
    }

    public static Stream<FixBS> choices(int n, int k) {
        FixBS base = new FixBS(n);
        base.set(0, k);
        return Stream.iterate(base, Objects::nonNull, prev -> nextChoice(n, prev));
    }

    private static FixBS nextChoice(int n, FixBS prev) {
        FixBS next = prev.copy();
        int max = n;
        int cnt = 1;
        while (max >= 0 && prev.get(--max)) {
            cnt++;
        }
        max = prev.previousSetBit(max);
        if (max < 0) {
            return null;
        }

        next.clear(max, n);
        next.set(max + 1, max + cnt + 1);

        return next;
    }

    public static Stream<FixBS> fixedFirst(int n, int k, int fst) {
        FixBS base = new FixBS(n);
        base.set(fst, fst + k);
        return Stream.iterate(base, Objects::nonNull, prev -> nextChoice(n, prev, fst));
    }

    private static FixBS nextChoice(int n, FixBS prev, int fst) {
        FixBS next = prev.copy();
        int max = n;
        int cnt = 1;
        while (max >= 0 && prev.get(--max)) {
            cnt++;
        }
        max = prev.previousSetBit(max);
        if (max == fst) {
            return null;
        }

        next.clear(max, n);
        next.set(max + 1, max + cnt + 1);

        return next;
    }

    public long[] words() {
        return words;
    }
}
