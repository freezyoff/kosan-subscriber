package com.freezyoff.kosan.subscriber.server.resolver;

import android.util.Log;

import com.freezyoff.kosan.subscriber.model.Location;
import com.freezyoff.kosan.subscriber.model.Room;
import com.freezyoff.kosan.subscriber.mqtt.MqttActionListener;
import com.freezyoff.kosan.subscriber.mqtt.MqttClient;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessage;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessageResolver;
import com.freezyoff.kosan.subscriber.server.ServerManager;
import com.freezyoff.kosan.subscriber.utils.Constants;
import com.freezyoff.kosan.subscriber.utils.Crypto;

import java.util.ArrayList;
import java.util.List;

public class DoorLockStateResolver extends MqttMessageResolver {

    final ServerManager serverManager;
    private String LOG_TAG = "DoorLockStateResolver";

    public DoorLockStateResolver(ServerManager manager){
        this.serverManager = manager;
    }

    @Override
    public void onConnected(MqttClient client) {}

    @Override
    public void onReconnecting(MqttClient client, Throwable cause) {}

    @Override
    public void onDisconnecting(MqttClient client) {
        unsubscribe(client, null);
    }

    @Override
    public void onMessageArrived(MqttMessage message) {
        Log.d(LOG_TAG, "#onMessageArrived() -> Topic: " + message.getTopic());
        Log.d(LOG_TAG, "#onMessageArrived() -> Payload: " + message.getPayload());
    }

    @Override
    public void onMessageDelivered(String topic) {}

    @Override
    public void subscribe(MqttClient client, MqttActionListener callback) {
        String[] topics = getInboundTopics();
        int size = topics.length;
        int qos[] = new int[size];
        for (int i=0; i<size; i++){
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

    private ServerManager getServerManager(){ return this.serverManager; }

    private final String[] getInboundTopics(){
        //@topic:  "kosan/user/<email-md5>/room/<roomid-md5>"
        List<String> topics = new ArrayList();

        List<Location> locations = getServerManager().getTargetUser().getValue().getSubscribedRooms();
        if (locations == null){
            return topics.toArray(new String[topics.size()]);
        }

        for(Location location: getServerManager().getTargetUser().getValue().getSubscribedRooms()){
            for (Room room: location.getRooms()){

                topics.add(
                        generateTopic(
                                Constants.MQTT.TOPIC_INBOUND_ROOM_DOOR_LOCK_STATE,
                                new String[]{
                                        "<email-md5>",
                                        "<roomid-md5>"
                                },
                                new String[]{
                                        Crypto.md5(getServerManager().getTargetUser().getValue().getEmail().toLowerCase()),
                                        Crypto.md5(""+room.getId())
                                }
                        )
                );

            }
        }

        return topics.toArray(new String[topics.size()]);
    }

}
