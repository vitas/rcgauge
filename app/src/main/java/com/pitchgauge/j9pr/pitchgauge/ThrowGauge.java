package com.pitchgauge.j9pr.pitchgauge;

import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Quaterniond;

import static java.lang.Math.PI;
import static java.lang.Math.abs;

public class ThrowGauge {

    boolean mResetArmed = false;
    boolean mUseQuats = true;
    double mChord = 0;
    double mAngle = 0;
    double mQuatAngle = 0;
    double mMaxThrow = 0;
    double mMinThrow = 0;
    double mTemperature = 0;
    double mMaxTravelAlarm = 0;
    double mMinTravelAlarm = 0;
    double mCurrentTravel = 0;

    // low pass filter
    long t0 = 0;
    long t1 = 0;
    double mQuatAngleFiltered = 0.0;
    double mAngleFiltered = 0.0;
    double tauLP = 0.07; // time constant tau in seconds

    // min/max thresholds
    long tMin = 0;
    int iMin = 0;
    double sumMin;
    long tMax = 0;
    int iMax = 0;
    double sumMax = 0;

    Vector3d mAcceleration = new Vector3d();
    Vector3d mAngularVelocity = new Vector3d();

    double mEulerRoll, mEulerPitch, mEulerYaw;
    double mRoll, mPitch, mYaw;
    Quaterniond mQBoard = new Quaterniond();
    Quaterniond mQBoardNeutral = new Quaterniond();


    boolean ignoreZ;
    public void setIgnoreZ(boolean ignoreZ) {
        this.ignoreZ = ignoreZ;
    }

    MainPrefs.throwCalcMethodT throwCalcMethod;
    public void setThrowCalcMethod(MainPrefs.throwCalcMethodT method) {
        this.throwCalcMethod = method;
    }

    public void SetNeutral()
    {
        mCurrentTravel = 0;
        mMinThrow = 0;
        mMaxThrow = 0;
        mResetArmed = false;
        /*** simple method ***/
        mAngle = 0;
        /*** quaternion method ***/
        toQuaternion(mQBoardNeutral, mEulerYaw, mEulerPitch, mEulerRoll);
        mQuatAngle = 0;
        mQuatAngleFiltered = 0;
    }

    public boolean HasResumed() {
        return !mResetArmed;
    }

    public void ResetMath() {
        mResetArmed = true;
    }

    public void SetChord(double chord) {
        mChord = chord;
    }

    public double GetChord() {
        return mChord;
    }

    public void SetAngles(float x, float y, float z)    //roll pitch yaw
    {
        mResetArmed = false;
        /**** simple method working good if board rest flat for neutral ****/
        mAngle = y;
        /*******************************************************************/

        /**** Second method use quaternions from board's euler angles *******/
        mEulerRoll = Math.toRadians(x);
        mEulerPitch = Math.toRadians(y);
        mEulerYaw = Math.toRadians(z);
        /********************************************************************/
    }

    public void SetAngle(double yAngle)
    {
        mAngle = yAngle;
    }

    public void SetAcceleration(float x, float y, float z){
        mAcceleration.set(x, y, z);
    }

    public void SetAngularVelocity(float x, float y, float z){
        mAngularVelocity.set(x, y, z);
    }

    public void SetTemperature(float t){
        mTemperature = t;
    }

    public double GetTemperature(){
        return mTemperature;
    }

    public double GetMinThrow() {
        return mMinThrow;
    }

    public double GetMaxThrow() {
        return mMaxThrow;
    }

    public double GetSetMaxThrow() {
        return mMaxTravelAlarm;
    }

    public double GetSetMinThrow() {
    	return mMinTravelAlarm;
    }

    public double GetAngle(){
        if(mUseQuats) {
            return Math.toDegrees(mQuatAngle);
        }
        return mAngle;
    }

    public double GetEulerRoll(){
        return mEulerRoll;
    }

    public double GetEulerPitch(){
        return mEulerPitch;
    }

    public double GetEulerYaw(){
        return mEulerYaw;
    }

