package com.freezyoff.kosan.subscriber.mqtt;

import android.content.Context;

import com.freezyoff.kosan.subscriber.utils.Constants;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class MqttClient {
    private final String LOG_TAG = "MqttClient";

    protected final Context context;
    protected InputStream caCert;
    protected String serverUri;
    protected String clientId;

    protected MqttAndroidClient mqttAndroidClient;
    protected MqttMessageResolverManager mqttMessageResolverManager;

    public MqttClient(Context context, String serverUri, String clientId, InputStream caCert){
        this.context = context;
        this.serverUri = serverUri;
        this.clientId = clientId;
        this.caCert = caCert;
        this.mqttAndroidClient = new MqttAndroidClient(context, getServerUri(), getClientId());
    }

    public Context getContext(){ return this.context; }

    public InputStream getCaCert() {
        return caCert;
    }

    public String getServerUri() {
        return serverUri;
    }

    public String getClientId() {
        return clientId;
    }

    public MqttMessageResolverManager getMessageResolverManager(){
        return this.mqttMessageResolverManager;
    }

    public void disconnect(){
        try {
            //unsubscribe all topics
            for(MqttMessageResolver resolver: getMessageResolverManager().getMessageResolvers()){
                resolver.onDisconnecting(this);
            }
            getClient().disconnect();
            getClient().unregisterResources();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void connect(MqttMessageResolverManager manager, MqttActionListener authActionListener, MqttCallback callback){
        setMessageResolverManager(manager);
        SSLContext sslContext = null;

        try{
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");

            KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            caKeyStore.load(null, null);
            CertificateFactory caCF = CertificateFactory.getInstance("X.509");
            X509Certificate ca = (X509Certificate) caCF.generateCertificate(getCaCert());
            String alias = ca.getSubjectX500Principal().getName();
            caKeyStore.setCertificateEntry(alias, ca);
            sslContext = SSLContext.getInstance("TLSv1.2");

            kmf.init(caKeyStore, null);
            sslContext.init(kmf.getKeyManagers(), null, null);

            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setCleanSession(false);
            mqttConnectOptions.setUserName(manager.getTargetUser().getValue().getEmail().toLowerCase());
            mqttConnectOptions.setPassword(manager.getTargetUser().getValue().getPassword().toCharArray());
            mqttConnectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
            mqttConnectOptions.setSocketFactory(sslContext.getSocketFactory());

            getClient().setCallback(callback);
            getClient().connect(mqttConnectOptions, null, authActionListener);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic, int qos, MqttActionListener callback){
        try{
            getClient().subscribe(topic, qos, null, callback);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String[] topic, int[] qos, MqttActionListener callback){
        try {
            getClient().subscribe(topic, qos, null, callback);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribe(String[] topics, MqttActionListener callback){
        try {
            getClient().unsubscribe(topics, null, callback);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribe(String topic, MqttActionListener callback){
        try {
            getClient().unsubscribe(topic, null, callback);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String payload, int qos, boolean retained, IMqttActionListener callback){
        try {
            getClient().publish(topic, payload.getBytes(), qos,  retained, null, callback);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String payload, int qos, IMqttActionListener callback){
        publish(topic, payload, qos, false, callback);
    }

    public void publish(String topic, String payload, IMqttActionListener callback){
        publish(topic, payload, Constants.MQTT.QOS, callback);
    }


    private MqttAndroidClient getClient(){ return mqttAndroidClient; }

    private void setMessageResolverManager(MqttMessageResolverManager manager){
        this.mqttMessageResolverManager = manager;
    }

}