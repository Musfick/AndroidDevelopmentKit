import android.annotation.SuppressLint;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class EasyLocationProvider implements LifecycleObserver {
    
//    Add dependency in build.gradle (Module:app)
//    implementation 'com.google.android.gms:play-services-location:16.0.0' 
    
//    Declare Necessary Permission in Manifest
//    <uses-permission android:name="android.permission.INTERNET"/>
//    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
//    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
      
//    How to use
//    In Activity
    
//    EasyLocationProvider easyLocationProvider; //Declare Global Variable
//    easyLocationProvider = new EasyLocationProvider.Builder(BottomNavigationActivity.this)
//                .setInterval(5000)
//                .setFastestInterval(2000)
//                //.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//                .setListener(new EasyLocationProvider.EasyLocationCallback() {
//                    @Override
//                    public void onGoogleAPIClient(GoogleApiClient googleApiClient, String message) {
//                        Log.e("EasyLocationProvider","onGoogleAPIClient: "+message);
//                    }
//
//                    @Override
//                    public void onLocationUpdated(double latitude, double longitude) {
//                        Log.e("EasyLocationProvider","onLocationUpdated:: "+ "Latitude: "+latitude+" Longitude: "+longitude);
//                    }
//
//                    @Override
//                    public void onLocationUpdateRemoved() {
//                        Log.e("EasyLocationProvider","onLocationUpdateRemoved");
//                    }
//                }).build();
//
//    getLifecycle().addObserver(easyLocationProvider);
    
//    Remove Location Update Callback
//            @Override
//        protected void onDestroy() {
//            easyLocationProvider.removeUpdates();
//            getLifecycle().removeObserver(easyLocationProvider);
//            super.onDestroy();
//        }

    private EasyLocationCallback callback;
    private Context context;
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private LocationSettingsRequest mLocationSettingsRequest;
    private long interval;
    private long fastestInterval;
    private int priority;
    private double Latitude = 0.0, Longitude = 0.0;

    private EasyLocationProvider(final Builder builder) {
        context = builder.context;
        callback = builder.callback;
        interval = builder.interval;
        fastestInterval = builder.fastestInterval;
        priority = builder.priority;
    }

    @SuppressLint("MissingPermission")
    public void requestLocationUpdate() {
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private void connectGoogleClient() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int resultCode = googleAPI.isGooglePlayServicesAvailable(context);
        if (resultCode == ConnectionResult.SUCCESS) {
            mGoogleApiClient.connect();
        } else {
            int REQUEST_GOOGLE_PLAY_SERVICE = 988;
            googleAPI.getErrorDialog((AppCompatActivity) context, resultCode, REQUEST_GOOGLE_PLAY_SERVICE);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private void onCreateLocationProvider() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private void onLocationResume() {
        buildGoogleApiClient();
    }

    @SuppressLint("MissingPermission")
    private synchronized void buildGoogleApiClient() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mSettingsClient = LocationServices.getSettingsClient(context);

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        callback.onGoogleAPIClient(mGoogleApiClient, "Connected");

                        mLocationRequest = new LocationRequest();
                        mLocationRequest.setInterval(interval);
                        mLocationRequest.setFastestInterval(fastestInterval);
                        mLocationRequest.setPriority(priority);
                        mLocationRequest.setSmallestDisplacement(0);

                        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
                        builder.addLocationRequest(mLocationRequest);
                        builder.setAlwaysShow(true);
                        mLocationSettingsRequest = builder.build();

                        mSettingsClient
                                .checkLocationSettings(mLocationSettingsRequest)
                                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                                    @Override
                                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                                        showLog("GPS is Enabled Requested Location Update");
                                        requestLocationUpdate();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                int statusCode = ((ApiException) e).getStatusCode();
                                switch (statusCode) {
                                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                        try {
                                            int REQUEST_CHECK_SETTINGS = 214;
                                            ResolvableApiException rae = (ResolvableApiException) e;
                                            rae.startResolutionForResult((AppCompatActivity) context, REQUEST_CHECK_SETTINGS);
                                        } catch (IntentSender.SendIntentException sie) {
                                            showLog("Unable to Execute Request");
                                        }
                                        break;
                                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                        showLog("Location Settings are Inadequate, and Cannot be fixed here. Fix in Settings");
                                }
                            }
                        }).addOnCanceledListener(new OnCanceledListener() {
                            @Override
                            public void onCanceled() {
                                showLog("onCanceled");
                            }
                        });
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        connectGoogleClient();
                        callback.onGoogleAPIClient(mGoogleApiClient, "Connection Suspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        callback.onGoogleAPIClient(mGoogleApiClient, "" + connectionResult.getErrorCode() + " " + connectionResult.getErrorMessage());
                    }
                })
                .addApi(LocationServices.API)
                .build();

        connectGoogleClient();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Latitude = locationResult.getLastLocation().getLatitude();
                Longitude = locationResult.getLastLocation().getLongitude();

                if (Latitude == 0.0 && Longitude == 0.0) {
                    showLog("New Location Requested");
                    requestLocationUpdate();
                } else {
                    callback.onLocationUpdated(Latitude, Longitude);
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    public void removeUpdates() {
        try {
            callback.onLocationUpdateRemoved();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showLog(String message) {
        Log.e("EasyLocationProvider", "" + message);
    }

    public interface EasyLocationCallback {
        void onGoogleAPIClient(GoogleApiClient googleApiClient, String message);

        void onLocationUpdated(double latitude, double longitude);

        void onLocationUpdateRemoved();
    }

    public static class Builder {
        private Context context;
        private EasyLocationCallback callback;
        private long interval = 10 * 1000;
        private long fastestInterval = 5 * 1000;
        private int priority = LocationRequest.PRIORITY_HIGH_ACCURACY;

        public Builder(Context context) {
            this.context = context;
        }

        public EasyLocationProvider build() {
            if (callback == null) {
                Toast.makeText(context, "EasyLocationCallback listener can not be null", Toast.LENGTH_SHORT).show();
            }

            return new EasyLocationProvider(this);
        }

        public Builder setListener(EasyLocationCallback callback) {
            this.callback = callback;
            return this;
        }

        public Builder setInterval(long interval) {
            this.interval = interval;
            return this;
        }

        public Builder setFastestInterval(int fastestInterval) {
            this.fastestInterval = fastestInterval;
            return this;
        }

        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }
    }
}
