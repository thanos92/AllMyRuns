package dv606.gc222bz.finalproject;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import dv606.gc222bz.finalproject.database.Run;
import dv606.gc222bz.finalproject.database.RunDetails;
import dv606.gc222bz.finalproject.database.RunsDataSource;
import dv606.gc222bz.finalproject.utilities.PreferenceHelper;
import dv606.gc222bz.finalproject.utilities.Utilities;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Run run;
    private long startTime;
    private RunsDataSource runsDataSource;
    ArrayList<RunDetails> runDetails = new ArrayList<>();
    ArrayList<LatLng> coordinatesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();

        String runName = intent.getStringExtra(getString(R.string.prefs_mapTitle));
        getSupportActionBar().setTitle(runName);

        runsDataSource = new RunsDataSource(this);
        runsDataSource.open();
        run = runsDataSource.getRunById(intent.getLongExtra(getString(R.string.run_id_extra), 0));

        //coordinatesList = Utilities.stringToCoordinatesList(run.getCoordinates());

        runDetails = runsDataSource.getRunDetails((intent.getLongExtra(getString(R.string.run_id_extra), 0)));
        startTime = intent.getLongExtra(getString(R.string.prefs_start_time), 0);

        for(RunDetails runDetail : runDetails){
            coordinatesList.add(Utilities.stringToLatLng(runDetail.getCoordinates()));
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        menu.getItem(0).setChecked(PreferenceHelper.getDisplayDirection(this));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.option_direction:{
                boolean isDirectionDisplayed = !item.isChecked();
                if(mMap != null){
                    mMap.clear();
                    makeRouteMarker(isDirectionDisplayed);
                }
                PreferenceHelper.setDisplayDirection(this, isDirectionDisplayed);
                item.setChecked(isDirectionDisplayed);
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mMap == null){
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {

                if(runDetails.size() > 0){

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng position : coordinatesList) {

                    builder.include(position);
                }

                    LatLngBounds bounds = builder.build();

                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 100);

                    makeRouteMarker(PreferenceHelper.getDisplayDirection(MapsActivity.this));


                    mMap.animateCamera(cu);

                    mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                        @Override
                        public View getInfoWindow(Marker arg0) {
                            return null;
                        }

                        @Override
                        public View getInfoContents(Marker marker) {

                            LinearLayout info = new LinearLayout(MapsActivity.this);
                            info.setOrientation(LinearLayout.VERTICAL);

                            TextView title = new TextView(MapsActivity.this);
                            title.setTextColor(Color.BLACK);
                            title.setGravity(Gravity.CENTER);
                            title.setTypeface(null, Typeface.BOLD);
                            title.setText(marker.getTitle());

                            TextView snippet = new TextView(MapsActivity.this);
                            snippet.setTextColor(Color.GRAY);
                            snippet.setText(marker.getSnippet());

                            info.addView(title);
                            info.addView(snippet);

                            return info;
                        }
                    });
                }


            }
        });
    }

    private void makeRouteMarker(boolean isDirectionDisplayed) {

        LatLng lastPosition = null;

        for (int i = 0; i < runDetails.size() ; i++) {

            RunDetails runDetail = runDetails.get(i);

            LatLng position = Utilities.stringToLatLng(runDetail.getCoordinates());

            long runTime = runDetail.getTime();
            String timeDiff = Utilities.formatLongToTimer(runTime - startTime);

            if(i != 0 && i != runDetails.size() - 1){

                if (lastPosition != null && isDirectionDisplayed) {
                    float degree = (float) Utilities.bearing(lastPosition.latitude, lastPosition.longitude, position.latitude, position.longitude);
                    Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow)).anchor(0.5f, 0.3f).position(position).title(timeDiff).snippet(makeSnipped(runDetail)).rotation(degree));
                } else {
                    Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_point)).anchor(0.5f, 0.5f).position(position).title(timeDiff).snippet(makeSnipped(runDetail)));
                }
            }

            Polyline line = mMap.addPolyline(new PolylineOptions()
                    .add(coordinatesList.toArray(new LatLng[coordinatesList.size()]))
                    .width(8)
                    .color(Color.RED));

            lastPosition = position;

            if (coordinatesList.size() > 1) {

                Marker startMarker = mMap.addMarker(new MarkerOptions() .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).position(coordinatesList.get(0)).title(timeDiff).snippet(makeSnipped(runDetail)));
                Marker endMarker = mMap.addMarker(new MarkerOptions() .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).position(coordinatesList.get(coordinatesList.size()-1)).title(timeDiff).snippet(makeSnipped(runDetail)));
            }


        }
    }

    public String makeSnipped (RunDetails runDetails){

        return String.format(getString(R.string.marker_snippet), PreferenceHelper.getDistanceWithUnit(MapsActivity.this, (int)runDetails.getDistance()), PreferenceHelper.getSpeedWithUnit(MapsActivity.this,runDetails.getSpeed()), PreferenceHelper.getCaloriesWithUnit(MapsActivity.this,runDetails.getCalories() ));
    }

}
