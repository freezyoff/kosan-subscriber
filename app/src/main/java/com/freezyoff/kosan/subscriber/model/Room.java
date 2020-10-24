package com.freezyoff.kosan.subscriber.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class Room implements Parcelable {

    public static final int NOT_SET = -1;
    public static final int LOCK_CLOSED = 1;
    public static final int LOCK_OPEN = 0;
    public static final int DOOR_CLOSED = 0;
    public static final int DOOR_OPEN = 1;

    protected int id;
    protected String name;
    protected Date validAfter;
    protected Date validBefore;
    protected int gracePeriodeDays;

    private int lockSignal;
    private int doorSignal;

    public Room(int id, String name, Date validAfter, Date validBefore, int gracePeriodeDays) {
        this.id = id;
        this.name = name;
        this.validAfter = validAfter;
        this.validBefore = validBefore;
        this.gracePeriodeDays = gracePeriodeDays;

        this.lockSignal = NOT_SET;
        this.doorSignal = NOT_SET;
    }

    public Room(int id, String name, long validAfter, long validBefore, int gracePeriodeDays) {
        this(id, name, new Date(validAfter), new Date(validBefore), gracePeriodeDays);
    }

    protected Room(Parcel in) {
        this(in.readInt(), in.readString(), in.readLong(), in.readLong(), in.readInt());
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
        for (int i = 0; i < json.length(); i++) {
            JSONObject cJson = json.getJSONObject(i);
            long after = cJson.getLong("valid_after");
            long before = cJson.getLong("valid_before");
            theRoom.add(
                    new Room(
                            cJson.getInt("id"),
                            cJson.getString("name"),
                            new Date(after),
                            new Date(before),
                            cJson.getInt("gracePeriodeDays")
                    )
            );
        }
        return theRoom;
    }

    public int getGracePeriodeDays() {
        return this.gracePeriodeDays;
    }

    public int getLockSignal() {
        return this.lockSignal;
    }

    public void setLockSignal(int signal) {
        this.lockSignal = signal;
    }

    public int getDoorSignal() {
        return this.doorSignal;
    }

    public void setDoorSignal(int signal) {
        this.doorSignal = signal;
    }

    public boolean isInGracePeriode() {
        SubcriptionInfo info = getSubscriptionInfo();
        return info.elapsed() == info.durations();
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

    /**
     * @TODO: remove this
     */
    public String signalsToString() {
        return "Room: " + getName() + " (" + getId() + "), Lock: " + getLockSignal() + ", Door: " + getDoorSignal();
    }

    public SubcriptionInfo getSubscriptionInfo() {
        return new SubcriptionInfo();
    }

    public GracePeriodeInfo getGracePeriodeInfo() {
        return new GracePeriodeInfo();
    }

    public class SubcriptionInfo {
        public Date start() {
            return getValidAfter();
        }

        public Date end() {
            return getValidBefore();
        }

        public long durations() {
            return end().getTime() - start().getTime();
        }

        public long elapsed() {
            long max = durations();
            long elapse = new Date().getTime() - start().getTime();

            //before valid microseconds
            if (elapse < 0) {
                return 0;
            }

            //after valid microseconds
            if (elapse > max) {
                return max;
            }

            //between valid microseconds
            return elapse;
        }

        public int progress() {
            float elapse = elapsed();
            float durations = durations();
            return (int) ((elapse / durations) * 100);
        }
    }

    public class GracePeriodeInfo extends SubcriptionInfo {
        public Date start() {
            return getValidBefore();
        }

        public Date end() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
            calendar.setTime(start());
            calendar.add(Calendar.DAY_OF_MONTH, getGracePeriodeDays());
            return calendar.getTime();
        }
    }
}
