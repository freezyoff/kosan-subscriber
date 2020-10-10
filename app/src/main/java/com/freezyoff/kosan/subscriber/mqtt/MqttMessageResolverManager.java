package com.freezyoff.kosan.subscriber.mqtt;

import java.util.List;

public interface MqttMessageResolverManager {

    List<MqttMessageResolver> getMessageResolvers();

    MqttMessageResolver getMessagaResolver(Class<? extends MqttMessageResolver> cls);


}
