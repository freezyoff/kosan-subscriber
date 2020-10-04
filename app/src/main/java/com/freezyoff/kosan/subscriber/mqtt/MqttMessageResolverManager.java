package com.freezyoff.kosan.subscriber.mqtt;

import androidx.lifecycle.MutableLiveData;

import com.freezyoff.kosan.subscriber.model.User;

import java.util.List;

public interface MqttMessageResolverManager {

    MutableLiveData<User> getTargetUser();

    List<MqttMessageResolver> getMessageResolvers();

    MqttMessageResolver getMessagaResolver(Class<? extends MqttMessageResolver> cls);


}
