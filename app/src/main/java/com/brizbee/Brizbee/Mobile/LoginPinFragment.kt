//
//  LoginPinFragment.kt
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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.android.volley.*
import com.android.volley.toolbox.JsonArrayRequest
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

class LoginPinFragment : Fragment() {
    private var application: MyApplication? = null
    private var editOrganizationCode: EditText? = null
    private var editUserPin: EditText? = null
    private var buttonLogin: Button? = null
    var progressDialog: AlertDialog? = null

    companion object {
        val TAG = LoginPinFragment::class.qualifiedName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        application = requireActivity().application as MyApplication
        val view = inflater.inflate(R.layout.login_pin_fragment, container, false)

        // Get references from layouts
        editOrganizationCode = view.findViewById(R.id.editOrganizationCode)
        editUserPin = view.findViewById(R.id.editUserPin)
        buttonLogin = view.findViewById(R.id.buttonLogin)

        // Set the click listener
        buttonLogin?.setOnClickListener { v -> onLoginClick(v) }

        // Focus on the organization code
        editOrganizationCode?.clearFocus()
        editOrganizationCode?.requestFocus()

        return view
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onLoginClick(view: View?) {
        thread(start = true) {
            login()
        }
    }

    private fun login() {
        activity?.runOnUiThread {
            // Prepare a progress dialog.
            val builder = AlertDialog.Builder(requireActivity())
            builder.setMessage("Working")
            builder.setCancelable(false)
            progressDialog = builder.create()
            progressDialog?.setCancelable(false)
            progressDialog?.setCanceledOnTouchOutside(false)
            progressDialog?.show()
        }

        val organizationCode = editOrganizationCode?.text.toString()
        val userPin = editUserPin?.text.toString()
        val url = "${application?.baseUrl}/api/Auth/Authenticate"

        // Request a string response from the provided URL.
        val jsonBody = JSONObject()
        jsonBody.put("PinOrganizationCode", organizationCode)
        jsonBody.put("PinUserPin", userPin)
        jsonBody.put("Method", "pin")

        val request = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            { response ->
                try {
                    val jwt = response.getString("token")

                    // Set application variables
                    application?.jwt = jwt

                    // Load metadata
                    timeZones
                    getUserAndOrganization()
                } catch (e: JSONException) {
                    activity?.runOnUiThread {
                        progressDialog?.dismiss()
                        showDialog("Could not understand the response from the server, please try again.")
                    }
                }
            }) { error ->
                activity?.runOnUiThread {
                    val response = error.networkResponse
                    when (response.statusCode) {
                        400 -> {
                            progressDialog?.dismiss()
                            showDialog("Not a valid organization code and PIN number, please try again.")
                        }
                        else -> {
                            progressDialog?.dismiss()
                            showDialog("Could not reach the server, please try again.")
                        }
                    }
                }
            }

        // Increase number of retries because we may be on a poor connection.
        request.retryPolicy = DefaultRetryPolicy(10000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

        // Add the request to the RequestQueue.
        MySingleton.getInstance(activity).addToRequestQueue(request)
    }

    val authHeaders: HashMap<String, String>
        get() {
            val headers = HashMap<String, String>()
            headers["Content-Type"] = "application/json"
            val jwt = application?.jwt

            if (!jwt.isNullOrEmpty()) {
                headers["Authorization"] = "Bearer $jwt"
            }

            return headers
        }

    private val timeZones: Unit
        get() {
            val url = "${application?.baseUrl}/api/Kiosk/TimeZones"
            val request: JsonArrayRequest = object : JsonArrayRequest(Method.GET, url, null,
                { response ->
                    try {
                        val timezones = arrayOfNulls<TimeZone>(response.length())
                        for (i in 0 until response.length()) {
                            val zone = TimeZone()
                            zone.countryCode = response.getJSONObject(i).getString("countryCode")
                            zone.id = response.getJSONObject(i).getString("id")
                            timezones[i] = zone
                        }

                        // Store user in application variable
                        application?.timeZones = timezones
                    } catch (e: JSONException) {
                        activity?.runOnUiThread {
                            progressDialog?.dismiss()
                            showDialog("Could not understand the response from the server, please try again.")
                        }
                    }
                }, { error ->
                    activity?.runOnUiThread {
                        val response = error.networkResponse
                        when (response.statusCode) {
                            else -> {
                                progressDialog?.dismiss()
                                showDialog("Could not reach the server, please try again.")
                            }
                        }
                    }
                }) {
                    override fun getHeaders(): Map<String, String> {
                        return authHeaders
                    }
                }

            // Increase number of retries because we may be on a poor connection.
            request.retryPolicy = DefaultRetryPolicy(10000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

            // Add the request to the RequestQueue.
            MySingleton.getInstance(activity).addToRequestQueue(request)
        }

    private fun getUserAndOrganization() {
        val intent = Intent(activity, StatusActivity::class.java)
        val url = "${application?.baseUrl}/api/Auth/Me"
        val request: JsonObjectRequest = object : JsonObjectRequest(Method.GET, url, null,
            { response ->
                try {
                    // Format for parsing timestamps from server
                    val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.ENGLISH)
                    val user = User()
                    user.createdAt = df.parse(response.getString("createdAt"))

                    if (!response.isNull("emailAddress")) {
                        user.emailAddress = response.getString("emailAddress")
                    }

                    user.id = response.getInt("id")
                    user.name = response.getString("name")
                    user.requiresLocation = response.getBoolean("requiresLocation")
                    user.usesMobileClock = response.getBoolean("usesMobileClock")
                    user.usesTimesheets = response.getBoolean("usesTimesheets")
                    user.timeZone = response.getString("timeZone")
                    val organizationJson = response.getJSONObject("organization")
                    val organization = Organization()
                    organization.createdAt = df.parse(organizationJson.getString("createdAt"))
                    organization.id = organizationJson.getInt("id")
                    organization.name = organizationJson.getString("name")

                    // Store user in application variable.
                    application?.user = user
                    application?.organization = organization

                    activity?.runOnUiThread {
                        progressDialog?.dismiss()
                        startActivity(intent)
                        activity?.finish() // Prevents going back
                    }
                } catch (e: JSONException) {
                    activity?.runOnUiThread {
                        progressDialog?.dismiss()
                        showDialog("Could not understand the response from the server, please try again.")
                    }
                } catch (e: ParseException) {
                    activity?.runOnUiThread {
                        progressDialog?.dismiss()
                        showDialog("Could not understand the response from the server, please try again.")
                    }
                }
            }, { error ->
                activity?.runOnUiThread {
                    val response = error.networkResponse
                    when (response.statusCode) {
                        else -> {
                            progressDialog?.dismiss()
                            showDialog("Could not reach the server, please try again.")
                        }
                    }
                }
            }) {
                override fun getHeaders(): Map<String, String> {
                    return authHeaders
                }
            }

        // Increase number of retries because we may be on a poor connection.
        request.retryPolicy = DefaultRetryPolicy(10000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

        // Add the request to the RequestQueue.
        MySingleton.getInstance(activity).addToRequestQueue(request)
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