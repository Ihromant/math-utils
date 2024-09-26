package ua.ihromant.mathutils.gomoku;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Algo {
    @Test
    public void test() {
        Model model = new Model().move(8, 8)
                .move(6, 8).move(8, 6)
                .move(6, 9).move(6, 7)
                .move(7, 9).move(8, 9)
                .move(8, 10).move(9, 11)
                .move(7, 8).move(7, 10)
                ;
        System.out.println(model);
        //Model next = model.move(8, 8);
        System.out.println(winningMove(model));
        Map<Frame, Coordinate> decisions = new ConcurrentHashMap<>();
        //model.descendants().forEach(md -> );
    }

    @Test
    public void testWinner() {
        Model model = new Model()
                .move(6, 7).move(7, 8)
                .move(5, 7).move(7, 9)
                .move(4, 7).move(7, 10)
                .move(3, 7).move(7, 11);
        assertTrue(model.getWinner());
    }

    @Test
    public void testWinningMove() {
        Model model = new Model().move(6, 7)
                .move(7, 8).move(5, 7)
                .move(7, 9).move(4, 7)
                .move(7, 10).move(3, 7);
        System.out.println(cap - search(model));
        //assertEquals(new Coordinate(7, 6), Objects.requireNonNull(winningMove(model)).lastMove());
        model = new Model().move(6, 7)
                .move(7, 8).move(5, 7)
                .move(7, 9).move(4, 7);
        assertEquals(new Coordinate(7, 6), Objects.requireNonNull(winningMove(model)).lastMove());
        //System.out.println(winningMove(model));
    }

    private static final int cap = 36;

    public static int search(Model node) {
        return alphaBeta(node, cap - node.getCrd().size(), Integer.MIN_VALUE, Integer.MAX_VALUE, node.currMove());
    }

    public static int alphaBeta(Model node, int depth, int alpha, int beta, boolean maximizingPlayer) {
        if (depth == 0) {
            return 0;
        }
        if (node.getWinner() != null) {
            boolean winner = node.getWinner();
            return winner ? cap - node.getCrd().size() : -cap + node.getCrd().size();
        }
        if (maximizingPlayer) {
            int value = Integer.MIN_VALUE;
            List<Model> descendants = node.descendants().toList();
            for (Model child : descendants) {
                value = Math.max(value, alphaBeta(child, depth - 1, alpha, beta, false));
                if (value >= beta) {
                    break;
                }
                alpha = Math.max(alpha, value);
            }
            return value;
        } else {
            int value = Integer.MAX_VALUE;
            List<Model> descendants = node.descendants().toList();
            for (Model child : descendants) {
                value = Math.min(value, alphaBeta(child, depth - 1, alpha, beta, true));
                if (value <= alpha) {
                    break;
                }
                beta = Math.min(beta, value);
            }
            return value;
        }
    }

    private Model winningMove(Model model) {
        if (model.getCrd().size() >= 47) {
            return null;
        }
        List<Model> myMoves = model.descendants().toList();
        Optional<Model> winning = myMoves.stream().filter(d -> d.getWinner() != null).findAny();
        if (winning.isPresent()) {
            return winning.get();
        }
        for (Model move : myMoves) {
            List<Model> opponentMoves = move.descendants().toList();
            if (opponentMoves.stream().allMatch(om -> om.getWinner() == null && winningMove(om) != null)) {
                return move;
            }
        }
        return null;
    }
}
