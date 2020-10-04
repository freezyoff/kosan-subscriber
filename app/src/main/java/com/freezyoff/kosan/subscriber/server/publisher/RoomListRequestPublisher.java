package com.freezyoff.kosan.subscriber.server.publisher;

import com.freezyoff.kosan.subscriber.server.ServerManager;
import com.freezyoff.kosan.subscriber.server.resolver.UserSubscribedRoomResolver;
import com.freezyoff.kosan.subscriber.utils.Constants;

public class RoomListRequestPublisher implements Runnable {

    final ServerManager serverManager;

    public RoomListRequestPublisher(ServerManager manager){
        this.serverManager = manager;
    }

    @Override
    public void run() {
        UserSubscribedRoomResolver resolver = (UserSubscribedRoomResolver)
                serverManager.getMessagaResolver(UserSubscribedRoomResolver.class);

        resolver.publish(serverManager.getClient(), null);

        serverManager.getHandler().postDelayed(this, Constants.MQTT.DELAY_OUTBOUND_ROOMS);
    }
}
