package lucns.oblivium.data.models;

public class Conversation {
    private String username;
    private long timestamp;

    public Conversation() {}

    public Conversation(String username, long timestamp) {
        this.username = username;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
