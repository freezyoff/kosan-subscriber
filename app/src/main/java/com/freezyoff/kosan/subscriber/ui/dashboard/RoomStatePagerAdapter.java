package com.freezyoff.kosan.subscriber.ui.dashboard;

import android.view.ViewGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.freezyoff.kosan.subscriber.model.Location;
import com.freezyoff.kosan.subscriber.model.Room;
import com.freezyoff.kosan.subscriber.server.ServerService;
import com.freezyoff.kosan.subscriber.ui.DashboardActivity;

import java.util.HashMap;

public class RoomStatePagerAdapter extends FragmentStatePagerAdapter {

    private ServerService serverService;
    private ViewPager roomStateViewPager;
    private HashMap<Integer, RoomStateFragment> viewFragments;
    private Spinner locationSpinner;
    private int lastCount = 0;

    public RoomStatePagerAdapter(DashboardActivity activity, ViewPager viewPager, Spinner spinner) {
        super(activity.getSupportFragmentManager());
        this.serverService = activity.getServerService();
        this.roomStateViewPager = viewPager;
        this.locationSpinner = spinner;
        this.viewFragments = new HashMap();
    }

    @Override
    public int getCount() {
        Location selected = (Location) locationSpinner.getSelectedItem();
        int currentCount = selected == null ? 0 : selected.getRooms().size();
        if (lastCount != currentCount) {
            lastCount = currentCount;
            notifyDataSetChanged();
        }
        return lastCount;
    }

    @Override
    public Fragment getItem(int position) {
        Location selectedLocation = (Location) locationSpinner.getSelectedItem();
        if (selectedLocation == null) {
            return null;
        }

        RoomStateFragment fragment = new RoomStateFragment(
                this.roomStateViewPager,
                selectedLocation.getRooms().get(position),
                new FragmentNavigationInfo(selectedLocation, position, lastCount)
        );

        viewFragments.put(position, fragment);

        return fragment;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.destroyItem(container, position, object);
        viewFragments.remove(position);
    }

    public void notifyRoomStateChange() {
        RoomStateFragment fragment = viewFragments.get(roomStateViewPager.getCurrentItem());
        if (fragment != null) {
            fragment.notifyRoomStateChange();
        }
    }

    public static class FragmentNavigationInfo {
        private int position;
        private int count;
        private Location location;

        private FragmentNavigationInfo(Location locationSource, int position, int count) {
            this.position = position;
            this.count = count;
            this.location = locationSource;
        }

        public boolean hasPrev() {
            return (getPageCount() > 0) && (getPosition() - 1 >= 0) && (getPageCount() - 1 >= getPosition() - 1);
        }

        public boolean hasNext() {
            return (getPageCount() > 0) && (getPageCount() - 1 >= getPosition() + 1);
        }

        public int getPosition() {
            return position;
        }

        public int getPageCount() {
            return count;
        }

        public Room getNext() {
            return hasNext() ? location.getRooms().get(getPosition() + 1) : null;
        }

        public Room getPrev() {
            return hasPrev() ? location.getRooms().get(getPosition() - 1) : null;
        }
    }

}
