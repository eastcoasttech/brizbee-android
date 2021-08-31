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
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.android.volley.*
import java.lang.Exception
import java.util.HashMap
import kotlin.concurrent.thread

class PunchOutConfirmActivity : AppCompatActivity() {
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
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

        // Allows getting the location.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
        finish()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onContinueClick(view: View?) {
        thread(start = true) {
            getLocation()
        }
    }

    private fun getLocation() {
        var locationEnabled = true

        runOnUiThread {
            // Prepare a progress dialog.
            val builder = AlertDialog.Builder(this)
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
            locationRequest.interval = (5 * 1000).toLong()
            locationRequest.fastestInterval = (1 * 1000).toLong()
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
        runOnUiThread {
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