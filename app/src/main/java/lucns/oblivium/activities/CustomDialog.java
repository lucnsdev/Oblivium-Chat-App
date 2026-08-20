package lucns.oblivium.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import lucns.oblivium.R;

public class CustomDialog {

    private Activity activity;

    public CustomDialog(Activity activity) {
        this.activity = activity;
    }

    private Dialog dialog;

    public Dialog generateDialog(int layoutId, boolean isCancelable) {
        dismiss();
        dialog = new Dialog(activity, R.style.DialogTheme);
        dialog.setCancelable(isCancelable);
        dialog.setContentView(layoutId);
        dialog.getWindow().setGravity(Gravity.CENTER);
        return dialog;
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }


    public void showDialogConnectionFailure() {
        showDialogConsent(R.string.connection_failure, R.string.connection_failure_description, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }


    public void showDialogConnectionFailure(View.OnClickListener onClickListener) {
        showDialogConsent(R.string.connection_failure, R.string.connection_failure_description, onClickListener);
    }

    public void showDialogConsent(int title, View.OnClickListener onClickListener) {
        showDialogConsent(activity.getString(title), "", onClickListener);
    }

    public void showDialogConsent(int title, int description, View.OnClickListener onClickListener) {
        showDialogConsent(activity.getString(title), activity.getString(description), onClickListener);
    }

    public void showDialogConsent(String title, String description, View.OnClickListener onClickListener) {
        Dialog d = generateDialog(R.layout.dialog_information, false);
        TextView textTitle = d.findViewById(R.id.textTitle);
        TextView textDescription = d.findViewById(R.id.textDescription);
        textTitle.setText(title);
        textDescription.setText(description);
        d.findViewById(R.id.button).setOnClickListener(onClickListener);
        d.show();
    }

    public void showDialogConfirmation(int title, int description, View.OnClickListener onClickListener) {
        showDialogConfirmation(activity.getString(title), activity.getString(description), activity.getString(R.string.fowrard_continue), activity.getString(R.string.back), onClickListener);
    }

    public void showDialogConfirmation(int title, int description, int textButtonPositive, int textButtonNegative, View.OnClickListener onClickListener) {
        showDialogConfirmation(activity.getString(title), activity.getString(description), activity.getString(textButtonPositive), activity.getString(textButtonNegative), onClickListener);
    }

    public void showDialogConfirmation(String title, String description, String textButtonPositive, String textButtonNegative, View.OnClickListener onClickListener) {
        Dialog d = generateDialog(R.layout.dialog_confirmation, false);
        TextView textTitle = d.findViewById(R.id.textTitle);
        TextView textDescription = d.findViewById(R.id.textDescription);
        textTitle.setText(title);
        textDescription.setText(description);
        Button buttonOne = d.findViewById(R.id.buttonOne);
        Button buttonTwo = d.findViewById(R.id.buttonTwo);
        buttonOne.setOnClickListener(onClickListener);
        buttonTwo.setOnClickListener(onClickListener);
        buttonOne.setText(textButtonPositive);
        buttonTwo.setText(textButtonNegative);
        d.show();
    }

    public void showWrongPasswordDialog(View.OnClickListener onClickListener) {
        Dialog d = generateDialog(R.layout.dialog_confirmation, false);
        TextView textTitle = d.findViewById(R.id.textTitle);
        TextView textDescription = d.findViewById(R.id.textDescription);
        textTitle.setText(R.string.wrong_password);
        textDescription.setText(R.string.wrong_password_description);
        Button buttonOne = d.findViewById(R.id.buttonOne);
        Button buttonTwo = d.findViewById(R.id.buttonTwo);
        buttonOne.setOnClickListener(onClickListener);
        buttonTwo.setOnClickListener(onClickListener);
        buttonOne.setText(R.string.correct_password);
        buttonTwo.setText(R.string.back);
        d.show();
    }

    public void showDialogWait(int resId) {
        showDialogWait(activity.getString(resId));
    }

    public void showDialogWait(String title) {
        Dialog d = generateDialog(R.layout.dialog_wait, false);
        TextView textTitle = d.findViewById(R.id.textTitle);
        textTitle.setText(title);
        d.show();
    }

    public void showBanDialog(View.OnClickListener onClickListener) {
        Dialog d = generateDialog(R.layout.dialog_information, false);
        TextView textTitle = d.findViewById(R.id.textTitle);
        TextView textDescription = d.findViewById(R.id.textDescription);
        textTitle.setText(R.string.ban);
        textDescription.setText(R.string.ban_description);
        d.findViewById(R.id.button).setOnClickListener(onClickListener);
        d.show();
    }

    public void showDialogInvite(String initialText, View.OnClickListener onClickListener, TextWatcher textWatcher) {
        showDialogText(activity.getString(R.string.send_invite), activity.getString(R.string.send_invite_description), activity.getString(R.string.invite), activity.getString(R.string.back), initialText, onClickListener, textWatcher);
    }

    public void showDialogText(String title, String description, String buttonO, String buttonT, String initialText, View.OnClickListener onClickListener, TextWatcher textWatcher) {
        Dialog d = generateDialog(R.layout.dialog_username, false);
        TextView textTitle = d.findViewById(R.id.textTitle);
        TextView textDescription = d.findViewById(R.id.textDescription);
        textTitle.setText(title);
        textDescription.setText(description);
        Button buttonOne = d.findViewById(R.id.buttonOne);
        Button buttonTwo = d.findViewById(R.id.buttonTwo);
        buttonOne.setText(buttonO);
        buttonTwo.setText(buttonT);
        buttonOne.setOnClickListener(onClickListener);
        buttonTwo.setOnClickListener(onClickListener);
        EditText editText = dialog.findViewById(R.id.editText);
        if (initialText != null) editText.setText(initialText);
        editText.addTextChangedListener(textWatcher);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                editText.setEnabled(true);
                editText.requestFocus();
                editText.setSelection(editText.getText().length());
                ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(editText, 0);
            }
        }, 100);
        d.show();
    }

    public void showDialogAbout() {
        showDialogConsent(R.string.about, R.string.about_description, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}
