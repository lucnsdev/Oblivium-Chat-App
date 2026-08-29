package lucns.oblivium.activities.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
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

import lucns.oblivium.R;
import lucns.oblivium.activities.CustomDialog;
import lucns.oblivium.activities.MainActivity;
import lucns.oblivium.adapters.ConversationAdapter;
import lucns.oblivium.data.models.Person;
import lucns.oblivium.services.ConversationStorageManager;
import lucns.oblivium.services.PacketSenderManager;
import lucns.oblivium.services.PersonsManager;
import lucns.oblivium.utils.Constants;
import lucns.oblivium.utils.Utils;
import lucns.oblivium.views.FragmentView;
import lucns.oblivium.views.HorizontalIndeterminateThreeBalls;

public class FragmentConversation extends FragmentView {
    private TextView textUsername;
    private Person person;
    private ConversationStorageManager conversationStorageManager;
    private PacketSenderManager packetSenderManager;
    private final String appName;
    private ListView listView;
    private TextView textEmpty;
    private EditText editText;
    private LinearLayout rootEditText;
    private ConversationAdapter listAdapter;
    private HorizontalIndeterminateThreeBalls threeBalls;
    private IdCatcher idCatcher;
    private CustomDialog dialog;

    public FragmentConversation(Activity activity) {
        super(activity);
        this.listAdapter = new ConversationAdapter(activity);
        this.dialog = new CustomDialog(activity);
        this.conversationStorageManager = new ConversationStorageManager(activity);
        this.conversationStorageManager.setCallback(new ConversationStorageManager.Callback() {
            @Override
            public void onConversationAvailable() {
                if (person.conversation == null || person.conversation.length == 0) {
                    threeBalls.setVisibility(INVISIBLE);
                    listView.setVisibility(INVISIBLE);
                    textEmpty.setVisibility(VISIBLE);
                    return;
                }
                textEmpty.setVisibility(INVISIBLE);
                threeBalls.setVisibility(INVISIBLE);
                listAdapter.setAll(person.conversation);
                listView.setVisibility(VISIBLE);
            }
        });
        this.appName = activity.getString(R.string.app_name).toLowerCase();
    }

    @Override
    public void onCreate() {
        setContentView(R.layout.fragment_conversation);
        textUsername = findViewById(R.id.textUsername);
        textEmpty = findViewById(R.id.textEmpty);
        listView = findViewById(R.id.listView);
        listView.setAdapter(listAdapter);
        threeBalls = findViewById(R.id.threeBalls);
        editText = findViewById(R.id.editText);
        rootEditText = findViewById(R.id.rootEditText);

        View.OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.buttonBack) {
                    ((MainActivity) getActivity()).goToPersons();
                } else if (v.getId() == R.id.buttonSend) {

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

    public void setPerson(Person person) {
        this.person = person;
        if (person.username.equals(getString(R.string.app_name).toLowerCase())) findViewById(R.id.iconVerified).setVisibility(VISIBLE);
        textUsername.setText("@" + person.username);
        threeBalls.setVisibility(VISIBLE);
        listView.setVisibility(INVISIBLE);
        if (idCatcher != null) idCatcher.cancel();
        if (Utils.hasInternetConnection()) {
            idCatcher = new IdCatcher(person);
            idCatcher.request();
        }
        conversationStorageManager.setPerson(person);
        conversationStorageManager.requestConversation();
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
            databaseReference = databaseReference.child(Constants.USERS).child(p.username).child("fcm_register_id");
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String id = dataSnapshot.getValue(String.class);
                    if (id != null && id.equals(p.registerId)) {
                        PersonsManager.getInstance(getActivity()).writePerson(p);
                        p.registerId = id;
                    }
                    if (canceled) return;

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }

    @Override
    public boolean onBackPressed() {
        listAdapter.removeAll();
        listView.setVisibility(INVISIBLE);
        ((MainActivity) getActivity()).goToPersons();
        return false;
    }
}
