package com.pitchgauge.j9pr.pitchgauge;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.databinding.Bindable;
import android.databinding.Observable;
import android.databinding.PropertyChangeRegistry;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import java.text.DecimalFormat;
import java.util.Locale;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;

public class ThrowGaugeViewModel extends AndroidViewModel implements Observable {
    private PropertyChangeRegistry callbacks = new PropertyChangeRegistry();
    private Handler mHandler;

    @Override
    public void addOnPropertyChangedCallback(
            Observable.OnPropertyChangedCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(
            Observable.OnPropertyChangedCallback callback) {
        callbacks.remove(callback);
    }

    /**
     * Notifies observers that all properties of this instance have changed.
     */
    void notifyChange() {
        callbacks.notifyCallbacks(this, 0, null);
    }

    /**
     * Notifies observers that a specific property has changed. The getter for the
     * property that changes should be marked with the @Bindable annotation to
     * generate a field in the BR class to be used as the fieldId parameter.
     *
     * @param fieldId The generated BR id for the Bindable field.
     */
    void notifyPropertyChanged(int fieldId) {
        callbacks.notifyCallbacks(this, fieldId, null);
    }
    public MutableLiveData<String> errorChord = new MutableLiveData<>();
    public MutableLiveData<String> errorAngle = new MutableLiveData<>();

    public ThrowGaugeViewModel(@NonNull Application application) {
        super(application);
        getThrowGauge().setValue(new ThrowGauge());
        getThrowGauge2().setValue(new ThrowGauge());
    }

    public void SetSendSensorHandler(Handler handler){
        mHandler = handler;
    }

    private MutableLiveData<ThrowGauge> mThrowGauge;
    private MutableLiveData<ThrowGauge> mThrowGauge2;

    public MutableLiveData<ThrowGauge> getThrowGauge() {
        if (mThrowGauge == null) {
            mThrowGauge = new MutableLiveData<ThrowGauge>();
        }
        return mThrowGauge;
    }

    public MutableLiveData<ThrowGauge> getThrowGauge2() {
        if (mThrowGauge2 == null) {
            mThrowGauge2 = new MutableLiveData<ThrowGauge>();
        }
        return mThrowGauge2;
    }

    public void setChord(String chord){
        if(!isNumeric(chord))
            return;
        if(chord == "")
            return;
        getThrowGauge().getValue().SetChord(Double.parseDouble(chord));
        getThrowGauge2().getValue().SetChord(Double.parseDouble(chord));

        notifyPropertyChanged(BR.travel);
        notifyPropertyChanged(BR.travel2);

    }

    public void setAccelerations(int pos, float x, float y, float z){
        if (pos == 0) {
            getThrowGauge().getValue().SetAcceleration(x, y, z);
        } else {
            getThrowGauge2().getValue().SetAcceleration(x, y, z);
        }
    }

    public void setVelocities(int pos, float x, float y, float z){
        if (pos == 0) {
            getThrowGauge().getValue().SetAngularVelocity(x, y, z);
        } else {
            getThrowGauge2().getValue().SetAngularVelocity(x, y, z);
        }
    }

    public void setAngles(int pos, float x, float y, float z){
        if (pos == 0) {
            getThrowGauge().getValue().SetAngles(x, y, z);
            notifyPropertyChanged(BR.angle);
            notifyPropertyChanged(BR.travel);
            notifyPropertyChanged(BR.maxTravel);
            notifyPropertyChanged(BR.minTravel);
            notifyPropertyChanged(BR.maxTravelColor);
            notifyPropertyChanged(BR.minTravelColor);
        } else {
            getThrowGauge2().getValue().SetAngles(x, y, z);
            notifyPropertyChanged(BR.angle2);
            notifyPropertyChanged(BR.travel2);
            notifyPropertyChanged(BR.maxTravel2);
            notifyPropertyChanged(BR.minTravel2);
            notifyPropertyChanged(BR.maxTravelColor2);
            notifyPropertyChanged(BR.minTravelColor2);
        }

    }

    public void setTemperature(float t){
        getThrowGauge().getValue().SetTemperature(t);
    }

    public void setAngle(String angle){
        try {
            if(!isNumeric(angle))
                return;
            if(angle == "")
                return;
            double newAngle = parseDecimal(angle);
            if(Math.abs(newAngle - getThrowGauge().getValue().GetAngle()) > 0.1f) {
                getThrowGauge().getValue().SetAngle(newAngle);
                notifyPropertyChanged(BR.angle);
                notifyPropertyChanged(BR.travel);
                notifyPropertyChanged(BR.maxTravel);
                notifyPropertyChanged(BR.minTravel);
                notifyPropertyChanged(BR.maxTravelColor);
                notifyPropertyChanged(BR.minTravelColor);
            }
        }
        catch (Exception e){
            
        }
    }

    public void setAngle2(String angle){
        try {
            if(!isNumeric(angle))
                return;
            if(angle == "")
                return;
            double newAngle = parseDecimal(angle);
            if(Math.abs(newAngle - getThrowGauge2().getValue().GetAngle()) > 0.1f) {
                getThrowGauge2().getValue().SetAngle(newAngle);
                notifyPropertyChanged(BR.angle2);
                notifyPropertyChanged(BR.travel2);
                notifyPropertyChanged(BR.maxTravel2);
                notifyPropertyChanged(BR.minTravel2);
                notifyPropertyChanged(BR.maxTravelColor2);
                notifyPropertyChanged(BR.minTravelColor2);
            }
        }
        catch (Exception e){

        }
    }

    @Bindable
    public double getTemperature(){
        return getThrowGauge().getValue().GetTemperature();
    }

    @Bindable
    public String getAngle() {
        return getApplication().getString(R.string.command_angle)+ " " + new DecimalFormat("  0.0").format(getThrowGauge().getValue().GetAngle());
    }

    @Bindable
    public String getAngle2() {
        return new DecimalFormat("  0.0").format(getThrowGauge2().getValue().GetAngle());
    }

    @Bindable
    public String getChord() {
        return Double.toString(getThrowGauge().getValue().GetChord());
    }

    @Bindable
    public String getEulerRoll(){
        return "EulerRoll: " + new DecimalFormat("###.#").format(getThrowGauge().getValue().GetEulerRoll());
    }

    @Bindable
    public String getEulerPitch(){
        return "EulerPitch: " + new DecimalFormat("###.#").format(getThrowGauge().getValue().GetEulerPitch());
    }

    @Bindable
    public String getEulerYaw(){
        return "EulerYaw: " + new DecimalFormat("###.#").format(getThrowGauge().getValue().GetEulerYaw());
    }

    @Bindable
    public String getQuatX(){
        return "QuatX: " + new DecimalFormat("#0.0#").format(getThrowGauge().getValue().GetQuatX());
    }

    @Bindable
    public String getQuatY(){
        return "QuatY: " + new DecimalFormat("#0.0#").format(getThrowGauge().getValue().GetQuatY());
    }

    @Bindable
    public String getQuatZ(){
        return "QuatZ: " + new DecimalFormat("#0.0#").format(getThrowGauge().getValue().GetQuatZ());
    }

    @Bindable
    public String getQuatW(){
        return "QuatW: " + new DecimalFormat("#0.0#").format(getThrowGauge().getValue().GetQuatW());
    }

    @Bindable
    public String getNeutralQuatX(){
        return "NeutralQuatX: " + new DecimalFormat("#0.0#").format(getThrowGauge().getValue().GetNeutralQuatX());
    }

    @Bindable
    public String getNeutralQuatY(){
        return "NeutralQuatY: " + new DecimalFormat("#0.0#").format(getThrowGauge().getValue().GetNeutralQuatY());
    }

    @Bindable
    public String getNeutralQuatZ(){
        return "NeutralQuatZ: " + new DecimalFormat("#0.0#").format(getThrowGauge().getValue().GetNeutralQuatZ());
    }

    @Bindable
    public String getNeutralQuatW(){
        return "NeutralQuatW: " + new DecimalFormat("#0.0#").format(getThrowGauge().getValue().GetNeutralQuatW());
    }

    @Bindable
    public String getTravel() {
        double res = getThrowGauge().getValue().GetThrow();
        String str = getApplication().getString(R.string.command_throw)+ " " + new DecimalFormat("  ###0.0").format(res); // rounded to 2 decimal places
        return str;
    }

    @Bindable
    public String getTravel2() {
        double res = getThrowGauge2().getValue().GetThrow();
        String str = new DecimalFormat("  ###0.0").format(res); // rounded to 2 decimal places
        return str;
    }

    @Bindable
    public String getMaxTravel() {
        double res = getThrowGauge().getValue().GetMaxThrow();
        String str = getApplication().getString(R.string.command_maxthrow)+ " " + new DecimalFormat("###.#").format(res); // rounded to 2 decimal places
        return str;
    }

    @Bindable
    public String getMaxTravel2() {
        double res = getThrowGauge2().getValue().GetMaxThrow();
        String str = new DecimalFormat("###.#").format(res); // rounded to 2 decimal places
        return str;
    }

    @Bindable
    public String getMinTravel() {
        double res = getThrowGauge().getValue().GetMinThrow();
        String str = getApplication().getString(R.string.command_minthrow)+ " " + new DecimalFormat("###.#").format(res); // rounded to 2 decimal places
        return str;
    }

    @Bindable
    public String getMinTravel2() {
        double res = getThrowGauge().getValue().GetMinThrow();
        String str = new DecimalFormat("###.#").format(res); // rounded to 2 decimal places
        return str;
    }

    @Bindable
    public Drawable getMinTravelColor() {
        if(getThrowGauge().getValue().IsBelowTravelMin())
            return new ColorDrawable(Color.parseColor("red"));
        else
            return new ColorDrawable(Color.parseColor("white"));
    }

    @Bindable
    public Drawable getMinTravelColor2() {
        if(getThrowGauge2().getValue().IsBelowTravelMin())
            return new ColorDrawable(Color.parseColor("red"));
        else
            return new ColorDrawable(Color.parseColor("white"));
    }

    @Bindable
    public Drawable getMaxTravelColor() {
        if(getThrowGauge().getValue().IsAboveTravelMax())
            return new ColorDrawable(Color.parseColor("red"));
        else
            return new ColorDrawable(Color.parseColor("white"));
    }

    @Bindable
    public Drawable getMaxTravelColor2() {
        if(getThrowGauge2().getValue().IsAboveTravelMax())
            return new ColorDrawable(Color.parseColor("red"));
        else
            return new ColorDrawable(Color.parseColor("white"));
    }

    public void setMinTravel(String value){
        int d = Integer.parseInt(value);
        getThrowGauge().getValue().SetMinTravel(-20d);
    }
    public void setMinTravel2(String value){
        int d = Integer.parseInt(value);
        getThrowGauge2().getValue().SetMinTravel(-20d);
    }

    public void setMaxTravel(String value){
        int d = Integer.parseInt(value);
        getThrowGauge().getValue().SetMaxTravel(d);
    }

    public void setMaxTravel2(String value){
        int d = Integer.parseInt(value);
        getThrowGauge2().getValue().SetMaxTravel(d);
    }

    public void onSetMaxTravelClicked() {
        getThrowGauge().getValue().SetMaxTravel();
        notifyPropertyChanged(BR.maxTravel);
        notifyPropertyChanged(BR.minTravel);
        notifyPropertyChanged(BR.maxTravelColor);
        notifyPropertyChanged(BR.minTravelColor);
    }

    public void onSetMinTravelClicked() {
        getThrowGauge().getValue().SetMinTravel();
        notifyPropertyChanged(BR.maxTravel);
        notifyPropertyChanged(BR.minTravel);
        notifyPropertyChanged(BR.maxTravelColor);
        notifyPropertyChanged(BR.minTravelColor);
    }

    public void resetSensorPosition(){
        ThrowGauge gauge = getThrowGauge().getValue();
        gauge.ResetMath();
        ThrowGauge gauge2 = getThrowGauge2().getValue();
        gauge2.ResetMath();
    }

    public boolean HasResumed(){
        ThrowGauge gauge = getThrowGauge().getValue();
        ThrowGauge gauge2 = getThrowGauge2().getValue();
        return gauge.HasResumed() || gauge2.HasResumed();
    }

    public void resetNeutral(){
        ThrowGauge gauge = getThrowGauge().getValue();
        gauge.SetNeutral();
        ThrowGauge gauge2 = getThrowGauge2().getValue();
        gauge2.SetNeutral();
        notifyPropertyChanged(BR.angle);
        notifyPropertyChanged(BR.angle2);
        notifyPropertyChanged(BR.travel);
        notifyPropertyChanged(BR.travel2);
        notifyPropertyChanged(BR.maxTravel);
        notifyPropertyChanged(BR.minTravel);
        notifyPropertyChanged(BR.maxTravel2);
        notifyPropertyChanged(BR.minTravel2);
        notifyPropertyChanged(BR.chord);
    }

    public void onResetAngleClicked() {

        Message msg = this.mHandler.obtainMessage(BluetoothState.MESSAGE_STATE_CHANGE);
        Bundle bundle = new Bundle();
        bundle.putString("Reset sensor", "New neutral");
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
    }

    public boolean isNumeric(String str){
        return str.matches("-?\\d+(.\\d+)?");
    }

    public double parseDecimal(String input) throws ParseException {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        ParsePosition parsePosition = new ParsePosition(0);
        Number number = numberFormat.parse(input, parsePosition);

        if(parsePosition.getIndex() != input.length()){
            throw new ParseException("Invalid input", parsePosition.getIndex());
        }
        return number.doubleValue();
    }
}
