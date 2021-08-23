//
//  PunchInConfirmActivity.kt
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

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.FusedLocationProviderClient
import android.os.Bundle
import org.json.JSONException
import com.google.android.gms.location.LocationServices
import android.content.Intent
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import com.android.volley.toolbox.JsonObjectRequest
import kotlin.Throws
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.android.volley.*
import java.lang.Exception
import java.util.HashMap
import kotlin.concurrent.thread

class PunchInConfirmActivity : AppCompatActivity() {
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var textConfirmTask: TextView? = null
    private var textConfirmCustomer: TextView? = null
    private var textConfirmJob: TextView? = null
    private var task: JSONObject? = null
    private var job: JSONObject? = null
    private var customer: JSONObject? = null
    private var locationCallback: LocationCallback? = null
    private var spinnerTimeZone: Spinner? = null
    private lateinit var timezonesIds: Array<String?>
    private var selectedTimeZone: String? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationAllowed = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_punch_in_confirm)

        // Get references from layouts
        spinnerTimeZone = findViewById(R.id.spinnerTimeZone)
        textConfirmTask = findViewById(R.id.textConfirmTask)
        textConfirmCustomer = findViewById(R.id.textConfirmCustomer)
        textConfirmJob = findViewById(R.id.textConfirmJob)

        try {
            task = JSONObject(intent.getStringExtra("task")!!)
            job = task?.getJSONObject("Job")
            customer = job?.getJSONObject("Customer")

            // Task Number and Name
            textConfirmTask?.text = String.format(
                "%s - %s",
                task?.getString("Number"),
                task?.getString("Name")
            )

            // Customer Number and Name
            textConfirmCustomer?.text = String.format(
                "%s - %s",
                customer?.getString("Number"),
                customer?.getString("Name")
            )

            // Job Number and Name
            textConfirmJob?.text = String.format(
                "%s - %s",
                job?.getString("Number"),
                job?.getString("Name")
            )
        } catch (e: JSONException) {
            showDialog("Could not display the task you selected, please go back and try again.")
        }

        // Allows getting the location.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Get timezones from application and configure spinner
        val timezones = (application as MyApplication).timeZones
        timezonesIds = arrayOfNulls(timezones.size)
        for (i in timezones.indices) {
            timezonesIds[i] = timezones[i].id
        }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item, timezonesIds
        )
        spinnerTimeZone?.adapter = adapter
        spinnerTimeZone?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View,
                position: Int, id: Long
            ) {
                Log.i(TAG, "An item has been selected at position:")
                Log.i(TAG, position.toString())

                // Store selected item
                val sid = spinnerTimeZone?.selectedItemPosition
                selectedTimeZone = timezonesIds[sid!!]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        // Set selected item to be the user's time zone.
        val user = (application as MyApplication).user
        for (i in timezonesIds.indices) {
            if (timezonesIds[i].equals(user.timeZone, ignoreCase = true)) {
                spinnerTimeZone?.setSelection(i)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCancelClick(view: View?) {
        val intent = Intent(this, StatusActivity::class.java)
        startActivity(intent)

        // Prevent going back.
        finish()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onContinueClick(view: View?) {
        thread(start = true) {
            getLocation()
        }
    }

    private fun getLocation() {
        var progressDialog: androidx.appcompat.app.AlertDialog? = null
        var locationEnabled = true

        runOnUiThread {
            // Prepare a progress dialog.
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setMessage("Getting your location")
            progressDialog = builder.create()
            progressDialog?.setCancelable(false)
            progressDialog?.setCanceledOnTouchOutside(false)
        }

        try {
            val user = (application as MyApplication).user

            // Recording current location is optional.
            if (!user.requiresLocation) {
                // Save without location.
                Log.i(TAG, "Saving without location because location is not required")
                save()
            }

            val lm = getSystemService(LOCATION_SERVICE) as LocationManager

            // Check if the location service is enabled.
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                locationEnabled = false

            // Cannot continue so alert the user.
            if (!locationEnabled) {
                runOnUiThread {
                    Log.i(TAG, "Location is not enabled and is required by BRIZBEE")
                    showDialog("Location is not enabled but is required by BRIZBEE")
                }
                return
            }

            // Attempt to get location updates.
            val locationRequest = LocationRequest.create()
            locationRequest.interval = 5 * 1000
            locationRequest.fastestInterval = 1 * 1000
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    if (locationResult == null) {
                        // Save without the location.
                        Log.i(TAG, "Saving without location because location cannot be acquired")

                        runOnUiThread {
                            progressDialog?.dismiss()
                        }

                        save()

                        return
                    }

                    val location = locationResult.locations[0]

                    // Get the coordinates of the location.
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude

                    // Stop getting location updates.
                    fusedLocationClient!!.removeLocationUpdates(locationCallback!!)

                    Log.i(TAG, "Saving with the location")

                    runOnUiThread {
                        progressDialog?.dismiss()
                    }

                    // Save with the location.
                    save()
                }
            }

            // Check or request permission for fine location.
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                locationAllowed = false
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                } else {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                }
            }

            // Do not continue without location permission.
            if (!locationAllowed)
                return

            runOnUiThread {
                progressDialog?.show()
            }

            // Start getting location updates.
            fusedLocationClient!!.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e(TAG, e.toString())

            runOnUiThread {
                progressDialog?.dismiss()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                    if ((ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) ==
                                PackageManager.PERMISSION_GRANTED)) {
                        // Continue with the existing workflow.
                        locationAllowed = true
                        getLocation()
                    }
                } else {
                    Log.w(TAG, "Does not have permission, cannot continue")
                    showDialog("Location permission has been denied but is required by BRIZBEE")
                }
                return
            }
        }
    }

    fun save() {
        var progressDialog: androidx.appcompat.app.AlertDialog? = null

        runOnUiThread {
            // Prepare a progress dialog.
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setMessage("Saving")
            builder.setCancelable(false)
            progressDialog = builder.create()
            progressDialog?.setCancelable(false)
            progressDialog?.setCanceledOnTouchOutside(false)
            progressDialog?.show()
        }

        val intent = Intent(this, StatusActivity::class.java)

        // Instantiate the RequestQueue.
        val url = "https://app-brizbee-prod.azurewebsites.net/odata/Punches/Default.PunchIn"

        // Request a string response from the provided URL.
        val jsonBody = JSONObject()
        try {
            jsonBody.put("TaskId", task!!["Id"])
            jsonBody.put("SourceHardware", "Mobile")
            jsonBody.put("InAtTimeZone", selectedTimeZone)
            jsonBody.put("SourceOperatingSystem", "Android")
            jsonBody.put("SourceOperatingSystemVersion", Build.VERSION.RELEASE)
            jsonBody.put("SourceBrowser", "N/A")
            jsonBody.put("SourceBrowserVersion", "N/A")
            if (currentLatitude != 0.0 && currentLatitude != 0.0) {
                jsonBody.put("LatitudeForInAt", currentLatitude.toString())
                jsonBody.put("LongitudeForInAt", currentLongitude.toString())
            } else {
                jsonBody.put("LatitudeForInAt", "")
                jsonBody.put("LongitudeForInAt", "")
            }
        } catch (e: JSONException) {
            runOnUiThread {
                progressDialog?.dismiss()
                showDialog("Could not prepare the request to the server, please try again.")
            }
        }
        val jsonRequest: JsonObjectRequest =
            object : JsonObjectRequest(Method.POST, url, jsonBody, Response.Listener {
                runOnUiThread {
                    progressDialog?.dismiss()
                    startActivity(intent)
                    finish()
                }
            }, Response.ErrorListener { error ->
                val response = error.networkResponse
                when (response.statusCode) {
                    else -> {
                        runOnUiThread {
                            progressDialog?.dismiss()
                            showDialog("Could not reach the server, please try again.")
                        }
                    }
                }
            }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Content-Type"] = "application/json"
                    val authExpiration = (application as MyApplication).authExpiration
                    val authToken = (application as MyApplication).authToken
                    val authUserId = (application as MyApplication).authUserId
                    if (authExpiration != null && authExpiration.isNotEmpty() && authToken != null && authToken.isNotEmpty() && authUserId != null && authUserId.isNotEmpty()) {
                        headers["AUTH_EXPIRATION"] = authExpiration
                        headers["AUTH_TOKEN"] = authToken
                        headers["AUTH_USER_ID"] = authUserId
                    }
                    return headers
                }
            }
        val socketTimeout = 10000
        val policy: RetryPolicy =
            DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        jsonRequest.retryPolicy = policy

        // Add the request to the RequestQueue.
        MySingleton.getInstance(this).addToRequestQueue(jsonRequest)
    }

    private fun showDialog(message: String) {
        // Build a dialog with the given message to show the user.
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    companion object {
        private const val TAG = "PunchInConfirmActivity"
    }
}