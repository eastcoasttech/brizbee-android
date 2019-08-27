package com.brizbee.android.client;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.brizbee.android.client.models.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PunchInConfirmActivity extends AppCompatActivity {
    double currentLatitude = 0.0;
    double currentLongitude = 0.0;
    private TextView textConfirmTask;
    private TextView textConfirmCustomer;
    private TextView textConfirmJob;
    private JSONObject task;
    private JSONObject job;
    private JSONObject customer;
    private LocationCallback locationCallback;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_punch_in_confirm);
        textConfirmTask = findViewById(R.id.textConfirmTask);
        textConfirmCustomer = findViewById(R.id.textConfirmCustomer);
        textConfirmJob = findViewById(R.id.textConfirmJob);

        try {
            task = new JSONObject(getIntent().getStringExtra("task"));
            job = task.getJSONObject("Job");
            customer = job.getJSONObject("Customer");

            // Task Number and Name
            textConfirmTask.setText(
                    String.format("%s - %s",
                            task.getString("Number"),
                            task.getString("Name"))
            );

            // Customer Number and Name
            textConfirmCustomer.setText(
                    String.format("%s - %s",
                            customer.getString("Number"),
                            customer.getString("Name"))
            );

            // Job Number and Name
            textConfirmJob.setText(
                    String.format("%s - %s",
                            job.getString("Number"),
                            job.getString("Name"))
            );
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Allows getting the last known location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    public void onCancelClick(View view) {
        final Intent intent = new Intent(this, StatusActivity.class);
        startActivity(intent);
        finish(); // prevents going back
    }

    public void onContinueClick(View view) {
        try {
//            // Recording current location is optional
//            User user = ((MyApplication) getApplication()).getUser();
//            if (user.getRequiresLocation()) {
//                getCurrentLocation();
////            fusedLocationClient.getLastLocation()
////                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
////                    @Override
////                    public void onSuccess(Location location) {
////                        // Got last known location. In some rare situations this can be null.
////                        if (location != null) {
////                            currentLongitude = location.getLongitude();
////                            currentLatitude = location.getLatitude();
////                            save();
////                        }
////                    }
////                });
//            } else {
//                save();
//            }

            save();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getCurrentLocation(){
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null /* Looper */);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        // Get the coordinates of the location
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        save();
                    }
                } else {
                    save();
                }
            };
        };
    }

    public void save() {
        final Intent intent = new Intent(this, StatusActivity.class);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Instantiate the RequestQueue
        String url = "https://brizbee.gowitheast.com/odata/Punches/Default.PunchIn";
        // Request a string response from the provided URL
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("TaskId", task.get("Id"));
            jsonBody.put("SourceForInAt", "Mobile");
            jsonBody.put("InAtTimeZone", "America/New_York");

            if (currentLatitude != 0.0 && currentLatitude != 0.0) {
                jsonBody.put("LatitudeForInAt", currentLatitude);
                jsonBody.put("LongitudeForInAt", currentLongitude);
            } else {
                jsonBody.put("LatitudeForInAt", "");
                jsonBody.put("LongitudeForInAt", "");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonBody, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        startActivity(intent);
                        finish();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String json = null;

                        NetworkResponse response = error.networkResponse;
                        if(response != null && response.data != null) {
                            switch(response.statusCode){
                                case 400:
//                            json = new String(response.data);
//                            json = trimMessage(json, "message");
//                            if(json != null) displayMessage(json);
                                    break;
                            }
                            builder.setMessage(new String(response.data))
                                    .setTitle(Integer.toString(response.statusCode))
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else {
                            builder.setMessage(error.toString())
                                    .setTitle("Oops")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");

                String authExpiration = ((MyApplication) getApplication()).getAuthExpiration();
                String authToken = ((MyApplication) getApplication()).getAuthToken();
                String authUserId = ((MyApplication) getApplication()).getAuthUserId();

                if (authExpiration != null && !authExpiration.isEmpty() &&
                        authToken != null && !authToken.isEmpty() &&
                        authUserId != null && !authUserId.isEmpty()) {
                    headers.put("AUTH_EXPIRATION", authExpiration);
                    headers.put("AUTH_TOKEN", authToken);
                    headers.put("AUTH_USER_ID", authUserId);
                }

                return headers;
            }
        };

        int socketTimeout = 10000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonRequest.setRetryPolicy(policy);

        // Add the request to the RequestQueue
        MySingleton.getInstance(this).addToRequestQueue(jsonRequest);
    }
}
