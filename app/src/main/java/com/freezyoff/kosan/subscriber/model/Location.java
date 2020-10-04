package com.freezyoff.kosan.subscriber.model;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.freezyoff.kosan.subscriber.utils.JSON;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class Location implements Parcelable {

    protected int id;
    protected String name;
    protected String address;
    protected String postcode;
    protected String phone;
    protected List<Room> rooms;

    public Location(int id, String name, String address, String postcode, String phone, List<Room> rooms){
        this.id = id;
        this.name = name;
        this.address = address;
        this.postcode = postcode;
        this.phone = phone;
        this.rooms = rooms;
    }

    protected Location(Parcel in) {
        this(
                in.readInt(),
                in.readString(),
                in.readString(),
                in.readString(),
                in.readString(),
                in.readArrayList(Room.class.getClassLoader())
        );
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getPhone() {
        return phone;
    }

    public List<Room> getRooms() { return rooms; }

    public static ArrayList<Location> fromJSON(JSONArray json) throws JSONException {
        ArrayList<Location> locations = new ArrayList();
        for(int i = 0; i < json.length(); i++){
            locations.add( fromJSON( json.getJSONObject(i) ) );
        }
        return locations;
    }

    public static Location fromJSON(JSONObject json) throws JSONException {
        return new Location(
                json.getInt("id"),
                json.getString("name"),
                json.getString("address"),
                json.getString("postcode"),
                json.getString("phone"),
                Room.fromJSON(json.getJSONArray("rooms"))
        );
    }

    public static List<Location> fromJSON(String json) throws JSONException {
        ArrayList<Location> locations = null;

        json = JSON.cleanQuotedString(json);

        JSONTokener tokener = new JSONTokener(json);
        Object currentToken = tokener.nextValue();
        if (currentToken instanceof JSONObject){
            locations = new ArrayList(1);
            locations.add( fromJSON( (JSONObject) currentToken ) );
        }
        else if (currentToken instanceof JSONArray){
            JSONArray jsonArray = (JSONArray) currentToken;
            locations = new ArrayList(jsonArray.length());
            for(int i=0; i<jsonArray.length(); i++){
                locations.add( fromJSON( jsonArray.getJSONObject(i) ) );
            }
        }

        return locations;
    }

    public static final Creator<Location> CREATOR = new Creator<Location>() {
        @Override
        public Location createFromParcel(Parcel in) {
            return new Location(in);
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(getId());
        dest.writeString(getName());
        dest.writeString(getAddress());
        dest.writeString(getPostcode());
        dest.writeString(getPhone());
        dest.writeTypedList(getRooms());
    }
}
