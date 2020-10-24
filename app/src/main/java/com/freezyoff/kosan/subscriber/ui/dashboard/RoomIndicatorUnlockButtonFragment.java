package com.freezyoff.kosan.subscriber.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.model.Location;
import com.freezyoff.kosan.subscriber.model.Room;
import com.freezyoff.kosan.subscriber.server.ServerService;

public class RoomIndicatorUnlockButtonFragment extends Fragment {

    private static final String LOG_TAG = "DoorIndicatorFragment";

    private final ServerService serverService;
    private final int selectedLocation;
    private final int selectedRoom;
    private View inflatedView;

    private RoomIndicatorUnlockButtonFragment.BroadcastReceiver broadcastReceiver;

    public RoomIndicatorUnlockButtonFragment(ServerService serverService, int selectedLocationPosition, int selectedRoomPosition) {
        super();
        this.serverService = serverService;
        this.selectedLocation = selectedLocationPosition;
        this.selectedRoom = selectedRoomPosition;

        broadcastReceiver = new RoomIndicatorUnlockButtonFragment.BroadcastReceiver();
    }

    private Room getRoom() {
        if (serverService == null || serverService.getAuthenticatedUser() == null) return null;

        Location location = serverService.getAuthenticatedUser().getSubscribedRoom(selectedLocation);
        if (location == null) return null;

        return location.getRoom(selectedRoom);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container != null) {
            container.removeAllViews();
        }

        inflatedView = inflater.inflate(R.layout.activity_dashboard_fragment_command_button, container, false);

        ImageView imageView = inflatedView.findViewById(R.id.btnLockCommand);
        imageView.setClickable(true);
        imageView.setOnClickListener(new OnClickListener());

        broadcastReceiver.register();

        return inflatedView;
    }

    @Override
    public void onDestroyView() {
        broadcastReceiver.unregister();
        super.onDestroyView();
    }

    class BroadcastReceiver extends android.content.BroadcastReceiver {

        public void register() {
            IntentFilter filters = new IntentFilter();
            filters.addAction(ServerService.ACTION_USER_SUBSCRIBED_ROOM_DOOR_AND_LOCK_STATE_CHANGED);
            getActivity().registerReceiver(this, filters);
        }

        public void unregister() {
            getActivity().unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == ServerService.ACTION_USER_SUBSCRIBED_ROOM_DOOR_AND_LOCK_STATE_CHANGED) {
                Room room = getRoom();
                if (room == null) return;

                ImageView imageView = inflatedView.findViewById(R.id.btnLockCommand);
                if (room.getDoorSignal() == Room.DOOR_OPEN) {
                    imageView.setEnabled(false);
                } else if (room.getDoorSignal() == Room.DOOR_CLOSED) {
                    imageView.setEnabled(true);
                }

            }
        }

    }

    class OnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Room room = getRoom();
            if (room == null) return;

            serverService.sendLockOpenCommand(room);

        }

    }
}
