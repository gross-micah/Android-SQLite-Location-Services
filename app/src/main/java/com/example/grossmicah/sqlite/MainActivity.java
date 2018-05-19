package com.example.grossmicah.sqlite;

import android.*;
import android.Manifest;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.location.Location;
import android.content.pm.PackageManager;
import android.app.Dialog;
import android.support.v4.app.ActivityCompat;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


public class MainActivity extends AppCompatActivity implements
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private TextView mLatText;
    private TextView mLongText;
    private Location mLastLocation;
    private LocationListener mLocationListener;
    private static final int LOCATION_PERMISSION_RESULT = 17;
    Button submit;
    Cursor curse;
    SimpleCursorAdapter mSQLAdapter;
    private static final String TAG = "SQLActivity";
    SQLiteDatabase myDB;
    SQLiteExample mySQLite;
    public double longitude = -123.2;
    public double latitude = 44.5;

    //large onCreate to handle everything in one view. With significant reference to CS496 module 7 materials
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sqlite);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        //mLatText = (TextView) findViewById(R.id.lat_output);
        //mLongText = (TextView) findViewById(R.id.long_output);
        //mLatText.setText("Activity Created");
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        //use the listener to constantly keep class variables 'longitude' and 'latitude' up to date.
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    //mLongText.setText(String.valueOf(location.getLongitude()));
                    //mLatText.setText(String.valueOf(location.getLatitude()));
                    longitude = location.getLongitude();
                    latitude = location.getLatitude();
                } else {
                    //mLongText.setText("-123.2");
                    //mLatText.setText("44.5");
                    longitude = -123.2;
                    latitude = 44.5;
                }
            }
        };
        populateTable();
        mySQLite = new SQLiteExample(this);
        myDB = mySQLite.getWritableDatabase();
        submit = (Button) findViewById(R.id.sql_add_row_button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myDB != null){
                    ContentValues values = new ContentValues();
                    values.put(SQLiteContract.Table.COLUMN_NAME_INPUT, ((EditText)findViewById(R.id.user_input)).getText().toString());
                    values.put(SQLiteContract.Table.COLUMN_NAME_LONGITUDE, String.valueOf(longitude));
                    values.put(SQLiteContract.Table.COLUMN_NAME_LATITUDE, String.valueOf(latitude));
                    myDB.insert("sqlitetable",null,values);
                    populateTable();
                } else {
                    Log.d(TAG, "Unable to access database for writing.");
                }
            }
        });

    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //mLatText.setText("onConnect");
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_RESULT);
            //mLongText.setText("Lacking Permissions");
            return;
        }
        updateLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Dialog errDialog = GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0);
        errDialog.show();
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == LOCATION_PERMISSION_RESULT) {
            if (grantResults.length > 0) {
                updateLocation();
            }
        }
    }

    private void updateLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mLastLocation != null){
            longitude = mLastLocation.getLongitude();
            latitude = mLastLocation.getLatitude();
        } else {
            //LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,mLocationListener);
            longitude = -123.2;
            latitude = 44.5;
        }
    }

    //taken from initial layout in CS496 Module 7 materials and modified for input, long + lat
    private void populateTable(){
        if(myDB != null) {
            try {
                if(mSQLAdapter != null && mSQLAdapter.getCursor() != null){
                    if(!mSQLAdapter.getCursor().isClosed()){
                        mSQLAdapter.getCursor().close();
                    }
                }
                curse = myDB.query("sqlitetable", new String[]{SQLiteContract.Table._ID, SQLiteContract.Table.COLUMN_NAME_INPUT,
                                SQLiteContract.Table.COLUMN_NAME_LONGITUDE, SQLiteContract.Table.COLUMN_NAME_LATITUDE}, SQLiteContract.Table._ID + " > ?", new String[]{"0"}, null, null, null);
                ListView SQLListView = (ListView) findViewById(R.id.sql_list_view);
                mSQLAdapter = new SimpleCursorAdapter(this,
                        R.layout.db_items,
                        curse,
                        new String[]{SQLiteContract.Table.COLUMN_NAME_INPUT, SQLiteContract.Table.COLUMN_NAME_LONGITUDE, SQLiteContract.Table.COLUMN_NAME_LATITUDE},
                        new int[]{R.id.input_value, R.id.longitude_value, R.id.latitude_value},
                        0);
                SQLListView.setAdapter(mSQLAdapter);
            } catch (Exception e) {
                Log.d(TAG, "Error loading data from database");
            }
        }
    }
}

//used to help implement table creation from CS496 module 7 materials
class SQLiteExample extends SQLiteOpenHelper {

    public SQLiteExample(Context context) {
        super(context, SQLiteContract.Table.DB_NAME, null, SQLiteContract.Table.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQLiteContract.Table.SQL_CREATE_TABLE);

        ContentValues values = new ContentValues();
        values.put(SQLiteContract.Table.COLUMN_NAME_LONGITUDE, -123.2);
        values.put(SQLiteContract.Table.COLUMN_NAME_LATITUDE, 44.5);
        values.put(SQLiteContract.Table.COLUMN_NAME_INPUT, "Hello SQLite");
        db.insert("sqlitetable",null,values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQLiteContract.Table.SQL_DROP_TABLE);
        onCreate(db);
    }
}

//with help from https://developer.android.com/training/basics/data-storage/databases.html and CS496 module 7 materials
final class SQLiteContract {
    private SQLiteContract(){};

    public final class Table implements BaseColumns {
        public static final String DB_NAME = "sqlite_db";
        public static final String COLUMN_NAME_INPUT = "Input";
        public static final String COLUMN_NAME_LONGITUDE = "Longitude";
        public static final String COLUMN_NAME_LATITUDE = "Latitude";
        public static final int DB_VERSION = 4;


        public static final String SQL_CREATE_TABLE = "CREATE TABLE sqlitetable(" + Table._ID + " INTEGER PRIMARY KEY NOT NULL," +
                Table.COLUMN_NAME_INPUT + " VARCHAR(255)," +
                Table.COLUMN_NAME_LONGITUDE + " DOUBLE," + Table.COLUMN_NAME_LATITUDE + " DOUBLE);";

        public static final String SQL_TEST_TABLE_INSERT = "INSERT INTO sqlitetable(" + COLUMN_NAME_INPUT + "," + COLUMN_NAME_LONGITUDE + "," + COLUMN_NAME_LATITUDE + ") VALUES ('testvalue', 666, -999);";

        public  static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS sqlitetable";
    }
}
