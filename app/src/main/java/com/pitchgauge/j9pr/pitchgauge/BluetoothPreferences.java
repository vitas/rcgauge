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
}
