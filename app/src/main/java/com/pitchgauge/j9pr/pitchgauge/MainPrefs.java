package com.pitchgauge.j9pr.pitchgauge;

import android.os.Parcel;
import android.os.Parcelable;

public class MainPrefs implements Parcelable {

    public MainPrefs() {
        units = unitsT.mm;
        zMode = zmodeT.IGNORE;
    }

    enum unitsT {
        mm,
        cm,
        m,
        inch
    };

    enum zmodeT {
        FULL,   // normal mode
        IGNORE  // ignore Z-axis
    };

    unitsT units;
    zmodeT zMode;

    protected MainPrefs(Parcel in) {
        units = unitsT.valueOf(in.readString());
        zMode = zmodeT.valueOf(in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(units.toString());
        dest.writeString(zMode.toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MainPrefs> CREATOR = new Creator<MainPrefs>() {
        @Override
        public MainPrefs createFromParcel(Parcel in) {
            return new MainPrefs(in);
        }

        @Override
        public MainPrefs[] newArray(int size) {
            return new MainPrefs[size];
        }
    };

    public unitsT getUnits() {
        return units;
    }
    public zmodeT getzMode() {
        return zMode;
    }

    public void setUnits(unitsT units) {
        this.units = units;
    }
    public void setzMode(zmodeT zMode) {
        this.zMode = zMode;
    }


}
