package lucns.oblivium.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) builder.append("\n");
                if (line.startsWith(String.valueOf(message.timestamp))) {
                    builder.append(message.timestamp);
                    builder.append(' ');
                    builder.append(message.toString());
                } else  {
                    builder.append(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        write(messagesPath,  builder.toString());
    }

    public void appendMessage(Message message) {
        append(messagesPath, message.timestamp + " " + message.toString());
    }

    private Message[] readAllMessages() {
        File file = new File(messagesPath);
        if (!file.exists()) return null;
        List<Message> list = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line, message;
            while ((line = reader.readLine()) != null) {
                message = line.substring(line.indexOf(" ") + 1);
                list.add(Message.fromString(message));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list.toArray(new Message[0]);
    }

    public Message getLastMessage() {
        String last = readLast(messagesPath);
        if (last == null) return null;
        return Message.fromString(last.substring(last.indexOf(' ') + 1));
    }

    private String readLast(String path) {
        if (!new File(path).exists()) return null;
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line, lastLine = null;
            while ((line = reader.readLine()) != null) {
                lastLine = line;
            }
            return lastLine;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void append(String path, String line) {
        File file = new File(path);
        try {
            if (!file.exists()) file.createNewFile();
            else if (file.length() > 0 && !line.startsWith("\n")) line = "\n" + line;
            FileChannel sbc = FileChannel.open(file.toPath(), StandardOpenOption.APPEND);
            sbc.write(ByteBuffer.wrap(line.getBytes()));
            sbc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(String path, String content) {
        File file = new File(path);
        try {
            if (!file.exists()) file.createNewFile();
            FileChannel sbc = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
            sbc.write(ByteBuffer.wrap(content.getBytes()));
            sbc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
