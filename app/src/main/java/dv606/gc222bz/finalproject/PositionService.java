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
import android.media.MediaPlayer;
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
import dv606.gc222bz.finalproject.utilities.Costants;
import dv606.gc222bz.finalproject.utilities.PreferenceHelper;
import dv606.gc222bz.finalproject.utilities.Utilities;


public class PositionService extends Service implements android.location.LocationListener, GpsStatus.Listener, SharedPreferences.OnSharedPreferenceChangeListener {

    //region fields
    public static final String START_INTENT =   "dv606.gc222bz.finalproject.START_INTENT";
    public static final String POSITION_GETTED_INTENT =   "dv606.gc222bz.finalproject.POSITION_GETTED_INTENT";
    public static final String READY_INTENT =   "dv606.gc222bz.finalproject.READY_INTENT";

    public static final int AWAIT_SAVE_STATE = 0, START_STATE = 1, STOP_STATE = 2, READY_STATE = 3;

    private final IBinder mBinder = new PositionServiceBinder();

    private Location mLastPreciseLocation, mLastLocation;
    private RunsDataSource mRunsDataSource;
    private long mStartTime, mEndTime, mLastTime, mLastLocationMillis;
    private int mActualState = STOP_STATE;
    private long mForegroundTime, mLastForegroundTime, mLastInterval;
    private int mDistance, mConsumedCalories;
    private float mSpeed, mMediumSpeed, maxSpeed;
    private boolean isGPSFix, isWarnPlayed = true;

    private long mGpsUpdateInterval;

    private SharedPreferences prefs;
    private MediaPlayer player;

    private LocationManager locationManager;

    private ArrayList<LatLng> collectedPoints = new ArrayList<>();
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;

    //endregion

    //region events
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

        Toast.makeText(this,""+mGpsUpdateInterval, Toast.LENGTH_SHORT).show();

        //initialize location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //request location update with medium time to allow the gps to fix to satellites
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER
                , Costants.GPS_START_INTERVAL,
                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

        locationManager.addGpsStatusListener(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int starId){
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent){

        //if the service is idle terminate it
        if(mActualState != START_STATE  && mActualState != READY_STATE){
            stopSelf();
        }

        return true;
    }


