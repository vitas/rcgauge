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

    public static final String UUID_INFO = "uuid_info";
    public static final String NAME_INFO = "name_info";
    public static final String POS_INFO = "pos_info";
    public static final String ADDRESS_INFO = "address_info";
    public static final String LIST_INFO = "list_info";

    public static String getKeyringAddress(Context context) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return defaultSharedPreferences.getString(ADDRESS_INFO, null);
    }

    public static void setKeyringAddress(Context context, String address) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        defaultSharedPreferences.edit().putString(ADDRESS_INFO, address).commit();
    }


    public static String getKeyringUUID(Context context) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return defaultSharedPreferences.getString(UUID_INFO, null);
    }

    public static void setKeyringUUID(Context context, String uuid) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        defaultSharedPreferences.edit().putString(UUID_INFO, uuid).commit();
    }

    public static String getKeyringName(Context context) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return defaultSharedPreferences.getString(NAME_INFO, null);
    }

    public static void setKeyringName(Context context, String name) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        defaultSharedPreferences.edit().putString(NAME_INFO, name).commit();
    }

    public static int getKeyringPos(Context context) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return defaultSharedPreferences.getInt(POS_INFO, -1);
    }

    public static void setKeyringPos(Context context, int pos) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        defaultSharedPreferences.edit().putInt(POS_INFO, pos).commit();
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
}
