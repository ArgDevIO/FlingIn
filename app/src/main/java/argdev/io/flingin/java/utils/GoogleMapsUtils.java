package argdev.io.flingin.java.utils;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.Distance;
import com.google.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;

import argdev.io.flingin.R;

public class GoogleMapsUtils extends AppCompatActivity {

    private static final String TAG = "GoogleMapsUtils_Debug";

    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;
    private static final int PROXIMITY_RADIUS = 5000;

    //Maps
    private GoogleMap mMap;
    private Boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private BroadcastReceiver broadcastReceiver;
    private LatLng currentLocationLtdLnd;
    private GeoApiContext mGeoApiContext;

    private Distance mDistance;

    private Activity activity;

    public GoogleMapsUtils(Activity activity) {
        this.activity = activity;
    }

    public LatLng getCurrentLocationLtdLnd() {
        return currentLocationLtdLnd;
    }

    public void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the device current location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);

        try {
            if (mLocationPermissionGranted) {
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Location currentLocation = (Location) task.getResult();
                        double ltd = currentLocation.getLatitude();
                        double lnd = currentLocation.getLongitude();

                        currentLocationLtdLnd = new LatLng(ltd, lnd);
                    }
                });
            } else {
                Log.d(TAG, "onComplete: current location is null");
            }
        } catch (SecurityException e) {
            Log.d(TAG, "getDeviceLocation: SecurityException " + e.getMessage());
        }
    }

    public void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};

        if (isLocationEnabled(activity)) {
            Log.d(TAG, "getLocationPermission: isEnabled? " + isLocationEnabled(activity));
            if (ContextCompat
                    .checkSelfPermission(
                            activity, FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                if (mGeoApiContext == null) {
                    mGeoApiContext = new GeoApiContext.Builder()
                            .apiKey(activity.getResources().getString(R.string.google_maps_key))
                            .build();
                }
            } else {
                ActivityCompat.requestPermissions(activity,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            Toast.makeText(activity, "Please, enable location services!", Toast.LENGTH_LONG).show();
            Intent locationSettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivity(locationSettings);
            activity.finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        mLocationPermissionGranted = false;
                        Log.d(TAG, "onRequestPermissionsResult: permission failed!");
                        return;
                    }
                }
                mLocationPermissionGranted = true;
                Log.d(TAG, "onRequestPermissionsResult: permission granted!");
            }
        }
    }

    private boolean isLocationEnabled(Context context) {
        int locationMode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            locationMode = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.LOCATION_MODE,
                    0
            );
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }
        return false;
    }

    public void calculateDirections(double latitude, double longitude, com.google.android.gms.maps.model.LatLng current){
        Log.d(TAG, "calculateDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                latitude,
                longitude
        );

        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

        directions.alternatives(true);
        if (current != null) {
            Log.d(TAG, "calculateDirections: " + current);
            directions.origin(new LatLng(current.latitude, current.longitude));
        } else {
            Log.d(TAG, "calculateDirections: " + currentLocationLtdLnd);
            directions.origin(currentLocationLtdLnd);
        }

        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
                mDistance = result.routes[0].legs[0].distance;
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage());
            }
        });
    }

    public Distance getmDistance() {
        return mDistance;
    }
}
