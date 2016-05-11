package dv606.gc222bz.finalproject.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;


public class RunsDataSource {

    private SQLiteDatabase database;
    private RunsDBHelper dbHelper;
    private String[] RunsColumns = {
            RunsDBHelper.COLUMN_ID, RunsDBHelper.COLUMN_START_DATE, RunsDBHelper.COLUMN_END_DATE
            ,RunsDBHelper.COLUMN_CALORIES_NAME,RunsDBHelper.COLUMN_SPEED_NAME, RunsDBHelper.COLUMN_DISTANCE_NAME,
            RunsDBHelper.COLUMN_COORDINATES_NAME, RunsDBHelper.COLUMN_RUN_NAME, RunsDBHelper.COLUMN_PAUSE_TIME
    };

    public RunsDataSource(Context context){
        dbHelper = new RunsDBHelper(context);
    }

    public void open() throws SQLException{
        database = dbHelper.getWritableDatabase();
    }

    public void close(){
        dbHelper.close();
    }


    public long insertRun(long startDate, long endDate,int calories, float speed,long pause, float distance, String coordinates, String name){
        ContentValues values = new ContentValues();

        values.put(RunsDBHelper.COLUMN_CALORIES_NAME, calories);
        values.put(RunsDBHelper.COLUMN_SPEED_NAME, speed);
        values.put(RunsDBHelper.COLUMN_DISTANCE_NAME, distance);
        values.put(RunsDBHelper.COLUMN_PAUSE_TIME, pause);
        values.put(RunsDBHelper.COLUMN_COORDINATES_NAME, coordinates);
        values.put(RunsDBHelper.COLUMN_START_DATE, startDate);
        values.put(RunsDBHelper.COLUMN_END_DATE, endDate);

        System.out.println(name);

        if (name != null){
            values.put(RunsDBHelper.COLUMN_RUN_NAME, name);
        }

        long insertId = database.insert(RunsDBHelper.RUNS_TABLE_NAME, null, values);
        return insertId;
    }


    public List<Run> getAllRun(){
        ArrayList<Run> runs = new ArrayList<Run>();

        Cursor cursor = database.query(true, RunsDBHelper.RUNS_TABLE_NAME, RunsColumns,null, null, null, null, RunsDBHelper.COLUMN_START_DATE + " DESC", null);

        if(cursor != null && cursor.getCount() > 0){
            while(cursor.moveToNext()){
                Run run = cursorToRun(cursor);
                runs.add(run);
            }

            cursor.close();
        }

        return runs;
    }

    public Run getRunById(long _id){
        Cursor cursor = database.query(true, RunsDBHelper.RUNS_TABLE_NAME, RunsColumns, RunsDBHelper.COLUMN_ID + "=" + _id, null, null, null,null, null);
        if(cursor != null && cursor.getCount() > 0){
            cursor.moveToFirst();
            return cursorToRun(cursor);
        }

        return null;
    }

    public boolean deleteRun(long _id){
        return database.delete(RunsDBHelper.RUNS_TABLE_NAME, RunsDBHelper.COLUMN_ID + "=" + _id, null) > 0;
    }



    public Run cursorToRun(Cursor cursor){
        Run run = new Run();
        run.setId(cursor.getLong(0));
        run.setStartDate(cursor.getLong(1));
        run.setEndDate(cursor.getLong(2));
        run.setCalories(cursor.getInt(3));
        run.setSpeed(cursor.getFloat(4));
        run.setDistance(cursor.getFloat(5));
        run.setCoordinates(cursor.getString(6));
        run.setName(cursor.getString(7));
        return  run;
    }

}