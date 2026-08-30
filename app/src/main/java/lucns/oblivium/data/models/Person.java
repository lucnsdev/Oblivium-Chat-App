package lucns.oblivium.data.models;

public class Person {
    public String username, registerId;
    public long timestamp;
    public Message lastMessage;

    public Person(String username, String registerId, long timestamp) {
        this.username = username;
        this.registerId = registerId;
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        if (lastMessage != null && lastMessage.timestamp > timestamp) return lastMessage.timestamp;
        return timestamp;
    }
}
