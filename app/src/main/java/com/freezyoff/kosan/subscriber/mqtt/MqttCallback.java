package com.freezyoff.kosan.subscriber.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;

public abstract class MqttCallback implements MqttCallbackExtended {
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {

    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}
