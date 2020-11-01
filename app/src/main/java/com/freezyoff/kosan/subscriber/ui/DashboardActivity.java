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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.server.ServerService;
import com.freezyoff.kosan.subscriber.ui.dashboard.LocationListAdapter;
import com.freezyoff.kosan.subscriber.ui.dashboard.RoomIndicatorDoorStateFragment;
import com.freezyoff.kosan.subscriber.ui.dashboard.RoomIndicatorLockStateFragment;
import com.freezyoff.kosan.subscriber.ui.dashboard.RoomIndicatorSubcriptionInfoFragment;
import com.freezyoff.kosan.subscriber.ui.dashboard.RoomIndicatorUnlockButtonFragment;
import com.freezyoff.kosan.subscriber.ui.dashboard.RoomListAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;

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

    private LocationListAdapter locationListAdapter;
    private Spinner locationSpinner;

    boolean backPressedExitApp = false;
    private RoomListAdapter roomListAdapter;
    private Spinner roomSpinner;
    private TextView txDate;
    private TextView txHour;

    private TimeBroadcastReciever timeBroadcastReciever;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        _prepareServiceBinding();
        _prepareBroadcastReceiver();
        _prepareView();
    }

    @Override
    protected void onDestroy() {
        _destroyServiceBinding();
        _destroyBroadcastReceiver();
        super.onDestroy();
    }

    private void _prepareServiceBinding() {
        Intent intent = new Intent(this, ServerService.class);
        bindService(intent, serverServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void _destroyServiceBinding() {
        locationListAdapter.unbindService();
        roomListAdapter.unbindService();
        unbindService(serverServiceConnection);
    }

    private void _prepareBroadcastReceiver() {
        timeBroadcastReciever = new TimeBroadcastReciever();
        timeBroadcastReciever.register();
    }

    private void _destroyBroadcastReceiver() {
        timeBroadcastReciever.unregister();
    }

    private void _prepareView() {
        //set views
        setContentView(R.layout.activity_dashboard);

        //Date & Hour Service
        txDate = findViewById(R.id.txDateView);
        txHour = findViewById(R.id.txHourView);

        //location spinner
        locationSpinner = findViewById(R.id.spinLocation);
        locationListAdapter = new LocationListAdapter(this, locationSpinner);
        locationSpinner.setAdapter(locationListAdapter);
        locationSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                roomListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        roomSpinner = findViewById(R.id.spinRoom);
        roomListAdapter = new RoomListAdapter(this, locationSpinner, roomSpinner);
        roomSpinner.setAdapter(roomListAdapter);
        roomSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                _prepareFragments();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                _prepareFragments();
            }
        });

    }

    private void _prepareFragments() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        _createLockIndicatorFragment(transaction, locationSpinner.getSelectedItemPosition(), roomSpinner.getSelectedItemPosition());
        _createSubcriptionIndicatorFragment(transaction, locationSpinner.getSelectedItemPosition(), roomSpinner.getSelectedItemPosition());
        _createDoorIndicatorFragment(transaction, locationSpinner.getSelectedItemPosition(), roomSpinner.getSelectedItemPosition());
        _createCommandButtonFragment(transaction, locationSpinner.getSelectedItemPosition(), roomSpinner.getSelectedItemPosition());
        transaction.commit();
    }

    private void _createLockIndicatorFragment(FragmentTransaction transaction, int selectedLocationIndex, int selectedRoomIndex) {
        RoomIndicatorLockStateFragment fragment = new RoomIndicatorLockStateFragment(serverService, selectedLocationIndex, selectedRoomIndex);
        transaction.replace(R.id.fragLockIndicator, fragment, "com.freezyoff.kosan.subcriber.dashboard.fragments");
    }

    private void _createSubcriptionIndicatorFragment(FragmentTransaction transaction, int selectedLocationIndex, int selectedRoomIndex) {
        RoomIndicatorSubcriptionInfoFragment fragment = new RoomIndicatorSubcriptionInfoFragment(serverService, selectedLocationIndex, selectedRoomIndex);
        transaction.replace(R.id.fragLease, fragment, "com.freezyoff.kosan.subcriber.dashboard.fragments");
    }

    private void _createDoorIndicatorFragment(FragmentTransaction transaction, int selectedLocationIndex, int selectedRoomIndex) {
        RoomIndicatorDoorStateFragment fragment = new RoomIndicatorDoorStateFragment(serverService, selectedLocationIndex, selectedRoomIndex);
        transaction.replace(R.id.fragDoorIndicator, fragment, "com.freezyoff.kosan.subcriber.dashboard.fragments");
    }

    private void _createCommandButtonFragment(FragmentTransaction transaction, int selectedLocationIndex, int selectedRoomIndex) {
        RoomIndicatorUnlockButtonFragment fragment = new RoomIndicatorUnlockButtonFragment(serverService, selectedLocationIndex, selectedRoomIndex);
        transaction.replace(R.id.fragCommandButton, fragment, "com.freezyoff.kosan.subcriber.dashboard.fragments");
    }

    @Override
    public void onBackPressed() {
        if (backPressedExitApp) {
            //@TODO: fix this, unregistered broadcast receiver
            finish();
            return;
        } else {
            backPressedExitApp = true;
            serverService.executeRunnable(new Runnable() {
                @Override
                public void run() {
                    backPressedExitApp = false;
                }
            }, 1000 * 3);
        }
    }

    class TimeBroadcastReciever extends BroadcastReceiver {

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        private final SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm:ss");

        void register() {
            IntentFilter filter = new IntentFilter(ServerService.ACTION_TIME_TICKED);
            registerReceiver(this, filter);
        }

        void unregister() {
            unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ServerService.ACTION_TIME_TICKED)) {
                Date cDate = (Date) intent.getSerializableExtra(ServerService.EXTRA_TIME);
                txDate.setText(dateFormat.format(cDate));
                txHour.setText(hourFormat.format(cDate));
            }
        }
    }

}