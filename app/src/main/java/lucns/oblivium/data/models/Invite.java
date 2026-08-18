package lucns.oblivium.data.models;

public class Invite {
    public String username;
    public long timestamp;
    public boolean sent;

    public Invite(String username, long timestamp, boolean sent) {
        this.username = username;
        this.timestamp = timestamp;
        this.sent = sent;
    }
}
