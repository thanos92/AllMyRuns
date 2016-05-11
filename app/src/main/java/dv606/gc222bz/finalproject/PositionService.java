package dv606.gc222bz.finalproject;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import dv606.gc222bz.finalproject.database.RunsDataSource;
import dv606.gc222bz.finalproject.utilities.PreferenceHelper;
import dv606.gc222bz.finalproject.utilities.Utilities;


public class PositionService extends Service implements android.location.LocationListener, GpsStatus.Listener{

    public static final String START_INTENT =   "dv606.gc222bz.finalproject.START_INTENT";
    public static final String STOP_INTENT =   "dv606.gc222bz.finalproject.STOP_INTENT";
    public static final String AWAIT_SAVE_INTENT =   "dv606.gc222bz.finalproject.AWAIT_SAVE_INTENT";
    public static final String POSITION_GETTED_INTENT =   "dv606.gc222bz.finalproject.POSITION_GETTED_INTENT";
    public static final String READY_INTENT =   "dv606.gc222bz.finalproject.READY_INTENT";

    public static final int AWAIT_SAVE_STATE = 0, START_STATE = 1, STOP_STATE = 2, READY_STATE = 3;

    private final IBinder mBinder = new PositionServiceBinder();

    private Location mLastPreciseLocation, mLastLocation;
    private RunsDataSource mRunsDataSource;
    private long mStartTime, mEndTime, mLastTime, mLastLocationMillis;
    private int mActualState = STOP_STATE;
    private long mForegroundTime, mLastForegroundTime;
    private int mDistance, mConsumedCalories;
    private float mSpeed, mMediumSpeed, maxSpeed;
    private boolean isGPSFix;

    private long mGpsUpdateInterval;

    private SharedPreferences prefs;

    private LocationManager locationManager;

