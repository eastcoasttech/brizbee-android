package com.brizbee.Brizbee.Mobile;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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

public class PunchInConfirmActivity extends AppCompatActivity {
    private static final String TAG = "PunchInConfirmActivity";
    double currentLatitude = 0.0;
    double currentLongitude = 0.0;
    private TextView textConfirmTask;
    private TextView textConfirmCustomer;
    private TextView textConfirmJob;
    private JSONObject task;
    private JSONObject job;
    private JSONObject customer;
    private LocationCallback locationCallback;
    private Spinner spinnerTimeZone;
    private String[] timezonesIds;
    private String selectedTimeZone;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_punch_in_confirm);

        // Get references from layouts
        spinnerTimeZone = findViewById(R.id.spinnerTimeZone);
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
            showDialog("Could not display the task you selected, please go back and try again.");
        }

        // Allows getting the last known location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get timezones from application and configure spinner
        TimeZone[] timezones = ((MyApplication) getApplication()).getTimeZones();
        timezonesIds = new String[timezones.length];
        for (int i = 0; i < timezones.length; i++) {
            timezonesIds[i] = timezones[i].getId();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, timezonesIds);
        spinnerTimeZone.setAdapter(adapter);

        spinnerTimeZone.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                Log.i(TAG, "An item has been selected at position:");
                Log.i(TAG, String.valueOf(position));

                // Store selected item
                int sid = spinnerTimeZone.getSelectedItemPosition();
                selectedTimeZone = timezonesIds[sid];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
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

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }

                fusedLocationClient.requestLocationUpdates(locationRequest,
                        locationCallback,
                        null /* Looper */);
            } else {
                // Save because user is not required to have location
                save();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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
            jsonBody.put("SourceHardware", "Mobile");
            jsonBody.put("InAtTimeZone", selectedTimeZone);
            jsonBody.put("SourceOperatingSystem", "Android");
            jsonBody.put("SourceOperatingSystemVersion", Build.VERSION.RELEASE);
            jsonBody.put("SourceBrowser", "N/A");
            jsonBody.put("SourceBrowserVersion", "N/A");

            if (currentLatitude != 0.0 && currentLatitude != 0.0) {
                jsonBody.put("LatitudeForInAt", String.valueOf(currentLatitude));
                jsonBody.put("LongitudeForInAt", String.valueOf(currentLongitude));
            } else {
                jsonBody.put("LatitudeForInAt", "");
                jsonBody.put("LongitudeForInAt", "");
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
                        switch (response.statusCode)
                        {
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

    private void showDialog(String message)
    {
        // Build a dialog with the given message to show the user
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.dismiss();
                    }
                });
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
}
