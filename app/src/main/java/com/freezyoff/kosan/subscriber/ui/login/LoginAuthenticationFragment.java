package com.freezyoff.kosan.subscriber.ui.login;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.server.ConnectCredentials;
import com.freezyoff.kosan.subscriber.server.ServerService;
import com.freezyoff.kosan.subscriber.ui.DashboardActivity;

public class LoginAuthenticationFragment extends Fragment {

    private ServiceConnection serviceConnection;
    private AuthenticationListener authenticationListener;
    private BroadcastListener broadcastListener;

    private View inflatedView;

    public LoginAuthenticationFragment() {
        super();
        serviceConnection = new ServiceConnection();
        broadcastListener = new BroadcastListener();
    }

    public AuthenticationListener getAuthenticationListener() {
        return authenticationListener;
    }

    public void setAuthenticationListener(AuthenticationListener listener) {
        authenticationListener = listener;
    }

    public void authenticate(String email, String password) {
        authenticate(new ConnectCredentials(email, password));
    }

    public void authenticate(ConnectCredentials connectCredentials) {
        serviceConnection.bind(connectCredentials);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        inflatedView = inflater.inflate(R.layout.fragment_login_authenticate, container, false);
        this.broadcastListener.register();
        return inflatedView;
    }

    @Override
    public void onDestroyView() {
        this.broadcastListener.unregister();
        this.serviceConnection.unbind();
        super.onDestroyView();
    }

    public interface AuthenticationListener {

        int AUTHENTICATION_FAILED_TIMEOUT = -1;
        int AUTHENTICATION_FAILED_WRONG_CREDENTIALS = -2;
        int AUTHENTICATION_FAILED_NO_SUBCRIPTION = -3;

        void onAuthenticationFailed(int code);

        void onAuthenticationSuccess(ServerService service);
    }

    class ServiceConnection implements android.content.ServiceConnection {
        private ServerService.Binder binder = null;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            this.binder = (ServerService.Binder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            this.binder = null;
        }

        public void bind(ConnectCredentials connectCredentials) {
            unbind();
            Intent intent = new Intent(getContext(), ServerService.class);
            intent.putExtra(ConnectCredentials.class.getName(), connectCredentials);
            getContext().bindService(intent, this, Context.BIND_AUTO_CREATE);
        }

        public void unbind() {
            if (binder != null) {
                getContext().unbindService(this);
            }
        }

        public ServerService getService() {
            return this.binder.getService();
        }

        public boolean isConnected() {
            return this.binder != null;
        }
    }

    class BroadcastListener extends BroadcastReceiver {
        public void register() {
            IntentFilter filters = new IntentFilter();
            filters.addAction(ServerService.ACTION_CONNECTION_TIMEOUT);
            filters.addAction(ServerService.ACTION_AUTHENTICATION_FAILED);
            filters.addAction(ServerService.ACTION_USER_ROOM_SUBSCRIPTION_NOT_FOUND);
            filters.addAction(ServerService.ACTION_USER_ROOM_SUBSCRIPTION_FOUND);
            getContext().registerReceiver(this, filters);
        }

        public void unregister() {
            getContext().unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {

                /**
                 * connection attempt to server timeout
                 */
                case ServerService.ACTION_CONNECTION_TIMEOUT:
                    getAuthenticationListener().onAuthenticationFailed(AuthenticationListener.AUTHENTICATION_FAILED_TIMEOUT);
                    serviceConnection.unbind();
                    break;

                /**
                 * connection to server failed
                 */
                case ServerService.ACTION_AUTHENTICATION_FAILED:
                    getAuthenticationListener().onAuthenticationFailed(AuthenticationListener.AUTHENTICATION_FAILED_WRONG_CREDENTIALS);
                    serviceConnection.unbind();
                    break;

                /**
                 * connection success, but user doesn't have any room subscription
                 */
                case ServerService.ACTION_USER_ROOM_SUBSCRIPTION_NOT_FOUND:
                    getAuthenticationListener().onAuthenticationFailed(AuthenticationListener.AUTHENTICATION_FAILED_NO_SUBCRIPTION);
                    serviceConnection.unbind();
                    break;

                /**
                 * connection success and user have room subscription. Redirect to Dashboard
                 * @see DashboardActivity
                 */
                case ServerService.ACTION_USER_ROOM_SUBSCRIPTION_FOUND:
                    getAuthenticationListener().onAuthenticationSuccess(serviceConnection.getService());
                    break;

            }
        }
    }
}
