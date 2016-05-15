package dv606.gc222bz.finalproject;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

import dv606.gc222bz.finalproject.database.Run;
import dv606.gc222bz.finalproject.database.RunsDataSource;
import dv606.gc222bz.finalproject.utilities.Utilities;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Run run;
    private RunsDataSource runsDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        Intent intent = getIntent();

        runsDataSource = new RunsDataSource(this);
        runsDataSource.open();
        run = runsDataSource.getRunById(intent.getLongExtra(getString(R.string.run_id_extra), 0));
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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                List<LatLng> coordinatesList = Utilities.stringToCoordinatesList(run.getCoordinates());

                LatLng lastPosition = null;

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng position : coordinatesList) {
                    if(lastPosition != null){
                        float degree = (float)Utilities.bearing(lastPosition.latitude, lastPosition.longitude, position.latitude, position.longitude);mMap.addMarker(new MarkerOptions() .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).position(coordinatesList.get(0)).title(getString(R.string.start_title_text)));
                        mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow)).anchor(0.5f, 0.3f).position(position).rotation(degree));
                    }

                    //mMap.addCircle(new CircleOptions().radius(0.50).strokeColor(Color.BLUE).fillColor(Color.BLUE).center(position));
                    builder.include(position);

                    Polyline line = mMap.addPolyline(new PolylineOptions()
                            .add(coordinatesList.toArray(new LatLng[coordinatesList.size()]))
                            .width(8)
                            .color(Color.RED));

                    lastPosition = position;

                }
                LatLngBounds bounds = builder.build();

                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 100);

                if(coordinatesList.size() > 1){

                    Marker startMarker = mMap.addMarker(new MarkerOptions() .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).position(coordinatesList.get(0)).title(getString(R.string.start_title_text)));
                    Marker endMarker = mMap.addMarker(new MarkerOptions() .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).position(coordinatesList.get(coordinatesList.size()-1)).title(getString(R.string.end_title_text)));
                }

                mMap.animateCamera(cu);
            }
        });
    }


}
