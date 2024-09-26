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
        assertEquals(new Coordinate(7, 6), Objects.requireNonNull(winningMove(model)).lastMove());
        model = new Model().move(6, 7)
                .move(7, 8).move(5, 7)
                .move(7, 9).move(4, 7);
        assertEquals(new Coordinate(7, 6), Objects.requireNonNull(winningMove(model)).lastMove());
        System.out.println(winningMove(model));
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
