package com.freezyoff.kosan.subscriber.ui.dashboard;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.freezyoff.kosan.subscriber.model.Location;
import com.freezyoff.kosan.subscriber.server.ServerService;
import com.freezyoff.kosan.subscriber.ui.DashboardActivity;

import java.util.List;

public class LocationListAdapter extends ArrayAdapter<Location> {
    private Spinner spinner;
    private int lastCount;

    private Location lastSelectedBeforeNotifyDataChange;

    public LocationListAdapter(@NonNull FragmentActivity activity, Spinner spinner) {
        super(activity, android.R.layout.simple_spinner_dropdown_item);

        this.spinner = spinner;
    }

    @Override
    public int getCount() {
        if (getServerService() == null) {
            return lastCount;
        }

        List<Location> locations = getServerService().getAuthenticatedUser().getSubscribedRooms();
        int currentCount = locations == null ? 0 : locations.size();
        if (lastCount != currentCount) {
            lastCount = currentCount;
            notifyDataSetChanged();
        }
        return lastCount;

    }

    @Nullable
    @Override
    public Location getItem(int position) {
        List<Location> locations = getServerService().getAuthenticatedUser().getSubscribedRooms();
        return locations == null ? null : locations.get(position);
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

        lastSelectedBeforeNotifyDataChange = (Location) spinner.getSelectedItem();

        super.notifyDataSetChanged();

        if (lastSelectedBeforeNotifyDataChange == null) return;

        List<Location> locations = getServerService().getAuthenticatedUser().getSubscribedRooms();
        if (locations == null) return;

        for (int i = 0; i < locations.size(); i++) {
            if (locations.get(i).getId() == lastSelectedBeforeNotifyDataChange.getId()) {
                spinner.setSelection(i);
            }
        }
    }

    private ServerService getServerService() {
        return ((DashboardActivity) getContext()).getServerService();
    }

}