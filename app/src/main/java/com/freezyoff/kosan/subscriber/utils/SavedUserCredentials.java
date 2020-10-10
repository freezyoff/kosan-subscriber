package com.freezyoff.kosan.subscriber.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.freezyoff.kosan.subscriber.server.ConnectCredentials;

public class SavedUserCredentials {
    public static final String PREFERENCE_USER = "com.freezyoff.kosan.subscriber.SharedPreferences.USER_SHARED_PREFERENCES";
    public static final String KEY_USER_EMAIL = "com.freezyoff.kosan.subscriber.SharedPreferences.KEY_SAVED_USER_EMAIL";
    public static final String KEY_USER_PASSWORD = "com.freezyoff.kosan.subscriber.SharedPreferences.KEY_SAVED_USER_PASSWORD";

    private static ConnectCredentials load(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_USER, Context.MODE_PRIVATE);
        String email = preferences.getString(KEY_USER_EMAIL, "--none--");
        String password = preferences.getString(KEY_USER_PASSWORD, "--none--");
        if (email.equals("--none--") || password.equals("--none--")) {
            return null;
        } else {
            return new ConnectCredentials(email, password);
        }
    }

    public static void save(Context context, String email, String password) {
        final SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_USER, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PASSWORD, password);
        editor.commit();
    }

    public static void clear(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_USER, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }

    public static ConnectCredentials get(Context context) {
        return load(context);
    }
}
