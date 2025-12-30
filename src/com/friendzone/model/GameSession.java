package com.friendzone.model;

public class GameSession {
    private long id;
    private long player1Id;
    private long player2Id;
    private String status;
    private String gameCode;

    public GameSession() {}

    public GameSession(long id, long player1Id, long player2Id, String status, String gameCode) {
        this.id = id;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.status = status;
        this.gameCode = gameCode;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPlayer1Id() { return player1Id; }
    public void setPlayer1Id(long player1Id) { this.player1Id = player1Id; }

    public long getPlayer2Id() { return player2Id; }
    public void setPlayer2Id(long player2Id) { this.player2Id = player2Id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGameCode() { return gameCode; }
    public void setGameCode(String gameCode) { this.gameCode = gameCode; }
}
