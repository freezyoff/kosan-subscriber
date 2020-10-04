package com.freezyoff.kosan.subscriber.server.resolver;

import android.os.Message;

import com.freezyoff.kosan.subscriber.model.Location;
import com.freezyoff.kosan.subscriber.mqtt.MqttActionListener;
import com.freezyoff.kosan.subscriber.mqtt.MqttClient;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessage;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessageResolver;
import com.freezyoff.kosan.subscriber.server.ServerManager;
import com.freezyoff.kosan.subscriber.utils.Constants;
import com.freezyoff.kosan.subscriber.utils.Crypto;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONException;
import java.util.List;

public class UserSubscribedRoomResolver extends MqttMessageResolver {
    private final String LOG_TAG = "UserSubscribedRoomResolver";

    protected final ServerManager serverManager;

    public UserSubscribedRoomResolver(ServerManager manager) {
        this.serverManager = manager;
    }

    private ServerManager getServerManager(){ return this.serverManager; }

    private final String getInboundTopic(){
        //@topic:  "kosan/user/<email-md5>/list/locations"
        return generateTopic(
                Constants.MQTT.TOPIC_INBOUND_ROOMS,
                "<email-md5>",
                Crypto.md5(getServerManager().getTargetUser().getValue().getEmail().toLowerCase())
        );
    }

    private final String getOutboundTopic(){
        //@topic: "kosan/user/<email-md5>/list/locations"
        return generateTopic(
                Constants.MQTT.TOPIC_OUTBOUND_ROOMS,
                "<email-md5>",
                Crypto.md5(getServerManager().getTargetUser().getValue().getEmail().toLowerCase())
        );
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

                Message message = serverManager.getHandler().obtainMessage();
                message.getData().putString(ServerManager.HANDLER_KEY_ACTION, ServerManager.HANDLER_ACTION_MQTT_SUBSCRIBE_SUCCESS);
                message.getData().putString(ServerManager.HANDLER_KEY_SENDER_CLASS, UserSubscribedRoomResolver.class.getName());
                message.sendToTarget();

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

            List<Location> locations = Location.fromJSON( mqttMessage.getPayload() );
            getServerManager().getTargetUser().getValue().setSubscribedRooms(locations);

            //send message to handler
            Message handlerMessage = serverManager.getHandler().obtainMessage();
            handlerMessage.getData().putString(ServerManager.HANDLER_KEY_ACTION, ServerManager.HANDLER_ACTION_MQTT_MESSAGE_ARRIVED);
            handlerMessage.getData().putString(ServerManager.HANDLER_KEY_SENDER_CLASS, UserSubscribedRoomResolver.class.getName());
            handlerMessage.sendToTarget();

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
