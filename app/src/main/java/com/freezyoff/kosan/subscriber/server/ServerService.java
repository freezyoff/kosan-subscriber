package com.freezyoff.kosan.subscriber.server;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.model.Location;
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
import com.freezyoff.kosan.subscriber.utils.SavedUserCredentials;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServerService extends Service implements android.os.Handler.Callback, MqttMessageResolverManager {
    public static final String ACTION_AUTHENTICATION_SUCCESS = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_AUTHENTICATION_SUCCESS";
    public static final String ACTION_AUTHENTICATION_FAILED = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_AUTHENTICATION_FAILED";
    public static final String ACTION_CONNECTION_TIMEOUT = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_CONNECTION_TIMEOUT";
    public static final String ACTION_CONNECTION_STATE_CHANGED = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_CONNECTION_STATE_CHANGED";
    public static final String ACTION_USER_ROOM_SUBSCRIPTION_FOUND = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_USER_ROOM_SUBSCRIPTION_FOUND";
    public static final String ACTION_USER_ROOM_SUBSCRIPTION_NOT_FOUND = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_USER_ROOM_SUBSCRIPTION_NOT_FOUND";
    public static final String ACTION_USER_SUBSCRIBED_ROOM_DOOR_AND_LOCK_STATE_CHANGED = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_USER_SUBSCRIBED_ROOM_DOOR_AND_LOCK_STATE_CHANGED";
    /**
     * Boolean extra flag for current mqtt state is connected
     */
    public static final String EXTRA_CONNECTED = "com.freezyoff.kosan.subscriber.MqttClient.EXTRA_CONNECTED";
    /**
     * Boolean extra flag for current mqtt connected via reconnection.
     */
    public static final String EXTRA_RECONNECTED = "com.freezyoff.kosan.subscriber.MqttClient.EXTRA_RECONNECTED";
    public static final String EXTRA_FAILED_CAUSE = "com.freezyoff.kosan.subscriber.MqttClient.EXTRA_FAILED_CAUSE";
    public static final String HANDLER_KEY_ACTION = "com.freezyoff.kosan.subscriber.ServerService.HANDLER_KEY_ACTION";
    public static final String HANDLER_KEY_SENDER_CLASS = "com.freezyoff.kosan.subscriber.ServerService.HANDLER_KEY_SENDER_CLASS";
    public static final String HANDLER_ACTION_MQTT_SUBSCRIBE_SUCCESS = "com.freezyoff.kosan.subscriber.ServerService.HANDLER_ACTION_MQTT_SUBSCRIBE_SUCCESS";
    public static final String HANDLER_ACTION_MQTT_MESSAGE_ARRIVED = "com.freezyoff.kosan.subscriber.ServerService.HANDLER_ACTION_MQTT_MESSAGE_ARRIVED";
    private static final String LOG_TAG = "ServerService";
    public static long DEFAULT_CONNECTION_TIMEOUT = 10000;
    private final IBinder binder = new ServerService.Binder();
    private ConnectCredentials connectCredentials;
    private User authenticatedUser;
    private HandlerThread handlerThread;
    private android.os.Handler handler;
    private MqttClient mqttClient;
    private HashMap<Class, MqttMessageResolver> messageResolver = new HashMap<>();
    private HashMap<Class, Runnable> messagePublisher = new HashMap<>();
    private boolean isConnected;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //IMPORTANT:
        //this service only support single Mqtt Connection

        //check the credentials
        ConnectCredentials newCredentials = intent.getParcelableExtra(ConnectCredentials.class.getName());

        //if not connected yet
        if (!isConnected()) {

            //intent not provide ConnectCredentials
            if (newCredentials == null) {

                //check current ConnectCredentials, if null throw exception
                if (getConnectCredentials() == null) {
                    throw new RuntimeException("Should include extra ConnectCredentials");
                }

                //current ConnectCredentials not null, connect with it
                else {
                    connect(getConnectCredentials());
                }
            }

            //intent provide ConnectCredentials
            else {
                connect(newCredentials);
            }
        }

        //assume connected
        else if (isConnected()) {

            //if intent not provide ConnectCredentials, we do nothing in connected state
            //intent provide ConnectCredentials
            if (newCredentials != null) {

                //compare current credentials with intent provided.
                //if email & password does have same value, do nothing
                //if email & password does not have same value, disconnect and connect with new credentials
                if (getConnectCredentials().getEmail() != newCredentials.getEmail() &&
                        getConnectCredentials().getPassword() != newCredentials.getPassword()) {

                    _destroyMqttClient();
                    connect(newCredentials);

                }

            }

        }

        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setConnected(false);

        _createHandler();
        _createMessageResolver();
        _createMessagePublisher();
    }

    @Override
    public void onDestroy() {
        setConnected(false);
        _destroyMqttClient();
        _destroyHandler();
        super.onDestroy();
    }

    private Context getContext() {
        return this;
    }

    private void _createHandler() {
//            if (handlerThread == null){
        handlerThread = new HandlerThread(R.string.app_name + ".HandlerThread");
        handlerThread.start();
//            }

//            if (handler == null){
        handler = new android.os.Handler(handlerThread.getLooper(), this);
//            }
    }

    private void _destroyHandler() {
        handler.removeCallbacksAndMessages(null);
        handlerThread.quitSafely();
    }

    private void _createMessageResolver() {
        messageResolver = new HashMap();
        messageResolver.put(UserSubscribedRoomResolver.class, new UserSubscribedRoomResolver(this));
        messageResolver.put(DoorLockStateResolver.class, new DoorLockStateResolver(this));
    }

    private void _createMessagePublisher() {
        messagePublisher = new HashMap();
        messagePublisher.put(RoomListRequestPublisher.class, new RoomListRequestPublisher(this));
    }

    private void _destroyMqttClient() {
        if (mqttClient != null) {
            mqttClient.disconnect();
        }
        mqttClient = null;
    }

    private void _createMqttClient() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, MqttException, IOException {
        mqttClient = new MqttClient(
                this,
                Constants.MQTT.SERVER_URI,
                connectCredentials.getEmail(),
                this.getResources().openRawResource(Constants.MQTT.CA_CERTIFICATE)
        );

        mqttClient.connect(
                connectCredentials.getEmail(),
                connectCredentials.getPassword(),
                this,
                new ServerAuthenticationCallback(),
                new ServerMessageCallback()
        );
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    private void setConnected(boolean flag) {
        this.isConnected = flag;
    }

    public ConnectCredentials getConnectCredentials() {
        return connectCredentials;
    }

    private void setConnectCredentials(ConnectCredentials credentials) {
        this.connectCredentials = credentials;
    }

    public User getAuthenticatedUser() {
        return this.authenticatedUser;
    }

    private void setAuthenticatedUser(User user) {
        this.authenticatedUser = user;
    }

    public MqttClient getMqttClient() {
        return this.mqttClient;
    }

    private void connect(ConnectCredentials connectCredentials) {
        connect(connectCredentials, DEFAULT_CONNECTION_TIMEOUT);
    }

    private void connect(ConnectCredentials connectCredentials, long connectionTimeot) {
        try {

            setConnectCredentials(connectCredentials);

            _createMqttClient();

            //dispatch connection timer
            getHandler().postDelayed(new ServerAuthenticationTimer(), connectionTimeot);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<MqttMessageResolver> getMessageResolvers() {
        return new ArrayList(messageResolver.values());
    }

    @Override
    public MqttMessageResolver getMessagaResolver(Class<? extends MqttMessageResolver> cls) {
        return messageResolver.get(cls);
    }

    private Runnable getMessagePublisher(Class<? extends Runnable> cls) {
        return messagePublisher.get(cls);
    }

    private android.os.Handler getHandler() {
        return this.handler;
    }

    public void executeServiceAction(Bundle bundle) {
        Message message = getHandler().obtainMessage();
        message.setData(bundle);
        message.sendToTarget();
    }

    public void executeServiceAction(Message message) {
        getHandler().sendMessage(message);
    }

    public void executeServiceAction(Runnable runnable) {
        getHandler().post(runnable);
    }

    public void executeServiceAction(Runnable runnable, long delayMillis) {
        getHandler().postDelayed(runnable, delayMillis);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        String sender = msg.getData().getString(HANDLER_KEY_SENDER_CLASS);
        String action = msg.getData().getString(HANDLER_KEY_ACTION);

        if (sender.equals(UserSubscribedRoomResolver.class.getName())) {

            if (action.equals(HANDLER_ACTION_MQTT_SUBSCRIBE_SUCCESS)) {
                handler.post(getMessagePublisher(RoomListRequestPublisher.class));
                return true;
            } else if (action.equals(HANDLER_ACTION_MQTT_MESSAGE_ARRIVED)) {

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
                    ServerService.Broadcaster.userSubscriptionFound(getContext());
                    DoorLockStateResolver resolver = (DoorLockStateResolver) getMessagaResolver(DoorLockStateResolver.class);
                    resolver.subscribe(getMqttClient(), null);
                    return true;
                }

                //user not subscribed to any rooms
                else {
                    ServerService.Broadcaster.userSubscriptionNotFound(getContext());
                    return true;
                }

            }

        }

        if (sender.equals(DoorLockStateResolver.class.getName())) {

            if (action.equals(HANDLER_ACTION_MQTT_MESSAGE_ARRIVED)) {
                ServerService.Broadcaster.broadcastUserSubcribedRoomDoorLockStateChanged(getContext());
                return true;
            }

        }

        return false;
    }

    static class Broadcaster {

        static void broadcastConnectionTimeout(Context context) {
            Intent intent = new Intent();
            intent.setAction(ACTION_CONNECTION_TIMEOUT);
            context.sendBroadcast(intent);
        }

        static void broadcastConnectionState(Context context, boolean state, boolean reconnect) {
            Intent intent = new Intent();
            intent.setAction(ACTION_CONNECTION_STATE_CHANGED);
            intent.putExtra(EXTRA_CONNECTED, state);
            intent.putExtra(EXTRA_RECONNECTED, reconnect);
            context.sendBroadcast(intent);
        }

        /**
         * user cannot connect to server due to wrong connetion credentials (Authentication Failed)
         *
         * @param context
         */
        static void broadcastAuthenticationFailed(Context context, String cause) {
            Intent intent = new Intent();
            intent.setAction(ACTION_AUTHENTICATION_FAILED);
            intent.putExtra(EXTRA_FAILED_CAUSE, cause);
            context.sendBroadcast(intent);
        }

        /**
         * user connected to server (Authentication success)
         *
         * @param context
         */
        static void broadcastAuthenticationSuccess(Context context) {
            Intent intent = new Intent();
            intent.setAction(ACTION_AUTHENTICATION_SUCCESS);
            context.sendBroadcast(intent);
        }

        /**
         * user authentication success (connected successfully) and has room subscription
         *
         * @param context
         */
        static void userSubscriptionFound(Context context) {
            Intent intent = new Intent();
            intent.setAction(ServerService.ACTION_USER_ROOM_SUBSCRIPTION_FOUND);
            context.sendBroadcast(intent);
        }

        /**
         * user authentication success (connected successfully) but has no room subscription
         *
         * @param context
         */
        public static void userSubscriptionNotFound(Context context) {
            Intent intent = new Intent();
            intent.setAction(ServerService.ACTION_USER_ROOM_SUBSCRIPTION_NOT_FOUND);
            context.sendBroadcast(intent);
        }

        /**
         * notify user subcribed room signal change
         *
         * @param context
         */
        public static void broadcastUserSubcribedRoomDoorLockStateChanged(Context context) {
            Intent intent = new Intent();
            intent.setAction(ServerService.ACTION_USER_SUBSCRIBED_ROOM_DOOR_AND_LOCK_STATE_CHANGED);
            context.sendBroadcast(intent);
        }

    }

    public class Binder extends android.os.Binder {
        public ServerService getService() {
            return ServerService.this;
        }
    }

    class ServerAuthenticationCallback extends MqttActionListener {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            SavedUserCredentials.save(getContext(), getConnectCredentials().getEmail(), getConnectCredentials().getPassword());
            setAuthenticatedUser(new User(connectCredentials.getEmail(), connectCredentials.getPassword()));
            ServerService.Broadcaster.broadcastAuthenticationSuccess(getContext());
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            ServerService.Broadcaster.broadcastAuthenticationFailed(getContext(), exception.getMessage());
            _destroyMqttClient();
        }
    }

    class ServerMessageCallback extends MqttCallback {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            Log.d(LOG_TAG, "connected");
            setConnected(true);
            ServerService.Broadcaster.broadcastConnectionState(getContext(), isConnected(), reconnect);
            for (MqttMessageResolver resolver : getMessageResolvers()) {
                resolver.onConnected(getMqttClient());
            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            Log.d(LOG_TAG, "disconnected");
            setConnected(false);
            ServerService.Broadcaster.broadcastConnectionState(getContext(), isConnected, false);
            for (MqttMessageResolver resolver : getMessageResolvers()) {
                resolver.onReconnecting(getMqttClient(), cause);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            for (MqttMessageResolver resolver : getMessageResolvers()) {
                String[] topics = token.getTopics();

                if (topics == null) continue;

                for (String topic : topics) {
                    if (resolver.isMatchTopic(topic)) {
                        resolver.onMessageDelivered(topic);
                    }
                }
            }
        }

        @Override
        public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {

            for (MqttMessageResolver resolver : getMessageResolvers()) {
                if (resolver.isMatchTopic(topic)) {
                    resolver.onMessageArrived(
                            new MqttMessage(getMqttClient(), topic, message)
                    );
                }
            }

        }
    }

    class ServerAuthenticationTimer implements Runnable {

        @Override
        public void run() {
            if (!isConnected()) {
                ServerService.Broadcaster.broadcastConnectionTimeout(ServerService.this);
            }
        }

    }
}
