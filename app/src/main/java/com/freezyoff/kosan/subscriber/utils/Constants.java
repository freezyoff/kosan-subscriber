package com.freezyoff.kosan.subscriber.utils;

import com.freezyoff.kosan.subscriber.R;

public interface Constants {

    interface MQTT{

        String SERVER_URI = "ssl://mqtt.kosan.co.id:8883";
        int CA_CERTIFICATE = R.raw.mqtt_kosan_co_id_ca;

        /**
         * MQTT Server default subscribe & publish
         */
        int QOS = 2;

        /**
         * MQTT Server inbound (request) topic that inform (in payload) user rooms list
         * @see #TOPIC_OUTBOUND_ROOMS
         */
        String TOPIC_INBOUND_ROOMS = "kosan/user/<email-md5>/locations";

        /**
         * MQTT Server inbound (request) topic that inform (in payload) current signal door & lock state of specified user room
         */
        String TOPIC_INBOUND_ROOM_DOOR_LOCK_STATE = "kosan/user/<email-md5>/room/<roomid-md5>";

        /**
         * MQTT Sever outbound (Request) that exepect server to response user rooms list
         * @see #TOPIC_INBOUND_ROOMS
         */
        String TOPIC_OUTBOUND_ROOMS = "kosan/user/<email-md5>/list/locations";

        /**
         * MQTT Server outbound (Request) delay for user room list
         * @see #TOPIC_OUTBOUND_ROOMS
         * @see #TOPIC_INBOUND_ROOMS
         */
        int DELAY_OUTBOUND_ROOMS = 1000*5*60;
    }

    //@FIXME: test only
    public static final String[] MQTT_CREDENTIALS = {"akhmad.musa.hadi@gmail.com", "1234567890"};
    public static final String MQTT_CLIENT_ID = MQTT_CREDENTIALS[0];

}
