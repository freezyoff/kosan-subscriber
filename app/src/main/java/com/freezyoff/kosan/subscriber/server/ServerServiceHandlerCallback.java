package com.freezyoff.kosan.subscriber.server;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.freezyoff.kosan.subscriber.model.Location;
import com.freezyoff.kosan.subscriber.model.Room;
import com.freezyoff.kosan.subscriber.model.User;
import com.freezyoff.kosan.subscriber.mqtt.MqttActionListener;
import com.freezyoff.kosan.subscriber.mqtt.MqttClient;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessageResolver;
import com.freezyoff.kosan.subscriber.server.publisher.LockOpenCommandPublisher;
import com.freezyoff.kosan.subscriber.server.publisher.UserSubcribedRoomPublisher;
import com.freezyoff.kosan.subscriber.server.resolver.DoorLockCommandResolver;
import com.freezyoff.kosan.subscriber.server.resolver.SubscribedRoomDoorStateResolver;
import com.freezyoff.kosan.subscriber.server.resolver.UserSubscribedRoomResolver;

import org.eclipse.paho.client.mqttv3.IMqttToken;

import java.util.HashMap;
import java.util.List;

public class ServerServiceHandlerCallback implements Handler.Callback {

    private final String LOG_TAG = "ServerService";

    private final ServerService serverService;

    private HashMap<Integer, Runnable> lockOpenCommandRunnables;

    ServerServiceHandlerCallback(ServerService service) {
        this.serverService = service;
        this.lockOpenCommandRunnables = new HashMap();
    }

    private Runnable getMessagePublisher(Class<? extends Runnable> cls) {
        return serverService.getMessagePublisher(cls);
    }

    private MqttMessageResolver getMessagaResolver(Class<? extends MqttMessageResolver> cls) {
        return serverService.getMessagaResolver(cls);
    }

    private User getAuthenticatedUser() {
        return serverService.getAuthenticatedUser();
    }

    private MqttClient getMqttClient() {
        return serverService.getMqttClient();
    }

    private Context getContext() {
        return serverService;
    }

    private ServerService getServerService() {
        return serverService;
    }

    private void _handleActionMqttSubscribeSuccess(Message msg) {
        String sender = msg.getData().getString(ServerService.NOTIFICATION_KEY_SENDER_CLASS);
        if (sender.equals(UserSubscribedRoomResolver.class.getName())) {
            serverService.executeRunnable(getMessagePublisher(UserSubcribedRoomPublisher.class));
        }
    }

    private void _handleActionMqttMessageArrived(final Message msg) {
        final String sender = msg.getData().getString(ServerService.NOTIFICATION_KEY_SENDER_CLASS);
        if (sender.equals(UserSubscribedRoomResolver.class.getName())) {
            //we should check room count per location first
            List<Location> locations = getAuthenticatedUser().getSubscribedRooms();
            int locationCount = locations == null ? 0 : locations.size();
            int roomCount = 0;
            if (locationCount > 0) {
                for (Location location : locations) {
                    roomCount += location.getRooms() == null ? 0 : location.getRooms().size();
                }
            }

            //user have subscription to rooms
            if (locationCount > 0 && roomCount > 0) {
                getServerService().broadcastUserSubscriptionFound();
                SubscribedRoomDoorStateResolver resolver = (SubscribedRoomDoorStateResolver) getMessagaResolver(SubscribedRoomDoorStateResolver.class);
                resolver.subscribe(getMqttClient(), null);
                return;
            }

            //user not subscribed to any rooms
            else {
                getServerService().broadcastUserSubscriptionNotFound();
                return;
            }

        } else if (sender.equals(SubscribedRoomDoorStateResolver.class.getName())) {
            getServerService().broadcastUserSubcribedRoomDoorLockStateChanged();
            return;
        } else if (sender.equals(DoorLockCommandResolver.class.getName())) {
            final Room room = msg.getData().getParcelable(ServerService.NOTIFICATION_KEY_TARGET_ROOM);
            if (room == null) return;

            DoorLockCommandResolver resolver = (DoorLockCommandResolver) getMessagaResolver(DoorLockCommandResolver.class);
            resolver.unsubscribe(getMqttClient(), room, new MqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    serverService.removeRunnable(lockOpenCommandRunnables.get(room.getId()));
                    lockOpenCommandRunnables.remove(room.getId());
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    serverService.notifyServiceHandler(msg);
                }
            });
        }
    }

    private void _handleActionMqttSendRoomLockOpenCommand(final Message msg) {
        final Room room = msg.getData().getParcelable(ServerService.NOTIFICATION_KEY_TARGET_ROOM);
        if (room == null) return;
        DoorLockCommandResolver resolver = (DoorLockCommandResolver) getMessagaResolver(DoorLockCommandResolver.class);
        resolver.subscribe(getMqttClient(), room, new MqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                if (!lockOpenCommandRunnables.containsKey(room.getId())) {
                    lockOpenCommandRunnables.put(room.getId(), new LockOpenCommandPublisher(serverService, room));
                    serverService.executeRunnable(lockOpenCommandRunnables.get(room.getId()));
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                serverService.notifyServiceHandler(msg);
            }
        });
    }


    @Override
    public boolean handleMessage(@NonNull Message msg) {
        String action = msg.getData().getString(ServerService.NOTIFICATION_KEY_ACTION);
        switch (action) {
            case ServerService.NOTIFICATION_ACTION_MQTT_SUBSCRIBE_SUCCESS:
                _handleActionMqttSubscribeSuccess(msg);
                return true;
            case ServerService.NOTIFICATION_ACTION_MQTT_MESSAGE_ARRIVED:
                _handleActionMqttMessageArrived(msg);
                return true;

            /**
             * @TODO: implement send user command for room door & lock
             * send lock open command to target Room
             */
            case ServerService.NOTIFICATION_ACTION_SEND_ROOM_LOCK_OPEN_COMMAND:
                _handleActionMqttSendRoomLockOpenCommand(msg);
                return true;

        }
        return false;
    }

}
