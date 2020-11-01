package com.freezyoff.kosan.subscriber.server;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import com.freezyoff.kosan.subscriber.R;
import com.freezyoff.kosan.subscriber.model.Room;
import com.freezyoff.kosan.subscriber.model.User;
import com.freezyoff.kosan.subscriber.mqtt.MqttActionListener;
import com.freezyoff.kosan.subscriber.mqtt.MqttCallback;
import com.freezyoff.kosan.subscriber.mqtt.MqttClient;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessage;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessageResolver;
import com.freezyoff.kosan.subscriber.mqtt.MqttMessageResolverManager;
import com.freezyoff.kosan.subscriber.server.publisher.UserSubcribedRoomPublisher;
import com.freezyoff.kosan.subscriber.server.resolver.DoorLockCommandResolver;
import com.freezyoff.kosan.subscriber.server.resolver.SubscribedRoomDoorStateResolver;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ServerService extends Service implements MqttMessageResolverManager {
    public static final String ACTION_TIME_TICKED = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_TIME_TICKED";
    public static final String EXTRA_TIME = "com.freezyoff.kosan.subscriber.MqttClient.EXTRA_TIME";

    public static final String ACTION_AUTHENTICATION_SUCCESS = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_AUTHENTICATION_SUCCESS";
    public static final String ACTION_AUTHENTICATION_FAILED = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_AUTHENTICATION_FAILED";
    public static final String ACTION_CONNECTION_TIMEOUT = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_CONNECTION_TIMEOUT";
    public static final String ACTION_CONNECTION_STATE_CHANGED = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_CONNECTION_STATE_CHANGED";
    public static final String ACTION_USER_ROOM_SUBSCRIPTION_FOUND = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_USER_ROOM_SUBSCRIPTION_FOUND";
    public static final String ACTION_USER_ROOM_SUBSCRIPTION_NOT_FOUND = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_USER_ROOM_SUBSCRIPTION_NOT_FOUND";
    public static final String ACTION_USER_SUBSCRIBED_ROOM_DOOR_AND_LOCK_STATE_CHANGED = "com.freezyoff.kosan.subscriber.MqttClient.ACTION_USER_SUBSCRIBED_ROOM_DOOR_AND_LOCK_STATE_CHANGED";
    /**
     * Boolean extra flag for current mqtt failed cause
     */
    public static final String EXTRA_FAILED_CAUSE = "com.freezyoff.kosan.subscriber.MqttClient.EXTRA_FAILED_CAUSE";

    /**
     * Boolean extra flag for current mqtt state is connected
     */
    public static final String EXTRA_CONNECTED = "com.freezyoff.kosan.subscriber.MqttClient.EXTRA_CONNECTED";

    /**
     * Boolean extra flag for current mqtt connected via reconnection.
     */
    public static final String EXTRA_RECONNECTED = "com.freezyoff.kosan.subscriber.MqttClient.EXTRA_RECONNECTED";
    public static final String NOTIFICATION_KEY_ACTION = "com.freezyoff.kosan.subscriber.ServerService.NOTIFICATION_KEY_ACTION";
    public static final String NOTIFICATION_KEY_SENDER_CLASS = "com.freezyoff.kosan.subscriber.ServerService.NOTIFICATION_KEY_SENDER_CLASS";
    public static final String NOTIFICATION_KEY_TARGET_ROOM = "com.freezyoff.kosan.subscriber.ServerService.NOTIFICATION_KEY_TARGET_ROOM";
    public static final String NOTIFICATION_ACTION_MQTT_SUBSCRIBE_SUCCESS = "com.freezyoff.kosan.subscriber.ServerService.NOTIFICATION_ACTION_MQTT_SUBSCRIBE_SUCCESS";
    public static final String NOTIFICATION_ACTION_MQTT_MESSAGE_ARRIVED = "com.freezyoff.kosan.subscriber.ServerService.NOTIFICATION_ACTION_MQTT_MESSAGE_ARRIVED";
    public static final String NOTIFICATION_ACTION_SEND_ROOM_LOCK_OPEN_COMMAND = "com.freezyoff.kosan.subscriber.ServerService.NOTIFICATION_ACTION_SEND_ROOM_LOCK_OPEN_COMMAND";
    public final static long DEFAULT_CONNECTION_TIMEOUT = 10000;
    private static final String LOG_TAG = "ServerService";
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
        _createTimeService();
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
        handlerThread = new HandlerThread(R.string.app_name + ".HandlerThread");
        handlerThread.start();
        handler = new android.os.Handler(handlerThread.getLooper(), new ServerServiceHandlerCallback(this));
    }

    private void _destroyHandler() {
        handler.removeCallbacksAndMessages(null);
        handlerThread.quitSafely();
    }

    private void _createTimeService() {
        executeRunnable(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent();
                intent.setAction(ACTION_TIME_TICKED);
                intent.putExtra(EXTRA_TIME, new Date());
                sendBroadcast(intent);
                executeRunnable(this, 1000);
            }

        });
    }

    private void _createMessageResolver() {
        messageResolver = new HashMap();
        messageResolver.put(UserSubscribedRoomResolver.class, new UserSubscribedRoomResolver(this));
        messageResolver.put(SubscribedRoomDoorStateResolver.class, new SubscribedRoomDoorStateResolver(this));
        messageResolver.put(DoorLockCommandResolver.class, new DoorLockCommandResolver(this));
    }

    private void _createMessagePublisher() {
        messagePublisher = new HashMap();
        messagePublisher.put(UserSubcribedRoomPublisher.class, new UserSubcribedRoomPublisher(this));
    }

    private void _destroyMqttClient() {
        if (mqttClient != null) {
            mqttClient.disconnect();
        }
        mqttClient = null;
    }

    private void _createMqttClient(ConnectCredentials connectCredentials) throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, MqttException, IOException {
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

        this.connectCredentials = connectCredentials;
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

            _createMqttClient(connectCredentials);

            //dispatch connection timer
            getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isConnected()) {
                        broadcastConnectionTimeout();
                    }
                }
            }, connectionTimeot);

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

    protected Runnable getMessagePublisher(Class<? extends Runnable> cls) {
        return messagePublisher.get(cls);
    }

    private android.os.Handler getHandler() {
        return this.handler;
    }

    public void notifyServiceHandler(Bundle bundle) {
        Message message = getHandler().obtainMessage();
        message.setData(bundle);
        message.sendToTarget();
    }

    public void notifyServiceHandler(Message message) {
        getHandler().sendMessage(message);
    }

    public void executeRunnable(Runnable runnable) {
        getHandler().post(runnable);
    }

    public void executeRunnable(Runnable runnable, long delayMillis) {
        getHandler().postDelayed(runnable, delayMillis);
    }

    public void removeRunnable(Runnable runnable) {
        getHandler().removeCallbacks(runnable);
    }

    protected void broadcastConnectionTimeout() {
        Intent intent = new Intent();
        intent.setAction(ACTION_CONNECTION_TIMEOUT);
        sendBroadcast(intent);
    }

    protected void broadcastConnectionState(boolean state, boolean reconnect) {
        Intent intent = new Intent();
        intent.setAction(ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(EXTRA_CONNECTED, state);
        intent.putExtra(EXTRA_RECONNECTED, reconnect);
        sendBroadcast(intent);
    }

    /**
     * user cannot connect to server due to wrong connetion credentials (Authentication Failed)
     *
     * @param context
     */
    protected void broadcastAuthenticationFailed(Context context, String cause) {
        Intent intent = new Intent();
        intent.setAction(ACTION_AUTHENTICATION_FAILED);
        intent.putExtra(EXTRA_FAILED_CAUSE, cause);
        context.sendBroadcast(intent);
    }

    /**
     * user connected to server (Authentication success)
     */
    protected void broadcastAuthenticationSuccess() {
        Intent intent = new Intent();
        intent.setAction(ACTION_AUTHENTICATION_SUCCESS);
        sendBroadcast(intent);
    }

    /**
     * user authentication success (connected successfully) and has room subscription
     */
    protected void broadcastUserSubscriptionFound() {
        Intent intent = new Intent();
        intent.setAction(ServerService.ACTION_USER_ROOM_SUBSCRIPTION_FOUND);
        sendBroadcast(intent);
    }

    /**
     * user authentication success (connected successfully) but has no room subscription
     */
    public void broadcastUserSubscriptionNotFound() {
        Intent intent = new Intent();
        intent.setAction(ServerService.ACTION_USER_ROOM_SUBSCRIPTION_NOT_FOUND);
        sendBroadcast(intent);
    }


    /**
     * user subcribed room signal change
     */
    public void broadcastUserSubcribedRoomDoorLockStateChanged() {
        Intent intent = new Intent();
        intent.setAction(ServerService.ACTION_USER_SUBSCRIBED_ROOM_DOOR_AND_LOCK_STATE_CHANGED);
        sendBroadcast(intent);
    }

    public class Binder extends android.os.Binder {
        public ServerService getService() {
            return ServerService.this;
        }
    }

    public void sendLockOpenCommand(Room room) {
        Bundle bundle = new Bundle();
        bundle.putString(ServerService.NOTIFICATION_KEY_ACTION, ServerService.NOTIFICATION_ACTION_SEND_ROOM_LOCK_OPEN_COMMAND);
        bundle.putParcelable(ServerService.NOTIFICATION_KEY_TARGET_ROOM, room);
        notifyServiceHandler(bundle);
    }

    class ServerAuthenticationCallback extends MqttActionListener {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            SavedUserCredentials.save(getContext(), getConnectCredentials().getEmail(), getConnectCredentials().getPassword());
            setAuthenticatedUser(new User(connectCredentials.getEmail(), connectCredentials.getPassword()));
            broadcastAuthenticationSuccess();
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            broadcastAuthenticationFailed(getContext(), exception.getMessage());
            _destroyMqttClient();
        }
    }

    class ServerMessageCallback extends MqttCallback {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            Log.d(LOG_TAG, "connected");
            setConnected(true);
            broadcastConnectionState(isConnected(), reconnect);
            for (MqttMessageResolver resolver : getMessageResolvers()) {
                resolver.onConnected(getMqttClient());
            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            Log.d(LOG_TAG, "disconnected");
            setConnected(false);
            broadcastConnectionState(isConnected, false);
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

}
