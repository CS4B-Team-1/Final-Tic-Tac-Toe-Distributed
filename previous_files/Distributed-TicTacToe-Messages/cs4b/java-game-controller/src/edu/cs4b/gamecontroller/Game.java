package edu.cs4b.gamecontroller;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Game {
    final private String gameId;
    
    private ConcurrentHashMap<String, String> players = new ConcurrentHashMap<>(); 
    private List<Integer> Board = new CopyOnWriteArrayList<>();
    private AtomicReference<String> currentTurn = new AtomicReference<>("");

    static final private int MAX_ROW_COL = 3;
    static final private int MAX_PLAYERS = 2;
    // static final public String STATUS_X_WON = "X Wins";
    // static final public String STATUS_O_WON = "O Wins";
    // static final public String STATUS_TIE = "Tie Game";
    // static final public String STATUS_ONGOING = "Ongoing";

    // public static enum Status {
    //     WON,
    //     OVER,
    //     ONGOING
    // }

    public Game(String gameId, String creatorId) {
        this.gameId = gameId;
        this.players.put(creatorId, "X");
        currentTurn = new AtomicReference<>(creatorId);
        for (int i = 0; i < 9; i++) {
            Board.add(0);
        }
    }

    public String getGameId() {
        return this.gameId;
    }

    public ConcurrentHashMap<String, String> getPlayers() {
        return new ConcurrentHashMap<>(this.players);
    }
    
    public void addPlayer(String playerId, String symbol) {
        if (players.keySet().size() < MAX_PLAYERS)
            players.put(playerId, symbol);
    }

    public void updateBoard(int row, int col, String symbol) {
        int symbolVal = 0;
        if (symbol.equals("X")) {
            symbolVal = 1;
        } else if (symbol.equals("O")) {
            symbolVal = -1;
        }

        int index = col + (row * MAX_ROW_COL);
        this.Board.set(index, Integer.valueOf(symbolVal));
    }

    public CopyOnWriteArrayList<Integer> getBoard() {
        return new CopyOnWriteArrayList<>(this.Board);
    }

    // returns either a -1, 0, or 1 depending on the value at the given (row,column)
    public int getValueAtPosition(int row, int col) {
        int index = col + (row * MAX_ROW_COL);
        return this.Board.get(index);
    }

    // public boolean setCurrentTurn(String prevTurnPlayerId, String currentTurnPlayerId) {
    //     return this.currentTurn.compareAndSet(prevTurnPlayerId, currentTurnPlayerId);
    // }

    public void setCurrentTurn(String newCurrentTurnPlayerId) {
        this.currentTurn.set(newCurrentTurnPlayerId);
    }

    public String getCurrentTurn() {
        return this.currentTurn.get();
    }

    // returns 0 if no winner was found.
    // otherwise, returns -1 for an O winner or 1 for an X winner
    public int checkWinner() {
        int rows = checkRows();
        if (rows != 0)
            return rows;
        int columns = checkColumns();
        if (columns != 0)
            return columns;
        int diagonals = checkDiagonals();
        if (diagonals != 0)
            return diagonals;
        return 0;
    }

    // returns -1 or 1 if a winner is found on the rows.
    // returns 0 if no winner was found.
    private int checkRows() {
        // for each row, check if each column contains the same symbol (integer)
        // since the "board" is a 1D list, to get the current column, add (row * MAX_ROW_COL) to its index. 
        boolean winner = true;
        for (int i = 0; i < MAX_ROW_COL; i++) {
            int symbol = Board.get(i * MAX_ROW_COL);
            if (symbol == 0) {
                winner = false;
                continue;
            }
            for (int j = 1; j < MAX_ROW_COL; j++) {
                int newSymbol = Board.get(j + (i * MAX_ROW_COL));
                if ((symbol == newSymbol) && (newSymbol != 0)) { // checks if two consecutive symbols and no zero ("empty" spot)
                    winner = true;
                    symbol = newSymbol;
                } else {
                    winner = false;
                    break;
                }
            }
            if (winner) return symbol;
        }
        return 0;
    }

    private int checkColumns() {
        boolean winner = true;
        // for each column, check consecutive row entries for the same symbol (integer)
        // to get the current row of the column, use (i + (j * MAX_ROW_COL))
        for (int i = 0; i < MAX_ROW_COL; i++) {
            int symbol = Board.get(i);
            if (symbol == 0) {
                winner = false;
                continue;
            }
            for (int j = 1; j < MAX_ROW_COL; j++) {
                int newSymbol = Board.get(i + (j * MAX_ROW_COL)); 
                if ((symbol == newSymbol) && (newSymbol != 0)) { // checks if two consecutive symbols and no zero ("empty" spot)
                    winner = true;
                    symbol = newSymbol;
                } else {
                    winner = false;
                    break;
                }
            }
            if (winner) return symbol;
        }
        return 0;
    }

    private int checkDiagonals() {
        boolean winner = true;
        int symbol;
        int newSymbol;

        // check from top left to bottom right
            // as row increases, column increases
        symbol = Board.get(0);
        if (symbol != 0) {
            int offset = 1;
            for (int i = 1; i < MAX_ROW_COL; i++) {
                newSymbol = Board.get((i * MAX_ROW_COL) + offset++);
                if ((symbol == newSymbol) && (newSymbol != 0)) { // checks if two consecutive symbols and no zero ("empty" spot)
                    winner = true;
                    symbol = newSymbol;
                } else {
                    winner = false;
                    break;
                }
            }
        } else winner = false;
        if (winner) return symbol;

        // check from top right to bottom left
            // as row increases, column decreases
        symbol = Board.get(MAX_ROW_COL - 1); // "top-right" index, i.e. for a 3x3 grid, "top-right" to "bottom-left" is index 2 -> index 4 -> index 6
        if (symbol != 0) {
            int offset = 2;
            for (int i = 1; i < MAX_ROW_COL; i++) {
                newSymbol = Board.get(((i + 1) * MAX_ROW_COL) - offset++);
                if ((symbol == newSymbol) && (newSymbol != 0)) { // checks if two consecutive symbols and no zero ("empty" spot)
                    winner = true;
                    symbol = newSymbol;
                } else {
                    winner = false;
                    break;
                }
            }
        } else winner = false;
        if (winner) return symbol;

        return 0;
    }
}