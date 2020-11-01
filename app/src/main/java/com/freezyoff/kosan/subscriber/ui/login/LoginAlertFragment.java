package com.freezyoff.kosan.subscriber.ui.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.freezyoff.kosan.subscriber.R;

public class LoginAlertFragment extends Fragment {

    public static final int ALERT_TIMEOUT = -1;
    public static final int ALERT_WRONG_CREDENTIALS = -2;
    public static final int ALERT_NO_SUBCRIPTION = -3;
    private TextView txTitle;
    private TextView txMessage;
    private Button btTryAgain;
    private int currentAlertType = 0;
    private ActionListener actionListener;

    public ActionListener getActionListener() {
        return this.actionListener;
    }

    public void setActionListener(ActionListener listener) {
        this.actionListener = listener;
    }

    /**
     * @param alertType
     * @see LoginAlertFragment#ALERT_TIMEOUT
     * @see LoginAlertFragment#ALERT_WRONG_CREDENTIALS
     * @see LoginAlertFragment#ALERT_NO_SUBCRIPTION
     */
    public void show(int alertType) {
        currentAlertType = alertType;
        switch (alertType) {
            case ALERT_TIMEOUT:
                _showText(
                        R.string.login_result_title_connection_timeout,
                        R.string.login_result_message_connection_timeout,
                        R.string.try_again
                );
                break;
            case ALERT_WRONG_CREDENTIALS:
                _showText(
                        R.string.login_result_title_login_failed,
                        R.string.login_result_message_authentication_failed,
                        R.string.try_again
                );
                break;
            case ALERT_NO_SUBCRIPTION:
                _showText(
                        R.string.login_result_title_access_expired,
                        R.string.login_result_message_access_expired,
                        R.string.try_again
                );
                break;
        }
    }

    private void _showText(int strResTitle, int strResMessage, int strResTryAgainButton) {
        txTitle.setText(strResTitle);
        txMessage.setText(strResMessage);
        btTryAgain.setText(strResTryAgainButton);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View inflatedView = inflater.inflate(R.layout.fragment_login_alert, container, false);
        txTitle = inflatedView.findViewById(R.id.txTitle);
        txMessage = inflatedView.findViewById(R.id.txMessage);
        btTryAgain = inflatedView.findViewById(R.id.btTryAgain);
        btTryAgain.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getActionListener().onButtonTryAgainClicked(currentAlertType);
            }

        });
        return inflatedView;
    }

    public interface ActionListener {
        void onButtonTryAgainClicked(int alertType);
    }
}
