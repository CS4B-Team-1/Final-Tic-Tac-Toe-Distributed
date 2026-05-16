package project.protocol;

public class Move {
    private int row;
    private int col;
    private static final int MAX_ROW_COL = 3;

    private Move(int row, int col) {
        this.row = row;
        this.col = col;
    }

    // create Move from index
    public static Move createMoveFromIndex(int index) {
        int row = -1;
        int col = -1;

        // extract column from index
        for (int i = 0; i < MAX_ROW_COL; i++) {
            if (((index - i) % MAX_ROW_COL) == 0) {
                col = i;
                break;
            } 
        }

        // extract row from index
        for (int i = 1; i < MAX_ROW_COL; i++) {
            if (((i * MAX_ROW_COL) - 1) - index >= 0) {
                row = i - 1;
                break;
            }
        }

        return new Move(row, col);
    }

    public static int toIndex(int row, int col) {
        return (row * MAX_ROW_COL) + col;
    }

    public int getIndex() {
        return toIndex(this.row, this.col);
    }

    public int getRow() {
        return this.row;
    }

    public int getCol() {
        return this.col;
    }
}
