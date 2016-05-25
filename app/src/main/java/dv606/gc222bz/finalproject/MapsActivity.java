package dv606.gc222bz.finalproject;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import dv606.gc222bz.finalproject.utilities.Costants;
import dv606.gc222bz.finalproject.utilities.PreferenceHelper;
import dv606.gc222bz.finalproject.utilities.Utilities;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Run run;
    private long startTime;
    private RunsDataSource runsDataSource;
    ArrayList<RunDetails> runDetails = new ArrayList<>();
    ArrayList<LatLng> coordinatesList = new ArrayList<>();
    private Dialog dialog;
    private Marker lastMarker;
    private ListView runListView;

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

        dialog = makeRunDetailsDialog(runDetails);

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
            case R.id.option_details:{
                if(dialog != null && !dialog.isShowing()){
                    dialog.show();
                }
                else if(dialog == null){
                    dialog = makeRunDetailsDialog(runDetails);
                    dialog.show();
                }
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

    public Dialog makeRunDetailsDialog(List<RunDetails> list){
        final Dialog listDialog = new Dialog(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_layout, null);
        RunDetailsAdapter adapter = new RunDetailsAdapter(this, list);
        runListView = (ListView) v.findViewById(R.id.run_details_list);
        TextView textView = (TextView) v.findViewById(R.id.title_text);
        textView.setText(getString(R.string.listdialog_title));
        runListView.setAdapter(adapter);
        runListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mMap != null){
                    dialog.dismiss();
                    RunDetails runDetail = MapsActivity.this.runDetails.get(position);
                    LatLng latLng = Utilities.stringToLatLng(runDetail.getCoordinates());

                    if(MapsActivity.this.lastMarker != null){
                        lastMarker.remove();
                    }
                    Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).position(latLng).title(Utilities.formatLongToTimer(runDetail.getTime() - startTime)).snippet(makeSnippet(runDetail)));
                    marker.showInfoWindow();
                    lastMarker = marker;

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));

                }
            }
        });

        listDialog.setTitle(R.string.listdialog_title);
        listDialog.setContentView(v);
        listDialog.setCancelable(true);



        return listDialog;
    }

    private void makeRouteMarker(boolean isDirectionDisplayed) {

        LatLng lastPosition = null;

        if(isDirectionDisplayed){

            for (int i = 0; i < runDetails.size() ; i++) {

                RunDetails runDetail = runDetails.get(i);

                LatLng position = Utilities.stringToLatLng(runDetail.getCoordinates());


                long runTime = runDetail.getTime();
                String timeDiff = Utilities.formatLongToTimer(runTime - startTime);

                if (i != 0 && i != runDetails.size() - 1 ) {

                    LatLng nexPosition = Utilities.stringToLatLng(runDetails.get(i+1).getCoordinates());

                    //to avoid duplicate point in the same position
                    if(position.latitude != nexPosition.latitude && position.longitude != nexPosition.longitude){

                        if (lastPosition != null) {
                            float degree = (float) Utilities.bearing(lastPosition.latitude, lastPosition.longitude, position.latitude, position.longitude);
                            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow)).anchor(0.5f, 0.3f).position(position).title(timeDiff).snippet(makeSnippet(runDetail)).rotation(degree));
                        }/*else {
                        Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_point)).anchor(0.5f, 0.5f).position(position).title(timeDiff).snippet(makeSnippet(runDetail)));
                    } */
                    }
                }

                lastPosition = position;
            }
        }

        Polyline line = mMap.addPolyline(new PolylineOptions()
                    .add(coordinatesList.toArray(new LatLng[coordinatesList.size()]))
                    .width(8)
                    .color(Color.RED));


            if (coordinatesList.size() > 0) {

                Marker startMarker = mMap.addMarker(new MarkerOptions() .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).position(coordinatesList.get(0)).title(getString(R.string.start_title)).snippet(makeSnippet(runDetails.get(0))));

                if(coordinatesList.size() > 1)  {
                Marker endMarker = mMap.addMarker(new MarkerOptions() .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).position(coordinatesList.get(coordinatesList.size()-1)).title(getString(R.string.end_title)).snippet(makeSnippet(runDetails.get(runDetails.size() - 1))));
                }

            }

    }

    public String makeSnippet(RunDetails runDetails){

        return String.format(getString(R.string.marker_snippet), PreferenceHelper.getDistanceWithUnit(MapsActivity.this, (int)runDetails.getDistance()), PreferenceHelper.getSpeedWithUnit(MapsActivity.this,runDetails.getSpeed()), PreferenceHelper.getCaloriesWithUnit(MapsActivity.this,runDetails.getCalories() ));
    }

    public class RunDetailsAdapter extends ArrayAdapter<RunDetails>{


        public RunDetailsAdapter(Context context, List<RunDetails> elements){
            super(context, R.layout.rundetails_item, elements);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View row = convertView;

            if(row == null){
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.rundetails_item, parent, false);
                row.setTag(R.id.time_list_detail, row.findViewById(R.id.time_list_detail));
                row.setTag(R.id.list_detail, row.findViewById(R.id.list_detail));
            }

            RunDetails item = getItem(position);

            TextView timeView = (TextView)row.findViewById(R.id.time_list_detail);
            TextView detailsView = (TextView)row.findViewById(R.id.list_detail);
            timeView.setText(Utilities.formatLongToTimer(item.getTime() - startTime));
            detailsView.setText(makeSnippet(item));

            return row;
        }

    }

}
