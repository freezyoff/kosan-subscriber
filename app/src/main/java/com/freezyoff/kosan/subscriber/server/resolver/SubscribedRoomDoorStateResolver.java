package com.freezyoff.kosan.subscriber.server.resolver;

import android.os.Bundle;

import com.freezyoff.kosan.subscriber.model.Location;
import com.freezyoff.kosan.subscriber.model.Room;
import com.freezyoff.kosan.subscriber.mqtt.MqttActionListener;
import com.freezyoff.kosan.subscriber.mqtt.MqttClient;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessage;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessageResolver;
import com.freezyoff.kosan.subscriber.server.ServerService;
import com.freezyoff.kosan.subscriber.utils.Constants;
import com.freezyoff.kosan.subscriber.utils.Crypto;

import java.util.ArrayList;
import java.util.List;

public class SubscribedRoomDoorStateResolver extends MqttMessageResolver {

    private final ServerService serverService;

    private String LOG_TAG = "DoorLockStateResolver";

    public SubscribedRoomDoorStateResolver(ServerService service) {
        this.serverService = service;
    }


    @Override
    public void onConnected(MqttClient client) {
    }

    @Override
    public void onReconnecting(MqttClient client, Throwable cause) {
    }

    @Override
    public void onDisconnecting(MqttClient client) {
        unsubscribe(client, null);
    }

    @Override
    public void onMessageArrived(MqttMessage message) {

        //Log.d(LOG_TAG, message.getPayload().toString());

        //check if valid topic
        //@topic: "kosan/user/<email-md5>/room/<roomid-md5>"
        String[] topicSegments = message.getTopic().split("/");
        if (topicSegments.length != 5) return;

        //check if valid payload
        String[] payloads = message.getPayload().split(" ");
        if (payloads.length != 2) return;
        String targetRoomId = topicSegments[4];

        //start find door lock state
        int lockSignal = Room.NOT_SET;
        int doorSignal = Room.NOT_SET;

        //find lockSignal & doorSignal
        for (String payload : payloads) {
            if (payload.startsWith("~ds")) {
                doorSignal = Integer.parseInt(payload.replace("~ds=", ""));
            } else if (payload.startsWith("~ls")) {
                lockSignal = Integer.parseInt(payload.replace("~ls=", ""));
            }
        }

        //find target room
        List<Location> locations = getServerService().getAuthenticatedUser().getSubscribedRooms();
        for (int i = 0; i < (locations == null ? 0 : locations.size()); i++) {

            List<Room> rooms = locations.get(i).getRooms();
            for (int y = 0; y < (rooms == null ? 0 : rooms.size()); y++) {

                //update signal
                String hash = Crypto.md5(rooms.get(y).getId() + "");
                if (hash.equalsIgnoreCase(targetRoomId)) {
                    rooms.get(y).setLockSignal(lockSignal);
                    rooms.get(y).setDoorSignal(doorSignal);
                }

            }

        }

        Bundle bundle = new Bundle();
        bundle.putString(ServerService.NOTIFICATION_KEY_ACTION, ServerService.NOTIFICATION_ACTION_MQTT_MESSAGE_ARRIVED);
        bundle.putString(ServerService.NOTIFICATION_KEY_SENDER_CLASS, SubscribedRoomDoorStateResolver.class.getName());
        getServerService().notifyServiceHandler(bundle);

    }

    @Override
    public void onMessageDelivered(String topic) {}

    @Override
    public void subscribe(MqttClient client, MqttActionListener callback) {
        String[] topics = getInboundTopics();
        int size = topics.length;
        int[] qos = new int[size];
        for (int i = 0; i<size; i++){
            qos[i] = Constants.MQTT.QOS;
        }
        client.subscribe(topics, qos, callback);
    }

    @Override
    public void unsubscribe(MqttClient client, MqttActionListener callback) {
        client.unsubscribe(getInboundTopics(), callback);
    }

    @Override
    public void publish(MqttClient client, MqttActionListener callback) {}

    @Override
    public boolean isMatchTopic(String topic) {
        for(String expected: getInboundTopics()){
            if (topic.equals(expected)){
                return true;
            }
        }
        return false;
    }

    private final String[] getInboundTopics() {
        //@topic:  "kosan/user/<email-md5>/room/<roomid-md5>"
        List<String> topics = new ArrayList();

        if (getServerService().getAuthenticatedUser() == null ||
                getServerService().getAuthenticatedUser().getSubscribedRooms() == null) {
            return topics.toArray(new String[topics.size()]);
        }

        List<Location> locations = getServerService().getAuthenticatedUser().getSubscribedRooms();
        for (Location location : locations) {

            //if null rooms
            if (location.getRooms() == null) continue;

            for (Room room : location.getRooms()) {

                topics.add(
                        generateTopic(
                                Constants.MQTT.TOPIC_INBOUND_ROOM_DOOR_LOCK_STATE,
                                new String[]{
                                        "<email-md5>",
                                        "<roomid-md5>"
                                },
                                new String[]{
                                        Crypto.md5(getServerService().getAuthenticatedUser().getEmail().toLowerCase()),
                                        Crypto.md5("" + room.getId())
                                }
                        )
                );

            }

        }

        return topics.toArray(new String[topics.size()]);
    }

    private ServerService getServerService() {
        return this.serverService;
    }

}