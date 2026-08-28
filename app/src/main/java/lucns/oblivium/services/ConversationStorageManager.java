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
        void onConversationAvailable();
    }

    private Callback callback;

    private final String basePath;
    private String personPath;
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
    }

    public void requestConversation() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                person.conversation = readAllMessages();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onConversationAvailable();
                    }
                });
            }
        }).start();
    }

    private Message jsonToMessage(String json) {
        String[] segments = json.split(Message.DELIMITER);
        Message message = new Message();
        try {
            JSONObject jsonObject = new JSONObject(segments[1]);
            message.timestamp = Long.parseLong(segments[0]);
            message.username = jsonObject.getString("username");
            message.text = jsonObject.optString("text", null);
            message.filePath = jsonObject.optString("file_path", null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return message;
    }

    private String messageToJson(Message message) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", person.username);
            jsonObject.put("text", message.text);
            jsonObject.put("sent", message.sent);
            if (message.filePath != null) jsonObject.put("file_path", message.filePath);
            return message.timestamp + Message.DELIMITER + jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateMessage(Message message) {
        File file = new File(personPath + "/conversation.json");
        if (!file.exists()) return;
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = reader.readLine()) != null) {
                if (builder.length() > 0) builder.append("\n");
                if (line.startsWith(String.valueOf(message.timestamp))) builder.append(messageToJson(message));
                else builder.append(jsonToMessage(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void appendMessage(Message message) {
        String content = messageToJson(message);
        if (content == null) return;
        append(personPath + "/conversation.json",  content);
    }

    private Message[] readAllMessages() {
        File file = new File(personPath + "/conversation.json");
        if (!file.exists()) return null;
        List<Message> list = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = reader.readLine()) != null) {
                list.add(jsonToMessage(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list.toArray(new Message[0]);
    }

    public Message getLastMessage() {
        String last = readLast(personPath + "/conversation.json");
        if (last == null) return null;
        return jsonToMessage(last);
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
