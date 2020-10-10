package com.freezyoff.kosan.subscriber.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class User implements Parcelable
{

    private String email;
    private String password;
    private List<Location> subscribedLocations;

    public User(String email, String password){
        this.email = email;
        this.password = password;
    }

    protected User(Parcel in) {
        this(in.readString(), in.readString());

        subscribedLocations = new ArrayList();
        in.readTypedList(subscribedLocations, Location.CREATOR);
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setSubscribedRooms(List<Location> locations){
        this.subscribedLocations = locations;
    }

    public List<Location> getSubscribedRooms(){
        return subscribedLocations;
    }

    public Location getSubscribedRoom(int index){
        return getSubscribedRooms().get(index);
    }

    public Location getSubscribedRoom(String name){
        for (Location location: getSubscribedRooms()){
            if (location.getName().equals(name)){
                return location;
            }
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(email);
        dest.writeString(getPassword());
        dest.writeTypedList(getSubscribedRooms());
    }
}
