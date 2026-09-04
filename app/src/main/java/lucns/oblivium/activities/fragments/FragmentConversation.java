package lucns.oblivium.activities.fragments;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

import lucns.oblivium.R;
import lucns.oblivium.activities.CustomDialog;
import lucns.oblivium.activities.MainActivity;
import lucns.oblivium.adapters.MessagesAdapter;
import lucns.oblivium.data.User;
import lucns.oblivium.data.models.Message;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.services.ConversationStorageManager;
import lucns.oblivium.services.PacketSenderManager;
import lucns.oblivium.services.PersonsManager;
import lucns.oblivium.utils.Constants;
import lucns.oblivium.utils.Utils;
import lucns.oblivium.views.FragmentView;
import lucns.oblivium.views.HorizontalIndeterminateThreeBalls;

public class FragmentConversation extends FragmentView {
    private TextView textUsername, textTime;
    private Person person;
    private PacketSenderManager packetSenderManager;
    private final String appName;
    private ListView listView;
    private TextView textEmpty;
    private EditText editText;
    private LinearLayout rootEditText;
    private MessagesAdapter listAdapter;
    private HorizontalIndeterminateThreeBalls threeBalls;
    private IdCatcher idCatcher;
    private CustomDialog dialog;
    private User user;
    private String fcmRegisterId;
    private Queue<Message> queue;

    public FragmentConversation(Activity activity) {
        super(activity);
        this.listAdapter = new MessagesAdapter(activity);
        this.dialog = new CustomDialog(activity);
        this.appName = activity.getString(R.string.app_name).toLowerCase();
        this.user = User.getInstance();
        this.queue = new LinkedList<>();
        packetSenderManager = PacketSenderManager.getInstance();
        packetSenderManager.setOnSentListener(new PacketSenderManager.OnSentListener() {

            @Override
            public void onSent(PacketSenderManager.Packet packet) {
                ConversationStorageManager conversationStorageManager = new ConversationStorageManager(getActivity());
                conversationStorageManager.setPerson(packet.getPerson());
                conversationStorageManager.updateMessage(packet.getMessage());
                if (person.username.equals(packet.getPerson().username)) {
                    Utils.vibrate();
                    listAdapter.update(packet.getMessage());
                }
                ((MainActivity) getActivity()).updatePersonItem(packet.getPerson());
            }
        });
    }

