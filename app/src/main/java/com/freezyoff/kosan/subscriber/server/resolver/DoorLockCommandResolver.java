package com.freezyoff.kosan.subscriber.server.resolver;

import android.os.Bundle;
import android.util.Log;

import com.freezyoff.kosan.subscriber.model.Location;
import com.freezyoff.kosan.subscriber.model.Room;
import com.freezyoff.kosan.subscriber.mqtt.MqttActionListener;
import com.freezyoff.kosan.subscriber.mqtt.MqttClient;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessage;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessageResolver;
import com.freezyoff.kosan.subscriber.server.ServerService;
import com.freezyoff.kosan.subscriber.utils.Constants;
import com.freezyoff.kosan.subscriber.utils.Crypto;

import org.eclipse.paho.client.mqttv3.IMqttToken;

import java.util.HashMap;
import java.util.List;

public class DoorLockCommandResolver extends MqttMessageResolver {
    private final ServerService serverService;
    private final HashMap<Integer, String> subcribeTopics;

    public DoorLockCommandResolver(ServerService serverService) {
        this.serverService = serverService;
        this.subcribeTopics = new HashMap<>();
    }

    private ServerService getServerService() {
        return serverService;
    }

    @Override
    public void onConnected(MqttClient client) {
    }

    @Override
    public void onReconnecting(MqttClient client, Throwable cause) {
        for (String topic : subcribeTopics.values()) {
            client.subscribe(topic, Constants.MQTT.QOS, null);
        }
    }

    @Override
    public void onDisconnecting(MqttClient client) {
    }

    @Override
    public void onMessageArrived(MqttMessage message) {
        Log.d("DoorLockCommandRes", message.getTopic());

        //check if valid topic
        //@topic: Constants.MQTT.TOPIC_INBOUND_LOCK_OPEN_COMMAND_EXECUTED
        //@topic: "kosan/user/<email-md5>/room/<roomid-md5>/command/executed"
        String[] topicSegments = message.getTopic().split("/");
        if (topicSegments.length != 7) return;

        String targetRoomId = topicSegments[4];

        //find target room
        List<Location> locations = getServerService().getAuthenticatedUser().getSubscribedRooms();
        for (int i = 0; i < (locations == null ? 0 : locations.size()); i++) {

            List<Room> rooms = locations.get(i).getRooms();
            for (int y = 0; y < (rooms == null ? 0 : rooms.size()); y++) {

                //update signal
                String hash = Crypto.md5(rooms.get(y).getId() + "");
                if (hash.equalsIgnoreCase(targetRoomId)) {

                    Bundle bundle = new Bundle();
                    bundle.putString(ServerService.NOTIFICATION_KEY_ACTION, ServerService.NOTIFICATION_ACTION_MQTT_MESSAGE_ARRIVED);
                    bundle.putString(ServerService.NOTIFICATION_KEY_SENDER_CLASS, this.getClass().getName());
                    bundle.putParcelable(ServerService.NOTIFICATION_KEY_TARGET_ROOM, rooms.get(y));
                    getServerService().notifyServiceHandler(bundle);

                }

            }

        }

    }

    @Override
    public void onMessageDelivered(String topic) {
    }

    /**
     * @param client
     * @param callback
     * @deprecated use {@link #unsubscribe(MqttClient, MqttActionListener)} instead
     */
    @Override
    public void subscribe(MqttClient client, MqttActionListener callback) {
    }

    public void subscribe(MqttClient client, final Room room, final MqttActionListener callback) {
        final String topic = generateTopic(
                Constants.MQTT.TOPIC_INBOUND_LOCK_OPEN_COMMAND_EXECUTED,
                new String[]{
                        "<email-md5>",
                        "<roomid-md5>"
                },
                new String[]{
                        Crypto.md5(getServerService().getAuthenticatedUser().getEmail().toLowerCase()),
                        Crypto.md5("" + room.getId())
                }
        );
        client.subscribe(topic, Constants.MQTT.QOS, new MqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                subcribeTopics.put(room.getId(), topic);
                if (callback != null) {
                    callback.onSuccess(asyncActionToken);
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                if (callback != null) {
                    callback.onFailure(asyncActionToken, exception);
                }
            }
        });
    }

    /**
     * @param client
     * @param callback
     * @deprecated use {@link #subscribe(MqttClient, MqttActionListener)} instead
     */
    @Override
    public void unsubscribe(MqttClient client, MqttActionListener callback) {
    }

    public void unsubscribe(MqttClient client, final Room room, final MqttActionListener callback) {
        client.unsubscribe(subcribeTopics.get(room.getId()), new MqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                subcribeTopics.remove(room.getId());
                if (callback != null) {
                    callback.onSuccess(asyncActionToken);
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                if (callback != null) {
                    callback.onFailure(asyncActionToken, exception);
                }
            }
        });
    }

    /**
     * @param client
     * @param callback
     * @deprecated
     */
    @Override
    public void publish(MqttClient client, MqttActionListener callback) {
    }

    public void publishLockOpenCommand(MqttClient client, Room room, MqttActionListener callback) {
        if (subcribeTopics.containsKey(room.getId())) {
            String topic = generateTopic(
                    Constants.MQTT.TOPIC_OUTBOUND_LOCK_OPEN_COMMAND,
                    new String[]{
                            "<email-md5>",
                            "<roomid-md5>"
                    },
                    new String[]{
                            Crypto.md5(getServerService().getAuthenticatedUser().getEmail().toLowerCase()),
                            Crypto.md5("" + room.getId())
                    }
            );
            Log.d("DoorLockCommand", topic);
            client.publish(topic, Room.LOCK_OPEN + "", Constants.MQTT.QOS, callback);
        }
    }

    @Override
    public boolean isMatchTopic(String topic) {
        for (String expected : subcribeTopics.values()) {
            if (topic.equals(expected)) {
                return true;
            }
        }
        return false;
    }

}
