package com.brizbee.android.client;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class StatusActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        loadStatus();
    }

    public void loadStatus() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://brizbee.gowitheast.com/odata/Punches/Default.Current?$expand=Task($expand=Job($expand=Customer))";

        JsonObjectRequest jsonRequest = new JsonObjectRequest
            (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        TextView textStatus = findViewById(R.id.textStatus);
                        TextView textTask = findViewById(R.id.textTask);
                        TextView textTaskHeader = findViewById(R.id.textTaskHeader);
                        TextView textCustomer = findViewById(R.id.textCustomer);
                        TextView textCustomerHeader = findViewById(R.id.textCustomerHeader);
                        TextView textJob = findViewById(R.id.textJob);
                        TextView textJobHeader = findViewById(R.id.textJobHeader);
                        TextView textSince = findViewById(R.id.textSince);
                        Button buttonPunchOut = findViewById(R.id.buttonPunchOut);

                        JSONArray value = response.getJSONArray("value");

                        if (value.length() > 0) {
                            JSONObject first = value.getJSONObject(0);
                            JSONObject task = first.getJSONObject("Task");
                            JSONObject job = task.getJSONObject("Job");
                            JSONObject customer = job.getJSONObject("Customer");

                            // Set color and text of status
                            textStatus.setTextColor(getResources().getColor(R.color.colorGreen));
                            textStatus.setText("You are PUNCHED IN");

                            // Task Number and Name
                            textTask.setText(
                                    String.format("%s - %s",
                                            task.getString("Number"),
                                            task.getString("Name"))
                            );

                            // Customer Number and Name
                            textCustomer.setText(
                                    String.format("%s - %s",
                                            customer.getString("Number"),
                                            customer.getString("Name"))
                            );

                            // Job Number and Name
                            textJob.setText(
                                    String.format("%s - %s",
                                            job.getString("Number"),
                                            job.getString("Name"))
                            );
                        } else {
                            // Set color and text of status
                            textStatus.setTextColor(getResources().getColor(R.color.colorRed));
                            textStatus.setText("You are PUNCHED OUT");

                            textCustomer.setVisibility(View.GONE);
                            textCustomerHeader.setVisibility(View.GONE);
                            textJob.setVisibility(View.GONE);
                            textJobHeader.setVisibility(View.GONE);
                            textTask.setVisibility(View.GONE);
                            textTaskHeader.setVisibility(View.GONE);
                            textSince.setVisibility(View.GONE);
                            buttonPunchOut.setVisibility(View.GONE);
                        }
                    } catch (JSONException e) {
                        builder.setMessage(e.toString())
                                .setTitle("Error")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
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

        // Add the request to the RequestQueue.
        queue.add(jsonRequest);
    }

    public void onPunchInClick(View view) {
        final Intent intent = new Intent(this, PunchInTaskIdActivity.class);
        startActivity(intent);
    }

    public void onPunchOutClick(View view) {
        final Intent intent = new Intent(this, PunchOutConfirmActivity.class);
        startActivity(intent);
    }

    public void logout(View view) {
        final Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish(); // prevents back
    }
}
