package com.freezyoff.kosan.subscriber.server;

import android.os.Parcel;
import android.os.Parcelable;

public class ConnectCredentials implements Parcelable {
    public static final Creator<ConnectCredentials> CREATOR = new Creator<ConnectCredentials>() {
        @Override
        public ConnectCredentials createFromParcel(Parcel in) {
            return new ConnectCredentials(in);
        }

        @Override
        public ConnectCredentials[] newArray(int size) {
            return new ConnectCredentials[size];
        }
    };
    private String email;
    private String password;

    public ConnectCredentials(String email, String password) {
        this.email = email;
        this.password = password;
    }

    protected ConnectCredentials(Parcel in) {
        email = in.readString();
        password = in.readString();
    }

    public String getPassword() {
        return this.password;
    }

    public String getEmail() {
        return this.email;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(email);
        dest.writeString(password);
    }
}
