//
//  RegisterStep1Activity.java
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
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RegisterStep1Activity extends AppCompatActivity {
    private TextView editOrganizationName;
    private TextView editUserName;
    private TextView editUserEmail;
    private TextView editUserPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_step_1);
        editOrganizationName = findViewById(R.id.editRegisterOrganizationName);
        editUserName = findViewById(R.id.editRegisterUserName);
        editUserEmail = findViewById(R.id.editRegisterUserEmail);
        editUserPassword = findViewById(R.id.editRegisterUserPassword);
        if (editUserName.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    public void onCancelClick(View view) {
        final Intent intent = new Intent(this, LoginActivity.class);
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
        String url = "https://app-brizbee-prod.azurewebsites.net/odata/Users/Default.Register";

        // Random organization code and user pin
        String organizationCode = String.valueOf(Math.floor((Math.random() * 99999) + 10000));
        String userPin = String.valueOf(Math.floor((Math.random() * 99999) + 10000));

        // Request a string response from the provided URL
        JSONObject jsonBody = new JSONObject();
        try {
            JSONObject organization = new JSONObject();
            organization.put("Name", editOrganizationName.getText());
            organization.put("Code", organizationCode);

            JSONObject user = new JSONObject();
            user.put("Name", editUserName.getText());
            user.put("EmailAddress", editUserEmail.getText());
            user.put("Password", editUserPassword.getText());
            user.put("Pin", userPin);
            user.put("TimeZone", "America/New_York");

            jsonBody.put("Organization", organization);
            jsonBody.put("User", user);
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
                        if (response != null && response.data != null) {
                            switch (response.statusCode) {
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

                return headers;
            }
        };

        int socketTimeout = 10000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonRequest.setRetryPolicy(policy);

        // Add the request to the RequestQueue
        MySingleton.getInstance(this).addToRequestQueue(jsonRequest);
    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
}
