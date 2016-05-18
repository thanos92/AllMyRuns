package dv606.gc222bz.finalproject.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


//hendle the creation of database and of the table
public class RunsDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "runs.db";
    private static final int DATABASE_VERSION = 1;
    public static final String COLUMN_ID ="_id";

    public static final String RUNS_TABLE_NAME ="my_runs";
    public static final String RUNS_DETAILS_TABLE_NAME ="my_runs_details";

    public static final String COLUMN_START_DATE ="start_date";
    public static final String COLUMN_END_DATE = "end_date";
    public static final String COLUMN_RUN_NAME = "run_name";
    public static final String COLUMN_DISTANCE_NAME = "distance";
    public static final String COLUMN_SPEED_NAME = "speed";
    public static final String COLUMN_COORDINATES_NAME = "coordinates";
    public static final String COLUMN_CALORIES_NAME = "calories";
    public static final String RUN_NAME_DEFAULT_VALUE = "My run";
    public static final String TIME_DETAILS_NAME = "time";
    public static final String ID_RUN_DETAILS_NAME = "id_run";
    public static final String TIME_NAME = "DIFF";


    private static final String RUNS_TABLE_CREATE = "create table " +
            RUNS_TABLE_NAME + " ("+ COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_RUN_NAME + " text NOT NULL DEFAULT '"+ RUN_NAME_DEFAULT_VALUE + "' , "+
            COLUMN_DISTANCE_NAME + " integer, "+
            COLUMN_COORDINATES_NAME + " text, " +
            COLUMN_CALORIES_NAME + " integer, "+
            COLUMN_SPEED_NAME + " real, "+
            COLUMN_START_DATE + " integer, " +
            COLUMN_END_DATE + " integer )";

    private static final String RUNS_DETAILS_TABLE_CREATE = "create table " +
            RUNS_DETAILS_TABLE_NAME
            + " ("+ COLUMN_ID + " integer primary key autoincrement, "+
            COLUMN_DISTANCE_NAME + " integer, "+
            ID_RUN_DETAILS_NAME + " integer, "+
            COLUMN_COORDINATES_NAME + " text, " +
            COLUMN_CALORIES_NAME + " integer, "+
            COLUMN_SPEED_NAME + " real, "+
            TIME_DETAILS_NAME + " integer )";


    public RunsDBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(RUNS_TABLE_CREATE);
        sqLiteDatabase.execSQL(RUNS_DETAILS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + RUNS_TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + RUNS_DETAILS_TABLE_CREATE);
        onCreate(sqLiteDatabase);
    }
}
