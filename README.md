# Restroom Reviewer Snippet

Here are code snippets from the restroom reviewer along with some gifs of the appliation in action.


## Files

1. **AddRestroom.java**: This code defines an AddRestroom activity in an Android app where users can add new restrooms to a map-based application. It handles user input validation for restroom details and reviews, uses SharedPreferences for authentication, and makes HTTP POST requests using AndroidNetworking to a server API for adding new restrooms and reviews asynchronously.
   
2. **AddReview.java**: This code defines an AddReview activity in an Android app where users can submit reviews for specific restrooms. It retrieves user authentication credentials from SharedPreferences, validates user input for the review comment and rating, and makes an asynchronous HTTP POST request using AndroidNetworking to a server API for adding new reviews.
   
3. **RestroomMaps.java**: This code defines an RestroomMaps activity in an Android app that integrates Google Maps functionality. It includes features such as displaying markers for restrooms retrieved from a server, adding new restrooms via long-click and button actions, handling user location permissions, clustering markers using ClusterManager, and allowing users to add reviews and view existing reviews for restrooms. It also manages user authentication using SharedPreferences and makes asynchronous HTTP requests using AndroidNetworking for data retrieval and storage.
   
4. **SingleMarker.java**: The SingleMarker class encapsulates a map marker's position, title, and additional details, implementing the ClusterItem interface. It manages review scores, comments, and unique identifiers (uID) for each marker on the map.


## Demo

![Demo](workingReviewer.gif)
