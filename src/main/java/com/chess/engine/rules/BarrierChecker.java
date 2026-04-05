package com.chess.engine.rules;

import com.chess.engine.model.Barrier;
import com.chess.engine.model.Square;
import org.springframework.stereotype.Component;

import java.util.List;

// checks whether a movement segment is blocked by any barrier.
@Component
public class BarrierChecker {

    public boolean isStepBlocked(Square from, Square to, List<Barrier> barriers) {
        return barriers.stream().anyMatch(b -> b.blocks(from, to));
    }

    public boolean isSlidingPathBlocked(Square origin, List<Square> pathSquares, List<Barrier> barriers) {
        Square prev = origin;
        for (Square current : pathSquares) {
            if (isStepBlocked(prev, current, barriers)) return true;
            prev = current;
        }
        return false;
    }

    // returns true only if ALL paths are blocked (meaning the jump is impossible).
    public boolean isJumpBlocked(Square from, Square to, List<Barrier> barriers) {
        if (barriers.isEmpty()) return false;

        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();

        if (dx == 0 || dy == 0) {
            return isStepBlocked(from, to, barriers);
        }

        boolean path1Blocked = false;
        boolean path2Blocked = false;

        // path 1 - move horizontally first
        if (Square.isValid(from.getX() + dx, from.getY())) {
            Square intermediate1 = Square.of(from.getX() + dx, from.getY());
            path1Blocked = isStepBlocked(from, intermediate1, barriers)
                || isStepBlocked(intermediate1, to, barriers);
        } else {
            path1Blocked = true; // off the board - this path is invalid
        }

        // path 2 - move vertically first
        if (Square.isValid(from.getX(), from.getY() + dy)) {
            Square intermediate2 = Square.of(from.getX(), from.getY() + dy);
            path2Blocked = isStepBlocked(from, intermediate2, barriers)
                || isStepBlocked(intermediate2, to, barriers);
        } else {
            path2Blocked = true;
        }

        return path1Blocked && path2Blocked;
    }
}
