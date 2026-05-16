package project.player.controller.SingletonData;

public class UsernameData {

    private static final UsernameData instance =
        new UsernameData();

    private String username;
    private String playerId;

    private UsernameData() {}

    public static UsernameData getInstance() {
        return instance;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
}
