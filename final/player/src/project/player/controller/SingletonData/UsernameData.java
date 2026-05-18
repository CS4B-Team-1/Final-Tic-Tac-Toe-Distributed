package project.player.controller.SingletonData;

public class UsernameData {

    /*
        player Username - the name the player enters at the beginning of the program. Only used for display purposes in the GUI
        player ID - the unique ID assigned to the player by the server on connection. This is the identifier for all players in the message system

        Sender ID - see Envelope.java, the Player ID is the Sender ID.
    */
    
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
