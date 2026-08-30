package lucns.oblivium.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;

import lucns.oblivium.data.models.Message;
import lucns.oblivium.data.models.Person;

public class ConversationStorageManager {

    public interface Callback {
        void onConversationAvailable(Message[] messages);
    }

    private Callback callback;

    private final String basePath;
    private String personPath, messagesPath;
    private Person person;

    public ConversationStorageManager(Context context) {
        this.basePath = context.getExternalFilesDir(null).getPath() + "/persons";
    }

    public ConversationStorageManager(Context context, Person person) {
        this.basePath = context.getExternalFilesDir(null).getPath() + "/persons";
        setPerson(person);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setPerson(Person person) {
        this.person = person;
        this.personPath = basePath + "/" + person.username;
        this.messagesPath = personPath + "/conversation.obl";
    }

    public void requestConversation() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message[] messages = readAllMessages();
                if (messages != null && messages.length > 0) person.lastMessage = messages[messages.length - 1];
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onConversationAvailable(messages);
                    }
                });
            }
        }).start();
    }

    public void updateMessage(Message message) {
        File file = new File(messagesPath);
        if (!file.exists()) return;
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = reader.readLine()) != null) {
                if (builder.length() > 0) builder.append("\n");
                if (line.startsWith(String.valueOf(message.timestamp))) builder.append(message.toString());
                else builder.append(message.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void appendMessage(Message message) {
        append(messagesPath,  message.toString());
    }

    private Message[] readAllMessages() {
        File file = new File(messagesPath);
        if (!file.exists()) return null;
        List<Message> list = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = reader.readLine()) != null) {
                list.add(Message.fromString(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list.toArray(new Message[0]);
    }

    public Message getLastMessage() {
        String last = readLast(messagesPath);
        if (last == null) return null;
        return Message.fromString(last);
    }

    private String readLast(String path) {
        if (!new File(path).exists()) return null;
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            do {
                line = reader.readLine();
            } while (line != null);
            return line;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void append(String path, String line) {
        File file = new File(path);
        if (file.length() > 0 && !line.startsWith("\n")) line = "\n" + line;
        try (FileChannel sbc = FileChannel.open(file.toPath(), StandardOpenOption.APPEND)) {
            sbc.write(ByteBuffer.wrap(line.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