    @Override
    public void onCreate() {
        setContentView(R.layout.fragment_conversation);
        textUsername = findViewById(R.id.textUsername);
        textTime = findViewById(R.id.textTime);
        textEmpty = findViewById(R.id.textEmpty);
        listView = findViewById(R.id.listView);
        listView.setAdapter(listAdapter);
        threeBalls = findViewById(R.id.threeBalls);
        editText = findViewById(R.id.editText);
        rootEditText = findViewById(R.id.rootEditText);

        ConversationStorageManager conversationStorageManager = new ConversationStorageManager(getActivity());
        View.OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.buttonBack) {
                    person = null;
                    ((MainActivity) getActivity()).goToPersons();
                    listAdapter.removeAll();
                } else if (v.getId() == R.id.buttonSend) {
                    Editable editable = editText.getText();
                    String text = editable.toString().trim();
                    if (text.isEmpty()) return;
                    Utils.vibrate();
                    editable.clear();
                    Message message = new Message(user.getUsername(), text);
                    listAdapter.add(message);
                    listView.setVisibility(VISIBLE);
                    textEmpty.setVisibility(INVISIBLE);
                    threeBalls.setVisibility(INVISIBLE);

                    person.lastMessage = message;
                    ((MainActivity) getActivity()).updatePersonItem(person);
                    conversationStorageManager.setPerson(person);
                    conversationStorageManager.appendMessage(message);
                    scrollMessages();
                    if (!Utils.hasInternetConnection() || fcmRegisterId == null) {
                        queue.add(message);
                    } else {
                        sendMessage(message);
                    }
                } else if (v.getId() == R.id.buttonAttach) {
                    dialog.showDialogMedia(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            Intent intent;
                            if (v.getId() == R.id.buttonImage) {
                                intent = new Intent(Intent.ACTION_PICK);
                                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                            } else if (v.getId() == R.id.buttonVideo) {
                                intent = new Intent(Intent.ACTION_PICK);
                                intent.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*");
                            } else {
                                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                intent.setType("*/*");
                            }
                            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                            getActivity().startActivityForResult(intent, 1234);
                        }
                    });
                }
            }
        };
        findViewById(R.id.buttonBack).setOnClickListener(onClickListener);
        findViewById(R.id.buttonSend).setOnClickListener(onClickListener);
        findViewById(R.id.buttonAttach).setOnClickListener(onClickListener);
    }

    private void sendMessage(Message message) {
        PacketSenderManager.Packet packet = new PacketSenderManager.Packet();
        packet.setPerson(person);
        packet.setMessage(message);
        packetSenderManager.put(packet);
    }

    private void dequeueMessages() {
        if (queue.isEmpty() || !Utils.hasInternetConnection() || fcmRegisterId == null) return;
        while (!queue.isEmpty()) {
            sendMessage(queue.remove());
        }
    }

    public void putMessage(Message message) {
        listAdapter.add(message);
        scrollMessages();
    }

    private void scrollMessages() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                listView.smoothScrollToPosition(listView.getAdapter().getCount() - 1);
            }
        }, 250);
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
        this.fcmRegisterId = null;
        rootEditText.setVisibility(INVISIBLE);
        if (person.username.equals(getString(R.string.app_name).toLowerCase())) findViewById(R.id.iconVerified).setVisibility(VISIBLE);
        textUsername.setText("@" + person.username);
        textTime.setText(Utils.getDateTime(person.getTimestamp()));
        threeBalls.setVisibility(VISIBLE);
        listView.setVisibility(INVISIBLE);
        if (idCatcher != null) idCatcher.cancel();
        if (Utils.hasInternetConnection()) {
            idCatcher = new IdCatcher(person);
            idCatcher.request();
        }
        ConversationStorageManager conversationStorageManager = new ConversationStorageManager(getActivity());
        conversationStorageManager.setPerson(person);
        conversationStorageManager.setCallback(new ConversationStorageManager.Callback() {
            @Override
            public void onConversationAvailable(Message[] messages) {
                if (!FragmentConversation.this.person.username.equals(person.username)) return;
                showEditText();
                if (messages == null || messages.length == 0) {
                    threeBalls.setVisibility(INVISIBLE);
                    listView.setVisibility(INVISIBLE);
                    textEmpty.setVisibility(VISIBLE);
                    return;
                }
                textEmpty.setVisibility(INVISIBLE);
                threeBalls.setVisibility(INVISIBLE);
                listAdapter.setAll(messages);
                listView.setVisibility(VISIBLE);
                scrollMessages();
            }
        });
        conversationStorageManager.requestConversation();
    }

    private void showEditText() {
        rootEditText.setAlpha(0f);
        rootEditText.setVisibility(VISIBLE);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(rootEditText, View.ALPHA, 0f, 1f);;
        alpha.setInterpolator(new LinearInterpolator());
        alpha.setDuration(500);
        alpha.start();
    }

    public void onFilePicked(Uri uri) {
        dialog.showDialogWait(R.string.loading);
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = copyFile(uri);
                dialog.dismiss();
                if (file == null || !file.exists() || file.length() == 0) {
                    dialog.showDialogConsent(R.string.error_file_read, new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                    return;
                }
                Log.d("lucas", "path " + file.getPath());
                file.delete();
            }
        }).start();
    }

    private File copyFile(Uri uri) {
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = new File(getActivity().getCacheDir(), "temp_file_" + System.currentTimeMillis());
            OutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            if (tempFile.exists() && tempFile.length() > 0) return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public View getMobileView() {
        return rootEditText;
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {
        dialog.dismiss();
    }

    @Override
    public void onDestroy() {
        dialog.dismiss();
    }

    private class IdCatcher {

        private final Person p;
        private boolean canceled;

        protected IdCatcher(Person person) {
            this.p = person;
        }

        protected void cancel() {
            canceled = true;
        }

        protected void request() {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(appName);
            databaseReference = databaseReference.child(Constants.USERS).child(p.username).child(Constants.FCM_ID);
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String id = dataSnapshot.getValue(String.class);
                    if (id != null && !id.equals(p.registerId)) {
                        PersonsManager.getInstance(getActivity()).writePerson(p);
                        p.registerId = id;
                    }
                    if (canceled) return;
                    fcmRegisterId = id;
                    dequeueMessages();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }

    @Override
    public boolean onBackPressed() {
        person = null;
        listAdapter.removeAll();
        listView.setVisibility(INVISIBLE);
        ((MainActivity) getActivity()).goToPersons();
        return false;
    }
}
