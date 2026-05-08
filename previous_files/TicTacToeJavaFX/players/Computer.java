package players;
import java.util.ArrayList;

public class Computer {
    private ArrayList<Integer> boardPosition;
    private boolean isComputerMaximizer;

    private static final int MINIMIZER_VALUE = -1;
    private static final int MAXIMIZER_VALUE = 1;

    //winning lines for checkWinner()
    private static final int[][] WINNING_LINES = {
        {0, 1, 2}, 
        {3, 4, 5},
        {6, 7, 8},
        {0, 3, 6}, 
        {1, 4, 7},
        {2, 5, 8},
        {0, 4, 8}, 
        {2, 4, 6}
    };

    public Computer(ArrayList<Integer> boardPosition, boolean isComputerMaximizer) {
        this.boardPosition = boardPosition;
        this.isComputerMaximizer = isComputerMaximizer;
    }

    // Loops through the board list, and returns all indices where the
    // value at that index is 0 (an "empty" tile)
    private ArrayList<Integer> availableMoves() {
        ArrayList<Integer> moves = new ArrayList<>();
        for (int i = 0; i < this.boardPosition.size(); i++) {
            if (boardPosition.get(i) == 0)
                moves.add(i);
        }
        return moves;
    }

    public boolean isMaximizer() {
        return this.isComputerMaximizer;
    }

    //Minimax algorithm that returns the best move. 
    public int getBestMove(ArrayList<Integer> boardPosition) {
        this.boardPosition = boardPosition; // update Computer's board grid
        int bestScore;
        int bestMove = Integer.MIN_VALUE;
        int moveValue;

        if (this.isComputerMaximizer){
            moveValue = MAXIMIZER_VALUE;
            bestScore = Integer.MIN_VALUE;
        }
        else{
            moveValue = MINIMIZER_VALUE;
            bestScore = Integer.MAX_VALUE;
        }

        // for each available move, do the following:
            // make the move on the board
            // call minimax() with depth 0 and isMaximizer
            // undo the move
            // update the best score & move (if the evaluated move has a better score)
        for (Integer move: this.availableMoves()) {
            // set the move on the board
            this.boardPosition.set(move, moveValue);
            // call minimax with depth 0 and the maximizer/minimizer value of the Computer

            int evaluatedScore = minimax(0, !this.isComputerMaximizer); // returns a score evaluation of the move
            // undo the move (reset the array representation of the move to "empty" -> 0)
            this.boardPosition.set(move, 0);

            if(this.isComputerMaximizer){
                // if the score is better than the best score, update the best move and best score
                if ( evaluatedScore > bestScore) {  // multiply the score by the maximizer/minimizer value here to always return a positive value for ease of evaluation
                    bestScore = evaluatedScore;
                    bestMove = move;
                }
            } else{
                // if the score is better than the best score, update the best move and best score
                if ( evaluatedScore < bestScore) {  // multiply the score by the maximizer/minimizer value here to always return a positive value for ease of evaluation
                    bestScore = evaluatedScore;
                    bestMove = move;
                }
            }

        }

        // returns the index of the best move to make
        return bestMove;
    }

    // check the current board for a winner
    public int checkWinner() {
        for (int[] line : WINNING_LINES) {
            int a = boardPosition.get(line[0]);
            int b = boardPosition.get(line[1]);
            int c = boardPosition.get(line[2]);

            if (a != 0 && a == b && b == c) {
                return a; 
            }
        }
        return 0; 
    }

    //  check if the board is full (tie)
    public boolean isBoardFull() {
        if (this.availableMoves().isEmpty())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public int minimax(int depth, boolean isMaximizer) {
        // check for a winner or tie
        int winnerValue = this.checkWinner();

        // choose fastest win for maximizer
        if (winnerValue == MAXIMIZER_VALUE) {
            return 10 - depth;
        }

        // choose slowest loss for minimizer
        if (winnerValue == MINIMIZER_VALUE) {
            return -10 + depth;
        }

        // check for a tie if there are no moves left
        if (this.isBoardFull()) {
            return 0;
        }   

        // if no win/tie, check every open square/move
        ArrayList<Integer> moves = this.availableMoves();

        if (isMaximizer) {
            // the maximizer chooses the move with the highest score
            int bestScore = Integer.MIN_VALUE;

            for (int i = 0; i < moves.size(); i++) {
                int moveIndex = moves.get(i);

                // make the move
                this.boardPosition.set(moveIndex, MAXIMIZER_VALUE);

                // the minimizer evaluates the new position
                int score = this.minimax(depth + 1, false);

                // undo the move
                this.boardPosition.set(moveIndex, 0);

                if (score > bestScore) {
                    bestScore = score;
                }
            }

            return bestScore;
        } else {
            // the minimizer chooses the move with the lowest score
            int bestScore = Integer.MAX_VALUE;

            for (int i = 0; i < moves.size(); i++) {
                int moveIndex = moves.get(i);

                // make the move
                this.boardPosition.set(moveIndex, MINIMIZER_VALUE);

                // the maximizer evaluates the new position
                int score = this.minimax(depth + 1, true);

                // undo the move
                this.boardPosition.set(moveIndex, 0);

                if (score < bestScore) {
                    bestScore = score;
                }
            }
            return bestScore;
        }
    }
}
