package lucns.oblivium.services;

import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import lucns.oblivium.data.models.Message;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.services.firebase.FirebaseMessagingSender;
import lucns.oblivium.utils.App;
import lucns.oblivium.utils.Utils;

public class PacketSenderManager {

    private static final long DELAY_BETWEEN_SEND = 1000;

    public interface OnSentListener {
        void onSent(Packet packet);
    }

    private Queue<Packet> queue;
    private FirebaseMessagingSender sender;
    private Map<Integer, Packet> map;
    private OnSentListener listener;
    private long lastSent;
    private Handler handler;
    private Runnable runnable;

    private static PacketSenderManager instance;

    public static PacketSenderManager getInstance() {
        if (instance == null) {
            synchronized (PacketSenderManager.class) {
                instance = new PacketSenderManager();
            }
        }
        return instance;
    }

    protected PacketSenderManager() {
        queue = new LinkedList<>();
        map = new HashMap<>();
        sender = new FirebaseMessagingSender(App.getContext(), new FirebaseMessagingSender.Callback() {
            @Override
            public void onFinish(int id, int responseCode, String responseMessage) {
                if (responseCode == 200) {
                    lastSent = System.currentTimeMillis();
                    Packet packet = map.remove(id);
                    packet.message.text.sent = true;
                    packet.person.lastMessage = packet.message;
                    if (listener != null) listener.onSent(packet);
                    dequeue();
                    return;
                }
                put(map.get(id));
            }
        });
        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                dequeue();
            }
        };
    }

    public void setOnSentListener(OnSentListener listener) {
        this.listener = listener;
    }

    public synchronized void dequeue() {
        if (!Utils.hasInternetConnection()) return;
        long difference = System.currentTimeMillis() - lastSent;
        if (difference < DELAY_BETWEEN_SEND) {
            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, Math.max(DELAY_BETWEEN_SEND - difference, 100));
            return;
        }
        if (queue.isEmpty()) return;
        Packet packet = queue.remove();
        sender.setDestineRegisterId(packet.person.registerId);
        map.put(sender.put(packet.message.toSend()), packet);
    }

    public void put(Packet packet) {
        boolean sending = !queue.isEmpty();
        queue.add(packet);
        if (sending) return;
        dequeue();
    }

    public static class Packet {
        private Person person;
        private Message message;

        public void setPerson(Person person) {
            this.person = person;
        }

        public Person getPerson() {
            return person;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public Message getMessage() {
            return message;
        }
    }
}
