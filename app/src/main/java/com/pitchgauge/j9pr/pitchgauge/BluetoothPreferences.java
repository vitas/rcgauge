package com.pitchgauge.j9pr.pitchgauge;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class BluetoothPreferences {

    public static final String LIST_INFO = "list_info";
    public static final String MAIN_PREFS = "main_prefs";

	// fetch tag from list of tags
    public static DeviceTag getTag(int inhash, ArrayList<DeviceTag> l) {
        DeviceTag result = new DeviceTag();
        for (DeviceTag sTag: l) {
            if (sTag.hashCode() == inhash) {
                return sTag;
            }
        }
        return new DeviceTag();
    }

    public static void setKeyrings(Context context, ArrayList<DeviceTag> arrPackage) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Gson gson = new Gson();
        String json = gson.toJson(arrPackage);
        defaultSharedPreferences.edit().putString(LIST_INFO, json).commit();
    }

    public static ArrayList<DeviceTag> getKeyrings(Context context) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String json = defaultSharedPreferences.getString(LIST_INFO, null);
        if (json != null) {

            if (!json.isEmpty()) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<DeviceTag>>() {}.getType();
                return gson.fromJson(json, type);
            }
        }
        return new ArrayList<>();
    }

    public static void setMainPrefs(Context context, MainPrefs prefs) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Gson gson = new Gson();
        String json = gson.toJson(prefs);
        defaultSharedPreferences.edit().putString(MAIN_PREFS, json).commit();
    }

    public static MainPrefs getMainPrefs(Context context) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String json = defaultSharedPreferences.getString(MAIN_PREFS, null);
        if (json != null) {
            if (!json.isEmpty()) {
                Gson gson = new Gson();
                Type type = new TypeToken<MainPrefs>() {}.getType();
                return gson.fromJson(json, type);
            }
        }
        return new MainPrefs();
    }

}
