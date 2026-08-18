package lucns.oblivium.services.firebase;

public class MessageIdManager {

    private long id;
    private static MessageIdManager instance;

    public static MessageIdManager getInstance() {
        if (instance == null) {
            synchronized (MessageIdManager.class) {
                instance = new MessageIdManager();
            }
        }
        return instance;
    }

    protected MessageIdManager() {}

    protected long next() {
        return id++;
    }
}
