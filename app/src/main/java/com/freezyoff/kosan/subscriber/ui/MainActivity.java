package com.freezyoff.kosan.subscriber.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.model.User;
import com.freezyoff.kosan.subscriber.server.ServerManager;
import com.freezyoff.kosan.subscriber.ui.main.UserViewModel;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = "MainActivity";
    protected UserViewModel userModel;
    protected ServerManager serverHandler;
    protected List<BroadcastReceiver> broadcastReceiverList;
    protected AlertDialog loginFailedDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prepareView();

        registerReceivers();

        userModel = ViewModelProviders.of(this).get(UserViewModel.class);

        serverHandler  = new ServerManager(this, userModel.getUser());
        serverHandler.start();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceivers();
        serverHandler.stop();
    }

    private void registerReceivers() {
        broadcastReceiverList = new ArrayList();
        broadcastReceiverList.add(new ServerAuthenticationBroadcastListener());
        broadcastReceiverList.add(new UserMetaBroadcastListener());

        //Broadcast Reciever for Mqtt Authentication
        IntentFilter mqttFilters = new IntentFilter();
        mqttFilters.addAction(ServerManager.ACTION_AUTHENTICATION_FAILED);
        mqttFilters.addAction(ServerManager.ACTION_AUTHENTICATION_SUCCESS);
        registerReceiver(broadcastReceiverList.get(0), mqttFilters);

        //@TODO:
        IntentFilter userFilters = new IntentFilter();
        userFilters.addAction(ServerManager.ACTION_USER_SUBSCRIBED_ROOM_FOUND);
        registerReceiver(broadcastReceiverList.get(1), userFilters);
    }

    private void unregisterReceivers(){
        for (BroadcastReceiver receiver: broadcastReceiverList){
            unregisterReceiver(receiver);
        }
    }

    private void prepareView(){
        //set views
        setContentView(R.layout.activity_main);

        //login failed dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.alert_login_failed_title);
        builder.setMessage(R.string.alert_login_failed_message);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.alert_login_failed_positive_btn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        loginFailedDialog = builder.create();

        //default view state
        toggleView(false);
    }

    private void toggleView(boolean authenticated){
        findViewById(R.id.vAuthenticate).setVisibility(authenticated? View.INVISIBLE : View.VISIBLE);
        findViewById(R.id.vDashboard).setVisibility(authenticated? View.VISIBLE : View.INVISIBLE);
    }

    class ServerAuthenticationBroadcastListener extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case ServerManager.ACTION_AUTHENTICATION_FAILED:
                    //show credentials mismatch dialog
                    loginFailedDialog.show();
                    break;

                case ServerManager.ACTION_AUTHENTICATION_SUCCESS:
                    //save user to shared preferences for future use
                    userModel.getUser().setValue(serverHandler.getTargetUser().getValue());
                    userModel.save();
                    break;

                case ServerManager.ACTION_CONNECTION_STATE_CHANGE:
                    //@TODO: connection state change should show to UI
                    break;
            }
        }
    }

    class UserMetaBroadcastListener extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerManager.ACTION_USER_SUBSCRIBED_ROOM_FOUND)){
                User user  = intent.getParcelableExtra(ServerManager.EXTRA_USER);
                Log.d(LOG_TAG, "User email: " + user.getEmail());
                Log.d(LOG_TAG, "Locations: " + user.getSubscribedRooms().size());

                //location spinner
                ArrayAdapter spinnerAdapter = new ArrayAdapter(getBaseContext(), android.R.layout.simple_spinner_item, user.getSubscribedRooms());
                Spinner spinner = findViewById(R.id.spinLocation);
                spinner.setAdapter(spinnerAdapter);

                toggleView(true);
            }
        }
    }

}