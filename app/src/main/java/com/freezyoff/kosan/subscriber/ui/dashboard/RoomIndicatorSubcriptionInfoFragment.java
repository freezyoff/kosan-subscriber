package com.freezyoff.kosan.subscriber.ui.dashboard;

import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.model.Location;
import com.freezyoff.kosan.subscriber.model.Room;
import com.freezyoff.kosan.subscriber.server.ServerService;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class RoomIndicatorSubcriptionInfoFragment extends Fragment {
    private static final String LOG_TAG = "DoorIndicatorFragment";

    private final ServerService serverService;
    private final int selectedLocation;
    private final int selectedRoom;
    private View inflatedView;

    public RoomIndicatorSubcriptionInfoFragment() {
        this(null, -1, -1);
    }

    public RoomIndicatorSubcriptionInfoFragment(ServerService serverService, int selectedLocationPosition, int selectedRoomPosition) {
        super();
        this.serverService = serverService;
        this.selectedLocation = selectedLocationPosition;
        this.selectedRoom = selectedRoomPosition;
    }

    private Room getRoom() {
        if (serverService == null || serverService.getAuthenticatedUser() == null) return null;

        Location location = serverService.getAuthenticatedUser().getSubscribedRoom(selectedLocation);
        if (location == null) return null;

        return location.getRoom(selectedRoom);
    }

    private void prepareView() {
        Room room = getRoom();
        TextView txSubcriptionDesc = inflatedView.findViewById(R.id.txSubcriptionDesc);
        TextView txSubcriptionStartDate = inflatedView.findViewById(R.id.txSubcriptionStartDate);
        TextView txSubcriptionStartHour = inflatedView.findViewById(R.id.txSubcriptionStartHour);
        TextView txSubcriptionEndDate = inflatedView.findViewById(R.id.txSubcriptionEndDate);
        TextView txSubcriptionEndHour = inflatedView.findViewById(R.id.txSubcriptionEndHour);
        ProgressBar prgSubcription = inflatedView.findViewById(R.id.prgSubcription);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getDefault());

        if (room == null) {
            txSubcriptionDesc.setText("");
            txSubcriptionStartDate.setText("");
            txSubcriptionStartHour.setText("");
            txSubcriptionEndDate.setText("");
            txSubcriptionEndHour.setText("");
            prgSubcription.setProgress(0);
        } else {
            boolean inGrace = room.isInGracePeriode();
            Room.SubcriptionInfo info = inGrace ? room.getGracePeriodeInfo() : room.getSubscriptionInfo();
            txSubcriptionDesc.setText(
                    inGrace ? "Periode Masa Tenggang:" : "Periode Sewa:"
            );
            txSubcriptionStartDate.setText(dateFormat.format(info.start()));
            txSubcriptionStartHour.setText(hourFormat.format(info.start()));
            txSubcriptionEndDate.setText(dateFormat.format(info.end()));
            txSubcriptionEndHour.setText(hourFormat.format(info.end()));

            if (inGrace) {
                prgSubcription.getProgressDrawable().setColorFilter(
                        ContextCompat.getColor(getContext(), R.color.activity_dashboard_fragment_subcription_progressbar_secondary),
                        PorterDuff.Mode.SRC_IN
                );
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                prgSubcription.setProgress(info.progress(), true);
            } else {
                prgSubcription.setProgress(info.progress());
            }
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container != null) {
            container.removeAllViews();
        }
        inflatedView = inflater.inflate(R.layout.activity_dashboard_fragment_subcription_info, container, false);
        prepareView();
        if (serverService != null) {
            serverService.executeRunnable(new RoomExpiredDateChecker());
        }
        return inflatedView;
    }

    class RoomExpiredDateChecker implements Runnable {

        @Override
        public void run() {
            if (getRoom() == null) {
                serverService.executeRunnable(this, 3000);
            }

            prepareView();
        }
    }
}
