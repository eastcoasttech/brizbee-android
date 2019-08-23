package com.brizbee.android.client;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PunchInConfirmActivity extends AppCompatActivity {
    TextView textConfirmTask;
    TextView textConfirmCustomer;
    TextView textConfirmJob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_punch_in_confirm);
        textConfirmTask = findViewById(R.id.textConfirmTask);
        textConfirmCustomer = findViewById(R.id.textConfirmCustomer);
        textConfirmJob = findViewById(R.id.textConfirmJob);

        try {
            JSONObject task = new JSONObject(getIntent().getStringExtra("task"));
            JSONObject job = task.getJSONObject("Job");
            JSONObject customer = job.getJSONObject("Customer");

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
    }

    public void onCancelClick(View view) {
        final Intent intent = new Intent(this, StatusActivity.class);
        startActivity(intent);
        finish(); // prevents going back
    }

    public void save() {
        final Intent intent = new Intent(this, StatusActivity.class);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://brizbee.gowitheast.com/odata/Punches/Default.PunchIn";

        JsonObjectRequest jsonRequest = new JsonObjectRequest
                (Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray value = response.getJSONArray("value");

                            if (value.length() > 0) {
                                JSONObject first = value.getJSONObject(0);
                                startActivity(intent);
                            } else {
                                // Notify the user that the task number does not exist
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Not a valid task number, try again.", Toast.LENGTH_SHORT);
                                toast.show();
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
}
