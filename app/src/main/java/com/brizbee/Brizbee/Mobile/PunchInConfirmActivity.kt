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
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.location.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*
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
    private var locationCallback: LocationCallback? = null
    private var spinnerTimeZone: Spinner? = null
    private lateinit var timezonesIds: Array<String?>
    private var selectedTimeZone: String? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationAllowed = true
    var progressDialog: AlertDialog? = null

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
        if (buttonConfirmInContinue?.isEnabled == true) {
            thread(start = true) {
                getLocation()
            }
        }

        buttonConfirmInContinue?.isEnabled = false // Prevent double submission
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

                    buttonConfirmInContinue?.isEnabled = true // Return control to user
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
                            progressDialog?.dismiss() // No need to return control to user
                        }

                        save()

                        return
                    }

                    // Stop getting location updates.
                    fusedLocationClient!!.removeLocationUpdates(locationCallback!!)

                    val location = locationResult.locations[0]

                    // Get the coordinates of the location.
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude

                    Log.i(TAG, "Saving with the location")

                    runOnUiThread {
                        progressDialog?.dismiss() // No need to return control to user
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
            if (!locationAllowed) {
                buttonConfirmInContinue?.isEnabled = true // Return control to user
                return
            }

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
                buttonConfirmInContinue?.isEnabled = true // Return control to user
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
        builder.append("${(application as MyApplication).baseUrl}/api/Kiosk/PunchIn")
            .append("?taskId=${task!!["Id"]}")
            .append("&sourceHardware=Mobile")
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