package com.brizbee.Brizbee.Mobile;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.brizbee.Brizbee.Mobile.models.Organization;
import com.brizbee.Brizbee.Mobile.models.TimeZone;
import com.brizbee.Brizbee.Mobile.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoginPinFragment extends Fragment
{
    private MyApplication application;
    private EditText editOrganizationCode;
    private EditText editUserPin;
    private Button button;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        application = ((MyApplication) this.getActivity().getApplication());

        View view = inflater.inflate(R.layout.login_pin_fragment, container, false);

        // Get references from layouts
        editOrganizationCode = view.findViewById(R.id.editOrganizationCode);
        editUserPin = view.findViewById(R.id.editUserPin);
        button = view.findViewById(R.id.buttonLogin);

        // Set the click listener
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onLoginClick(v);
            }
        });

        // Focus on the organization code
        editOrganizationCode.clearFocus();
        editOrganizationCode.requestFocus();

        return view;
    }

    public void onLoginClick(View view)
    {
        setEnabled(false); // Disable the form

        String organizationCode = editOrganizationCode.getText().toString();
        String userPin = editUserPin.getText().toString();
        String url ="https://brizbee.gowitheast.com/odata/Users/Default.Authenticate";

        // Request a string response from the provided URL
        JSONObject jsonBody = new JSONObject();
        try
        {
            JSONObject session = new JSONObject();
            session.put("PinOrganizationCode", organizationCode);
            session.put("PinUserPin", userPin);
            session.put("Method", "pin");
            jsonBody.put("Session", session);
        }
        catch (JSONException e)
        {
            showDialog("Could not prepare the request to the server, please try again.");
        }
        JsonObjectRequest jsonRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonBody, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        try
                        {
                            String authUserId = response.getString("AuthUserId");
                            String authToken = response.getString("AuthToken");
                            String authExpiration = response.getString("AuthExpiration");

                            // Set application variables
                            application.setAuthExpiration(authExpiration);
                            application.setAuthToken(authToken);
                            application.setAuthUserId(authUserId);

                            // Load metadata
                            getTimeZones();
                            getUserAndOrganization(Integer.parseInt(authUserId));
                        }
                        catch (JSONException e)
                        {
                            showDialog("Could not understand the response from the server, please try again.");
                            setEnabled(true); // Enable the form
                        }
                    }
                },
                        new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        NetworkResponse response = error.networkResponse;
                        if (response != null && response.data != null)
                        {
                            switch (response.statusCode)
                            {
                                case 400:
                                    showDialog("Not a valid organization code and PIN number, please try again.");
                                    break;
                                default:
                                    showDialog("Could not reach the server, please try again.");
                                    break;
                            }
                            setEnabled(true); // Enable the form
                        }
                        else
                        {
                            showDialog("Could not reach the server, please try again.");
                            setEnabled(true); // Enable the form
                        }
                    }
                });

        int socketTimeout = 10000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonRequest.setRetryPolicy(policy);

        // Add the request to the RequestQueue
        MySingleton.getInstance(this.getActivity()).addToRequestQueue(jsonRequest);
    }

    public HashMap<String, String> getAuthHeaders()
    {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");

        String authExpiration = application.getAuthExpiration();
        String authToken = application.getAuthToken();
        String authUserId = application.getAuthUserId();

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

    public void getTimeZones()
    {
        String url = "https://brizbee.gowitheast.com/odata/Organizations/Default.Timezones";

        JsonObjectRequest jsonRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        try
                        {
                            JSONArray value = response.getJSONArray("value");
                            TimeZone[] timezones = new TimeZone[value.length()];
                            for(int i = 0; i < value.length(); i++)
                            {
                                TimeZone zone = new TimeZone();
                                zone.setCountryCode(value.getJSONObject(i).getString("CountryCode"));
                                zone.setId(value.getJSONObject(i).getString("Id"));
                                timezones[i] = zone;
                            }

                            // Store user in application variable
                            application.setTimeZones(timezones);
                        } catch (JSONException e)
                        {
                            showDialog("Could not understand the response from the server, please try again.");
                            setEnabled(true); // Enable the form
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
                                setEnabled(true); // Enable the form
                            }
                        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError
            {
                return getAuthHeaders();
            }
        };

        int socketTimeout = 10000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonRequest.setRetryPolicy(policy);

        // Add the request to the RequestQueue
        MySingleton.getInstance(this.getActivity()).addToRequestQueue(jsonRequest);
    }

    public void getUserAndOrganization(int userId)
    {
        final Activity activity = this.getActivity();
        final Intent intent = new Intent(activity, StatusActivity.class);

        String url = String.format("https://brizbee.gowitheast.com/odata/Users(%d)?$expand=Organization", userId);

        JsonObjectRequest jsonRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        try
                        {
                            // Format for parsing timestamps from server
                            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.ENGLISH);

                            JSONObject userJson = response;
                            User user = new User();
                            user.setCreatedAt(df.parse(userJson.getString("CreatedAt")));
                            user.setEmailAddress(userJson.getString("EmailAddress"));
                            user.setId(userJson.getInt("Id"));
                            user.setName(userJson.getString("Name"));
                            user.setRequiresLocation(userJson.getBoolean("RequiresLocation"));
                            user.setTimeZone(userJson.getString("TimeZone"));

                            JSONObject organizationJson = response.getJSONObject("Organization");
                            Organization organization = new Organization();
                            organization.setCreatedAt(df.parse(organizationJson.getString("CreatedAt")));
                            organization.setId(organizationJson.getInt("Id"));
                            organization.setName(organizationJson.getString("Name"));

                            // Store user in application variable
                            application.setUser(user);
                            application.setOrganization(organization);

                            startActivity(intent);
                            activity.finish(); // prevents going back
                        } catch (JSONException e)
                        {
                            showDialog("Could not understand the response from the server, please try again.");
                            setEnabled(true); // Enable the form
                        } catch (ParseException e)
                        {
                            showDialog("Could not understand the response from the server, please try again.");
                            setEnabled(true); // Enable the form
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
                                setEnabled(true); // Enable the form
                            }
                        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError
            {
                return getAuthHeaders();
            }
        };

        int socketTimeout = 10000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonRequest.setRetryPolicy(policy);

        // Add the request to the RequestQueue
        MySingleton.getInstance(this.getActivity()).addToRequestQueue(jsonRequest);
    }

    public void setEnabled(boolean enabled)
    {
        editOrganizationCode.setEnabled(enabled);
        editUserPin.setEnabled(enabled);
        button.setEnabled(enabled);
    }

    private void showDialog(String message)
    {
        // Build a dialog with the given message to show the user
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}