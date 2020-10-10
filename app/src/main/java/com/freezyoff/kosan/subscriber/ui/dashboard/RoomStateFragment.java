package com.freezyoff.kosan.subscriber.ui.dashboard;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.model.Room;

import java.util.Random;

public class RoomStateFragment extends Fragment {

    private final String LOG_TAG = "RoomStateFragment";

    private View createdView;
    private Room room;
    private ViewPager viewPager;
    private RoomStatePagerAdapter.FragmentNavigationInfo navigationInfo;
    private Random viewGeneratorRandom = new Random();

    public RoomStateFragment(ViewPager viewPager, Room room, RoomStatePagerAdapter.FragmentNavigationInfo navigationInfo) {
        this.room = room;
        this.viewPager = viewPager;
        this.navigationInfo = navigationInfo;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        createdView = inflater.inflate(
                R.layout.activity_dashboard_fragment_subscribed_room_door_and_lock_state,
                container,
                false
        );

        //mofify navigation view
        TextView target = createdView.findViewById(R.id.vPagerNavPrev);
        if (navigationInfo.hasPrev() && navigationInfo.getPrev() != null) {
            target.setVisibility(View.VISIBLE);
            target.setText(navigationInfo.getPrev().getName());
            target.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                }
            });
        } else {
            target.setVisibility(View.INVISIBLE);
        }

        target = createdView.findViewById(R.id.vPagerNavNext);
        if (navigationInfo.hasNext() && navigationInfo.getNext() != null) {
            target.setVisibility(View.VISIBLE);
            target.setText(navigationInfo.getNext().getName());
            target.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                }
            });
        } else {
            target.setVisibility(View.INVISIBLE);
        }

        TextView txName = createdView.findViewById(R.id.vPagerCurrentNav);
        txName.setText(this.room.getName());

        ImageView btDoorLock = createdView.findViewById(R.id.btDoorLock);
        btDoorLock.setImageDrawable(
                new LayerDrawable(
                        new Drawable[]{
                                getResources().getDrawable(R.drawable.bt_door_outer_ring_locked),
                                getResources().getDrawable(R.drawable.bt_door_inner_ring_locked),
                                getResources().getDrawable(R.drawable.ic_lock_closed)
                        }
                )
        );
        btDoorLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        return createdView;
    }

    public void notifyRoomStateChange() {
        ImageView btDoorLock = createdView.findViewById(R.id.btDoorLock);
        btDoorLock.setImageDrawable(generateLayerDrawable());
    }

    private LayerDrawable generateLayerDrawable() {
        return new LayerDrawable(
                new Drawable[]{
                        viewGeneratorRandom.nextBoolean() ?
                                getResources().getDrawable(R.drawable.bt_door_outer_ring_locked) :
                                getResources().getDrawable(R.drawable.bt_door_outer_ring_open),
                        viewGeneratorRandom.nextBoolean() ?
                                getResources().getDrawable(R.drawable.bt_door_inner_ring_locked) :
                                getResources().getDrawable(R.drawable.bt_door_inner_ring_open),
                        viewGeneratorRandom.nextBoolean() ?
                                getResources().getDrawable(R.drawable.ic_lock_closed) :
                                getResources().getDrawable(R.drawable.ic_lock_opened)
                }
        );
    }

}