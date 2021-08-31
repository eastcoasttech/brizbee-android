//
//  PunchOutConfirmActivity.java
//  BRIZBEE Mobile for Android
//
//  Copyright © 2021 East Coast Technology Services, LLC
//
//  This file is part of BRIZBEE Mobile for Android.
//
//  BRIZBEE Mobile for Android is free software: you can redistribute
//  it and/or modify it under the terms of the GNU General Public
//  License as published by the Free Software Foundation, either
//  version 3 of the License, or (at your option) any later version.
//
//  BRIZBEE Mobile for Android is distributed in the hope that it will
//  be useful, but WITHOUT ANY WARRANTY; without even the implied
//  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with BRIZBEE Mobile for Android.
//  If not, see <https://www.gnu.org/licenses/>.
//

package com.brizbee.Brizbee.Mobile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.brizbee.Brizbee.Mobile.models.TimeZone;
import com.brizbee.Brizbee.Mobile.models.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PunchOutConfirmActivity extends AppCompatActivity {
    double currentLatitude = 0.0;
    double currentLongitude = 0.0;
    private LocationCallback locationCallback;
    private Spinner spinnerTimeZone;
    private String[] timezonesIds;
    private String selectedTimeZone;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_punch_out_confirm);

        // Get references from layouts
        spinnerTimeZone = findViewById(R.id.spinnerTimeZone);

        // Allows getting the last known location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get timezones from application and configure spinner
        TimeZone[] timezones = ((MyApplication) getApplication()).getTimeZones();
        timezonesIds = new String[timezones.length];
        for (int i = 0; i < timezones.length; i++) {
            timezonesIds[i] = timezones[i].getId();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, timezonesIds);

        spinnerTimeZone.setAdapter(adapter);

        spinnerTimeZone.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                // Store selected item
                int sid = spinnerTimeZone.getSelectedItemPosition();
                selectedTimeZone = timezonesIds[sid];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        // Set selected item to be the user's time zone
        User user = ((MyApplication) getApplication()).getUser();
        for (int i = 0; i < timezonesIds.length; i++) {
            if (timezonesIds[i].equalsIgnoreCase(user.getTimeZone())) {
                spinnerTimeZone.setSelection(i);
            }
        }
    }

    public void onCancelClick(View view) {
        final Intent intent = new Intent(this, StatusActivity.class);
        startActivity(intent);
        finish(); // prevents going back
    }

    public void onContinueClick(View view) {
        try {
            // Recording current location is optional
            User user = ((MyApplication) getApplication()).getUser();
            if (user.getRequiresLocation()) {
                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setInterval(10000);
                locationRequest.setFastestInterval(5000);
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult != null) {
                            Location location = locationResult.getLocations().get(0);

                            // Get the coordinates of the location
                            currentLatitude = location.getLatitude();
                            currentLongitude = location.getLongitude();

                            // Stop getting location updates
                            fusedLocationClient.removeLocationUpdates(locationCallback);

                            // Save with the location
                            save();
                        } else {
                            // Save without the location
                            save();
                        }
                    }

                    ;
                };

                fusedLocationClient.requestLocationUpdates(locationRequest,
                        locationCallback,
                        null /* Looper */);
            } else {
                // Save because user is not required to have location
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        final Intent intent = new Intent(this, StatusActivity.class);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Instantiate the RequestQueue
        String url = "https://app-brizbee-prod.azurewebsites.net/odata/Punches/Default.PunchOut";

        // Request a string response from the provided URL
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("SourceHardware", "Mobile");
            jsonBody.put("OutAtTimeZone", selectedTimeZone);
            jsonBody.put("SourceOperatingSystem", "Android");
            jsonBody.put("SourceOperatingSystemVersion", Build.VERSION.RELEASE);
            jsonBody.put("SourceBrowser", "N/A");
            jsonBody.put("SourceBrowserVersion", "N/A");

            if (currentLatitude != 0.0 && currentLatitude != 0.0) {
                jsonBody.put("LatitudeForOutAt", String.valueOf(currentLatitude));
                jsonBody.put("LongitudeForOutAt", String.valueOf(currentLongitude));
            } else {
                jsonBody.put("LatitudeForOutAt", "");
                jsonBody.put("LongitudeForOutAt", "");
            }
        } catch (JSONException e) {
            showDialog("Could not prepare the request to the server, please try again.");
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
                        NetworkResponse response = error.networkResponse;
                        switch (response.statusCode) {
                            default:
                                showDialog("Could not reach the server, please try again.");
                                break;
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

    private void showDialog(String message) {
        // Build a dialog with the given message to show the user
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
}
