package com.friendzone.model;

public class User {
    private long id;
    private String username;
    private String nickname;
    private int elo;
    private double money;
    private String gender; // MALE, FEMALE, OTHER
    private String avatarUrl; // URL or path to avatar image

    public User() {}

    public User(long id, String username, String nickname, int elo, double money, String gender) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.elo = elo;
        this.money = money;
        this.gender = gender;
        this.avatarUrl = "default.png"; // Default avatar
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public int getElo() { return elo; }
    public void setElo(int elo) { this.elo = elo; }

    public double getMoney() { return money; }
    public void setMoney(double money) { this.money = money; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", username=" + username + ", nickname=" + nickname + ", elo=" + elo + ", money=" + money + ", gender=" + gender + '}';
    }
}
