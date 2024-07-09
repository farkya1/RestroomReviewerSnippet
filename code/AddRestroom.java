import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AddRestroom extends AppCompatActivity {

    // the latitude and longitude of where the user clicks
    private LatLng latLng;

    private JSONObject requestBodyReview;
    private String finalEncodedString;

    /**
     * Create the Add restroom activity.
     * @param savedInstanceState The instance to restore, if created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load mMap from the saved instance state
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_restroom);

        Bundle bundle = getIntent().getExtras();
        Double latitude = bundle.getDouble("ClickLatitude");
        Double longitude = bundle.getDouble("ClickLongitude");
        latLng = new LatLng(latitude,longitude);


    }

    /**
     * Submission for the new restroom. This method will ensure that the entries are valid.
     * If they have issues, a toast message will provided to help assist the user.
     * If they are valid, then the new restroom will be added to the map.
     */
    public void submitRestroom(View view) {


        //Gets the shared preferences for username and password
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        String mUsername = sharedPref.getString("username", "");

        String mPassword = sharedPref.getString(mUsername, "");

        String authString = mUsername+":"+mPassword;
        String encodedString = "";

        //ensures the api will work in all versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use java.util.Base64 for API 26 and above
            encodedString = java.util.Base64.getEncoder().encodeToString(authString.getBytes());
        } else {
            // Use android.util.Base64 for API 23 to 25
            encodedString = android.util.Base64.encodeToString(authString.getBytes(), Base64.DEFAULT);
        }
        encodedString = "Basic "+ encodedString;
        finalEncodedString = encodedString;

        EditText editRestroomName = (EditText) findViewById(R.id.new_restroom_title);

        if(editRestroomName.getText().toString().isEmpty()){
            Toast.makeText(AddRestroom.this, "Enter Restroom Title", Toast.LENGTH_LONG).show();
            return;
        }

        EditText editComment = (EditText) findViewById(R.id.review_comment);

        RatingBar ratingBar = (RatingBar) findViewById(R.id.ratingBar);

        //the post request for the new bathroom
        JSONObject requestBodyRestroom = new JSONObject();
        try {
            requestBodyRestroom.put("lat", latLng.latitude);
            requestBodyRestroom.put("long", latLng.longitude);
            requestBodyRestroom.put("name", editRestroomName.getText());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //the post request for the new review
        requestBodyReview = new JSONObject();
        try {
            requestBodyReview.put("text", editComment.getText());
            requestBodyReview.put("rating", ratingBar.getRating());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //thread to add the new review
        Thread addReviewThread = new Thread(() -> AndroidNetworking.post(this.getString(R.string.domain) + "/restroomreviewer/create_review/index.php")
                .addHeaders("ngrok-skip-browser-warning", "true")
                .addHeaders("Authorization",finalEncodedString)
                .addJSONObjectBody(requestBodyReview)

                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {


                        Intent data = new Intent(AddRestroom.this, RestroomMaps.class);

                        startActivity(data);


                    }

                    @Override
                    public void onError(ANError anError) {

                        Toast.makeText(AddRestroom.this, anError.getErrorBody(), Toast.LENGTH_LONG).show();
                    }
                }));

        //thread to make new restrooms
        Thread addRestroomThread = new Thread(() -> AndroidNetworking.post(this.getString(R.string.domain) + "/restroomreviewer/create_restroom/index.php")
                .addHeaders("ngrok-skip-browser-warning", "true")
                .addHeaders("Authorization",finalEncodedString)
                .addJSONObjectBody(requestBodyRestroom)


                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            //gets the newly created restroom id for a new review
                            requestBodyReview.put("uid", response.getInt("uid"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        addReviewThread.start();




                    }

                    @Override
                    public void onError(ANError anError) {

                        Toast.makeText(AddRestroom.this, "Data Network Failure", Toast.LENGTH_LONG).show();
                    }
                }));

        addRestroomThread.start();
    }
}
