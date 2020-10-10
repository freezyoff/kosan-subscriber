package com.freezyoff.kosan.subscriber.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.model.Location;
import com.freezyoff.kosan.subscriber.server.ServerService;
import com.freezyoff.kosan.subscriber.ui.dashboard.LocationListAdapter;
import com.freezyoff.kosan.subscriber.ui.dashboard.RoomStatePagerAdapter;

import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private final static String LOG_TAG = "MainActivity";

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

    private List<BroadcastReceiver> broadcastReceiverList;
    private LocationListAdapter locationListAdapter;
    private Spinner locationSpinner;
    private ViewPager roomStateViewPager;
    private RoomStatePagerAdapter roomStateViewPagerAdapter;

    private UserSubscribedRoomDoorLockStateBroadcastReceiver userSubscribedRoomDoorLockStateBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _prepareServiceBinding();
        _prepareBroadcastReceiver();

        _prepareView();
    }

    @Override
    protected void onDestroy() {
        _destroyBroadcasReceiver();
        _destroyServiceBinding();
        super.onDestroy();
    }

    private void _prepareServiceBinding() {
        Intent intent = new Intent(this, ServerService.class);
        bindService(intent, serverServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void _destroyServiceBinding() {
        unbindService(serverServiceConnection);
    }

    private void _prepareBroadcastReceiver() {
        userSubscribedRoomDoorLockStateBroadcastReceiver = new UserSubscribedRoomDoorLockStateBroadcastReceiver();
        userSubscribedRoomDoorLockStateBroadcastReceiver.register();
    }

    private void _destroyBroadcasReceiver() {
        userSubscribedRoomDoorLockStateBroadcastReceiver.unregister();
    }

    private void _prepareView() {
        //set views
        setContentView(R.layout.activity_dashboard);

        //location spinner
        locationSpinner = findViewById(R.id.spinLocation);
        locationListAdapter = new LocationListAdapter(this, locationSpinner);
        locationSpinner.setAdapter(locationListAdapter);
        locationSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Location location = (Location) locationSpinner.getAdapter().getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //view pager
        roomStateViewPager = findViewById(R.id.vPager);
        roomStateViewPagerAdapter = new RoomStatePagerAdapter(this, roomStateViewPager, locationSpinner);
        roomStateViewPager.setAdapter(roomStateViewPagerAdapter);

    }

    public ServerService getServerService() {
        return this.serverService;
    }

    class UserSubscribedRoomDoorLockStateBroadcastReceiver extends BroadcastReceiver {

        public void register() {
            IntentFilter filters = new IntentFilter();
            filters.addAction(ServerService.ACTION_USER_SUBSCRIBED_ROOM_DOOR_AND_LOCK_STATE_CHANGED);
            registerReceiver(this, filters);
        }

        public void unregister() {
            unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == ServerService.ACTION_USER_SUBSCRIBED_ROOM_DOOR_AND_LOCK_STATE_CHANGED) {

                roomStateViewPagerAdapter.notifyRoomStateChange();

            }
        }

        public View getCurrentDoorStateView() {
            return roomStateViewPager.getChildAt(roomStateViewPager.getCurrentItem());
        }

    }
}