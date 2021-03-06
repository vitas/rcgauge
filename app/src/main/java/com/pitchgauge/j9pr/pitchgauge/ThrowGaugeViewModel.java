package com.pitchgauge.j9pr.pitchgauge;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.databinding.Bindable;
import android.databinding.Observable;
import android.databinding.PropertyChangeRegistry;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;

import java.text.DecimalFormat;
import java.util.Locale;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;

public class ThrowGaugeViewModel extends AndroidViewModel implements Observable {
    private PropertyChangeRegistry callbacks = new PropertyChangeRegistry();
    private Handler mHandler;
    private boolean mMultiDevice;

    private String btStatusStr = "BT1";
    private String btStatusStr2 = "BT2";

    private Drawable btStatusCol = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
    private Drawable btStatusCol2 = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);

    private int witLinkStatus[] = {BluetoothState.WIT_IDLE, BluetoothState.WIT_IDLE};

    public void setBtStatusColor(Drawable colour) {
        this.btStatusCol = colour;
    }

    public void setBtStatusColor2(Drawable colour) {
        this.btStatusCol2 = colour;
    }

    private boolean buttonResetAngleEnable = true;
    private boolean buttonCalibrateEnable = true;


    // parse number string using language specfic number format (comma separator , or .)
    // get rid of number dialog sending . instead of , (android bug)
    private double parseStringDouble(String s) {
        double d;
        try {
            d = Double.parseDouble(s.toString().replace(',', '.'));
        } catch (NumberFormatException e) {
            d = 0.0;
        }
        return d;
    }

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

    public void setChordValue(String chord) {
    }

    public void setChordValueDia(String chord) {
        double d = parseStringDouble(chord);
        getThrowGauge().getValue().SetChord(d);
        getThrowGauge2().getValue().SetChord(d);
        notifyPropertyChanged(BR.chordValue);
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
            setWitLinkStatus(0, BluetoothState.WIT_DATA_ARRIVING);
            notifyPropertyChanged(BR.angle);
            notifyPropertyChanged(BR.travel);
            notifyPropertyChanged(BR.maxTravel);
            notifyPropertyChanged(BR.minTravel);
            notifyPropertyChanged(BR.maxTravelSet);
            notifyPropertyChanged(BR.minTravelSet);
            notifyPropertyChanged(BR.maxTravelColor);
            notifyPropertyChanged(BR.minTravelColor);
            notifyPropertyChanged(BR.travelColor);
            notifyPropertyChanged(BR.btStatusColor);
        } else {
            getThrowGauge2().getValue().SetAngles(x, y, z);
            setWitLinkStatus(1, BluetoothState.WIT_DATA_ARRIVING);
            notifyPropertyChanged(BR.angle2);
            notifyPropertyChanged(BR.travel2);
            notifyPropertyChanged(BR.maxTravel2);
            notifyPropertyChanged(BR.minTravel2);
            notifyPropertyChanged(BR.maxTravelSet);
            notifyPropertyChanged(BR.minTravelSet);
            notifyPropertyChanged(BR.maxTravelColor2);
            notifyPropertyChanged(BR.minTravelColor2);
            notifyPropertyChanged(BR.travelColor2);
            notifyPropertyChanged(BR.btStatusColor2);
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
                notifyPropertyChanged(BR.travelColor);
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
                notifyPropertyChanged(BR.travelColor2);
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
        return new DecimalFormat("0.0").format(getThrowGauge().getValue().GetAngle());
    }

    @Bindable
    public String getAngle2() {
        return new DecimalFormat("0.0").format(getThrowGauge2().getValue().GetAngle());
    }

    @Bindable
    public String getChordValue() {
        return new DecimalFormat("0.0").format(getThrowGauge().getValue().GetChord());
    }
    // this version of getChordValue does not show decimal point, if not needed, for dialog
    public String getChordValueNum() {
        return new DecimalFormat("#.#").format(getThrowGauge().getValue().GetChord());
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
    public String getBtStatus() {
        String str = btStatusStr;
        return str;
    }

    public void setBtStatus(String str) {
        btStatusStr = str;
        notifyPropertyChanged(BR.btStatus);
    }

    @Bindable
    public String getBtStatus2() {
        String str = btStatusStr2;
        return str;
    }

    public void setBtStatus2(String str) {
        btStatusStr2 = str;
        notifyPropertyChanged(BR.btStatus2);
    }

    @Bindable
    public Drawable getBtStatusColor() {
        return btStatusCol;
    }

    @Bindable
    public Drawable getBtStatusColor2() {
        return btStatusCol2;
    }

    @Bindable
    public boolean getButtonResetAngleEnable() {
        return buttonResetAngleEnable;
    }

    public void setButtonResetAngleEnable(boolean enable) {
         buttonResetAngleEnable = enable;
    }

    @Bindable
    public boolean getButtonCalibrateEnable() {
        return buttonCalibrateEnable;
    }

    public void setButtonCalibrateEnable(boolean enable) {
        buttonCalibrateEnable = enable;
    }


    @Bindable
    public String getTravel() {
        double res = getThrowGauge().getValue().GetThrow();
        String str = new DecimalFormat("0.0").format(res); // rounded to 2 decimal places
        return str;
    }

    @Bindable
    public String getTravel2() {
        double res = getThrowGauge2().getValue().GetThrow();
        String str = new DecimalFormat("0.0").format(res); // rounded to 2 decimal places
        return str;
    }

    @Bindable
    public String getMaxTravel() {
        double res = getThrowGauge().getValue().GetMaxThrow();
        String str = new DecimalFormat("0.0").format(res); // rounded to 2 decimal places
        return str;
    }

    @Bindable
    public String getMaxTravel2() {
        double res = getThrowGauge2().getValue().GetMaxThrow();
        String str = new DecimalFormat("0.0").format(res); // rounded to 2 decimal places
        return str;
    }

    @Bindable
    public String getMinTravel() {
        double res = Math.abs(getThrowGauge().getValue().GetMinThrow());
        String str = new DecimalFormat("0.0").format(res); // rounded to 2 decimal places
        return str;
    }

    @Bindable
    public String getMinTravel2() {
        double res = Math.abs(getThrowGauge2().getValue().GetMinThrow());
        String str = new DecimalFormat("0.0").format(res); // rounded to 2 decimal places
        return str;
    }

    @Bindable
    public String getMaxTravelSet() {
        double res = getThrowGauge().getValue().GetSetMaxThrow();
        String str = "Max " + new DecimalFormat("0.0").format(res);
        return str;
    }

    public String getMaxTravelSetNum() {
        double res = getThrowGauge().getValue().GetSetMaxThrow();
        String str = new DecimalFormat("#.#").format(res);
        return str;
    }

    @Bindable
    public String getMinTravelSet() {
        double res = getThrowGauge().getValue().GetSetMinThrow();
        String str = "Min " + new DecimalFormat("0.0").format(res);
        return str;
    }

    public String getMinTravelSetNum() {
        double res = getThrowGauge().getValue().GetSetMinThrow();
        String str = new DecimalFormat("#.#").format(res);
        return str;
    }

    @Bindable
    public Drawable getTravelColor() {
        if(getThrowGauge().getValue().IsBelowTravelMin() || getThrowGauge().getValue().IsAboveTravelMax())
            return ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
        else
            return ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_green, null);
    }

    @Bindable
    public Drawable getTravelColor2() {
        if(getThrowGauge2().getValue().IsBelowTravelMin() || getThrowGauge2().getValue().IsAboveTravelMax())
            return ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
        else
            return ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_green, null);
    }

    @Bindable
    public Drawable getMinTravelColor() {
        if(getThrowGauge().getValue().IsBelowTravelMin())
            return ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
        else
            return ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_green, null);
    }

    @Bindable
    public Drawable getMinTravelColor2() {
        if(getThrowGauge2().getValue().IsBelowTravelMin())
            return ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
        else
            return ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_green, null);
    }

    @Bindable
    public Drawable getMaxTravelColor() {
        if(getThrowGauge().getValue().IsAboveTravelMax())
            return ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
        else
            return ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_green, null);
    }

    @Bindable
    public Drawable getMaxTravelColor2() {
        if(getThrowGauge2().getValue().IsAboveTravelMax())
            return ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
        else
            return ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_green, null);
    }

    public int getSecondSensorVisible() {
        return mMultiDevice?View.VISIBLE:View.GONE;
    }

    public void setMinTravelDia(String value){
        double d = parseStringDouble(value);
        getThrowGauge().getValue().SetMinTravel(d);
    }
    public void setMinTravelDia2(String value){
        double d = parseStringDouble(value);
        getThrowGauge2().getValue().SetMinTravel(d);
    }

    public void setMaxTravelDia(String value){
        double d = parseStringDouble(value);
        getThrowGauge().getValue().SetMaxTravel(d);
    }
    public void setMaxTravelDia2(String value){
        double d = parseStringDouble(value);
        getThrowGauge2().getValue().SetMaxTravel(d);
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
        notifyPropertyChanged(BR.travelColor);
        notifyPropertyChanged(BR.travelColor2);
    }

    public void onResetAngleClicked() {

        Message msg = this.mHandler.obtainMessage(BluetoothState.MESSAGE_STATE_CHANGE);
        Bundle bundle = new Bundle();
        bundle.putString("Reset sensor", "New neutral");
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
    }

    public void onCalibrateClicked() {

        Message msg = this.mHandler.obtainMessage(BluetoothState.MESSAGE_STATE_CHANGE);
        Bundle bundle = new Bundle();
        bundle.putString("Reset sensor", "Calibrate");
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

    public void setMultiDevice(boolean multiDevice) {
        mMultiDevice = multiDevice;
    }

    public boolean getMultiDevice() {
        return mMultiDevice;
    }

    public int getWitLinkStatus(int channel) {
        return witLinkStatus[channel];
    }

    public void setWitLinkStatus(int channel, int witLinkStatus) {
        this.witLinkStatus[channel] = witLinkStatus;
    }

}
