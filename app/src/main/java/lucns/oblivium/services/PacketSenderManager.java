package lucns.oblivium.services;

import org.json.JSONObject;

public class PacketSenderManager {

    private static PacketSenderManager instance;

    public static PacketSenderManager getInstance() {
        if (instance == null) {
            synchronized (PacketSenderManager.class) {
                instance = new PacketSenderManager();
            }
        }
        return instance;
    }

    protected PacketSenderManager() {}

    public void put(Packet packet) {

    }

    public static class Packet {

        public JSONObject data;
        public String fcmRegisterId;
        public int id;
    }
}
