package com.pitchgauge.j9pr.pitchgauge;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class DeviceTag implements Parcelable {
    String address;
    String name;
    int pos = -1;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    @Override
    public String toString() {
        String p = (pos >=0)?" ("+ (pos+1)+")":"";
        return name +  p + "\n" + address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceTag deviceTag = (DeviceTag) o;
        return Objects.equals(getAddress(), deviceTag.getAddress());
    }

    public DeviceTag() {

    }

    protected DeviceTag(Parcel in) {
        name = in.readString();
        address = in.readString();
        pos = in.readInt();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAddress());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(address);
        dest.writeInt(pos);
    }

    public static final Creator<DeviceTag> CREATOR = new Creator<DeviceTag>() {
        @Override
        public DeviceTag createFromParcel(Parcel in) {
            return new DeviceTag(in);
        }

        @Override
        public DeviceTag[] newArray(int size) {
            return new DeviceTag[size];
        }
    };
}
