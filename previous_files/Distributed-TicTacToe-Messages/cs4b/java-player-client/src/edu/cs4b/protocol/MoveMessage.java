package edu.cs4b.protocol;

/**
 * Sent when a player makes a move on the board.
 * Demonstrates a Message with multiple int fields.
 */
public class MoveMessage implements Message {

    private static final long serialVersionUID = 1L;

    private final int row;
    private final int col;

    public MoveMessage(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public String toString() {
        return "MoveMessage{row=" + row + ", col=" + col + "}";
    }
}
