package dv606.gc222bz.finalproject;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import dv606.gc222bz.finalproject.utilities.Costants;
import dv606.gc222bz.finalproject.utilities.PreferenceHelper;
import dv606.gc222bz.finalproject.utilities.Utilities;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private PositionService mPositionServiceBinder;
    private TextView mDistanceText;
    private TextView mSpeedText;
    private TextView mTimerText;
    private TextView mCaloriesText;
    private boolean isRunning = false;
    private GoogleMap mMap;
    Handler handler = new Handler();

    private BroadcastReceiver receiver;

    private Button mStartButton;
    private Button mStopButton;
    private Menu mMenu;


    private LatLng lastPosition = null;
    private long mLastTimer;

    private ProgressDialog progressDialog;
    private AlertDialog.Builder confirmDialog;
    private boolean awaitStarting = false;




    protected ServiceConnection mServerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mPositionServiceBinder = ((PositionService.PositionServiceBinder) binder).getService();
            mPositionServiceBinder.stopForeground();
            int state = mPositionServiceBinder.getState();

            if( state == PositionService.START_STATE){
                isRunning = true;
                new Thread(new TimerTask(mPositionServiceBinder.getmForegroundTime())).start();
                setIndicator(mPositionServiceBinder.getmDistance(), mPositionServiceBinder.getmMediumSpeed(), mPositionServiceBinder.getmConsumedCalories());
                mStopButton.setVisibility(View.VISIBLE);
                mStartButton.setVisibility(View.INVISIBLE);
                setEnabledOptionMenu(false);
            }
            else if(awaitStarting){ //is true when onActivityResult is called
                awaitStarting = false;
                startPositionService();
            }
            else{
                resetIndicator();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPositionServiceBinder = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTimerText = (TextView) findViewById(R.id.timer_text);
        mDistanceText = (TextView) findViewById(R.id.distance_text);
        mSpeedText = (TextView) findViewById(R.id.cal_speed);
        mCaloriesText = (TextView) findViewById(R.id.calories_text);

        mStartButton = (Button) findViewById(R.id.start_button);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startPositionService();
            }
        });

        mStopButton = (Button) findViewById(R.id.stop_button);

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

               resetAll();
                setEnabledOptionMenu(true);

                mPositionServiceBinder.stopPositionService(true);

                confirmDialog = makeConfirmDialog();
                confirmDialog.show();



            }
        });

        mStopButton.setVisibility(View.INVISIBLE);
        mStartButton.setVisibility(View.VISIBLE);

        resetIndicator();
    }

    public void startPositionService(){

        //if the gps is enabled start the service otherwise display a dialog
        if(Utilities.isGpsEnabled(MainActivity.this)){

            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage(getString(R.string.gps_progress_message));
            progressDialog.setCancelable(false);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel_message), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mPositionServiceBinder.stopPositionService(false);
                    resetAll();
                }
            });

            progressDialog.show();
            mPositionServiceBinder.startPositionService();
        }
        else{

            Utilities.showGpsDialog(MainActivity.this, 100);
        }
    }

    public void resetAll(){

        isRunning = false;

        mStartButton.setVisibility(View.VISIBLE);
        mStopButton.setVisibility(View.INVISIBLE);

        if(mMap != null){
            mMap.clear();
        }

        resetIndicator();
    }

    public void resetIndicator(){
        mTimerText.setText(Costants.TIMER_ZERO_VALUE);
        mDistanceText.setText(PreferenceHelper.getDistanceWithUnit(this, 0));
        mSpeedText.setText(PreferenceHelper.getSpeedWithUnit(this, 0));
        mCaloriesText.setText(PreferenceHelper.getCaloriesWithUnit(this, 0));
    }

    public void setIndicator(int distance, float speed, int calories){
        mDistanceText.setText(PreferenceHelper.getDistanceWithUnit(this, distance));
        mSpeedText.setText(PreferenceHelper.getSpeedWithUnit(this, speed));
        mCaloriesText.setText(PreferenceHelper.getCaloriesWithUnit(this, calories));

    }

    private void resumeMap(List<LatLng> points){

        if(mMap != null){
            mMap.clear();
            for(LatLng point : points){
                makePoint(point);
            }

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(points.size() - 1 ), 20));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if(mPositionServiceBinder != null){
          resumeMap( mPositionServiceBinder.getCollectedPoints());
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.mMenu = menu;
        menu.getItem(0).setChecked(PreferenceHelper.getCameraAutoEnabled(this));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.option_history: {
                Intent intent = new Intent(this, HistoryActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.option_camera_auto:{
                PreferenceHelper.setCameraAutoEnabled(this, !item.isChecked());
                item.setChecked(!item.isChecked());
                return true;
            }
            case R.id.option_enable_audio:{
                PreferenceHelper.setAudioEnabled(this, item.isChecked());
                item.setChecked(!item.isChecked());
                return true;
            }
            case R.id.option_settings:{
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            default: return false;
        }
    }

    private void makePoint(LatLng position) {
        mMap.addCircle(new CircleOptions().radius(0.50).strokeColor(Color.RED).fillColor(Color.RED).center(position));
    }

    private void setEnabledOptionMenu(boolean isEnabled){

        if(mMenu != null){

            int menuSize = mMenu.size();

            for(int i = 0; i< menuSize; i++){
                MenuItem item = mMenu.getItem(i);
                if(item.getItemId() != R.id.option_camera_auto){
                    item.setEnabled(isEnabled);
                }
            }
        }
    }

    //region lifecycle event
    @Override
    public void onResume(){
        super.onResume();

        if(PreferenceHelper.isFirstStart(this)){
            makeWelcomeDialog(false).show();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(PositionService.POSITION_GETTED_INTENT);
        filter.addAction(PositionService.START_INTENT);
        filter.addAction(PositionService.READY_INTENT);


        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(intent.getAction().equals(PositionService.START_INTENT)){

                    long startTime = intent.getLongExtra(getString(R.string.start_time_extra), 0);
                    isRunning = true;
                    new Thread(new TimerTask(startTime)).start();

                    mStopButton.setVisibility(View.VISIBLE);
                    mStartButton.setVisibility(View.INVISIBLE);
                    setEnabledOptionMenu(false);
                    resetIndicator();
                }
                else if(intent.getAction().equals(PositionService.POSITION_GETTED_INTENT)){

                    if(progressDialog != null && progressDialog.isShowing()){

                        progressDialog.hide();
                    }

                    double lat = intent.getDoubleExtra(getString(R.string.lat_extra), 0);
                    double lon = intent.getDoubleExtra(getString(R.string.lon_extra), 0);

                    float speed = intent.getFloatExtra(getString(R.string.calculated_speed_extra), 0);
                    int distance = intent.getIntExtra(getString(R.string.distance_extra), 0);
                    int calories = intent.getIntExtra(getString(R.string.calories_extra), 0);

                    setIndicator(distance, speed, calories);

                     LatLng position = new LatLng(lat, lon);

                    if(mMap != null && PreferenceHelper.getCameraAutoEnabled(MainActivity.this)){

                        makePoint(position);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 20));
                    }

                }
            }
        };

        registerReceiver(receiver, filter);

        if(mMap == null){
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }



    }

    @Override
    public void onStart(){
        super.onStart();

        Intent intent = new Intent(this, PositionService.class);
        startService(intent);

        if(mPositionServiceBinder == null){
            bindService(intent, mServerConn, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        isRunning = false;

        if(mPositionServiceBinder != null && mPositionServiceBinder.getState() == PositionService.START_STATE){
            
            mPositionServiceBinder.makeForeground(mLastTimer);
        }


        if(mPositionServiceBinder != null){
            unbindService(mServerConn);
            mPositionServiceBinder = null;
        }

        if(receiver != null){
            unregisterReceiver(receiver);
            receiver = null;
        }
    }
    //endregion


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
           awaitStarting = true;
    }

    public AlertDialog.Builder makeConfirmDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        final EditText edittext = new EditText(this);
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(10);
        edittext.setFilters(FilterArray);
        edittext.setHint(R.string.run_name_default_text);
        alert.setMessage(getString(R.string.give_name_message));
        alert.setTitle(getString(R.string.save_run_message));
        alert.setView(edittext, 90, 0,90,0);


        alert.setPositiveButton(getString(R.string.yes_text), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                String activityName = edittext.getText().toString();

                if(!TextUtils.isEmpty(activityName.trim())){
                 mPositionServiceBinder.saveData(true, activityName);
                }
               else{
                 mPositionServiceBinder.saveData(true, null);
                }

            }
        });


        alert.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                                 KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mPositionServiceBinder.saveData(false, null);
                    arg0.dismiss();
                }
                return true;
            }
        });



        alert.setNegativeButton(getString(R.string.no_text), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mPositionServiceBinder.saveData(false, null);
            }
        });

        return alert;
    }


    public AlertDialog.Builder makeWelcomeDialog(boolean displayError){

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);

        final EditText edittext = new EditText(this);
        edittext.setInputType(InputType.TYPE_CLASS_NUMBER);
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(3);
        edittext.setFilters(FilterArray);

        if(displayError){
            edittext.setError(getString(R.string.value_not_valid_error));
        }

        //edittext.setHint(R.string.run_name_default_text);
        alert.setMessage(getString(R.string.calories_welcome_message));
        alert.setTitle(getString(R.string.insert_welcom_message));
        alert.setView(edittext, 90, 0,90,0);


        alert.setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                String activityName = edittext.getText().toString();

                if(!TextUtils.isEmpty(activityName.trim())){
                    int value = Integer.parseInt(activityName);
                    if(value >= Costants.MIN_WEIGHT && value > Costants.MAX_WEIGHT){
                        makeWelcomeDialog(true).show();
                    }
                    else{

                        PreferenceHelper.setWeightPrefs(MainActivity.this, activityName);
                    }
                }
                else{
                    makeWelcomeDialog(true).show();
                }

            }
        });


        return alert;
    }

    public class TimerTask implements Runnable {

        private final long delay;
        private final long startTime;

        public TimerTask(long delay){
            this.delay = delay;
            this.startTime = System.nanoTime();
        }

        @Override
        public void run() {

            while(isRunning){
                long millis = (System.nanoTime() - startTime)/Costants.MILLIS_TO_SECONDS_FACTOR + delay;
                mLastTimer = millis;
                String hms = Utilities.formatLongToTimer(millis);
                handler.post(new TimerMessage(hms));
                SystemClock.sleep(100);
            }

        }
    }

    private class TimerMessage implements Runnable {

        final String timer;

        public TimerMessage(String timer){
            this.timer = timer;
        }

        @Override
        public void run() {
            mTimerText.setText(timer);
        }
    };

}
