import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.bushbaker.restroomreviewer.databinding.ActivityRestroomMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.Manifest.permission;
import android.annotation.SuppressLint;

import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Random;


public class RestroomMaps extends AppCompatActivity implements
        OnMapReadyCallback,
        OnMyLocationButtonClickListener,
        OnMyLocationClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMapClickListener,
        ClusterManager.OnClusterItemClickListener<SingleMarker> {
    // The GoogleMap object that handles displaying and interactions with the map.
    private GoogleMap mMap;
    // The binding object that will be used to access the views in the layout
    private ActivityRestroomMapsBinding binding;
    // A list of MarkerOptions that will be used to store the markers on the map
    public java.util.ArrayList<MarkerOptions> mMarkers = new java.util.ArrayList<MarkerOptions>();
    // For adding Restroom button
    private boolean isAddRestroomMode = false;

    // For Add Review Button marker
    private SingleMarker currentMarker;

    // The shared preferences object that will be used to store the markers in persistent storage.
    private SharedPreferences mSharedPref;
    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in {@link
     * #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean permissionDenied = false;
    /**
     * For getting the user's location information.
     */
    private FusedLocationProviderClient fusedLocationClient;

    private final float ZOOM_LEVEL_INIT = 16.0f;

    // Declare a variable for the cluster manager.
    private ClusterManager<SingleMarker> clusterManager;

    private ArrayList<SingleMarker> mClusters;

    /**
     * A pre-registered activity result launcher that will be used to add a restroom to the map.
     */
    ActivityResultLauncher<Intent> addRestroomFromResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        assert data != null;
                        String new_restroom = data.getStringExtra("new_restroom");
                        assert new_restroom != null;
                        String[] new_restroom_components = new_restroom.split(":");
                        String name = new_restroom_components[0];
                        float latitude = Float.parseFloat(new_restroom_components[1]);
                        float longitude = Float.parseFloat(new_restroom_components[2]);
                        MarkerOptions marker = new MarkerOptions().position(new LatLng(latitude, longitude)).title(name);
                        mMarkers.add(marker);
                        mMap.addMarker(marker);
                    }
                }
            });

    /**
     * Create the RestroomMaps activity.
     * @param savedInstanceState The saved instance state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (savedInstanceState != null) {
            mMarkers = savedInstanceState.getParcelableArrayList("markers");
        } else {
            // Grab from shared preferences
            mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            if (mSharedPref.contains("markers")) {
                String markers = mSharedPref.getString("markers", "");
                // Add the markers to the map
                if (!markers.equals("")) {
                    String[] markers_components = markers.split(";");
                    for (String marker : markers_components) {
                        String[] marker_components = marker.split(":");
                        String name = marker_components[0];
                        float latitude = Float.parseFloat(marker_components[1]);
                        float longitude = Float.parseFloat(marker_components[2]);
                        MarkerOptions new_marker = new MarkerOptions().position(new LatLng(latitude, longitude)).title(name);
                        mMarkers.add(new_marker);
                    }
                }
            }
        }

        // Get the binding object in order to access the views in the layout
        binding = ActivityRestroomMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get the intent that started this activity
        Intent intent = getIntent();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        AndroidNetworking.initialize(getApplicationContext());
    }

    /**
     * Destroy method to save the markers to the shared preferences.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = mSharedPref.edit();
        StringBuilder markerSaveString = new StringBuilder();
        for (MarkerOptions marker : mMarkers) {
            markerSaveString.append(marker.getTitle()).append(":").append(marker.getPosition().latitude).append(":").append(marker.getPosition().longitude).append(";");
        }
        editor.putString("markers", markerSaveString.toString());
        editor.apply();
        binding = null;
    }

    /**
     * Saves the instance in case the activity is destroyed or paused.
     * @param savedInstanceState The saved instance state.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        savedInstanceState.putParcelableArrayList("markers", mMarkers);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Restores the instance in case the activity is destroyed or paused.
     * @param savedInstanceState The saved instance state.
     */
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mMarkers = savedInstanceState.getParcelableArrayList("markers");
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sparty and move the camera to it.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        enableMyLocation();

        clusterManager = new ClusterManager<SingleMarker>(this, mMap);

        mMap.setOnCameraIdleListener(clusterManager);

        mMap.setOnMarkerClickListener(clusterManager);
        clusterManager.setOnClusterItemClickListener(this);

        //mMap.setOnMarkerClickListener(clusterManager);

        mClusters = new ArrayList<SingleMarker>();




        // Check if the "My Location" layer is enabled
        if (!mMap.isMyLocationEnabled()) {
            // Move the location to sparty
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(spartyStatue, ZOOM_LEVEL_INIT));
        }

        //Gets shared prefence information of the user password and username
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        String mUsername = sharedPref.getString("username", "");

        String mPassword = sharedPref.getString(mUsername, "");

        String authString = mUsername+":"+mPassword;
        String encodedString = "";

        //Checks what api the device to ensure it will work on the device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use java.util.Base64 for API 26 and above
            encodedString = java.util.Base64.getEncoder().encodeToString(authString.getBytes());
        } else {
            // Use android.util.Base64 for API 23 to 25
            encodedString = android.util.Base64.encodeToString(authString.getBytes(), Base64.DEFAULT);
        }
        encodedString = "Basic "+ encodedString;
        final String finalEncodedString = encodedString;

        //Thread that will pull all the markers and add them to the map
        Thread mapReadyThread = new Thread(() -> AndroidNetworking.get(this.getString(R.string.domain) + "/restroomreviewer/get_restrooms/index.php")
                .addHeaders("ngrok-skip-browser-warning", "true")
                .addHeaders("Authorization",finalEncodedString)

                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {


                        //iterate over all the markers
                        for(int i = 0; i < response.length(); i++){
                            try {
                                JSONObject bathroom = response.getJSONObject(i);
                                Double latitude = bathroom.getDouble("lat");
                                Double longitude = bathroom.getDouble("long");
                                String name = bathroom.getString("name");
                                float rating = (float)bathroom.getDouble("rating");
                                int uID = bathroom.getInt("uid");

                                mClusters.add(new SingleMarker(latitude,longitude,name,String.valueOf(rating),uID));



                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        clusterManager.addItems(mClusters);


                    }

                    @Override
                    public void onError(ANError anError) {

                        Toast.makeText(RestroomMaps.this, "Data Network Failure", Toast.LENGTH_LONG).show();
                    }
                }));

        mapReadyThread.start();

    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        // 1. Check if permissions are granted, if so, enable the my location layer
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                LatLng currPos = new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currPos, ZOOM_LEVEL_INIT));

                            }
                        }
                    });
            return;
        }

        // 2. Otherwise, request location permissions from the user.
        PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true);
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        // Do we need this?
        // Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION) || PermissionUtils
                .isPermissionGranted(permissions, grantResults,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Permission was denied. Display an error message
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true;
        }
    }



    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            permissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    /**
     * Add a restroom to the map. This will place a marker on the map at the user's current location
     * or a passed in location.
     * @param view The view that was clicked, in this case, our add restroom button.
     */
    public void addRestroom(android.view.View view) {
        Intent intent = new Intent(this, AddRestroom.class);
        addRestroomFromResult.launch(intent);
    }

    public void onAddRestroomButtonClick(View view) {
        isAddRestroomMode = true;
        Toast.makeText(this, "Tap on the map to add a restroom", Toast.LENGTH_SHORT).show();
    }

    //caches the marker to see if you click twice which means you can leave a review
    private SingleMarker prevMarker;

    /**
     * called when the user clicks the marker
     * @param item the item clicked
     *
     * @return if false will display the title
     */
    @Override
    public boolean onClusterItemClick(SingleMarker item) {
        if(prevMarker != null && prevMarker == item) {
            prevMarker = null;
            launchLookReviews(item);
            return true;
        } else {
            prevMarker = item;
            currentMarker = item;
            showAddReviewButton();
            return false;
        }
    }

    /**
     * Show add review button when location is clicked.
     */
    private void showAddReviewButton() {
        Button addReviewButton = findViewById(R.id.add_review_button);
        addReviewButton.setVisibility(View.VISIBLE);
        addReviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentMarker != null) {
                    launchLookReviews(currentMarker);
                }
                view.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Launch reviews when pin is clicked.
     * @param item restroom marker
     */
    private void launchLookReviews(SingleMarker item) {
        Intent intent = new Intent(RestroomMaps.this, LookReviews.class);
        Bundle bundle = new Bundle();
        bundle.putString("RestroomTitle", item.getTitle());
        bundle.putString("BathroomRating", item.GetReview());
        bundle.putInt("BathroomUID", item.GetuID());
        bundle.putSerializable("CommentInfo", item.GetCommentInfo());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * whens when the user long clicks the map
     * @param latLng the location on the map
     */
    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        if(prevMarker != null){
            prevMarker = null;
        }

        Intent intent = new Intent(this, AddRestroom.class);
        Bundle bundle = new Bundle();
        bundle.putDouble("ClickLatitude",latLng.latitude);
        bundle.putDouble("ClickLongitude",latLng.longitude);
        intent.putExtras(bundle);

        startActivity(intent);

    }


    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        if(prevMarker != null){
            prevMarker = null;
        }
        Button addReviewButton = findViewById(R.id.add_review_button);
        addReviewButton.setVisibility(View.GONE);
        currentMarker = null;

        // Check if the plus button has been selected
        if (isAddRestroomMode) {
            Intent intent = new Intent(this, AddRestroom.class);
            Bundle bundle = new Bundle();
            bundle.putDouble("ClickLatitude",latLng.latitude);
            bundle.putDouble("ClickLongitude",latLng.longitude);
            intent.putExtras(bundle);

            startActivity(intent);

            isAddRestroomMode = false;
        }


    }
}
