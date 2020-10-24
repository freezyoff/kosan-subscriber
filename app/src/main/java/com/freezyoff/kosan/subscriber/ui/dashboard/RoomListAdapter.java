package com.freezyoff.kosan.subscriber.ui.dashboard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.freezyoff.kosan.subscriber.model.Room;
import com.freezyoff.kosan.subscriber.server.ServerService;

import java.util.List;

public class RoomListAdapter extends ArrayAdapter<Room> {

    private ServerService serverService;
    private Spinner locationSpinner;
    private Spinner roomSpinner;
    private int lastCount;
    private Room lastSelectedBeforeNotifyDataChange;
    private ServiceConnection serverServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServerService.Binder binder = (ServerService.Binder) service;
            serverService = binder.getService();
            notifyDataSetChanged();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serverService = null;
        }
    };

    public RoomListAdapter(@NonNull FragmentActivity activity, Spinner locationSpinner, Spinner roomSpinner) {
        super(activity, android.R.layout.simple_spinner_dropdown_item);
        this.locationSpinner = locationSpinner;
        this.roomSpinner = roomSpinner;
        _prepareServiceBinding();
    }

    private void _prepareServiceBinding() {
        Intent intent = new Intent(getContext(), ServerService.class);
        getContext().bindService(intent, serverServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void _destroyServiceBinding() {
        getContext().unbindService(serverServiceConnection);
    }

    private ServerService getServerService() {
        return serverService;
    }

    public void unbindService() {
        _destroyServiceBinding();
    }

    private int getSelectedLocationPosition() {
        return this.locationSpinner.getSelectedItemPosition();
    }

    private List<Room> getRooms() {
        if (getSelectedLocationPosition() >= 0) {
            return getServerService().getAuthenticatedUser().getSubscribedRoom(getSelectedLocationPosition()).getRooms();
        }
        return null;
    }

    @Override
    public int getCount() {
        if (getServerService() == null) {
            return lastCount;
        }

        List<Room> rooms = getRooms();
        int currentCount = rooms == null ? 0 : rooms.size();
        if (lastCount != currentCount) {
            lastCount = currentCount;
            notifyDataSetChanged();
        }
        return lastCount;

    }

    @Nullable
    @Override
    public Room getItem(int position) {
        List<Room> rooms = getRooms();
        return rooms == null ? null : rooms.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView textView = (TextView) super.getView(position, convertView, parent);
        textView.setText(getItem(position).getName());
        return textView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView textView = (TextView) super.getView(position, convertView, parent);
        textView.setText(getItem(position).getName());
        return textView;
    }

    @Override
    public void notifyDataSetChanged() {

        lastSelectedBeforeNotifyDataChange = (Room) this.roomSpinner.getSelectedItem();

        super.notifyDataSetChanged();

        if (lastSelectedBeforeNotifyDataChange == null) return;

        List<Room> room = getRooms();
        if (room == null) return;

        for (int i = 0; i < room.size(); i++) {
            if (room.get(i).getId() == lastSelectedBeforeNotifyDataChange.getId()) {
                this.roomSpinner.setSelection(i);
            }
        }
    }

}