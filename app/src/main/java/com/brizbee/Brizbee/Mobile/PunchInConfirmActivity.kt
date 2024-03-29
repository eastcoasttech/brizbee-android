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
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.DefaultRetryPolicy
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class PunchInConfirmActivity : AppCompatActivity() {
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var textConfirmTask: TextView? = null
    private var textConfirmCustomer: TextView? = null
    private var textConfirmJob: TextView? = null
    private var buttonConfirmInContinue: Button? = null
    private var task: JSONObject? = null
    private var job: JSONObject? = null
    private var customer: JSONObject? = null
    private var spinnerTimeZone: Spinner? = null
    private lateinit var timezonesIds: Array<String?>
    private var selectedTimeZone: String? = null
    private var locationAllowed = true
    var progressDialog: AlertDialog? = null

    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // LocationRequest - Requirements for the location updates, i.e., how often you
    // should receive updates, the priority, etc.
    private lateinit var locationRequest: LocationRequest

    // LocationCallback - Called when FusedLocationProviderClient has a new Location.
    private lateinit var locationCallback: LocationCallback

    // Used only for local storage of the last known location. Usually, this would be saved to your
    // database, but because this is a simplified sample without a full database, we only need the
    // last location to create a Notification if the user navigates away from the app.
    private var currentLocationResult: LocationResult? = null

    companion object {
        val TAG = PunchInConfirmActivity::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_punch_in_confirm)

        // Get references from layouts.
        spinnerTimeZone = findViewById(R.id.spinnerTimeZone)
        textConfirmTask = findViewById(R.id.textConfirmTask)
        textConfirmCustomer = findViewById(R.id.textConfirmCustomer)
        textConfirmJob = findViewById(R.id.textConfirmJob)
        buttonConfirmInContinue = findViewById(R.id.buttonConfirmInContinue)

        // Get the current user.
        val user = (application as MyApplication).user

        try {
            task = JSONObject(intent.getStringExtra("task")!!)
            job = task?.getJSONObject("job")
            customer = job?.getJSONObject("customer")

            // Task Number and Name
            textConfirmTask?.text = String.format(
                "%s - %s",
                task?.getString("number"),
                task?.getString("name")
            )

            // Customer Number and Name
            textConfirmCustomer?.text = String.format(
                "%s - %s",
                customer?.getString("number"),
                customer?.getString("name")
            )

            // Job Number and Name
            textConfirmJob?.text = String.format(
                "%s - %s",
                job?.getString("number"),
                job?.getString("name")
            )
        } catch (e: JSONException) {
            showDialog("Could not display the task you selected, please go back and try again.")
        }

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
            val intervalMillis = TimeUnit.SECONDS.toMillis(5)
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis).apply {
                setDurationMillis(TimeUnit.SECONDS.toMillis(30))
                setMaxUpdateAgeMillis(TimeUnit.SECONDS.toMillis(60))
            }.build()

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
                    Log.d(TAG, "Showing dialog")
                    dialog.show()
                }

                Log.i(TAG, "Requesting location updates")

                // Allows getting the location.
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)

                        Log.d(TAG, "Location callback triggered")

                        Log.i(TAG, "Stopping location updates and attempting to get coordinates")

                        // Stop getting location updates.
                        fusedLocationProviderClient.removeLocationUpdates(locationCallback)

                        val currentLocation = locationResult.lastLocation

                        // Get the coordinates of the location.
                        if (currentLocation != null) {
                            currentLatitude = currentLocation.latitude
                            currentLongitude = currentLocation.longitude
                        }

                        runOnUiThread {
                            Log.d(TAG, "Dismissing dialog")
                            dialog.dismiss()
                        }
                    }
                }

                Log.d(TAG, "Starting to request updates")

                // Start getting location updates.
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
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
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCancelClick(view: View?) {
        val intent = Intent(this, StatusActivity::class.java)
        startActivity(intent)
        finish()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onContinueClick(view: View?) {
        if (buttonConfirmInContinue?.isEnabled == true) {
            thread(start = true) {
                save()
            }
        }

        buttonConfirmInContinue?.isEnabled = false // Prevent double submission
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
            if (user.requiresLocation && currentLatitude == 0.0 && currentLongitude == 0.0) {
                showDialog("Location is not available but is required by BRIZBEE")

                runOnUiThread {
                    buttonConfirmInContinue?.isEnabled = true // Return control to user
                }

                return
            }

            runOnUiThread {
                buttonConfirmInContinue?.isEnabled = false // Prevent double submission

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
            builder.append("${(application as MyApplication).baseUrl}/api/Kiosk/PunchIn")
                .append("?taskId=${task!!["id"]}")
                .append("&sourceHardware=Mobile")
                .append("&timeZone=${selectedTimeZone}")
                .append("&sourceOperatingSystem=Android")
                .append("&sourceOperatingSystemVersion=${Build.VERSION.RELEASE}")
                .append("&sourceBrowser=N/A")
                .append("&sourceBrowserVersion=N/A")

            if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                builder.append("&latitude=$currentLatitude")
                builder.append("&longitude=$currentLongitude")
            } else {
                builder.append("&latitude=")
                builder.append("&longitude=")
            }

            val request: MyJsonObjectRequest = object : MyJsonObjectRequest(Method.POST, builder.toString(), null,
                {
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
                buttonConfirmInContinue?.isEnabled = true // Return control to user
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