package com.freezyoff.kosan.subscriber.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.model.Room;
import com.freezyoff.kosan.subscriber.server.ServerService;

public class RoomIndicatorLockStateFragment extends RoomIndicatorFragment {
    private static final String LOG_TAG = "LockIndicatorFragment";

    public RoomIndicatorLockStateFragment(ServerService serverService, int selectedLocationPosition, int selectedRoomPosition) {
        super(serverService, selectedLocationPosition, selectedRoomPosition);
    }

    @Override
    protected void updateView(Context context, Intent intent) {
        if (intent.getAction() == ServerService.ACTION_USER_SUBSCRIBED_ROOM_DOOR_AND_LOCK_STATE_CHANGED) {
            Room room = getRoom();
            if (room == null) return;

            ImageView imgView = inflatedView.findViewById(R.id.imgIndicator);
            TextView txView = inflatedView.findViewById(R.id.txIndicator);

            if (room.getLockSignal() == Room.LOCK_OPEN) {
                imgView.setImageResource(R.drawable.indicator2_state_lock_unlocked);
//                txView.setText("Terbuka");
            } else if (room.getLockSignal() == Room.LOCK_CLOSED) {
                imgView.setImageResource(R.drawable.indicator2_state_lock_locked);
//                txView.setText("Terkunci");
            } else {
                imgView.setImageResource(R.drawable.indicator2_state_unknown);
                txView.setText("");
            }
        }
    }

}
