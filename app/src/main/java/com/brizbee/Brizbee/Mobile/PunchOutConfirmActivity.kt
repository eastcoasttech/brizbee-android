//
//  PunchOutConfirmActivity.java
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
import com.google.android.gms.location.LocationCallback
import android.widget.Spinner
import com.google.android.gms.location.FusedLocationProviderClient
import android.os.Bundle
import com.google.android.gms.location.LocationServices
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.content.Intent
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import android.os.Build
import com.android.volley.toolbox.JsonObjectRequest
import kotlin.Throws
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.android.volley.*
import java.lang.Exception
import java.util.HashMap
import kotlin.concurrent.thread

class PunchOutConfirmActivity : AppCompatActivity() {
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var buttonConfirmOutContinue: Button? = null
    private var locationCallback: LocationCallback? = null
    private var spinnerTimeZone: Spinner? = null
    private lateinit var timezonesIds: Array<String?>
    private var selectedTimeZone: String? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationAllowed = true
    var progressDialog: AlertDialog? = null

    companion object {
        val TAG = PunchOutConfirmActivity::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_punch_out_confirm)

        // Get references from layouts.
        spinnerTimeZone = findViewById(R.id.spinnerTimeZone)
        buttonConfirmOutContinue = findViewById(R.id.buttonConfirmOutContinue)

        // Get the current user.
        val user = (application as MyApplication).user

        // Allows getting the location.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (user.requiresLocation)
        {
            val lm = getSystemService(LOCATION_SERVICE) as LocationManager

            // Check if the location service is enabled.
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.w(TAG, "Location services are not enabled")
                showDialog("Location services are not enabled")
            }

            // Build a dialog to tell the user we are getting the location
            val builder = AlertDialog.Builder(this)
            builder.setCancelable(false)
            builder.setMessage("Getting your location")
            val dialog = builder.create()

            // Attempt to get location updates.
            val locationRequest = LocationRequest.create()
            locationRequest.interval = (5 * 1000).toLong()
            locationRequest.fastestInterval = (1 * 1000).toLong()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    Log.d(TAG, "Location callback triggered")

                    Log.d(TAG, "Location data acquired")
                    Log.i(TAG, "Stopping location updates and getting coordinates")

                    // Stop getting location updates.
                    fusedLocationClient?.removeLocationUpdates(locationCallback!!)

                    val location = locationResult.locations[0]

                    // Get the coordinates of the location.
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude

                    runOnUiThread {
                        dialog.dismiss()
                    }
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

            thread(start = true) {
                runOnUiThread {
                    dialog.show()
                }

                // Start getting location updates.
                fusedLocationClient?.requestLocationUpdates(
                    locationRequest,
                    locationCallback!!,
                    Looper.getMainLooper()
                )
            }
        }

        // Get timezones from application and configure spinner.
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
        for (i in timezonesIds.indices) {
            if (timezonesIds[i].equals(user.timeZone, ignoreCase = true)) {
                spinnerTimeZone?.setSelection(i)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.i(TAG, "Stopping location updates and cancelling all queued requests")

        // Ensure requests do not continue in the background.
        MySingleton.getInstance(this).requestQueue.cancelAll(TAG)

        // Stop getting location updates.
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient!!.removeLocationUpdates(locationCallback!!)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCancelClick(view: View?) {
        val intent = Intent(this, StatusActivity::class.java)
        startActivity(intent)
        finish()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onContinueClick(view: View?) {
        if (buttonConfirmOutContinue?.isEnabled == true) {
            thread(start = true) {
                save()
            }
        }

        buttonConfirmOutContinue?.isEnabled = false // Prevent double submission
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
                        // TODO Go back to save
                    }
                } else {
                    Log.w(TAG, "Does not have permission, cannot continue")
                    showDialog("Location permission has been denied but is required by BRIZBEE")
                }
                return
            }
        }
    }

    private fun save() {
        try {
            val user = (application as MyApplication).user

            // If coordinates are not available, but the user location is required,
            // then prompt the user and do not continue.
            if (user.requiresLocation && currentLatitude == 0.0 && currentLatitude == 0.0) {
                showDialog("Location is not available but is required by BRIZBEE")
                return
            }

            runOnUiThread {
                buttonConfirmOutContinue?.isEnabled = false // Prevent double submission

                // Prepare a progress dialog.
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Saving")
                builder.setCancelable(false)
                progressDialog = builder.create()
                progressDialog?.setCancelable(false)
                progressDialog?.setCanceledOnTouchOutside(false)
                progressDialog?.show()
            }

            val intent = Intent(this, StatusActivity::class.java)

            // Instantiate the RequestQueue.
            val builder = StringBuilder()
            builder.append("${(application as MyApplication).baseUrl}/api/Kiosk/PunchOut")
                .append("?sourceHardware=Mobile")
                .append("&timeZone=${selectedTimeZone}")
                .append("&sourceOperatingSystem=Android")
                .append("&sourceOperatingSystemVersion=${Build.VERSION.RELEASE}")
                .append("&sourceBrowser=N/A")
                .append("&sourceBrowserVersion=N/A")

            if (currentLatitude != 0.0 && currentLatitude != 0.0) {
                builder.append("&latitude=$currentLatitude")
                builder.append("&longitude=$currentLongitude")
            } else {
                builder.append("&latitude=")
                builder.append("&longitude=")
            }

            val request: JsonObjectRequest = object : JsonObjectRequest(Method.POST, builder.toString(), null,
                { response ->
                    runOnUiThread {
                        progressDialog?.dismiss()
                        startActivity(intent)
                        finish()
                    }
                }, { error ->
                    runOnUiThread {
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
                    return (application as MyApplication).authHeaders
                }
            }

            // Increase number of retries because we may be on a poor connection.
            request.retryPolicy = DefaultRetryPolicy(10000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

            // Add the request to the RequestQueue.
            request.tag = TAG
            MySingleton.getInstance(this).addToRequestQueue(request)
        } catch (e: Exception) {
            Log.e(TAG, e.toString())

            runOnUiThread {
                progressDialog?.dismiss()
                buttonConfirmOutContinue?.isEnabled = true // Return control to user
            }
        }
    }

    private fun showDialog(message: String) {
        runOnUiThread {
            // Build a dialog with the given message to show the user
            val builder = AlertDialog.Builder(this)
            builder.setMessage(message)
                .setPositiveButton(
                    "OK"
                ) { dialog, _ -> dialog.dismiss() }
            val dialog = builder.create()
            dialog.show()
        }
    }
}