    public double GetQuatX(){
        return mQBoard.x;
    }

    public double GetQuatY(){
        return mQBoard.y;
    }

    public double GetQuatZ(){
        return mQBoard.z;
    }

    public double GetQuatW(){
        return mQBoard.w;
    }

    public double GetNeutralQuatX(){
        return mQBoardNeutral.x;
    }

    public double GetNeutralQuatY(){
        return mQBoardNeutral.y;
    }

    public double GetNeutralQuatZ(){
        return mQBoardNeutral.z;
    }

    public double GetNeutralQuatW(){
        return mQBoardNeutral.w;
    }

    public double ResolveSimpleThrow() {

        // get delta T
        t1 = System.currentTimeMillis();
        long deltaT = t1 - t0;
        t0 = t1;

        // low pass filter mAngleFiltered
        if (t0 != 0) {
            double kLP = tauLP / (tauLP + deltaT / 1000.0);
            mAngleFiltered =  mAngleFiltered + ( 1 - kLP) * (mAngle - mAngleFiltered);
        } else {
            mAngleFiltered = 0.0;
        }

        mCurrentTravel = - mChord * Math.sin( Math.toRadians(mAngleFiltered));

        if(mCurrentTravel < mMinThrow)
            mMinThrow = mCurrentTravel;
        if(mCurrentTravel > mMaxThrow)
            mMaxThrow = mCurrentTravel;
        return mCurrentTravel;
    }

    public void SetMaxTravel(double travel) {
        mMaxTravelAlarm = travel;
    }

    public void SetMinTravel(double travel){
        travel = -1 * abs(travel);
        mMinTravelAlarm = travel;
    }

    public boolean IsAboveAngleMax(){
        if (mMaxTravelAlarm == 0) {
            return false;
        }
        if (mChord == 0) {
            return Math.toDegrees(mQuatAngleFiltered) > mMaxTravelAlarm;
        } else {
            return false;
        }
    }

    public boolean IsBelowAngleMin(){
        if (mMinTravelAlarm == 0) {
            return false;
        }
        if (mChord == 0) {
            return Math.toDegrees(mQuatAngleFiltered) < mMinTravelAlarm;
        } else {
            return false;
        }
    }

    public boolean IsAboveTravelMax(){
        if (mMaxTravelAlarm == 0) {
            return false;
        }
        if (mChord == 0) {
            return false;
        } else {
            return mCurrentTravel > mMaxTravelAlarm;
        }
    }

    public boolean IsBelowTravelMin(){
        if (mMinTravelAlarm == 0) {
            return false;
        }
        if (mChord == 0) {
            return false;
        } else {
            return mCurrentTravel < mMinTravelAlarm;
        }
    }