    private ArrayList<LatLng> collectedPoints = new ArrayList<>();
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate(){

        mRunsDataSource = new RunsDataSource(this);
        mRunsDataSource.open();

        mGpsUpdateInterval = PreferenceHelper.getMinGpsUpdateTime(PositionService.this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER
                , mGpsUpdateInterval,
                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

        locationManager.addGpsStatusListener(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener(){

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals(getString(R.string.prefs_gps_frequency))){

                    mGpsUpdateInterval = PreferenceHelper.getMinGpsUpdateTime(PositionService.this);
                    locationManager.removeUpdates(PositionService.this);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER
                            , mGpsUpdateInterval,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, PositionService.this);
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int starId){
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent){

        if(mActualState != START_STATE){
            stopSelf();
        }

        return true;
    }


    @Override
    public void onDestroy(){

        mRunsDataSource.close();
    }

    public void startPositionService(){
        changeState(READY_STATE);
    }

    public void stopPositionService(boolean saveData){
        mEndTime = System.currentTimeMillis();

        if(saveData){
            changeState(AWAIT_SAVE_STATE);
        }
        else{
            changeState(STOP_STATE);
            resetField();
        }
    }

    public void saveData(boolean saveData, String name){

        changeState(STOP_STATE);

        if(saveData){
            mRunsDataSource.insertRun(mStartTime, mEndTime, mConsumedCalories, mSpeed, 0, mDistance, Utilities.coordinatesToString(collectedPoints), name);
        }

        resetField();

    }

    public void resetField(){
        mConsumedCalories = 0;
        mStartTime = 0;
        mEndTime = 0;
        mLastTime = 0;
        collectedPoints.clear();
        mLastPreciseLocation = null;
        mDistance = 0; mSpeed = 0; maxSpeed = 0; mMediumSpeed = 0;
        mForegroundTime = 0;
        mLastForegroundTime = 0;
    }

    public void changeState(int state){

        mActualState = state;

        if(state == START_STATE){
            Intent startIntent = new Intent();
            startIntent.setAction(START_INTENT);
            startIntent.putExtra(getString(R.string.start_time_extra), 0);
            sendBroadcast(startIntent);
        }
    }


    public void makeForeground(long foregroundTime){
        this.mForegroundTime = foregroundTime;
        mLastForegroundTime = System.currentTimeMillis();

        Notification.Builder notificationBuilder= new Notification.Builder(getApplicationContext());
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
        notificationBuilder.setContentTitle("All my runs in execution");
        notificationBuilder.setContentText("Tap to resum");
        notificationBuilder.setLights(0x00ffff00, 1000, 0);
        notificationBuilder.setOngoing(true);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,  PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        Notification notification = notificationBuilder.build();
        startForeground(1, notification);
    }

    public void stopForeground(){
        stopForeground(true);
    }

    public int getState(){
        return mActualState;
    }

    public long getmForegroundTime(){
        long pausedTime = System.currentTimeMillis() - mLastForegroundTime;
        return this.mForegroundTime + pausedTime;
    }

    public List<LatLng> getCollectedPoints(){
        return collectedPoints;
    }


    //region api listner

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        mLastLocationMillis = SystemClock.elapsedRealtime();

        System.out.println(location.getAccuracy());

        if(location != null && location.hasAccuracy() && location.getAccuracy() <= 50 && isBetterLocation(location, mLastPreciseLocation) && (mActualState == READY_STATE || mActualState == START_STATE)){

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            collectedPoints.add(new LatLng(latitude,longitude));


            if(mActualState == READY_STATE){
                changeState(START_STATE);
                mStartTime = System.currentTimeMillis();

                mDistance = 0;
                mSpeed = 0;
                mMediumSpeed = mSpeed;
                maxSpeed = mSpeed;
                mLastTime = System.currentTimeMillis();
                mLastPreciseLocation = location;
            }
            else {

                if(mLastPreciseLocation.getLatitude() !=latitude && mLastPreciseLocation.getLongitude() != longitude){

                    long currentTime = System.currentTimeMillis();

                    int result = Math.round(location.distanceTo(mLastPreciseLocation));

                    System.out.println(mLastPreciseLocation.getLatitude() +"   " + mLastPreciseLocation.getLongitude() + "   " + location.getLatitude() + "   "+ location.getLongitude() +"  "+ result);
                    mDistance = mDistance + result;

                    long timeInSecond = ((currentTime - mLastTime) / 1000);

                    if(timeInSecond != 0){

                        float calculatedSpeed = ((float)result / timeInSecond);


                        if(calculatedSpeed > maxSpeed){
                            maxSpeed = calculatedSpeed;
                        }

                        if(mMediumSpeed != 0){
                            mMediumSpeed = (mMediumSpeed + calculatedSpeed) / 2;
                        }
                        else {
                            mMediumSpeed = calculatedSpeed;
                        }

                        mSpeed = calculatedSpeed;

                        mLastTime = currentTime;
                        mLastPreciseLocation = location;

                    }
                }
            }

            int weight = Integer.parseInt(PreferenceHelper.getWeightPrefs(PositionService.this));
            mConsumedCalories = Utilities.calculateCalories(weight, mDistance);
            sendPositionBroadcast(mDistance, mConsumedCalories, mMediumSpeed, latitude, longitude);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void sendPositionBroadcast(int distance, int calories , float calculatedSpeed, double lat, double lon){

        Intent positionIntent = new Intent();
        positionIntent.setAction(POSITION_GETTED_INTENT);
        positionIntent.putExtra(getString(R.string.calories_extra), calories);
        positionIntent.putExtra(getString(R.string.distance_extra), distance);
        positionIntent.putExtra(getString(R.string.calculated_speed_extra), calculatedSpeed);
        positionIntent.putExtra(getString(R.string.lat_extra), lat);
        positionIntent.putExtra(getString(R.string.lon_extra), lon);
        sendBroadcast(positionIntent);
    }

    public float getmMediumSpeed(){
        return mMediumSpeed;
    }

    public int getmDistance(){
        return mDistance;
    }

    public int getmConsumedCalories(){return mConsumedCalories;}


    private static final int TWO_MINUTES = 1000 * 60 * 2;


    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 30;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                if (mLastLocation != null){
                    isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < mGpsUpdateInterval * 2;
                }

                if (isGPSFix) { // A fix has been acquired.

                    // Do something.
                } else {

                }

                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                // Do something.

                isGPSFix = true;

                break;
        }
    }


    public class PositionServiceBinder extends Binder {
        PositionService getService() {
            return PositionService.this;
        }
    }
}
