package com.freezyoff.kosan.subscriber.ui.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.utils.VirtualKeyboard;

import java.util.regex.Pattern;

public class LoginFormFragment extends Fragment {

    public static final int VALIDATE_OK = 0;
    public static final int VALIDATE_EMAIL_FAILED = -1;
    public static final int VALIDATE_PASSWORD_FAILED = -2;
    private View inflatedView;
    private FormValidationListener listener;

    public LoginFormFragment() {
        this(null);
    }

    public LoginFormFragment(FormValidationListener listener) {
        super();
        setFormValidationListener(listener);
    }

    public FormValidationListener getFormValidationListener() {
        return this.listener;
    }

    public void setFormValidationListener(FormValidationListener listener) {
        this.listener = listener;
    }

    private int validateFields(EditText txEmail, EditText txPwd) {

        if (!Pattern.matches("^(.*)@(.*)\\.(.*)$", txEmail.getText().toString())) {
            return VALIDATE_EMAIL_FAILED;
        }

        if (txPwd.getText().toString().length() < 5) {
            return VALIDATE_PASSWORD_FAILED;
        }

        return VALIDATE_OK;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        inflatedView = inflater.inflate(R.layout.fragment_login_form, container, false);
        Button btn = inflatedView.findViewById(R.id.btnLogin);
        btn.setOnClickListener(new LoginButtonOnClickListener());
        return inflatedView;
    }

    public interface FormValidationListener {
        void onValidateEmailFailed(String email);

        void onValidatePasswordFailed(String password);

        void onValidationSuccess(String email, String password);
    }

    class LoginButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            VirtualKeyboard.hideKeyboard(getActivity());
            final EditText txEmail = inflatedView.findViewById(R.id.txEmail);
            final EditText txPassword = inflatedView.findViewById(R.id.txPassword);
            switch (validateFields(txEmail, txPassword)) {
                case VALIDATE_EMAIL_FAILED:
                    listener.onValidateEmailFailed(txEmail.getText().toString());
                    break;
                case VALIDATE_PASSWORD_FAILED:
                    listener.onValidatePasswordFailed(txPassword.getText().toString());
                    break;
                case VALIDATE_OK:
                    listener.onValidationSuccess(txEmail.getText().toString(), txPassword.getText().toString());
            }
        }

    }
}