    public double ResolveQuatsThrow() {

        double mCurrentTmp = 0;

        if (ignoreZ) {
            toQuaternion(mQBoard, 0, mEulerPitch, mEulerRoll);
        } else {
            toQuaternion(mQBoard, mEulerYaw, mEulerPitch, mEulerRoll);
        }
        Vector3d delta = EulerAnglesBetween(mQBoard, mQBoardNeutral);
        int sign = delta.y > 0 ? -1 : 1;
        mQBoard.difference(mQBoardNeutral);
        mQuatAngle = mQBoard.angle() * sign;
        if (Double.isNaN(mQuatAngle)) {
            mQuatAngle = 0;
        }

        // get delta T
        t1 = System.currentTimeMillis();
        long deltaT = t1 - t0;
        t0 = t1;

        // low pass filter mQuatAngle
        if (t0 != 0) {
            double kLP = tauLP / (tauLP + deltaT / 1000.0);
            mQuatAngleFiltered =  mQuatAngleFiltered + ( 1 - kLP) * (mQuatAngle - mQuatAngleFiltered);
        } else {
            mQuatAngleFiltered = 0.0;
        }

        mQuatAngle = mQuatAngleFiltered;

        // calculate throw distance
        switch (throwCalcMethod) {
            case ORTHO: // orthogonal distance
                mCurrentTravel = mChord * Math.sin(mQuatAngleFiltered);
                break;
            case CHORD: // chord distance
                // limit calculation to -90 to +90 deg deflection range
                if ((mQuatAngleFiltered > PI/2.0) || (mQuatAngleFiltered < -PI/2.0)) {
                    mCurrentTravel = 0.0;
                } else {
                    mCurrentTravel = mChord * 2 * Math.sin(mQuatAngleFiltered / 2.0);
                }
                break;
        }

        //  chord=0 show angle min/max instead travel min/max being 0.0
        if (mChord == 0) {
            mCurrentTmp = Math.toDegrees(mQuatAngleFiltered);
        } else {
            mCurrentTmp = mCurrentTravel;
        }

        // min/max travel with glitch filter
        if(mCurrentTmp < mMinThrow) {
            tMin += deltaT;
            sumMin += mCurrentTmp;
            iMin ++;
            if (tMin > 800) {
                mMinThrow = sumMin / iMin;
                tMin = 0;
                iMin = 0;
                sumMin = 0;
            }
        } else {
            tMin = 0;
            iMin = 0;
            sumMin = 0;
        }
        if(mCurrentTmp > mMaxThrow) {
            tMax += deltaT;
            sumMax += mCurrentTmp;
            iMax ++;
            if (tMax > 800) {
                mMaxThrow = sumMax / iMax;
                tMax = 0;
                iMax = 0;
                sumMax = 0;
            }
        } else {
            tMax = 0;
            iMax = 0;
            sumMax = 0;
        }
        return mCurrentTravel;
    }

    public double GetThrow(){
        if(mUseQuats){
            return ResolveQuatsThrow();
        }
        else{
            return ResolveSimpleThrow();
        }
    }

    void toQuaternion(Quaterniond q, double yaw, double pitch, double roll) // yaw (Z), pitch (Y), roll (X)
    {
        // Abbreviations for the various angular functions
        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);

        q.w = cy * cp * cr + sy * sp * sr;
        q.x = cy * cp * sr - sy * sp * cr;
        q.y = sy * cp * sr + cy * sp * cr;
        q.z = sy * cp * cr - cy * sp * sr;
    }

    void toEulerAngle(Quaterniond q, double yaw, double pitch, double roll)
    {
        // roll (x-axis rotation)
        double sinr_cosp = +2.0 * (q.w() * q.x() + q.y() * q.z());
        double cosr_cosp = +1.0 - 2.0 * (q.x() * q.x() + q.y() * q.y());
        roll = Math.atan2(sinr_cosp, cosr_cosp);

        // pitch (y-axis rotation)
        double sinp = +2.0 * (q.w() * q.y() - q.z() * q.x());
        if (Math.abs(sinp) >= 1)
            pitch = java.lang.Math.copySign(Math.PI / 2, sinp); // use 90 degrees if out of range
        else
            pitch = Math.asin(sinp);

        // yaw (z-axis rotation)
        double siny_cosp = +2.0 * (q.w() * q.z() + q.x() * q.y());
        double cosy_cosp = +1.0 - 2.0 * (q.y() * q.y() + q.z() * q.z());
        yaw = Math.atan2(siny_cosp, cosy_cosp);
    }

    private Vector3d EulerAnglesBetween(Quaterniond from, Quaterniond to) {
        Vector3d fromV = new Vector3d();
        Vector3d toV = new Vector3d();
        from.getEulerAnglesXYZ(fromV);
        to.getEulerAnglesXYZ(toV);

        Vector3d delta = toV.sub(fromV);

        if (delta.x > 180)
            delta.x -= 360;
        else if (delta.x < -180)
            delta.x += 360;

        if (delta.y > 180)
            delta.y -= 360;
        else if (delta.y < -180)
            delta.y += 360;

        if (delta.z > 180)
            delta.z -= 360;
        else if (delta.z < -180)
            delta.z += 360;

        return delta;
    }
}
