package com.freezyoff.kosan.subscriber.server.publisher;

import com.freezyoff.kosan.subscriber.server.ServerService;
import com.freezyoff.kosan.subscriber.server.resolver.UserSubscribedRoomResolver;
import com.freezyoff.kosan.subscriber.utils.Constants;

public class UserSubcribedRoomPublisher implements Runnable {

    private final ServerService serverService;
    private final int DEFAULT_PUBLISH_DELAYS = 1000 * 5;

    public UserSubcribedRoomPublisher(ServerService service) {
        this.serverService = service;
    }

    @Override
    public void run() {
        UserSubscribedRoomResolver resolver = (UserSubscribedRoomResolver)
                serverService.getMessagaResolver(UserSubscribedRoomResolver.class);

        resolver.publish(serverService.getMqttClient(), null);

        boolean subsribedRoomResolved = serverService.getAuthenticatedUser().getSubscribedRooms() != null &&
                serverService.getAuthenticatedUser().getSubscribedRooms().size() > 0;
        serverService.executeRunnable(
                this,
                subsribedRoomResolved ?
                        Constants.MQTT.DELAY_OUTBOUND_ROOMS :
                        DEFAULT_PUBLISH_DELAYS
        );
    }
}
