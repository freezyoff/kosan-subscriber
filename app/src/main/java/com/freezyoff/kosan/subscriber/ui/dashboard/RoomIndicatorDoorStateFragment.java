package com.freezyoff.kosan.subscriber.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.model.Room;
import com.freezyoff.kosan.subscriber.server.ServerService;

public class RoomIndicatorDoorStateFragment extends RoomIndicatorFragment {
    private static final String LOG_TAG = "DoorIndicatorFragment";

    public RoomIndicatorDoorStateFragment(ServerService serverService, int selectedLocationPosition, int selectedRoomPosition) {
        super(serverService, selectedLocationPosition, selectedRoomPosition);
    }

    @Override
    protected void updateView(Context context, Intent intent) {
        if (intent.getAction() == ServerService.ACTION_USER_SUBSCRIBED_ROOM_DOOR_AND_LOCK_STATE_CHANGED) {
            Room room = getRoom();
            if (room == null) return;

            ImageView imgView = inflatedView.findViewById(R.id.imgIndicator);
            TextView txView = inflatedView.findViewById(R.id.txIndicator);
            if (room.getDoorSignal() == Room.DOOR_OPEN) {
                imgView.setImageResource(R.drawable.indicator2_state_door_opened);
                txView.setText("Terbuka");
            } else if (room.getDoorSignal() == Room.DOOR_CLOSED) {
                imgView.setImageResource(R.drawable.indicator2_state_door_closed);
                txView.setText("Tertutup");
            } else {
                imgView.setBackgroundResource(R.drawable.indicator2_state_unknown);
                txView.setText("");
            }
        }
    }

}