    @Override
    public void onDestroy(){

        //clear the resources
        if(player != null){
            player.reset();
            player.release();
            player = null;
        }

        locationManager.removeUpdates(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        mRunsDataSource.close();
    }



    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        mLastLocationMillis = SystemClock.elapsedRealtime();
        

        Toast.makeText(this,""+location.getAccuracy(), Toast.LENGTH_SHORT).show();

        //allow only the position with the specified accuracy
        if(location != null && location.hasAccuracy() && location.getAccuracy() <= 20 && isBetterLocation(location, mLastPreciseLocation) && (mActualState == READY_STATE || mActualState == START_STATE)){

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            collectedPoints.add(new LatLng(latitude,longitude));

            //first position with the given precision
            if(mActualState == READY_STATE){

                changeState(START_STATE);

                if(Costants.GPS_INIT_INTERVAL != mGpsUpdateInterval) {
                    setGPSInterval(mGpsUpdateInterval);
                }

                if(PreferenceHelper.getIsAudioEnabled(this)){
                    player = MediaPlayer.create(PositionService.this, R.raw.activity_started);
                    player.start();
                }

                mStartTime = System.currentTimeMillis();
                mDistance = 0;
                mSpeed = 0;
                mLastInterval = 0;
                mMediumSpeed = mSpeed;
                maxSpeed = mSpeed;
                mLastTime = System.currentTimeMillis();
                mLastPreciseLocation = location;
            }
            else {

                if(mLastPreciseLocation.getLatitude() !=latitude && mLastPreciseLocation.getLongitude() != longitude){

                    long currentTime = System.currentTimeMillis();

                    //calculate the distance between the current position and the last position
                    int result = Math.round(location.distanceTo(mLastPreciseLocation));

                    mDistance = mDistance + result;

                    //calculate the time elapsed between the current position and the last position
                    long timeInSecond = ((currentTime - mLastTime) / 1000);

                    if(timeInSecond != 0){

                        mLastInterval = mLastInterval + timeInSecond;

                        mMediumSpeed = ((float)mDistance / (float)mLastInterval);

                        //use space/time formula to calculate the speed
                        float calculatedSpeed = ((float)result / timeInSecond);


                        if(calculatedSpeed > maxSpeed){
                            maxSpeed = calculatedSpeed;
                        }

                        /*if(mMediumSpeed != 0){
                            mMediumSpeed = (mMediumSpeed + calculatedSpeed) / 2;
                        }
                        else {
                            mMediumSpeed = calculatedSpeed;
                        }*/

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
        isGPSFix = false;
        playDisabledGpsSound();
    }

    @Override
    public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                if (mLastLocation != null){
                    //if the last location is old of an interval the gps is disconnected
                    isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < (mGpsUpdateInterval * 2);
                }

                if (isGPSFix) { // A fix has been acquired.
                    playEnabledGpsSound();
                    isWarnPlayed = false;
                } else {
                    playDisabledGpsSound();
                }

                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:

                isGPSFix = true;

                break;
        }
    }

    //if the user modify the gps update time a new request will be created and set in the location manager
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if(key.equals(getString(R.string.prefs_gps_frequency))){

            mGpsUpdateInterval = PreferenceHelper.getMinGpsUpdateTime(PositionService.this);
        }
    }
    //endregion

    //region method

    public void sendPositionBroadcast(int distance, int calories , float calculatedSpeed, double lat, double lon){

        //send a broadcast to the main activity in order to update the ui with the new data
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
        boolean isSignificantlyLessAccurate = accuracyDelta > 20;

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

    public void playEnabledGpsSound(){
        //play sound only when the location service is running and no other audio is playing
        if(isWarnPlayed && mActualState == START_STATE && PreferenceHelper.getIsAudioEnabled(this) && (player == null || !player.isPlaying())){
            player = MediaPlayer.create(PositionService.this, R.raw.gps_connected);
            player.start();
        }
    }

    public void playDisabledGpsSound(){
        //play sound only when the location service is running and no other audio is playing
        if(!isWarnPlayed && mActualState == START_STATE && PreferenceHelper.getIsAudioEnabled(this) && (player == null || !player.isPlaying())){
            player = MediaPlayer.create(PositionService.this, R.raw.gps_connection_lost);
            player.start();
            isWarnPlayed = true;
        }
    }

    public void startPositionService()
    {
        changeState(READY_STATE);
        setGPSInterval(Costants.GPS_INIT_INTERVAL);
    }

    private void setGPSInterval(long gpsInitInterval) {


            locationManager.removeUpdates(PositionService.this);

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER
                    , gpsInitInterval,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, PositionService.this);
    }

    public void stopPositionService(boolean saveData){
        mEndTime = System.currentTimeMillis();

        setGPSInterval(Costants.GPS_START_INTERVAL);

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
            mRunsDataSource.insertRun(mStartTime, mEndTime, mConsumedCalories, mSpeed, mDistance, Utilities.coordinatesToString(collectedPoints), name);
        }

        resetField();

    }

    public void resetField(){
        //reset all field
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

        //save the time when the bound activity is suspended
        this.mForegroundTime = foregroundTime;
        mLastForegroundTime = System.currentTimeMillis();

        //build the notification
        Notification.Builder notificationBuilder= new Notification.Builder(getApplicationContext());
        notificationBuilder.setSmallIcon(R.drawable.ic_notification_icon);
        notificationBuilder.setContentTitle(getString(R.string.notification_title_text));
        notificationBuilder.setContentText(getString(R.string.notification_content_text));
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

    //endregion

    public class PositionServiceBinder extends Binder {
        PositionService getService() {
            return PositionService.this;
        }
    }
}
