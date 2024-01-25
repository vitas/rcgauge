package com.pitchgauge.j9pr.pitchgauge;

import android.os.Parcel;
import android.os.Parcelable;

public class MainPrefs implements Parcelable {

    public MainPrefs() {
        units = unitsT.mm;
        zMode = zmodeT.IGNORE;
        throwCalcMethod = throwCalcMethodT.ORTHO;
        witModel = witModelT.AUTO;
        sensorConfigMode = sensorConfigModeT.AUTO;
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

    // throw calculation method
    enum throwCalcMethodT {
        ORTHO, // vertical (orthogonal) distance
        CHORD  // chord distance
    }

    // witmotion model
    enum witModelT {
        BWT61CL,
        BWT901CL,
        AUTO
    };

    // sensor configuration
    enum sensorConfigModeT {
        AUTO,
        MANUAL
    };

    unitsT units;
    zmodeT zMode;
    throwCalcMethodT throwCalcMethod;
    witModelT witModel;
    sensorConfigModeT sensorConfigMode;

    protected MainPrefs(Parcel in) {
        units = unitsT.valueOf(in.readString());
        zMode = zmodeT.valueOf(in.readString());
        throwCalcMethod = throwCalcMethodT.valueOf(in.readString());
        witModel = witModelT.valueOf(in.readString());
        sensorConfigMode = sensorConfigModeT.valueOf(in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(units.toString());
        dest.writeString(zMode.toString());
        dest.writeString(witModel.toString());
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
    public sensorConfigModeT getSensorConfigMode() {
        return sensorConfigMode;
    }
    public witModelT getWitModel() {
        return witModel;
    }
    public throwCalcMethodT getThrowCalcMethod() {
        return throwCalcMethod;
    }

    public void setUnits(unitsT units) {
        this.units = units;
    }
    public void setzMode(zmodeT zMode) {
        this.zMode = zMode;
    }
    public void setThrowCalcMethod(throwCalcMethodT throwCalcMethod) {
        this.throwCalcMethod = throwCalcMethod;
    }
    public void setWitModel(witModelT witModel) {
        this.witModel = witModel;
    }
    public void setSensorConfigMode(sensorConfigModeT sensorConfigMode) {
        this.sensorConfigMode = sensorConfigMode;
    }

}
