package com.brizbee.android.client;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.brizbee.android.client.models.TimeZone;
import com.brizbee.android.client.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PunchOutConfirmActivity extends AppCompatActivity {
    private Spinner spinnerTimeZone;
    private String[] timezonesIds;
    private String selectedTimeZone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_punch_out_confirm);

        // Get references from layouts
        spinnerTimeZone = findViewById(R.id.spinnerTimeZone);

        // Get timezones from application and configure spinner
        TimeZone[] timezones = ((MyApplication) getApplication()).getTimeZones();
        timezonesIds = new String[timezones.length];
        for (int i = 0; i < timezones.length; i++) {
            timezonesIds[i] = timezones[i].getId();
        }

        ArrayAdapter<String> adapter= new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, timezonesIds);

        spinnerTimeZone.setAdapter(adapter);

        spinnerTimeZone.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                // Store selected item
                int sid= spinnerTimeZone.getSelectedItemPosition();
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
        save();
    }

    public void save() {
        final Intent intent = new Intent(this, StatusActivity.class);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Instantiate the RequestQueue
        String url = "https://brizbee.gowitheast.com/odata/Punches/Default.PunchOut";

        // Request a string response from the provided URL
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("SourceForOutAt", "Mobile");
            jsonBody.put("OutAtTimeZone", selectedTimeZone);
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
