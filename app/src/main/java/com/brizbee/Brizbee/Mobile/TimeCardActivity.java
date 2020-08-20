package com.brizbee.Brizbee.Mobile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import com.brizbee.Brizbee.Mobile.models.Customer;
import com.brizbee.Brizbee.Mobile.models.Job;
import com.brizbee.Brizbee.Mobile.models.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TimeCardActivity extends AppCompatActivity {
    private static final String TAG = "TimeCardActivity";
    private EditText editDate;
    private Spinner spinnerMCustomer;
    private Spinner spinnerMJob;
    private Spinner spinnerMTask;
    private Spinner spinnerHours;
    private Spinner spinnerMinutes;
    private List<Integer> hours = new ArrayList<>();
    private List<Integer> minutes = new ArrayList<>();
    private Customer selectedCustomer;
    private Job selectedJob;
    private Task selectedTask;
    private int selectedHour;
    private int selectedMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_card);

        // Populate hours
        Integer[] h = new Integer[24];
        Arrays.setAll(h, (index) -> 0 + index);
        Collections.addAll(hours, h);

        // Populate minutes
        Integer[] m = new Integer[60];
        Arrays.setAll(m, (index) -> 0 + index);
        Collections.addAll(minutes, m);

        // Get references from layouts
        spinnerMCustomer = findViewById(R.id.spinnerMCustomer);
        spinnerMJob = findViewById(R.id.spinnerMJob);
        spinnerMTask = findViewById(R.id.spinnerMTask);
        spinnerHours = findViewById(R.id.spinnerHours);
        spinnerMinutes = findViewById(R.id.spinnerMinutes);
        editDate = this.findViewById(R.id.editDate);

        // Configure the hours spinner
        spinnerHours.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                // Store the selected item
                int sid= spinnerHours.getSelectedItemPosition();
                selectedHour = hours.get(sid);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        ArrayAdapter<Integer> hourAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, hours);
        spinnerHours.setAdapter(hourAdapter);

        // Configure the minutes spinner
        spinnerMinutes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                // Store the selected item
                int sid= spinnerMinutes.getSelectedItemPosition();
                selectedMinute = minutes.get(sid);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        ArrayAdapter<Integer> minuteAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, minutes);
        spinnerMinutes.setAdapter(minuteAdapter);

        // Set the default date to today
        DateFormat df = new android.text.format.DateFormat();
        editDate.setText(df.format("yyyy-MM-dd", new java.util.Date()));

        // Refresh the customer list
        refreshCustomers();

        // Focus on the hours
        spinnerHours.clearFocus();
        spinnerHours.requestFocus();
    }

    public void onContinueClick(View view)
    {
//        // Take the user to the status activity
//        final Intent intent = new Intent(this, StatusActivity.class);
//        startActivity(intent);
//        finish(); // prevents going back
        save();
    }

    public void onCancelClick(View view)
    {
        // Take the user to the status activity
        final Intent intent = new Intent(this, StatusActivity.class);
        startActivity(intent);
        finish(); // prevents going back
    }

    private void refreshCustomers() {
        String url = "https://brizbee.gowitheast.com/odata/Customers?$count=true&$orderby=Number&$top=20&$skip=0";
        MySingleton singleton = MySingleton.getInstance(this);
        TimeCardActivity activity = this;

        // Run long-running HTTP request in a separate thread
        new Thread() {
            @Override
            public void run() {
                JsonObjectRequest jsonRequest = new JsonObjectRequest
                        (Request.Method.GET, url, null,
                                response -> {
                                    try {
                                        // Format for parsing timestamps from server
                                        java.text.DateFormat dfServer = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'", Locale.ENGLISH);

                                        // Build a customer object for each item in the response
                                        List<Customer> customers = new ArrayList<>();
                                        JSONArray value = response.getJSONArray("value");
                                        for (int i = 0; i < value.length(); i++)
                                        {
                                            JSONObject j = value.getJSONObject(i);
                                            Customer customer = new Customer();
                                            customer.setName(j.getString("Name"));
                                            customer.setNumber(j.getString("Number"));
                                            customer.setId(j.getInt("Id"));

                                            Date createdAt = dfServer.parse(j.getString("CreatedAt"));
                                            customer.setCreatedAt(createdAt);

                                            customers.add(customer);
                                        }

                                        // Update the list of customers and configure the spinner
                                        runOnUiThread(() -> {
                                            ArrayAdapter<Customer> adapter = new ArrayAdapter<>(activity,
                                                    android.R.layout.simple_spinner_dropdown_item, customers);
                                            spinnerMCustomer.setAdapter(adapter);

                                            spinnerMCustomer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                @Override
                                                public void onItemSelected(AdapterView<?> parent, View view,
                                                                           int position, long id) {
                                                    // Store the selected item
                                                    int sid= spinnerMCustomer.getSelectedItemPosition();
                                                    selectedCustomer = customers.get(sid);

                                                    Log.i(TAG, selectedCustomer.getName());

                                                    // Refresh job list
                                                    refreshJobs();
                                                }

                                                @Override
                                                public void onNothingSelected(AdapterView<?> parent) {
                                                    // Do nothing
                                                }
                                            });

                                        });
                                    } catch (JSONException | ParseException e) {
                                        showDialog("Could not understand the response from the server, please try again.");
                                    }
                                },
                                error -> {
                                    NetworkResponse response = error.networkResponse;
                                    switch (response.statusCode)
                                    {
                                        default:
                                            showDialog("Could not reach the server, please try again.");
                                            break;
                                    }
                                }) {
                    @Override
                    public Map<String, String> getHeaders() {
                        HashMap<String, String> headers = new HashMap<>();
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

                // Configure timeout
                int socketTimeout = 10000;
                RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                jsonRequest.setRetryPolicy(policy);

                // Add the request to the RequestQueue
                singleton.addToRequestQueue(jsonRequest);
            }
        }.start();
    }

    private void refreshJobs() {
        String url = "https://brizbee.gowitheast.com/odata/Jobs?$count=true&$orderby=Number&$top=20&$skip=0&$filter=CustomerId eq " + selectedCustomer.getId();
        MySingleton singleton = MySingleton.getInstance(this);
        TimeCardActivity activity = this;

        // Run long-running HTTP request in a separate thread
        new Thread() {
            @Override
            public void run() {
                JsonObjectRequest jsonRequest = new JsonObjectRequest
                        (Request.Method.GET, url, null,
                                response -> {
                                    try {
                                        // Format for parsing timestamps from server
                                        java.text.DateFormat dfServer = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'", Locale.ENGLISH);

                                        // Build a job object for each item in the response
                                        List<Job> jobs = new ArrayList<>();
                                        JSONArray value = response.getJSONArray("value");
                                        for (int i = 0; i < value.length(); i++)
                                        {
                                            JSONObject j = value.getJSONObject(i);
                                            Job job = new Job();
                                            job.setName(j.getString("Name"));
                                            job.setNumber(j.getString("Number"));
                                            job.setId(j.getInt("Id"));

                                            Date createdAt = dfServer.parse(j.getString("CreatedAt"));
                                            job.setCreatedAt(createdAt);

                                            jobs.add(job);
                                        }

                                        // Update the list of jobs and configure the spinner
                                        runOnUiThread(() -> {
                                            ArrayAdapter<Job> adapter = new ArrayAdapter<>(activity,
                                                    android.R.layout.simple_spinner_dropdown_item, jobs);
                                            spinnerMJob.setAdapter(adapter);

                                            spinnerMJob.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                @Override
                                                public void onItemSelected(AdapterView<?> parent, View view,
                                                                           int position, long id) {
                                                    // Store the selected item
                                                    int sid= spinnerMJob.getSelectedItemPosition();
                                                    selectedJob = jobs.get(sid);

                                                    Log.i(TAG, selectedJob.getName());

                                                    // Refresh task list
                                                    refreshTasks();
                                                }

                                                @Override
                                                public void onNothingSelected(AdapterView<?> parent) {
                                                    // Do nothing
                                                }
                                            });

                                        });
                                    } catch (JSONException | ParseException e) {
                                        showDialog("Could not understand the response from the server, please try again.");
                                    }
                                },
                                error -> {
                                    NetworkResponse response = error.networkResponse;
                                    switch (response.statusCode)
                                    {
                                        default:
                                            showDialog("Could not reach the server, please try again.");
                                            break;
                                    }
                                }) {
                    @Override
                    public Map<String, String> getHeaders() {
                        HashMap<String, String> headers = new HashMap<>();
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

                // Configure timeout
                int socketTimeout = 10000;
                RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                jsonRequest.setRetryPolicy(policy);

                // Add the request to the RequestQueue
                singleton.addToRequestQueue(jsonRequest);
            }
        }.start();
    }

    private void refreshTasks() {
        String url = "https://brizbee.gowitheast.com/odata/Tasks?$count=true&$orderby=Number&$top=20&$skip=0&$filter=JobId eq " + selectedJob.getId();
        MySingleton singleton = MySingleton.getInstance(this);
        TimeCardActivity activity = this;

        // Run long-running HTTP request in a separate thread
        new Thread() {
            @Override
            public void run() {
                JsonObjectRequest jsonRequest = new JsonObjectRequest
                        (Request.Method.GET, url, null,
                                response -> {
                                    try {
                                        // Format for parsing timestamps from server
                                        java.text.DateFormat dfServer = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'", Locale.ENGLISH);

                                        // Build a task object for each item in the response
                                        List<Task> tasks = new ArrayList<>();
                                        JSONArray value = response.getJSONArray("value");
                                        for (int i = 0; i < value.length(); i++)
                                        {
                                            JSONObject j = value.getJSONObject(i);
                                            Task task = new Task();
                                            task.setName(j.getString("Name"));
                                            task.setNumber(j.getString("Number"));
                                            task.setId(j.getInt("Id"));

                                            Date createdAt = dfServer.parse(j.getString("CreatedAt"));
                                            task.setCreatedAt(createdAt);

                                            tasks.add(task);
                                        }

                                        // Update the list of tasks and configure the spinner
                                        runOnUiThread(() -> {
                                            ArrayAdapter<Task> adapter = new ArrayAdapter<>(activity,
                                                    android.R.layout.simple_spinner_dropdown_item, tasks);
                                            spinnerMTask.setAdapter(adapter);

                                            spinnerMTask.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                @Override
                                                public void onItemSelected(AdapterView<?> parent, View view,
                                                                           int position, long id) {
                                                    // Store the selected item
                                                    int sid= spinnerMTask.getSelectedItemPosition();
                                                    selectedTask = tasks.get(sid);

                                                    Log.i(TAG, selectedTask.getName());
                                                }

                                                @Override
                                                public void onNothingSelected(AdapterView<?> parent) {
                                                    // Do nothing
                                                }
                                            });

                                        });
                                    } catch (JSONException | ParseException e) {
                                        showDialog("Could not understand the response from the server, please try again.");
                                    }
                                },
                                error -> {
                                    NetworkResponse response = error.networkResponse;
                                    switch (response.statusCode)
                                    {
                                        default:
                                            showDialog("Could not reach the server, please try again.");
                                            break;
                                    }
                                }) {
                    @Override
                    public Map<String, String> getHeaders() {
                        HashMap<String, String> headers = new HashMap<>();
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

                // Configure timeout
                int socketTimeout = 10000;
                RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                jsonRequest.setRetryPolicy(policy);

                // Add the request to the RequestQueue
                singleton.addToRequestQueue(jsonRequest);
            }
        }.start();
    }

    private void save() {
        final Intent intent = new Intent(this, StatusActivity.class);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        int minutes = selectedMinute + (selectedHour * 60);
//        Date date = editDate.get

        // Instantiate the RequestQueue
        String url = "https://brizbee.gowitheast.com/odata/TimesheetEntries";
        // Request a string response from the provided URL
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("TaskId", selectedTask.getId());
            jsonBody.put("Minutes", minutes);
            jsonBody.put("EnteredAt", 0);
        } catch (JSONException e) {
            showDialog("Could not prepare the request to the server, please try again.");
        }
        JsonObjectRequest jsonRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonBody, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Take the user to the status activity
                        startActivity(intent);
                        finish(); // prevents going back
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
