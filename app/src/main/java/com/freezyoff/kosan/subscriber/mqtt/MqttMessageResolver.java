package com.freezyoff.kosan.subscriber.mqtt;

/**
 * @FIXME: change name to MqttMessageResolverManager
 */
public abstract class MqttMessageResolver {

    public abstract void onConnected(MqttClient client);
    public abstract void onReconnecting(MqttClient client, Throwable cause);
    public abstract void onDisconnecting(MqttClient client);

    public abstract void onMessageArrived(MqttMessage message);
    public abstract void onMessageDelivered(String topic);

    public abstract void subscribe(MqttClient client, MqttActionListener callback);
    public abstract void unsubscribe(MqttClient client, MqttActionListener callback);
    public abstract void publish(MqttClient client, MqttActionListener callback);

    public abstract boolean isMatchTopic(String topic);

    protected static String generateTopic(String src, String[] patterns, String[] replacements){
        for(int i=0; i<patterns.length; i++){
            src = generateTopic(src, patterns[i], replacements[i]);
        }
        return src;
    }

    protected static String generateTopic(String src, String pattern, String replacement){
        return src.replace(pattern, replacement);
    }

}
