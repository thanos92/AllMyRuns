package dv606.gc222bz.finalproject.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dv606.gc222bz.finalproject.R;

/**
 * Created by desti on 25/04/2016.
 */
public class Utilities {

    public static LatLng stringToLatLng(String coordinates){
        String[] latLng = coordinates.split(",");
        LatLng latLng1 = new LatLng(Double.parseDouble(latLng[0]), Double.parseDouble(latLng[1]));
        return latLng1;
    }

    public static String coordinatesToString(List<LatLng> coordinatesList){

        String result ="";

        for(int i = 0; i < coordinatesList.size(); i++){
            LatLng element = coordinatesList.get(i);
            result = result +element.latitude+","+element.longitude;

            if(i != (coordinatesList.size() -1)){
                result = result + " ";
            }
        }

        return result;
    }

    public static List<LatLng> stringToCoordinatesList(String coordinatesString){
        ArrayList<LatLng> results = new ArrayList<>();
        String [] latLngStringlist = coordinatesString.split(" ");

        for(String latLngString : latLngStringlist){
            results.add(stringToLatLng(latLngString));
        }

        return results;
    }

    public static int getDiffDay(Date startDate, Date endDate){
        long diff = endDate.getTime() - startDate.getTime();
        return (int)TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }


    public static void showGpsDialog(final Activity activity, final int requestCode){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("GPS Not Found");
        builder.setMessage("Want To Enable?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialogInterface, int i)
            {
                Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivityForResult(settingsIntent, requestCode);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialogInterface, int i)
            {

            }
        });

        builder.create().show();
    }

    public static boolean isGpsEnabled(final Context context){
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE );
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static AlertDialog.Builder makeWelcomedialog(Context context, String message, String title){

        final AlertDialog.Builder alert = new AlertDialog.Builder(context);

        //edittext.setHint(R.string.run_name_default_text);
        alert.setMessage(message);
        alert.setTitle(title);


        alert.setPositiveButton(context.getString(R.string.ok_text), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });


        return alert;
    }

    public static int calculateCalories(float weightInKilogram, float distanceInMeters){
        return Math.round(weightInKilogram * 0.9f * (distanceInMeters / 1000));
    }

    public static String formatLongToTimer(long millis){
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }

    public static double bearing(double lat1, double lon1, double lat2, double lon2){

        double longitude1 = lon1;
        double longitude2 = lon2;
        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff= Math.toRadians(longitude2-longitude1);
        double y= Math.sin(longDiff)*Math.cos(latitude2);
        double x=Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);

        return (Math.toDegrees(Math.atan2(y, x))+360)%360;
    }


}
