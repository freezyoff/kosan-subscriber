package com.freezyoff.kosan.subscriber.server.resolver;

import android.os.Bundle;
import android.util.Log;

import com.freezyoff.kosan.subscriber.model.Location;
import com.freezyoff.kosan.subscriber.mqtt.MqttActionListener;
import com.freezyoff.kosan.subscriber.mqtt.MqttClient;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessage;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessageResolver;
import com.freezyoff.kosan.subscriber.server.ServerService;
import com.freezyoff.kosan.subscriber.utils.Constants;
import com.freezyoff.kosan.subscriber.utils.Crypto;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONException;

import java.util.List;

public class UserSubscribedRoomResolver extends MqttMessageResolver {
    private final String LOG_TAG = "UserSub...RoomResolver";

    private final ServerService serverService;

    public UserSubscribedRoomResolver(ServerService service) {
        this.serverService = service;
    }

    private ServerService getServerService() {
        return this.serverService;
    }

    private final String getInboundTopic() {
        //avoid null pointer exception
        String email = "";
        if (getServerService().getAuthenticatedUser() != null) {
            email = getServerService().getAuthenticatedUser().getEmail().toLowerCase();
        }

        //@topic:  "kosan/user/<email-md5>/list/locations"
        return generateTopic(Constants.MQTT.TOPIC_INBOUND_ROOMS, "<email-md5>", Crypto.md5(email));
    }

    private final String getOutboundTopic() {
        //avoid null pointer exception
        String email = "";
        if (getServerService().getAuthenticatedUser() != null) {
            email = getServerService().getAuthenticatedUser().getEmail().toLowerCase();
        }

        //@topic: "kosan/user/<email-md5>/list/locations"
        return generateTopic(Constants.MQTT.TOPIC_OUTBOUND_ROOMS, "<email-md5>", Crypto.md5(email));
    }

    /**
     * immediately subscribe to message
     * @param client
     */
    @Override
    public void onConnected(final MqttClient client) {

        subscribe(client, new MqttActionListener(){
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                super.onSuccess(asyncActionToken);

                Bundle bundle = new Bundle();
                bundle.putString(ServerService.NOTIFICATION_KEY_ACTION, ServerService.NOTIFICATION_ACTION_MQTT_SUBSCRIBE_SUCCESS);
                bundle.putString(ServerService.NOTIFICATION_KEY_SENDER_CLASS, UserSubscribedRoomResolver.class.getName());
                getServerService().notifyServiceHandler(bundle);

            }
        });

    }

    @Override
    public void onReconnecting(MqttClient client, Throwable cause) {}

    @Override
    public void onDisconnecting(MqttClient client) {
        unsubscribe(client, null);
    }

    public void onMessageArrived(MqttMessage mqttMessage) {

        try {

            List<Location> locations = Location.fromJSON(mqttMessage.getPayload());
            Log.d(LOG_TAG, "Location count: " + locations.size());
            getServerService().getAuthenticatedUser().setSubscribedRooms(locations);

            //send message to handler
            Bundle bundle = new Bundle();
            bundle.putString(ServerService.NOTIFICATION_KEY_ACTION, ServerService.NOTIFICATION_ACTION_MQTT_MESSAGE_ARRIVED);
            bundle.putString(ServerService.NOTIFICATION_KEY_SENDER_CLASS, UserSubscribedRoomResolver.class.getName());
            getServerService().notifyServiceHandler(bundle);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onMessageDelivered(String topic) {

    }

    @Override
    public void subscribe(MqttClient client, MqttActionListener callback) {
        client.subscribe(getInboundTopic(), Constants.MQTT.QOS, callback);
    }

    @Override
    public void unsubscribe(MqttClient client, MqttActionListener callback) {
        client.unsubscribe(getInboundTopic(), callback);
    }

    @Override
    public void publish(MqttClient client, MqttActionListener callback) {
        client.publish(getOutboundTopic(), "", null);
    }

    @Override
    public boolean isMatchTopic(String topic) {
        return getInboundTopic().equals(topic);
    }

}
