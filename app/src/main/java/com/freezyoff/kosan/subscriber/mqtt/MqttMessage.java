package com.freezyoff.kosan.subscriber.mqtt;

import android.content.Context;

public class MqttMessage {
    private MqttClient client;
    private String topic;
    private String payload;

    public MqttMessage(MqttClient client, String topic, org.eclipse.paho.client.mqttv3.MqttMessage message){
        this(client, topic, new String(message.getPayload()));
    }

    public MqttMessage(MqttClient client, String topic, String payload){
        this.client = client;
        this.topic = topic;
        this.payload = payload;
    }

    public Context getContext(){ return getClient().getContext(); }

    public MqttClient getClient() {
        return client;
    }

    public String getTopic() { return topic; }

    public String getPayload() {
        return payload;
    }


}
