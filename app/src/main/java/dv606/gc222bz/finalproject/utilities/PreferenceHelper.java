package dv606.gc222bz.finalproject.utilities;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import dv606.gc222bz.finalproject.R;

public class PreferenceHelper {

    public final static String PREFS_SPEED =  "prefs_speed";
    public final static String PREFS_DISTANCE =  "prefs_distance";
    public final static String PREFS_FIRST_START =  "prefs_first_start";
    public final static String PREFS_WEIGHT =  "prefs_weight";
    public final static String PREFS_CAMERA = "prefs_camera_auto";



    public static boolean isFirstStart(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFirstStart = prefs.getBoolean(PREFS_FIRST_START, true);

        if(isFirstStart){
            SharedPreferences.Editor prefEditor = prefs.edit();
            prefEditor.putBoolean(PREFS_FIRST_START, false);
            prefEditor.apply();
        }

        return isFirstStart;
    }

    public static void setWeightPrefs(Context context, String weight){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putString(PREFS_WEIGHT, weight);
        prefEditor.apply();
    }

    public static String getWeightPrefs(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREFS_WEIGHT, "");
    }

    public static String getSpeedWithUnit(Context context, float speed){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int unitIndex = Integer.parseInt(prefs.getString(PREFS_SPEED, "1"));
        String [] conversionFactors = context.getResources().getStringArray(R.array.speed_conversion_factor);
        float conversionFactor = Float.parseFloat(conversionFactors[unitIndex]);
        String [] measureUnit = context.getResources().getStringArray(R.array.speed_measure_unit);
        return String.format("%1$s %2$s", String.format("%.1f", speed * conversionFactor), measureUnit[unitIndex]);
    }
    
    public static String getCaloriesWithUnit(Context context, int calories){
        return String.format(context.getString(R.string.calories_unit_text), calories);
    }

    public static boolean getCameraAutoEnabled(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_CAMERA, true);
    }

    public static void setCameraAutoEnabled(Context context, boolean isEnabled){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putBoolean(PREFS_CAMERA, isEnabled);
        prefEditor.apply();
    }

    public static String getDistanceWithUnit(Context context, int distance){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int unitIndex = Integer.parseInt(prefs.getString(PREFS_DISTANCE, "1"));
        int [] conversionFactors = context.getResources().getIntArray(R.array.distance_conversion_factor);
        String [] measureUnit = context.getResources().getStringArray(R.array.distance_measure_unit);

        if(conversionFactors[unitIndex] == 1){
            return String.format("%1$s %2$s", distance, measureUnit[unitIndex]);
        }

        return String.format("%1$s %2$s", String.format("%.1f", (float)distance / conversionFactors[unitIndex]), measureUnit[unitIndex]);
    }

    public static long getMinGpsUpdateTime(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String timeString = prefs.getString(context.getString(R.string.prefs_gps_frequency), "5");
        return Long.parseLong(timeString) * 1000;

    }

}
