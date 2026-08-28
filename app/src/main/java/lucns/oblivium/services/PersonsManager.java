package lucns.oblivium.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import lucns.oblivium.data.models.Message;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.utils.Annotator;

public class PersonsManager {

    public interface Callback {
        void onPersonsAvailable();
    }

    private Callback callback;
    private final String personsPath;
    private Person[] persons;
    private ConversationStorageManager conversationStorageManager;
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
        this.conversationStorageManager = new ConversationStorageManager(context);
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
            writePerson(person);
            return;
        }
        Person[] persons2 = new Person[persons.length + 1];
        System.arraycopy(persons, 0, persons2, 0, persons.length);
        persons2[persons.length] = person;
        persons = persons2;
        writePerson(person);
        callback.onPersonsAvailable();
    }

    public void comparePersons(Person[] remotePersons) {
        if (persons == null) {
            persons = remotePersons;
            for (Person p : persons) writePerson(p);
        } else {
            Map<String, Person> map = new HashMap<>();
            for (Person p : persons) map.put(p.username, p);
            for (Person p : remotePersons) {
                if (map.containsKey(p.username)) {
                    Person person = map.get(p.username);
                    if (person.registerId == null || (!person.registerId.equals(p.registerId) && p.registerId != null)) {
                        person.registerId = p.registerId;
                        writePerson(person);
                    }
                    continue;
                }
                map.put(p.username, p);
                writePerson(p);
            }
            persons = map.values().toArray(new Person[0]);
        }
        callback.onPersonsAvailable();
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
                    persons[i] = readPerson(folders[i].getName());
                    conversationStorageManager.setPerson(persons[i]);
                    persons[i].conversation = new Message[]{conversationStorageManager.getLastMessage()};
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

    public void deleteAll() {
        persons = new Person[0];
        File[] files = new File(personsPath).listFiles();
        if (files == null) return;
        for (File file : files) {
            File[] fs = file.listFiles();
            for (File f : fs) f.delete();
            file.delete();
        }
    }

    public Person readPerson(String username) {
        Annotator annotator = new Annotator();
        annotator.setFullPath(personsPath + "/" + username + "/data.json");
        try {
            JSONObject jsonObject = new JSONObject(annotator.getContent());
            return new Person(username, jsonObject.optString("fcm_register_id"), jsonObject.getLong("timestamp"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void writePerson(Person person) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", person.username);
            if (person.registerId != null) jsonObject.put("fcm_register_id", person.registerId);
            jsonObject.put("timestamp", person.timestamp);
            Annotator annotator = new Annotator();
            annotator.setFullPath(personsPath + "/" + person.username + "/data.json");
            annotator.setContent(jsonObject.toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
