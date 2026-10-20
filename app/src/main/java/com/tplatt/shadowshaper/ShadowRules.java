package com.tplatt.shadowshaper;

/**
 * Shared match constants and rules. No Android dependency.
 *
 * Confirmed: 15-card decks, two of a name, <=3 Exotic, <=5 Mythic,
 * six rounds, N Light points in round N, no Light carryover, score is
 * surviving Power, first player (Offensive) goes first each round.
 *
 * Opening draw 5/4 comes from player gameplay reports, not official
 * documentation. A one-card draw each additional round and six columns
 * per side are provisional until compared against the retail client.
 */
public final class ShadowRules {
    private ShadowRules() { }
    public static final int DECK_SIZE=15;
    public static final int COPY_LIMIT=2;
    public static final int EXOTIC_LIMIT=3;
    public static final int MYTHIC_LIMIT=5;
    public static final int ROUNDS=6;
    public static final int ROWS=2;
    public static final int COLUMNS=6; // provisional board dimension
    public static final int OFFENSIVE_OPENING_HAND=5;
    public static final int DEFENSIVE_OPENING_HAND=4;
    public static final int NEW_ROUND_DRAW=1; // provisional draw timing
    public static final int FRONT_ROW=1, BACK_ROW=0;

    public static int lightForRound(int round) {
        if (round<1||round>ROUNDS)
            throw new IllegalArgumentException("Round must be in range 1..6");
        return round;
    }
    public static int openingHand(boolean offensive) {
        return offensive ? OFFENSIVE_OPENING_HAND : DEFENSIVE_OPENING_HAND;
    }
    public static boolean isOnBoard(int row,int column) {
        return row>=0 && row<ROWS && column>=0 && column<COLUMNS;
    }
    public static boolean isLastRound(int round) {
        return round==ROUNDS;
    }
    public static String result(int playerPower,int cpuPower) {
        return playerPower>cpuPower?"VICTORY":playerPower<cpuPower?"DEFEAT":"DRAW";
    }
}
