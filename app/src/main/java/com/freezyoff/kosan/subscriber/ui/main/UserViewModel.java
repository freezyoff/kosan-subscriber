package com.freezyoff.kosan.subscriber.ui.main;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.freezyoff.kosan.subscriber.model.User;

public class UserViewModel extends AndroidViewModel{

    public static final String PREFERENCE_USER = "com.freezyoff.kosan.subscriber.SharedPreferences.USER_SHARED_PREFERENCES";

    public static final String KEY_USER_EMAIL = "com.freezyoff.kosan.subscriber.SharedPreferences.KEY_SAVED_USER_EMAIL";
    public static final String KEY_USER_PASSWORD = "com.freezyoff.kosan.subscriber.SharedPreferences.KEY_SAVED_USER_PASSWORD";

    private MutableLiveData<User> mutableUser;

    public UserViewModel(@NonNull Application application) {
        super(application);

        if (mutableUser == null){
            mutableUser = new MutableLiveData();
            load(application);
        }
    }

    public MutableLiveData<User> getUser(){ return mutableUser; }

    /**
     * save to shared preferences
     */
    public void save(){
        final SharedPreferences preferences = getApplication().getSharedPreferences(PREFERENCE_USER, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_USER_EMAIL, mutableUser.getValue().getEmail());
        editor.putString(KEY_USER_PASSWORD, mutableUser.getValue().getPassword());
        editor.commit();
    }

    /**
     * load from shared preferences
     * @param application
     */
    private void load(Application application){
        SharedPreferences preferences = application.getSharedPreferences(PREFERENCE_USER, Context.MODE_PRIVATE);
        String email = preferences.getString(KEY_USER_EMAIL, "");
        String password = preferences.getString(KEY_USER_PASSWORD, "");
        if (email.equals("") || password.equals("")){
            mutableUser.setValue(null);
        }

        mutableUser.setValue( new User(email, password) );
    }
}
