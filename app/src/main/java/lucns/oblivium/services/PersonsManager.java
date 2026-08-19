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
import lucns.oblivium.utils.Annotator;

public class PersonsManager {

    public interface Callback {
        void onPersonsAvailable();

        void onConversationAvailable();
    }

    private Callback callback;
    private final String personsPath;
    private Person[] persons;
    private static PersonsManager instance;

    public static PersonsManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PersonsManager.class) {
                instance = new PersonsManager(context);
            }
        }
        return instance;
    }

    private PersonsManager(Context context) {
        this.personsPath = context.getExternalFilesDir(null).getPath() + "/persons";
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public Person[] getPersons() {
        return persons;
    }

    public void addPerson(Person person) {
        if (persons == null) {
            persons = new Person[]{person};
            savePersonToStorage(person);
            return;
        }
        Person[] persons2 = new Person[persons.length + 1];
        persons2[persons.length] = person;
        persons = persons2;
        savePersonToStorage(person);
        callback.onPersonsAvailable();
    }

    public void comparePersons(Person[] remotePersons) {
        if (persons == null) {
            persons = remotePersons;
            for (Person p : persons) savePersonToStorage(p);
            callback.onPersonsAvailable();
            return;
        }
        // fazer comparacao aqui
    }

    public void requestConversation(Person person) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                person.conversation = readAllMessages(person.username);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onConversationAvailable();
                    }
                });
            }
        }).start();
    }

    public boolean hasPersons() {
        File[] folders = new File(personsPath).listFiles();
        return folders != null && folders.length > 0;
    }

    public void requestPersons() {
        if (persons != null) {
            callback.onPersonsAvailable();
            return;
        }
        File[] folders = new File(personsPath).listFiles();
        if (folders == null || folders.length == 0) {
            callback.onPersonsAvailable();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                persons = new Person[folders.length];
                for (int i = 0; i < folders.length; i++) {
                    persons[i] = new Person(folders[i].getName());
                    persons[i].conversation = new Message[]{getLastMessage(folders[i].getName())};
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onPersonsAvailable();
                    }
                });
            }
        }).start();
    }

    private Message jsonToMessage(String json) {
        Message message = new Message();
        try {
            JSONObject jsonObject = new JSONObject(json);
            message.username = jsonObject.getString("username");
            message.text = jsonObject.optString("text", null);
            message.timestamp = jsonObject.getLong("timestamp");
            message.filePath = jsonObject.optString("file_path", null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return message;
    }

    public void appendMessage(String username, Message message) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", username);
            jsonObject.put("text", message.text);
            jsonObject.put("timestamp", message.timestamp);
            if (message.filePath != null) jsonObject.put("file_path", message.filePath);
            append(personsPath + "/" + username + "/conversation.json", jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Message[] readAllMessages(String username) {
        File file = new File(personsPath + "/" + username + "/conversation.json");
        if (!file.exists()) return null;
        List<Message> list = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            do {
                line = reader.readLine();
                list.add(jsonToMessage(line));
            } while (line != null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list.toArray(new Message[0]);
    }

    private Message getLastMessage(String username) {
        String last = readLast(personsPath + "/" + username + "/conversation.json");
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

    private void savePersonToStorage(Person person) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", person.username);
            jsonObject.put("fcm_register_id", person.registerId);
            jsonObject.put("timestamp", person.timestamp);
            Annotator annotator = new Annotator();
            annotator.setFullPath(personsPath + "/" + person.username + "/data.json");
            annotator.setContent(jsonObject.toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
