package ua.ihromant.mathutils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BatchIsomorphismTest {
    private record DesignData(int groupIdx, int[][] base) {
        @Override
        public String toString() {
            return "(" + groupIdx + " " + Arrays.deepToString(base) + ')';
        }
    }

    private static final String CYCLIC = """
            [[0, 1, 3, 12, 30, 76], [0, 4, 10, 23, 47, 92], [0, 5, 22, 36, 61, 77], [0, 7, 40, 66, 98, 118], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 12, 30, 76], [0, 4, 10, 23, 47, 92], [0, 5, 54, 70, 95, 109], [0, 7, 40, 66, 98, 118], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 12, 30, 76], [0, 4, 10, 23, 47, 92], [0, 5, 22, 36, 61, 77], [0, 7, 15, 35, 67, 93], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 12, 30, 76], [0, 4, 10, 23, 47, 92], [0, 5, 54, 70, 95, 109], [0, 7, 15, 35, 67, 93], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 12, 30, 76], [0, 4, 38, 83, 107, 120], [0, 5, 22, 36, 61, 77], [0, 7, 40, 66, 98, 118], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 12, 30, 76], [0, 4, 38, 83, 107, 120], [0, 5, 54, 70, 95, 109], [0, 7, 40, 66, 98, 118], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 12, 30, 76], [0, 4, 38, 83, 107, 120], [0, 5, 22, 36, 61, 77], [0, 7, 15, 35, 67, 93], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 12, 30, 76], [0, 4, 38, 83, 107, 120], [0, 5, 54, 70, 95, 109], [0, 7, 15, 35, 67, 93], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 47, 92], [0, 4, 13, 40, 100, 107], [0, 5, 48, 56, 106, 120], [0, 10, 22, 38, 71, 95], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 47, 92], [0, 4, 13, 40, 100, 107], [0, 5, 11, 25, 75, 83], [0, 10, 22, 38, 71, 95], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 47, 92], [0, 4, 23, 30, 90, 117], [0, 5, 48, 56, 106, 120], [0, 10, 22, 38, 71, 95], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 47, 92], [0, 4, 23, 30, 90, 117], [0, 5, 11, 25, 75, 83], [0, 10, 22, 38, 71, 95], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 47, 92], [0, 4, 23, 30, 90, 117], [0, 5, 11, 25, 75, 83], [0, 10, 41, 65, 98, 114], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 47, 92], [0, 4, 23, 30, 90, 117], [0, 5, 48, 56, 106, 120], [0, 10, 41, 65, 98, 114], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 47, 92], [0, 4, 13, 40, 100, 107], [0, 5, 11, 25, 75, 83], [0, 10, 41, 65, 98, 114], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 47, 92], [0, 4, 13, 40, 100, 107], [0, 5, 48, 56, 106, 120], [0, 10, 41, 65, 98, 114], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 27, 34, 50], [0, 4, 36, 66, 74, 111], [0, 5, 14, 72, 85, 120], [0, 10, 22, 39, 83, 108], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 27, 34, 50], [0, 4, 19, 56, 64, 94], [0, 5, 14, 72, 85, 120], [0, 10, 22, 39, 83, 108], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 27, 34, 50], [0, 4, 36, 66, 74, 111], [0, 5, 14, 72, 85, 120], [0, 10, 28, 53, 97, 114], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 27, 34, 50], [0, 4, 19, 56, 64, 94], [0, 5, 14, 72, 85, 120], [0, 10, 28, 53, 97, 114], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 27, 34, 50], [0, 4, 36, 66, 74, 111], [0, 5, 11, 46, 59, 117], [0, 10, 22, 39, 83, 108], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 27, 34, 50], [0, 4, 19, 56, 64, 94], [0, 5, 11, 46, 59, 117], [0, 10, 22, 39, 83, 108], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 27, 34, 50], [0, 4, 36, 66, 74, 111], [0, 5, 11, 46, 59, 117], [0, 10, 28, 53, 97, 114], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 27, 34, 50], [0, 4, 19, 56, 64, 94], [0, 5, 11, 46, 59, 117], [0, 10, 28, 53, 97, 114], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 9, 91, 103], [0, 4, 34, 47, 54, 99], [0, 5, 19, 56, 67, 116], [0, 16, 41, 69, 87, 109], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 9, 91, 103], [0, 4, 34, 47, 54, 99], [0, 5, 15, 64, 75, 112], [0, 16, 41, 69, 87, 109], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 9, 91, 103], [0, 4, 31, 76, 83, 96], [0, 5, 19, 56, 67, 116], [0, 16, 41, 69, 87, 109], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 9, 91, 103], [0, 4, 31, 76, 83, 96], [0, 5, 15, 64, 75, 112], [0, 16, 41, 69, 87, 109], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 9, 91, 103], [0, 4, 34, 47, 54, 99], [0, 5, 19, 56, 67, 116], [0, 16, 33, 55, 73, 101], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 9, 91, 103], [0, 4, 34, 47, 54, 99], [0, 5, 15, 64, 75, 112], [0, 16, 33, 55, 73, 101], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 9, 91, 103], [0, 4, 31, 76, 83, 96], [0, 5, 19, 56, 67, 116], [0, 16, 33, 55, 73, 101], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 9, 91, 103], [0, 4, 31, 76, 83, 96], [0, 5, 15, 64, 75, 112], [0, 16, 33, 55, 73, 101], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 7, 59, 82], [0, 5, 18, 29, 37, 54], [0, 9, 39, 66, 80, 100], [0, 10, 22, 38, 53, 86], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 7, 59, 82], [0, 5, 77, 94, 102, 113], [0, 9, 39, 66, 80, 100], [0, 10, 22, 38, 53, 86], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 7, 59, 82], [0, 5, 18, 29, 37, 54], [0, 9, 39, 66, 80, 100], [0, 10, 50, 83, 98, 114], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 7, 59, 82], [0, 5, 77, 94, 102, 113], [0, 9, 39, 66, 80, 100], [0, 10, 50, 83, 98, 114], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 7, 59, 82], [0, 5, 77, 94, 102, 113], [0, 9, 35, 55, 69, 96], [0, 10, 50, 83, 98, 114], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 7, 59, 82], [0, 5, 18, 29, 37, 54], [0, 9, 35, 55, 69, 96], [0, 10, 50, 83, 98, 114], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 7, 59, 82], [0, 5, 77, 94, 102, 113], [0, 9, 35, 55, 69, 96], [0, 10, 22, 38, 53, 86], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 7, 59, 82], [0, 5, 18, 29, 37, 54], [0, 9, 35, 55, 69, 96], [0, 10, 22, 38, 53, 86], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 28, 69], [0, 4, 37, 87, 107, 119], [0, 5, 14, 52, 78, 86], [0, 6, 30, 61, 97, 110], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 28, 69], [0, 4, 37, 87, 107, 119], [0, 5, 45, 53, 79, 117], [0, 6, 30, 61, 97, 110], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 28, 69], [0, 4, 11, 23, 43, 93], [0, 5, 45, 53, 79, 117], [0, 6, 22, 35, 71, 102], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 28, 69], [0, 4, 11, 23, 43, 93], [0, 5, 14, 52, 78, 86], [0, 6, 22, 35, 71, 102], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 28, 69], [0, 4, 11, 23, 43, 93], [0, 5, 14, 52, 78, 86], [0, 6, 30, 61, 97, 110], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 28, 69], [0, 4, 11, 23, 43, 93], [0, 5, 45, 53, 79, 117], [0, 6, 30, 61, 97, 110], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 28, 69], [0, 4, 37, 87, 107, 119], [0, 5, 45, 53, 79, 117], [0, 6, 22, 35, 71, 102], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 18, 28, 69], [0, 4, 37, 87, 107, 119], [0, 5, 14, 52, 78, 86], [0, 6, 22, 35, 71, 102], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 4, 53, 107, 113], [0, 2, 18, 47, 88, 118], [0, 5, 51, 62, 87, 99], [0, 7, 35, 68, 102, 111], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 4, 53, 107, 113], [0, 2, 18, 47, 88, 118], [0, 5, 51, 62, 87, 99], [0, 7, 22, 31, 65, 98], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 4, 53, 107, 113], [0, 2, 10, 40, 81, 110], [0, 5, 51, 62, 87, 99], [0, 7, 35, 68, 102, 111], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 4, 53, 107, 113], [0, 2, 10, 40, 81, 110], [0, 5, 51, 62, 87, 99], [0, 7, 22, 31, 65, 98], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 4, 53, 107, 113], [0, 2, 18, 47, 88, 118], [0, 5, 32, 44, 69, 80], [0, 7, 35, 68, 102, 111], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 4, 53, 107, 113], [0, 2, 18, 47, 88, 118], [0, 5, 32, 44, 69, 80], [0, 7, 22, 31, 65, 98], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 4, 53, 107, 113], [0, 2, 10, 40, 81, 110], [0, 5, 32, 44, 69, 80], [0, 7, 35, 68, 102, 111], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 4, 53, 107, 113], [0, 2, 10, 40, 81, 110], [0, 5, 32, 44, 69, 80], [0, 7, 22, 31, 65, 98], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 19, 47, 58], [0, 4, 17, 24, 49, 54], [0, 6, 33, 67, 103, 118], [0, 9, 35, 73, 83, 95], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 19, 47, 58], [0, 4, 76, 81, 106, 113], [0, 6, 33, 67, 103, 118], [0, 9, 35, 73, 83, 95], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 19, 47, 58], [0, 4, 17, 24, 49, 54], [0, 6, 33, 67, 103, 118], [0, 9, 40, 52, 62, 100], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 19, 47, 58], [0, 4, 76, 81, 106, 113], [0, 6, 33, 67, 103, 118], [0, 9, 40, 52, 62, 100], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 19, 47, 58], [0, 4, 17, 24, 49, 54], [0, 6, 14, 29, 65, 99], [0, 9, 35, 73, 83, 95], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 19, 47, 58], [0, 4, 76, 81, 106, 113], [0, 6, 14, 29, 65, 99], [0, 9, 35, 73, 83, 95], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 19, 47, 58], [0, 4, 17, 24, 49, 54], [0, 6, 14, 29, 65, 99], [0, 9, 40, 52, 62, 100], [0, 21, 42, 63, 84, 105]]
            [[0, 1, 3, 19, 47, 58], [0, 4, 76, 81, 106, 113], [0, 6, 14, 29, 65, 99], [0, 9, 40, 52, 62, 100], [0, 21, 42, 63, 84, 105]]
            """;
    private static final String COMM = """
            [[0, 1, 9, 24, 53, 119], [0, 2, 71, 82, 110, 113], [0, 3, 40, 101, 107, 123], [0, 13, 33, 56, 75, 93], [0, 7, 14, 63, 70, 77]]
            [[0, 1, 9, 24, 53, 119], [0, 2, 71, 82, 110, 113], [0, 3, 40, 101, 107, 123], [0, 13, 41, 43, 64, 109], [0, 7, 14, 63, 70, 77]]
            [[0, 1, 9, 24, 53, 119], [0, 2, 71, 82, 110, 113], [0, 3, 54, 85, 97, 112], [0, 13, 33, 56, 75, 93], [0, 7, 14, 63, 70, 77]]
            [[0, 1, 9, 24, 53, 119], [0, 2, 71, 82, 110, 113], [0, 3, 54, 85, 97, 112], [0, 13, 41, 43, 64, 109], [0, 7, 14, 63, 70, 77]]
            [[0, 1, 9, 24, 53, 119], [0, 2, 74, 78, 88, 99], [0, 3, 40, 101, 107, 123], [0, 13, 33, 56, 75, 93], [0, 7, 14, 63, 70, 77]]
            [[0, 1, 9, 24, 53, 119], [0, 2, 74, 78, 88, 99], [0, 3, 40, 101, 107, 123], [0, 13, 41, 43, 64, 109], [0, 7, 14, 63, 70, 77]]
            [[0, 1, 9, 24, 53, 119], [0, 2, 74, 78, 88, 99], [0, 3, 54, 85, 97, 112], [0, 13, 33, 56, 75, 93], [0, 7, 14, 63, 70, 77]]
            [[0, 1, 9, 24, 53, 119], [0, 2, 74, 78, 88, 99], [0, 3, 54, 85, 97, 112], [0, 13, 41, 43, 64, 109], [0, 7, 14, 63, 70, 77]]
            """;

    @Test
    public void checkIsomorphicDesigns() {
        Map<Map<Integer, Integer>, List<DesignData>> grouped = new HashMap<>();
        ObjectMapper om = new ObjectMapper();
        Group ccl = new CyclicGroup(126);
        Arrays.stream(CYCLIC.split("\n")).forEach(str -> {
            try {
                int[][] base = om.readValue(str, int[][].class);
                Liner lnr = Liner.byDiffFamily(ccl, base);
                grouped.computeIfAbsent(lnr.hyperbolicFreq(), ky -> new ArrayList<>()).add(new DesignData(6, base));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        Group comm = new CyclicProduct(2, 3, 3, 7);
        Arrays.stream(COMM.split("\n")).forEach(str -> {
            try {
                int[][] base = om.readValue(str, int[][].class);
                Liner lnr = Liner.byDiffFamily(comm, base);
                grouped.computeIfAbsent(lnr.hyperbolicFreq(), ky -> new ArrayList<>()).add(new DesignData(16, base));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        int k = 6;
        int fixed = 0;
        int v = 126;
        IntStream.range(1, 16).forEach(i -> {
            if (i == 6) {
                return;
            }
            Group group;
            try {
                group = GroupIndex.group(126, i);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-fix" + fixed + ".txt");
            try (FileInputStream fis = new FileInputStream(f);
                 InputStreamReader isr = new InputStreamReader(fis);
                 BufferedReader br = new BufferedReader(isr)) {
                Map<List<FixBS>, Liner> liners = new ConcurrentHashMap<>();
                br.lines().forEach(str -> {
                    if (str.contains("{{") || str.contains("[{") || str.contains("[[")) {
                        String[] split = str.substring(2, str.length() - 2).split("}, \\{");
                        int[][] base = Arrays.stream(split).map(bl -> Arrays.stream(bl.split(", "))
                                .mapToInt(Integer::parseInt).toArray()).toArray(int[][]::new);
                        Liner l = new Liner(v, Arrays.stream(base).flatMap(bl -> BibdFinder5CyclicTest.blocks(bl, v, group)).toArray(int[][]::new));
                        if (liners.putIfAbsent(Arrays.stream(base).map(a -> FixBS.of(v, a)).toList(), l) == null) {
                            Map<Integer, Integer> freq = l.hyperbolicFreq();
                            grouped.computeIfAbsent(freq, ky -> new ArrayList<>()).add(new DesignData(i, base));
                        }
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        grouped.entrySet().stream().filter(e -> e.getValue().size() > 1)
                .forEach(e -> System.out.println(e.getKey() + " " + e.getValue()));
    }

    @Test
    public void testIsomorphic1() throws IOException {
        int v = 126;
        Group gr8 = GroupIndex.group(v, 8);
        Group gr10 = GroupIndex.group(v, 10);
        ObjectMapper om = new ObjectMapper();
        FixBS[] canons = Stream.of(new Liner(v, Arrays.stream(om.readValue("[[0, 1, 2, 5, 8, 14], [0, 3, 7, 15, 34, 52], [0, 6, 74, 95, 99, 116], [0, 9, 36, 59, 78, 91], [0, 13, 22, 47, 97, 114], [0, 16, 35, 46, 84, 90], [0, 17, 21, 63, 77, 115], [0, 33, 37, 64, 103, 109]]", int[][].class)).flatMap(bl -> BibdFinder5CyclicTest.blocks(bl, v, gr8)).toArray(int[][]::new)),
                        new Liner(v, Arrays.stream(om.readValue("[[0, 1, 2, 5, 8, 14], [0, 3, 4, 98, 105, 113], [0, 6, 43, 81, 85, 109], [0, 9, 36, 52, 65, 84], [0, 10, 21, 27, 74, 77], [0, 12, 32, 69, 96, 122], [0, 13, 39, 91, 119, 120]]", int[][].class)).flatMap(bl -> BibdFinder5CyclicTest.blocks(bl, v, gr10)).toArray(int[][]::new)),
                        new Liner(v, Arrays.stream(om.readValue("[[0, 1, 2, 5, 8, 14], [0, 3, 4, 28, 88, 120], [0, 6, 24, 49, 108, 122], [0, 7, 53, 95, 102, 117], [0, 9, 19, 39, 75, 83], [0, 16, 37, 69, 99, 109], [0, 17, 38, 41, 81, 84], [0, 23, 59, 60, 90, 119]]", int[][].class)).flatMap(bl -> BibdFinder5CyclicTest.blocks(bl, v, gr8)).toArray(int[][]::new)),
                        new Liner(v, Arrays.stream(om.readValue("[[0, 1, 2, 5, 8, 14], [0, 3, 4, 29, 43, 94], [0, 6, 50, 60, 104, 113], [0, 7, 30, 35, 56, 64], [0, 12, 48, 65, 76, 117]]", int[][].class)).flatMap(bl -> BibdFinder5CyclicTest.blocks(bl, v, gr10)).toArray(int[][]::new)))
                .parallel().map(Liner::getCanonicalOld).toArray(FixBS[]::new);
        System.out.println(canons[0].equals(canons[1]));
        System.out.println(canons[2].equals(canons[3]));
    }

    @Test
    public void testIsomorphicDesigns() throws IOException {
        SemiDirectProduct sdp = new SemiDirectProduct(new CyclicGroup(25), new CyclicGroup(5));
        int fixed = 1;
        int k = 6;
        int v = sdp.order() + 1;
        Group table = sdp.asTable();
        Map<Map<Integer, Integer>, List<DesignData>> grouped = new HashMap<>();
        File f = new File("/home/ihromant/maths/g-spaces/old_initial", k + "-" + sdp.name() + "-fix" + fixed + ".txt");
        try (FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            Map<List<FixBS>, Liner> liners = new ConcurrentHashMap<>();
            br.lines().forEach(str -> {
                if (str.contains("{{") || str.contains("[{") || str.contains("[[")) {
                    String[] split = str.substring(2, str.length() - 2).split("], \\[");
                    int[][] base = Arrays.stream(split).map(bl -> Arrays.stream(bl.split(", "))
                            .mapToInt(Integer::parseInt).toArray()).toArray(int[][]::new);
                    Liner l = new Liner(v, Arrays.stream(base).flatMap(bl -> BibdFinder5CyclicTest.blocks(bl, v, table)).toArray(int[][]::new));
                    if (liners.putIfAbsent(Arrays.stream(base).map(a -> FixBS.of(v, a)).toList(), l) == null) {
                        Map<Integer, Integer> freq = l.hyperbolicFreq();
                        grouped.computeIfAbsent(freq, ky -> new ArrayList<>()).add(new DesignData(0, base));
                    }
                }
            });
        }
        Map<Map<Integer, Integer>, List<DesignData>> filtered = grouped.entrySet().stream().filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        List<Map<Integer, Integer>> idxes = new ArrayList<>(filtered.keySet());
        System.out.println(idxes);
        List<DesignData> data = filtered.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(dd -> new DesignData(idxes.indexOf(e.getKey()), dd.base()))).toList();
        System.out.println(data.size() + " " + data);
        List<FixBS> canon = data.stream().parallel().map(dd -> new Liner(v, Arrays.stream(dd.base())
                .flatMap(bl -> BibdFinder5CyclicTest.blocks(bl, v, table)).toArray(int[][]::new)).getCanonicalOld()).toList();
        for (int i = 0; i < canon.size(); i++) {
            for (int j = i + 1; j < canon.size(); j++) {
                if (canon.get(i).equals(canon.get(j))) {
                    System.out.println(i + " <-> " + j + " " + idxes.get(data.get(i).groupIdx()) + " " + Arrays.deepToString(data.get(i).base()) + " " + Arrays.deepToString(data.get(j).base()));
                }
            }
        }
    }

    @Test
    public void fingerprintTest() throws IOException {
        Map<String, String> unique = new HashMap<>();
        Files.lines(Path.of("/home/ihromant/maths/g-spaces/bunch/", "6-Z3xZ3semiZ3xZ5.txt")).forEach(l -> {
            if (!l.contains("[[")) {
                return;
            }
            unique.putIfAbsent(l.substring(l.indexOf('{'), l.indexOf('}')), l);
        });
        unique.values().forEach(System.out::println);
    }

    // 6-Z3xZ3semiZ3xZ5.txt <-> 3
    // 6-Z45semiZ3.txt <-> 4
    @Test
    public void testForCyclic6() throws IOException {
        ObjectMapper om = new ObjectMapper();
        Map<String, int[][]> lns = new HashMap<>();
        Group group = GroupIndex.group(135, 3);
        int[][] auths = BibdFinder6CyclicTest.auth(group);
        Files.lines(Path.of("/home/ihromant/maths/g-spaces/bunch/", "6-Z3xZ3semiZ3xZ5.txt")).forEach(l -> {
            if (!l.contains("[[")) {
                return;
            }
            int[][] arr1;
            try {
                arr1 = om.readValue(l.substring(l.indexOf("[{"), l.indexOf("}]") + 2).replace('{', '[').replace('}', ']'), int[][].class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            int[][] arr2;
            try {
                arr2 = om.readValue(l.substring(l.indexOf("[["), l.indexOf("]]") + 2), int[][].class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            int[][] base = Stream.concat(Arrays.stream(arr1), Arrays.stream(arr2)).sorted(Combinatorics::compareArr).toArray(int[][]::new);
            for (int[] auth : auths) {
                if (BibdFinder6CyclicTest.bigger(base, Arrays.stream(base).map(bl -> BibdFinder6CyclicTest.minimalTuple(bl, auth, group)).sorted(Combinatorics::compareArr).toArray(int[][]::new))) {
                    return;
                }
            }
            lns.putIfAbsent(l, base);
        });
        lns.keySet().forEach(System.out::println);
    }

    private static int[][] read(ObjectMapper om, String val) {
        try {
            return om.readValue(val.substring(val.indexOf("[["), val.indexOf("]]") + 2), int[][].class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
