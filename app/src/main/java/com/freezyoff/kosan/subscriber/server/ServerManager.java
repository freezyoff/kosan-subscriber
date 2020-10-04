package com.freezyoff.kosan.subscriber.server;

import android.content.Context;
import android.content.Intent;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.model.User;
import com.freezyoff.kosan.subscriber.mqtt.MqttActionListener;
import com.freezyoff.kosan.subscriber.mqtt.MqttCallback;
import com.freezyoff.kosan.subscriber.mqtt.MqttClient;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessage;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessageResolver;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessageResolverManager;
import com.freezyoff.kosan.subscriber.server.publisher.RoomListRequestPublisher;
import com.freezyoff.kosan.subscriber.server.resolver.DoorLockStateResolver;
import com.freezyoff.kosan.subscriber.server.resolver.UserSubscribedRoomResolver;
import com.freezyoff.kosan.subscriber.utils.Constants;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServerManager implements android.os.Handler.Callback, MqttMessageResolverManager {

    private final String LOG_TAG = "ServerManager";

    public static final String ACTION_AUTHENTICATION_SUCCESS = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_AUTHENTICATION_SUCCESS";
    public static final String ACTION_AUTHENTICATION_FAILED = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_AUTHENTICATION_FAILED";
    public static final String ACTION_CONNECTION_STATE_CHANGE = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_CONNECTION_STATE_CHANGE";
    public static final String ACTION_USER_SUBSCRIBED_ROOM_FOUND = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_USER_SUBSCRIBED_ROOM_FOUND";

    /**
     * Boolean extra flag for current mqtt state is connected
     */
    public static final String EXTRA_CONNECTED = "com.freezyoff.kosan.subscriber.MqttClient.EXTRA_CONNECTED";

    /**
     * Boolean extra flag for current mqtt connected via reconnection.
     */
    public static final String EXTRA_RECONNECTED = "com.freezyoff.kosan.subscriber.MqttClient.EXTRA_RECONNECTED";

    public static final String EXTRA_FAILED_CAUSE = "com.freezyoff.kosan.subscriber.MqttClient.EXTRA_FAILED_CAUSE";

    public static final String EXTRA_USER =  "com.freezyoff.kosan.subscriber.MqttClient.EXTRA_USER";

    public static final String HANDLER_KEY_ACTION = "com.freezyoff.kosan.subscriber.ServerManager.HANDLER_KEY_ACTION";
    public static final String HANDLER_KEY_SENDER_CLASS = "com.freezyoff.kosan.subscriber.ServerManager.HANDLER_KEY_SENDER_CLASS";
    public static final String HANDLER_ACTION_MQTT_SUBSCRIBE_SUCCESS = "com.freezyoff.kosan.subscriber.ServerManager.HANDLER_ACTION_MQTT_SUBSCRIBE_SUCCESS";
    public static final String HANDLER_ACTION_MQTT_MESSAGE_ARRIVED = "com.freezyoff.kosan.subscriber.ServerManager.HANDLER_ACTION_MQTT_MESSAGE_ARRIVED";

    final Context context;
    final MutableLiveData<User> targetUser;

    HandlerThread handlerThread;
    android.os.Handler handler;

    MqttClient mqttClient;

    HashMap<Class, MqttMessageResolver> messageResolver = new HashMap<>();
    HashMap<Class, Runnable> messagePublisher = new HashMap<>();

    boolean isConnected;

    public ServerManager(Context context, MutableLiveData<User> userCredentials){
        this.context = context;
        this.targetUser = userCredentials;

        _createMessageResolver();
        _createMessagePublisher();
    }

    private Context getContext(){ return this.context; }

    private void _createMessageResolver(){
        messageResolver = new HashMap();
        messageResolver.put(UserSubscribedRoomResolver.class, new UserSubscribedRoomResolver(this));
        messageResolver.put(DoorLockStateResolver.class, new DoorLockStateResolver(this));
    }

    private void _createMessagePublisher(){
        messagePublisher = new HashMap();
        messagePublisher.put(RoomListRequestPublisher.class, new RoomListRequestPublisher(this));
    }

    private void setConnected(boolean flag){ this.isConnected = flag; }

    public boolean isConnected(){ return this.isConnected; }

    public MutableLiveData<User> getTargetUser(){ return this.targetUser; }

    public MqttClient getClient(){ return this.mqttClient; }

    public android.os.Handler getHandler(){ return this.handler; }

    public void start(){
        isConnected = false;
        handlerThread = new HandlerThread(R.string.app_name + ".HandlerThread");
        handlerThread.start();

        handler = new android.os.Handler(handlerThread.getLooper(), this);

        mqttClient = new MqttClient(
                context,
                Constants.MQTT.SERVER_URI,
                targetUser.getValue().getEmail(),
                context.getResources().openRawResource(Constants.MQTT.CA_CERTIFICATE)
        );
        getClient().connect(this, new ServerAuthenticationCallback(), new ServerMessageCallback());
    }

    public void stop(){
        isConnected = false;
        getClient().disconnect();
        handler.removeCallbacksAndMessages(null);
        handler = null;
        handlerThread.quitSafely();
        handlerThread = null;
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        String sender = msg.getData().getString(HANDLER_KEY_SENDER_CLASS);
        String action = msg.getData().getString(HANDLER_KEY_ACTION);

        if (sender.equals(UserSubscribedRoomResolver.class.getName())){

            if ( action.equals(HANDLER_ACTION_MQTT_SUBSCRIBE_SUCCESS) ){
                handler.post(getMessagePublisher(RoomListRequestPublisher.class));
            }
            else if ( action.equals(HANDLER_ACTION_MQTT_MESSAGE_ARRIVED) ){
                Broadcaster.broadcastUserSubscribedRoomFound(getContext(), getTargetUser().getValue());
                DoorLockStateResolver resolver = (DoorLockStateResolver) getMessagaResolver(DoorLockStateResolver.class);
                resolver.subscribe(getClient(), null);
            }

            return true;
        }

        return false;
    }

    @Override
    public List<MqttMessageResolver> getMessageResolvers() {
        return new ArrayList(messageResolver.values());
    }

    @Override
    public MqttMessageResolver getMessagaResolver(Class<? extends MqttMessageResolver> cls) {
        return messageResolver.get(cls);
    }

    private Runnable getMessagePublisher(Class<? extends Runnable> cls){
        return messagePublisher.get(cls);
    }

    class ServerAuthenticationCallback extends MqttActionListener {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Broadcaster.broadcastAuthenticationSuccess(getContext());
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Broadcaster.broadcastAuthenticationFailed(getContext(), exception.getMessage());
        }
    }

    class ServerMessageCallback extends MqttCallback {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            setConnected(true);
            Broadcaster.broadcastConnectionState(getContext(), isConnected(), reconnect);
            for(MqttMessageResolver resolver: getMessageResolvers()){
                resolver.onConnected(getClient());
            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            setConnected(false);
            Broadcaster.broadcastConnectionState(getContext(), isConnected, false);
            for(MqttMessageResolver resolver: getMessageResolvers()){
                resolver.onReconnecting(getClient(), cause);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            for(MqttMessageResolver resolver: getMessageResolvers()){
                String[] topics = token.getTopics();

                if (topics == null) continue;

                for(String topic: topics){
                    if (resolver.isMatchTopic(topic)){
                        resolver.onMessageDelivered(topic);
                    }
                }
            }
        }

        @Override
        public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {

            for(MqttMessageResolver resolver: getMessageResolvers()){
                if (resolver.isMatchTopic(topic)){
                    resolver.onMessageArrived(
                            new MqttMessage(getClient(), topic, message)
                    );
                }
            }

        }
    }

    static class Broadcaster{
        static void broadcastConnectionState(Context context, boolean state, boolean reconnect) {
            Intent intent = new Intent();
            intent.setAction(ACTION_CONNECTION_STATE_CHANGE);
            intent.putExtra(EXTRA_CONNECTED, state);
            intent.putExtra(EXTRA_RECONNECTED, reconnect);
            context.sendBroadcast(intent);
        }

        static void broadcastAuthenticationFailed(Context context, String cause){
            Intent intent = new Intent();
            intent.setAction(ACTION_AUTHENTICATION_FAILED);
            intent.putExtra(EXTRA_FAILED_CAUSE, cause);
            context.sendBroadcast(intent);
        }

        static void broadcastAuthenticationSuccess(Context context){
            Intent intent = new Intent();
            intent.setAction(ACTION_AUTHENTICATION_SUCCESS);
            context.sendBroadcast(intent);
        }

        static void broadcastUserSubscribedRoomFound(Context context, User user){
            Intent intent = new Intent();
            intent.setAction(ServerManager.ACTION_USER_SUBSCRIBED_ROOM_FOUND);
            intent.putExtra(EXTRA_USER, user);
            context.sendBroadcast(intent);
        }
    }
}
