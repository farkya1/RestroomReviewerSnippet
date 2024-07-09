import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SingleMarker implements ClusterItem {
    //The position of the SingleMarker
    private final LatLng position;

    private final int uID;

    //The title of the SIngleMarker
    private final String title;

    //The second title of the SingleMarker
    private String snippet;

    private int review = 0;

    private int numReviews = 0;

    //set of the rater accounts to see if rater has already rated
    private Set<String> raterAccounts = new HashSet<String>();

    //an arraylist of all the comment information belonging to the marker
    private ArrayList<CommentInformation> commentInfo = new ArrayList<CommentInformation>();


    /**
     * The constructor for the class
     * @param lat  the latitude of the marker
     * @param lng  the longitude of the marker
     * @param title   the title of the marker
     * @param snippet  the second title of the marker
     */
    public SingleMarker(double lat, double lng, String title, String snippet, int uID) {
        position = new LatLng(lat, lng);
        this.title = title;
        this.snippet = snippet;
        this.uID = uID;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }


    @Nullable
    @Override
    public Float getZIndex() {
        return 0f;
    }

    public void AddReview(int review){
        this.review += review;

        numReviews += 1;
        snippet = GetReview();
    }

    public String GetReview(){


        return snippet;
    }

    public void AddComment(String date, String author, String comment){
        commentInfo.add(new CommentInformation(date,author,comment));

    }

    public ArrayList<CommentInformation> GetCommentInfo(){
        return commentInfo;
    }

    public int GetuID(){
        return uID;
    }

}
