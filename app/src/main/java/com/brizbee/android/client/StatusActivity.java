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
import com.brizbee.android.client.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StatusActivity extends AppCompatActivity {
    private TextView textHello;
    private TextView textStatus;
    private TextView textTask;
    private TextView textTaskHeader;
    private TextView textCustomer;
    private TextView textCustomerHeader;
    private TextView textJob;
    private TextView textJobHeader;
    private TextView textSince;
    private TextView textSinceHeader;
    private TextView textTimeZone;
    private Button buttonPunchOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        // Get references from layouts
        textHello = findViewById(R.id.textHello);
        textStatus = findViewById(R.id.textStatus);
        textTask = findViewById(R.id.textTask);
        textTaskHeader = findViewById(R.id.textTaskHeader);
        textCustomer = findViewById(R.id.textCustomer);
        textCustomerHeader = findViewById(R.id.textCustomerHeader);
        textJob = findViewById(R.id.textJob);
        textJobHeader = findViewById(R.id.textJobHeader);
        textSince = findViewById(R.id.textSince);
        textSinceHeader = findViewById(R.id.textSinceHeader);
        textTimeZone = findViewById(R.id.textTimeZone);
        buttonPunchOut = findViewById(R.id.buttonPunchOut);

        loadUser();
        loadStatus();
    }

    public void loadStatus() {
        // Instantiate the RequestQueue
        String url ="https://brizbee.gowitheast.com/odata/Punches/Default.Current?$expand=Task($expand=Job($expand=Customer))";

        JsonObjectRequest jsonRequest = new JsonObjectRequest
            (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        // Format for parsing timestamps from server
                        DateFormat dfServer = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
                        DateFormat dfHuman = new SimpleDateFormat("MMM dd, yyyy h:mma", Locale.ENGLISH);

                        JSONArray value = response.getJSONArray("value");

                        if (value.length() > 0) {
                            JSONObject first = value.getJSONObject(0);
                            JSONObject task = first.getJSONObject("Task");
                            JSONObject job = task.getJSONObject("Job");
                            JSONObject customer = job.getJSONObject("Customer");

                            // Set color and text of status
                            textStatus.setTextColor(getResources().getColor(R.color.colorGreenDark));
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

                            // Format the since timestamp
                            Date since = dfServer.parse(first.getString("InAt"));
                            textSince.setText(dfHuman.format(since));
                            textTimeZone.setText(first.getString("InAtTimeZone"));
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
                            textSinceHeader.setVisibility(View.GONE);
                            textTimeZone.setVisibility(View.GONE);
                            buttonPunchOut.setVisibility(View.GONE);
                        }
                    } catch (JSONException e) {
                        showDialog("Could not understand the response from the server, please try again.");
                    } catch (ParseException e) {
                        showDialog("Could not understand the response from the server, please try again.");
                    }
                }
            },
                    new Response.ErrorListener()
                    {
                @Override
                public void onErrorResponse(VolleyError error)
                {
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
            public Map<String, String> getHeaders() throws AuthFailureError
            {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");

                String authExpiration = ((MyApplication) getApplication()).getAuthExpiration();
                String authToken = ((MyApplication) getApplication()).getAuthToken();
                String authUserId = ((MyApplication) getApplication()).getAuthUserId();

                if (authExpiration != null && !authExpiration.isEmpty() &&
                        authToken != null && !authToken.isEmpty() &&
                        authUserId != null && !authUserId.isEmpty())
                {
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

    public void loadUser() {
        User user = ((MyApplication) getApplication()).getUser();

        if (user != null) {
            textHello.setText(String.format("Hello, %s", user.getName()));
        }
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
