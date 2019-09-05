package com.brizbee.Brizbee.Mobile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PunchInTaskIdActivity extends AppCompatActivity implements View.OnClickListener
{
    private Button buttonScan;
    private TextView editTaskNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_punch_in_task_id);

        // Get references from layouts
        buttonScan = findViewById(R.id.buttonScan);
        editTaskNumber = findViewById(R.id.editTaskNumber);

        // Set the click listener
        buttonScan.setOnClickListener(this);

        // Focus on the task id
        if(editTaskNumber.requestFocus())
        {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    public void onClick(View v)
    {
        // Respond to clicks
        if(v.getId() == R.id.buttonScan)
        {
            //scan
            IntentIntegrator scanIntegrator = new IntentIntegrator(this);
            scanIntegrator.initiateScan();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        // Retrieve the scan result
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanningResult != null)
        {
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName(); // barcode type
            editTaskNumber.setText(""); // clear the text
            editTaskNumber.setText(scanContent); // set the scanned value
            confirm(); // Verify that the task number is valid
        }
        else
        {
            showDialog("No task number could be scanned, please try again.");
        }
    }

    public void onCancelClick(View view)
    {
        final Intent intent = new Intent(this, StatusActivity.class);
        startActivity(intent);
        finish(); // prevents going back
    }

    public void onContinueClick(View view)
    {
        confirm();
    }

    public void confirm()
    {
        // Lookup the task number
        final Intent intent = new Intent(this, PunchInConfirmActivity.class);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        EditText editTaskNumber = findViewById(R.id.editTaskNumber);

        // Instantiate the RequestQueue
        String url = String.format("https://brizbee.gowitheast.com/odata/Tasks?$expand=Job($expand=Customer)&$filter=Number eq '%s'", editTaskNumber.getText());

        JsonObjectRequest jsonRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        try {
                            JSONArray value = response.getJSONArray("value");

                            if (value.length() > 0) {
                                JSONObject first = value.getJSONObject(0);

                                // Pass the task as a string
                                intent.putExtra("task", first.toString());
                                startActivity(intent);
                            } else {
                                // Notify the user that the task number does not exist
                                showDialog("That's not a valid task number, please try again.");
                            }
                        } catch (JSONException e) {
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
