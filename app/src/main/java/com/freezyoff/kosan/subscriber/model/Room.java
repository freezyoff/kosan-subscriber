package com.freezyoff.kosan.subscriber.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Room implements Parcelable {
    protected int id;
    protected String name;
    protected Date validAfter;
    protected Date validBefore;

    public Room(int id, String name, Date validAfter, Date validBefore){
        this.id = id;
        this.name = name;
        this.validAfter = validAfter;
        this.validBefore = validBefore;
    }

    public Room(int id, String name, long validAfter, long validBefore){
        this(id, name, new Date(validAfter), new Date(validBefore));
    }

    protected Room(Parcel in) {
        this(in.readInt(), in.readString(), in.readLong(), in.readLong());
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Date getValidAfter() {
        return validAfter;
    }

    public Date getValidBefore() {
        return validBefore;
    }


    public static List<Room> fromJSON(JSONArray json) throws JSONException {
        ArrayList<Room> theRoom = new ArrayList();
        for(int i=0; i<json.length(); i++){
            JSONObject cJson = json.getJSONObject(i);
            long after = cJson.getLong("valid_after") * 1000;
            long before = cJson.getLong("valid_before") * 1000;
            theRoom.add(
                    new Room(
                            cJson.getInt("id"),
                            cJson.getString("name"),
                            new Date(after),
                            new Date(before)
                    )
            );
        }
        return theRoom;
    }

    public static final Creator<Room> CREATOR = new Creator<Room>() {
        @Override
        public Room createFromParcel(Parcel in) {
            return new Room(in);
        }

        @Override
        public Room[] newArray(int size) {
            return new Room[size];
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
        dest.writeLong(getValidAfter().getTime());
        dest.writeLong(getValidBefore().getTime());
    }

}
