package ua.ihromant.mathutils.gomoku;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Getter
public class Model {
    private static final int size = 15;
    private static final int row = 5;
    private static final int[][] vectors = new int[][]{{1, 0}, {0, 1}, {1, 1}, {1, -1}};
    public static final Range range = new Range(0, size - 1);
    private final Set<Coordinate> crd;
    private final Boolean[][] fields;
    private final Frame[] frames;
    private final Boolean winner;
    private final int x;
    private final int y;

    public Model() {
        this.fields = new Boolean[size][size];
        this.fields[size / 2][size / 2] = Boolean.TRUE;
        this.crd = Set.of(new Coordinate(size / 2, size / 2));
        this.frames = generateFrames();
        this.winner = null;
        this.x = size / 2;
        this.y = size / 2;
    }

    private Model(Model that, int x, int y) {
        this.fields = Arrays.stream(that.fields).map(Boolean[]::clone).toArray(Boolean[][]::new);
        this.crd = new HashSet<>(that.crd);
        this.fields[x][y] = currMove();
        this.crd.add(new Coordinate(x, y));
        this.frames = generateFrames();
        this.winner = findWinner(x, y);
        this.x = x;
        this.y = y;
    }

    public Model move(int x, int y) {
        if (winner != null || crd.contains(new Coordinate(x, y))) {
            throw new IllegalArgumentException();
        }
        return new Model(this, x, y);
    }

    public Coordinate lastMove() {
        return new Coordinate(x, y);
    }

    private Boolean findWinner(int x, int y) {
        Boolean move = fields[x][y];
        for (int[] vector : vectors) {
            int beg = -row + 1;
            ex: for (int cff = beg; cff < 1; cff++) {
                for (int d = 0; d < row; d++) {
                    int sh = d + cff;
                    int px = x + vector[0] * sh;
                    int py = y + vector[1] * sh;
                    if (!range.contains(px) || !range.contains(py) || move != fields[px][py]) {
                        continue ex;
                    }
                }
                return move;
            }
        }
        return null;
    }

    public Boolean currMove() {
        return crd.size() % 2 == 0;
    }

    public Stream<Model> descendants() {
        return crd.stream().flatMap(Coordinate::neighbors)
                .filter(c -> fields[c.x()][c.y()] == null).distinct().map(c -> new Model(this, c.x(), c.y()));
    }

    private static Boolean[][] mirror(Boolean[][] arr) {
        Boolean[][] res = new Boolean[arr.length][arr.length];
        for (int i = 0; i < arr.length; i++) {
            System.arraycopy(arr[size - i - 1], 0, res[i], 0, arr.length);
        }
        return res;
    }

    private static Boolean[][] rotate(Boolean[][] arr) {
        Boolean[][] res = new Boolean[arr.length][arr.length];
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr.length; j++) {
                res[i][j] = arr[j][size - i - 1];
            }
        }
        return res;
    }

    public Frame frame() {
        return new Frame(fields);
    }

    private Frame[] generateFrames() {
        return new Frame[]{new Frame(fields), new Frame(rotate(fields)), new Frame(rotate(rotate(fields))), new Frame(rotate(rotate(rotate(fields)))),
                new Frame(mirror(fields)), new Frame(rotate(mirror(fields))),
                new Frame(rotate(rotate(mirror(fields)))), new Frame(rotate(rotate(rotate(mirror(fields)))))};
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append('+');
        result.append("-".repeat(size));
        result.append('+');
        result.append('\n');
        for (int i = size - 1; i >= 0; i--) {
            result.append('|');
            for (int j = 0; j < size; j++) {
                Boolean fld = fields[j][i];
                result.append(fld == null ? ' ' : fld ? 'X' : 'O');
            }
            result.append('|');
            result.append('\n');
        }
        result.append('+');
        result.append("-".repeat(size));
        result.append('+');
        result.append('\n');
        return result.toString();
    }
}
