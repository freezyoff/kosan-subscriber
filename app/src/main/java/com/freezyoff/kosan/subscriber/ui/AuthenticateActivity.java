package com.freezyoff.kosan.subscriber.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.server.ConnectCredentials;
import com.freezyoff.kosan.subscriber.server.ServerService;

import java.util.HashMap;
import java.util.Map;

public class AuthenticateActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AuthenticateActivity";
    private static final String STATE_VIEW_CONNECTING = "com.freezyoff.kosan.subscriber.AuthenticateActivity.STATE_VIEW_CONNECTING";
    private static final String STATE_VIEW_CONNECTION_TIMEOUT = "com.freezyoff.kosan.subscriber.AuthenticateActivity.STATE_VIEW_TIMEOUT";
    private static final String STATE_VIEW_LOGIN_FAILED = "com.freezyoff.kosan.subscriber.AuthenticateActivity.STATE_VIEW_LOGIN_FAILED";
    private static final String STATE_VIEW_NO_ROOM_SUBSCRIPTION = "com.freezyoff.kosan.subscriber.AuthenticateActivity.STATE_VIEW_NO_ROOM_SUBSCRIPTION";
    private ServerService serverService;
    private ServiceConnection serverServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServerService.Binder binder = (ServerService.Binder) service;
            serverService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serverService = null;
        }
    };
    private BroadcastListener broadcastListener;
    private HashMap<String, View> stateViewMap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _prepareServiceBinding();

        _prepareView();
        _toggleStateView(STATE_VIEW_CONNECTING);

        _prepareBroadcastReceiver();

    }

    @Override
    protected void onDestroy() {
        _destroyServiceBinding();
        _destroyBroadcastReceiver();
        super.onDestroy();
    }

    private void _prepareServiceBinding() {
        ConnectCredentials connectCredentials = getIntent().getParcelableExtra(ConnectCredentials.class.getName());
        Intent intent = new Intent(this, ServerService.class);
        intent.putExtra(ConnectCredentials.class.getName(), connectCredentials);
        bindService(intent, serverServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void _destroyServiceBinding() {
        unbindService(serverServiceConnection);
    }

    private void _prepareBroadcastReceiver() {
        //Broadcast Reciever for Mqtt Authentication
        IntentFilter mqttFilters = new IntentFilter();
        mqttFilters.addAction(ServerService.ACTION_CONNECTION_TIMEOUT);
        mqttFilters.addAction(ServerService.ACTION_AUTHENTICATION_FAILED);
        mqttFilters.addAction(ServerService.ACTION_USER_ROOM_SUBSCRIPTION_NOT_FOUND);
        mqttFilters.addAction(ServerService.ACTION_USER_ROOM_SUBSCRIPTION_FOUND);

        broadcastListener = new BroadcastListener();
        broadcastListener.register(mqttFilters);
    }

    private void _destroyBroadcastReceiver() {
        broadcastListener.unregister();
    }

    private void _prepareView() {
        setContentView(R.layout.activity_authenticate);

        stateViewMap = new HashMap();
        stateViewMap.put(STATE_VIEW_CONNECTING, findViewById(R.id.vLoader));
        stateViewMap.put(STATE_VIEW_CONNECTION_TIMEOUT, findViewById(R.id.vConnectionTimeout));
        stateViewMap.put(STATE_VIEW_LOGIN_FAILED, findViewById(R.id.vLoginFailed));
        stateViewMap.put(STATE_VIEW_NO_ROOM_SUBSCRIPTION, findViewById(R.id.vNoSubscription));

        View.OnClickListener btBackListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        };

        //set button back action listener
        stateViewMap.get(STATE_VIEW_CONNECTION_TIMEOUT).findViewById(R.id.btBack).setOnClickListener(btBackListener);
        stateViewMap.get(STATE_VIEW_LOGIN_FAILED).findViewById(R.id.btBack).setOnClickListener(btBackListener);
        stateViewMap.get(STATE_VIEW_NO_ROOM_SUBSCRIPTION).findViewById(R.id.btBack).setOnClickListener(btBackListener);
    }

    private void _toggleStateView(String flag) {
        for (Map.Entry<String, View> currentSet : stateViewMap.entrySet()) {
            currentSet.getValue().setVisibility(currentSet.getKey() == flag ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void redirectToDashboard() {
        finish();
        Intent redirectIntent = new Intent(AuthenticateActivity.this, DashboardActivity.class);
        redirectIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(redirectIntent);
    }

    class BroadcastListener extends BroadcastReceiver {

        void register(IntentFilter filters) {
            registerReceiver(this, filters);
        }

        void unregister() {
            unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {

                /**
                 * connection attempt to server timeout
                 */
                case ServerService.ACTION_CONNECTION_TIMEOUT:
                    _toggleStateView(STATE_VIEW_CONNECTION_TIMEOUT);
                    break;

                /**
                 * connection to server failed
                 */
                case ServerService.ACTION_AUTHENTICATION_FAILED:
                    _toggleStateView(STATE_VIEW_LOGIN_FAILED);
                    break;

                /**
                 * connection success, but user doesn't have any room subscription
                 */
                case ServerService.ACTION_USER_ROOM_SUBSCRIPTION_NOT_FOUND:
                    _toggleStateView(STATE_VIEW_NO_ROOM_SUBSCRIPTION);
                    break;

                /**
                 * connection success and user have room subscription. Redirect to Dashboard
                 * @see DashboardActivity
                 */
                case ServerService.ACTION_USER_ROOM_SUBSCRIPTION_FOUND:
                    Log.d(LOG_TAG, "Authentication success and user have rooms. Redirect to Dashboard");
                    redirectToDashboard();
                    break;

            }
        }
    }

}