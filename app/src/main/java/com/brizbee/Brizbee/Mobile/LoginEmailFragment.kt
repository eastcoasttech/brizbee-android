//
//  LoginEmailFragment.kt
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

package com.brizbee.Brizbee.Mobile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.brizbee.Brizbee.Mobile.models.Organization
import com.brizbee.Brizbee.Mobile.models.TimeZone
import com.brizbee.Brizbee.Mobile.models.User
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class LoginEmailFragment : Fragment() {
    private var application: MyApplication? = null
    private var editEmailAddress: EditText? = null
    private var editPassword: EditText? = null
    private var buttonLogin: Button? = null
    private var progressBar: ProgressBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        application = requireActivity().application as MyApplication
        val view = inflater.inflate(R.layout.login_email_fragment, container, false)

        // Get references from layouts
        editEmailAddress = view.findViewById(R.id.editEmailAddress)
        editPassword = view.findViewById(R.id.editPassword)
        buttonLogin = view.findViewById(R.id.buttonLogin)
        progressBar = view.findViewById(R.id.login_email_fragment_progress)

        // Set the click listener
        buttonLogin?.setOnClickListener { v -> onLoginClick(v) }

        // Focus on the email address
        editEmailAddress?.clearFocus()
        editEmailAddress?.requestFocus()

        return view
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onRegisterClick(view: View?) {
        val intent = Intent(this.activity, RegisterStep1Activity::class.java)
        startActivity(intent)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onLoginClick(view: View?) {
        thread(start = true) {
            login()
        }
    }

    private fun login() {
        setEnabled(false) // Disable the form

        val emailAddress = editEmailAddress!!.text.toString()
        val password = editPassword!!.text.toString()
        val url = "https://app-brizbee-prod.azurewebsites.net/odata/Users/Default.Authenticate"

        // Request a string response from the provided URL
        val jsonBody = JSONObject()
        try {
            val session = JSONObject()
            session.put("EmailAddress", emailAddress)
            session.put("EmailPassword", password)
            session.put("Method", "email")
            jsonBody.put("Session", session)
        } catch (e: JSONException) {
            showDialog("Could not prepare the request to the server, please try again.")
        }
        val jsonRequest = JsonObjectRequest(Request.Method.POST, url, jsonBody, { response ->
            try {
                val authUserId = response.getString("AuthUserId")
                val authToken = response.getString("AuthToken")
                val authExpiration = response.getString("AuthExpiration")

                // Set application variables
                application!!.authExpiration = authExpiration
                application!!.authToken = authToken
                application!!.authUserId = authUserId
                timeZones
                getUserAndOrganization(authUserId.toInt())
            } catch (e: JSONException) {
                showDialog("Could not understand the response from the server, please try again.")
                setEnabled(true) // Enable the form
            }
        }
        ) { error ->
            val response = error.networkResponse
            if (response?.data != null) {
                when (response.statusCode) {
                    400 -> showDialog("Not a valid Email and password, please try again.")
                    else -> showDialog("Could not reach the server, please try again.")
                }
            } else {
                showDialog("Could not reach the server, please try again.")
            }
            setEnabled(true) // Enable the form
        }

        // Increase number of retries because we may be on a poor connection
        val socketTimeout = 10000
        val policy: RetryPolicy = DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        jsonRequest.retryPolicy = policy

        // Add the request to the RequestQueue
        MySingleton.getInstance(this.activity).addToRequestQueue(jsonRequest)
    }

    val authHeaders: HashMap<String, String>
        get() {
            val headers = HashMap<String, String>()
            headers["Content-Type"] = "application/json"
            val authExpiration = application!!.authExpiration
            val authToken = application!!.authToken
            val authUserId = application!!.authUserId
            if (authExpiration != null && authExpiration.isNotEmpty() && authToken != null && authToken.isNotEmpty() && authUserId != null && authUserId.isNotEmpty()) {
                headers["AUTH_EXPIRATION"] = authExpiration
                headers["AUTH_TOKEN"] = authToken
                headers["AUTH_USER_ID"] = authUserId
            }
            return headers
        }

    private val timeZones: Unit
        get() {
            val url = "https://app-brizbee-prod.azurewebsites.net/odata/Organizations/Default.Timezones"
            val jsonRequest: JsonObjectRequest = object : JsonObjectRequest(Method.GET, url, null, Response.Listener { response ->
                try {
                    val value = response.getJSONArray("value")
                    val timezones = arrayOfNulls<TimeZone>(value.length())
                    for (i in 0 until value.length()) {
                        val zone = TimeZone()
                        zone.countryCode = value.getJSONObject(i).getString("CountryCode")
                        zone.id = value.getJSONObject(i).getString("Id")
                        timezones[i] = zone
                    }

                    // Store user in application variable
                    application!!.timeZones = timezones
                } catch (e: JSONException) {
                    showDialog("Could not understand the response from the server, please try again.")
                    setEnabled(true) // Enable the form
                }
            },
                    Response.ErrorListener { error ->
                        val response = error.networkResponse
                        when (response.statusCode) {
                            else -> showDialog("Could not reach the server, please try again.")
                        }
                        setEnabled(true) // Enable the form
                    }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    return authHeaders
                }
            }

            // Increase number of retries because we may be on a poor connection
            val socketTimeout = 10000
            val policy: RetryPolicy = DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            jsonRequest.retryPolicy = policy

            // Add the request to the RequestQueue
            MySingleton.getInstance(this.activity).addToRequestQueue(jsonRequest)
        }

    private fun getUserAndOrganization(userId: Int) {
        val activity: Activity? = this.activity
        val intent = Intent(activity, StatusActivity::class.java)
        val url = String.format("https://app-brizbee-prod.azurewebsites.net/odata/Users(%d)?\$expand=Organization", userId)
        val jsonRequest: JsonObjectRequest = object : JsonObjectRequest(Method.GET, url, null, Response.Listener { response ->
            try {
                // Format for parsing timestamps from server
                val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.ENGLISH)
                val user = User()
                user.createdAt = df.parse(response.getString("CreatedAt"))
                user.emailAddress = response.getString("EmailAddress")
                user.id = response.getInt("Id")
                user.name = response.getString("Name")
                user.requiresLocation = response.getBoolean("RequiresLocation")
                user.usesMobileClock = response.getBoolean("UsesMobileClock")
                user.usesTimesheets = response.getBoolean("UsesTimesheets")
                user.timeZone = response.getString("TimeZone")
                val organizationJson = response.getJSONObject("Organization")
                val organization = Organization()
                organization.createdAt = df.parse(organizationJson.getString("CreatedAt"))
                organization.id = organizationJson.getInt("Id")
                organization.name = organizationJson.getString("Name")

                // Store user in application variable
                application!!.user = user
                application!!.organization = organization
                startActivity(intent)
                activity!!.finish() // prevents going back
            } catch (e: JSONException) {
                showDialog("Could not understand the response from the server, please try again.")
                setEnabled(true) // Enable the form
            } catch (e: ParseException) {
                showDialog("Could not understand the response from the server, please try again.")
                setEnabled(true) // Enable the form
            }
        },
                Response.ErrorListener { error ->
                    val response = error.networkResponse
                    when (response.statusCode) {
                        else -> showDialog("Could not reach the server, please try again.")
                    }
                    setEnabled(true) // Enable the form
                }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                return authHeaders
            }
        }

        // Increase number of retries because we may be on a poor connection
        val socketTimeout = 10000
        val policy: RetryPolicy = DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        jsonRequest.retryPolicy = policy

        // Add the request to the RequestQueue
        MySingleton.getInstance(activity).addToRequestQueue(jsonRequest)
    }

    fun setEnabled(enabled: Boolean) {
        activity?.runOnUiThread {
            editEmailAddress!!.isEnabled = enabled
            editPassword!!.isEnabled = enabled
            buttonLogin!!.isEnabled = enabled

            if (enabled) {
                progressBar!!.visibility = View.INVISIBLE
                buttonLogin!!.visibility = View.VISIBLE
            } else {
                progressBar!!.visibility = View.VISIBLE
                buttonLogin!!.visibility = View.INVISIBLE
            }
        }
    }

    private fun showDialog(message: String) {
        activity?.runOnUiThread {
            // Build a dialog with the given message to show the user
            val builder = AlertDialog.Builder(requireActivity())
            builder.setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            val dialog = builder.create()
            dialog.show()
        }
    }
}