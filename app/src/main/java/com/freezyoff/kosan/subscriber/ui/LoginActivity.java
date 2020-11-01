package com.freezyoff.kosan.subscriber.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.server.ConnectCredentials;
import com.freezyoff.kosan.subscriber.server.ServerService;
import com.freezyoff.kosan.subscriber.ui.login.LoginAlertFragment;
import com.freezyoff.kosan.subscriber.ui.login.LoginAuthenticationFragment;
import com.freezyoff.kosan.subscriber.ui.login.LoginFormFragment;
import com.freezyoff.kosan.subscriber.utils.SavedUserCredentials;

public class LoginActivity extends AppCompatActivity {

    private String LOG_TAG = "LoginActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SavedUserCredentials.clear(this);

        if (hasSavedUser()) {
            getLoginAuthenticationFragment().authenticate(getSavedUser());
        }

        prepareView(R.layout.activity_login);
    }

    private void prepareView(int layoutResource) {
        setContentView(layoutResource);
        getLoginFormFragment().setFormValidationListener(new LoginFormFragmentListener());
        getLoginAuthenticationFragment().setAuthenticationListener(new LoginAuthenticationFragmentListener());
        getLoginAlertFragment().setActionListener(new LoginAlertFragmentListener());
        showFormFragment();
    }

    private void showFormFragment() {
        getSupportFragmentManager().beginTransaction()
                .show(getLoginFormFragment())
                .hide(getLoginAuthenticationFragment())
                .hide(getLoginAlertFragment())
                .commit();
    }

    private void showAuthenticationFragment() {
        getSupportFragmentManager().beginTransaction()
                .hide(getLoginFormFragment())
                .show(getLoginAuthenticationFragment())
                .hide(getLoginAlertFragment())
                .commit();
    }

    private void showAlertFragment(int alertType) {
        getLoginAlertFragment().show(alertType);
        getSupportFragmentManager().beginTransaction()
                .hide(getLoginFormFragment())
                .hide(getLoginAuthenticationFragment())
                .show(getLoginAlertFragment())
                .commit();
    }

    private LoginFormFragment getLoginFormFragment() {
        return (LoginFormFragment) getSupportFragmentManager().findFragmentById(R.id.fragLoginForm);
    }

    private LoginAuthenticationFragment getLoginAuthenticationFragment() {
        return (LoginAuthenticationFragment) getSupportFragmentManager().findFragmentById(R.id.fragLoginAuthentication);
    }

    private LoginAlertFragment getLoginAlertFragment() {
        return (LoginAlertFragment) getSupportFragmentManager().findFragmentById(R.id.fragLoginAlert);
    }

    private ConnectCredentials getSavedUser() {
        return SavedUserCredentials.get(this);
    }

    private boolean hasSavedUser() {
        return SavedUserCredentials.get(this.getBaseContext()) != null;
    }

    private void showAlertDialog(int titleSrc, int messageSrc) {
        new AlertDialog.Builder(this)
                .setTitle(titleSrc)
                .setMessage(messageSrc)
                .setCancelable(false)
                .setPositiveButton(R.string.activity_login_alert_positive_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private void redirectToDashboard() {
        finish();
        Intent redirectIntent = new Intent(this, DashboardActivity.class);
        redirectIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(redirectIntent);
    }

    /**
     * Listener that catch event from LoginFormFragment
     *
     * @see LoginFormFragment#setFormValidationListener(LoginFormFragment.FormValidationListener)
     * @see LoginFormFragment#getFormValidationListener()
     */
    class LoginFormFragmentListener implements LoginFormFragment.FormValidationListener {

        @Override
        public void onValidateEmailFailed(String email) {
            showAlertDialog(R.string.activity_login_alert_invalid_input_title, R.string.activity_login_alert_invalid_input_email);
        }

        @Override
        public void onValidatePasswordFailed(String password) {
            showAlertDialog(R.string.activity_login_alert_invalid_input_title, R.string.activity_login_alert_invalid_input_password);
        }

        @Override
        public void onValidationSuccess(String email, String password) {
            showAuthenticationFragment();
            getLoginAuthenticationFragment().authenticate(email, password);
        }

    }

    class LoginAuthenticationFragmentListener implements LoginAuthenticationFragment.AuthenticationListener {

        @Override
        public void onAuthenticationFailed(int code) {
            switch (code) {
                case LoginAuthenticationFragment.AuthenticationListener.AUTHENTICATION_FAILED_TIMEOUT:
                    showAlertFragment(LoginAlertFragment.ALERT_TIMEOUT);
                    break;
                case LoginAuthenticationFragment.AuthenticationListener.AUTHENTICATION_FAILED_WRONG_CREDENTIALS:
                    showAlertFragment(LoginAlertFragment.ALERT_WRONG_CREDENTIALS);
                    break;
                case LoginAuthenticationFragment.AuthenticationListener.AUTHENTICATION_FAILED_NO_SUBCRIPTION:
                    showAlertFragment(LoginAlertFragment.ALERT_NO_SUBCRIPTION);
            }
        }

        @Override
        public void onAuthenticationSuccess(ServerService service) {
            redirectToDashboard();
        }
    }

    class LoginAlertFragmentListener implements LoginAlertFragment.ActionListener {

        @Override
        public void onButtonTryAgainClicked(int alertType) {
            showFormFragment();
        }

    }
}
