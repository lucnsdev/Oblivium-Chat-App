package lucns.oblivium.data.models;

import lucns.oblivium.services.PersonsManager;

public class Person {
    public String username, registerId;
    public long timestamp;
    public Message[] conversation;

    public Person(String username) {
        this.username = username;
    }

    public Person(String username, long timestamp) {
        this.username = username;
        this.timestamp = timestamp;
    }

    public Person(String username, String registerId, long timestamp) {
        this.username = username;
        this.registerId = registerId;
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        if (conversation != null && conversation[conversation.length - 1].timestamp > timestamp) return conversation[conversation.length - 1].timestamp;
        return timestamp;
    }

    public void append(Message message) {
        Message[] messages = new Message[conversation.length + 1];
        System.arraycopy(conversation, 0, messages, 0, conversation.length);
        messages[conversation.length] = message;
        conversation = messages;
        PersonsManager.getInstance(null).appendMessage(username, message);
    }
}
