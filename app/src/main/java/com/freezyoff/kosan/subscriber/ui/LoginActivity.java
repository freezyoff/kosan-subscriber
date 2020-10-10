package com.freezyoff.kosan.subscriber.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.server.ConnectCredentials;
import com.freezyoff.kosan.subscriber.utils.SavedUserCredentials;

import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private String LOG_TAG = "LoginActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        if (hasSavedUser()) {
            redirectDashboardActivity(getSavedUser());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        prepareView();
    }

    private ConnectCredentials getSavedUser() {
        return SavedUserCredentials.get(this);
    }

    private boolean hasSavedUser() {
        return SavedUserCredentials.get(this.getBaseContext()) != null;
    }

    private void prepareView() {
        findViewById(R.id.btLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText txEmail = findViewById(R.id.txEmail);
                final EditText txPassword = findViewById(R.id.txPassword);
                if (validateInput(txEmail, txPassword)) {
                    redirectDashboardActivity(
                            new ConnectCredentials(
                                    txEmail.getText().toString(),
                                    txPassword.getText().toString()
                            )
                    );
                }

            }
        });

    }

    private boolean validateInput(EditText txEmail, EditText txPwd) {
        //validate email
        if (!Pattern.matches("^(.*)@(.*)\\.(.*)$", txEmail.getText().toString())) {
            showInvalidInputDialog(R.string.alert_login_invalid_input_title, R.string.alert_login_invalid_input_email);
            return false;
        }

        if (txPwd.getText().toString().length() < 5) {
            showInvalidInputDialog(R.string.alert_login_invalid_input_title, R.string.alert_login_invalid_input_password);
            return false;
        }

        return true;
    }

    private void showInvalidInputDialog(int titleSrc, int messageSrc) {
        new AlertDialog.Builder(this)
                .setTitle(titleSrc)
                .setMessage(messageSrc)
                .setCancelable(false)
                .setPositiveButton(R.string.alert_login_positive_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private void redirectDashboardActivity(ConnectCredentials clientCredentials) {
        Log.d(LOG_TAG, LoginActivity.class.getName() + "#redirectDashboardActivity");
        Intent intent = new Intent(this, AuthenticateActivity.class);
        intent.putExtra(ConnectCredentials.class.getName(), clientCredentials);
        startActivity(intent);
    }
}
