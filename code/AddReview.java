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

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONException;
import org.json.JSONObject;

public class AddReview extends AppCompatActivity {


    private int restroomUID;
    /**
     * Create the Add restroom activity.
     * @param savedInstanceState The instance to restore, if created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load mMap from the saved instance state
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_review);
        Bundle bundle = getIntent().getExtras();
        String restroomTitle = bundle.getString("RestroomTitle");
        restroomUID = bundle.getInt("RestroomUID");
        TextView restroomName = (TextView)findViewById(R.id.restroom_name);
        restroomName.setText(restroomTitle);



    }

    /**
     * Submission for the new restroom. This method will ensure that the entries are valid.
     * If they have issues, a toast message will provided to help assist the user.
     * If they are valid, then the new restroom will be added to the map.
     */
    public void submitReview(View view) {

        //gets the correct username and password
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        String mUsername = sharedPref.getString("username", "");

        String mPassword = sharedPref.getString(mUsername, "");

        String authString = mUsername+":"+mPassword;
        String encodedString = "";

        //ensured api will work
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use java.util.Base64 for API 26 and above
            encodedString = java.util.Base64.getEncoder().encodeToString(authString.getBytes());
        } else {
            // Use android.util.Base64 for API 23 to 25
            encodedString = android.util.Base64.encodeToString(authString.getBytes(), Base64.DEFAULT);
        }
        encodedString = "Basic "+ encodedString;
        final String finalEncodedString = encodedString;

        EditText editText = (EditText) findViewById(R.id.review_comment);
        RatingBar ratingBar = (RatingBar) findViewById(R.id.ratingBar);

        //the json for the post request
        JSONObject requestBodyReview = new JSONObject();
        try {
            requestBodyReview.put("uid", restroomUID);
            requestBodyReview.put("email", mUsername);
            requestBodyReview.put("text", editText.getText());
            requestBodyReview.put("rating", ratingBar.getRating());
        } catch (JSONException e) {
            e.printStackTrace();
        }



        //thread to post the new review
        Thread addReviewThread = new Thread(() -> AndroidNetworking.post(this.getString(R.string.domain) + "/restroomreviewer/create_review/index.php")
                .addHeaders("ngrok-skip-browser-warning", "true")
                .addHeaders("Authorization",finalEncodedString)
                .addJSONObjectBody(requestBodyReview)

                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //review got added
                        Intent data = new Intent(AddReview.this, RestroomMaps.class);

                        startActivity(data);


                    }

                    @Override
                    public void onError(ANError anError) {

                        Toast.makeText(AddReview.this, "Data Network Failure", Toast.LENGTH_LONG).show();
                    }
                }));

        addReviewThread.start();


    }
}
