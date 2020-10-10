package com.freezyoff.kosan.subscriber.server.publisher;

import com.freezyoff.kosan.subscriber.server.ServerService;
import com.freezyoff.kosan.subscriber.server.resolver.UserSubscribedRoomResolver;
import com.freezyoff.kosan.subscriber.utils.Constants;

public class RoomListRequestPublisher implements Runnable {

    private final ServerService serverService;

    public RoomListRequestPublisher(ServerService service) {
        this.serverService = service;
    }

    @Override
    public void run() {
        UserSubscribedRoomResolver resolver = (UserSubscribedRoomResolver)
                serverService.getMessagaResolver(UserSubscribedRoomResolver.class);

        resolver.publish(serverService.getMqttClient(), null);

        serverService.executeServiceAction(this, Constants.MQTT.DELAY_OUTBOUND_ROOMS);
    }
}
