package com.freezyoff.kosan.subscriber.server.publisher;

import com.freezyoff.kosan.subscriber.model.Room;
import com.freezyoff.kosan.subscriber.server.ServerService;
import com.freezyoff.kosan.subscriber.server.resolver.DoorLockCommandResolver;

public class LockOpenCommandPublisher implements Runnable {

    private static final String LOG_TAG = "RoomCommandPublisher";

    private final ServerService serverService;
    private final Room room;
    private final int DEFAULT_PUBLISH_DELAYS = 1000 * 2;

    public LockOpenCommandPublisher(ServerService service, Room room) {
        this.serverService = service;
        this.room = room;
    }

    private void repost() {
        serverService.executeRunnable(this, DEFAULT_PUBLISH_DELAYS);
    }

    @Override
    public void run() {
        DoorLockCommandResolver resolver = (DoorLockCommandResolver)
                serverService.getMessagaResolver(DoorLockCommandResolver.class);

        resolver.publishLockOpenCommand(serverService.getMqttClient(), room, null);
        repost();
    }

}
