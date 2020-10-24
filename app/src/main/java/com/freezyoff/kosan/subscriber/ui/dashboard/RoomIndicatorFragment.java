package com.freezyoff.kosan.subscriber.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.model.Location;
import com.freezyoff.kosan.subscriber.model.Room;
import com.freezyoff.kosan.subscriber.server.ServerService;

public abstract class RoomIndicatorFragment extends Fragment {
    protected final ServerService serverService;
    protected final int selectedLocation;
    protected final int selectedRoom;
    protected View inflatedView;

    private RoomIndicatorFragment.BroadcastReceiver broadcastReceiver;

    public RoomIndicatorFragment(ServerService serverService, int selectedLocationPosition, int selectedRoomPosition) {
        super();
        this.serverService = serverService;
        this.selectedLocation = selectedLocationPosition;
        this.selectedRoom = selectedRoomPosition;

        broadcastReceiver = new RoomIndicatorFragment.BroadcastReceiver();
    }

    protected Room getRoom() {
        if (serverService == null || serverService.getAuthenticatedUser() == null) return null;

        Location location = serverService.getAuthenticatedUser().getSubscribedRoom(selectedLocation);
        if (location == null) return null;

        return location.getRoom(selectedRoom);
    }

    protected abstract void updateView(Context context, Intent intent);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container != null) {
            container.removeAllViews();
        }
        this.inflatedView = inflater.inflate(R.layout.activity_dashboard_fragment_indicator, container, false);
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
            updateView(context, intent);
        }

    }
}
