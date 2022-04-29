//
//  StatusActivity.kt
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
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONException
import org.json.JSONObject
import java.lang.NullPointerException
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws

class StatusActivity : AppCompatActivity() {
    private var textHello: TextView? = null
    private var textStatus: TextView? = null
    private var textTask: TextView? = null
    private var textTaskHeader: TextView? = null
    private var textCustomer: TextView? = null
    private var textCustomerHeader: TextView? = null
    private var textJob: TextView? = null
    private var textJobHeader: TextView? = null
    private var textSince: TextView? = null
    private var textSinceHeader: TextView? = null
    private var textTimeZone: TextView? = null
    private var buttonPunchOut: Button? = null
    private var buttonPunchIn: Button? = null
    private var buttonLogout: Button? = null
    private var buttonManualEntry: Button? = null
    private var buttonInventory: Button? = null
    private var isTimeCardEnabled: Boolean = false

    companion object {
        val TAG = StatusActivity::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        // Get references from layouts
        textHello = findViewById(R.id.textHello)
        textStatus = findViewById(R.id.textStatus)
        textTask = findViewById(R.id.textTask)
        textTaskHeader = findViewById(R.id.textTaskHeader)
        textCustomer = findViewById(R.id.textCustomer)
        textCustomerHeader = findViewById(R.id.textCustomerHeader)
        textJob = findViewById(R.id.textJob)
        textJobHeader = findViewById(R.id.textJobHeader)
        textSince = findViewById(R.id.textSince)
        textSinceHeader = findViewById(R.id.textSinceHeader)
        textTimeZone = findViewById(R.id.textTimeZone)
        buttonPunchOut = findViewById(R.id.buttonPunchOut)
        buttonPunchIn = findViewById(R.id.buttonPunchIn)
        buttonLogout = findViewById(R.id.buttonLogout)
        buttonManualEntry = findViewById(R.id.buttonManualEntry)
        buttonInventory = findViewById(R.id.buttonInventory)

        loadUser()
        loadStatus()
    }

    override fun onStop() {
        super.onStop()
        MySingleton.getInstance(this).requestQueue.cancelAll(TAG)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPunchInClick(view: View?) {
        val intent = Intent(this, PunchInTaskIdActivity::class.java)
        startActivity(intent)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPunchOutClick(view: View?) {
        val intent = Intent(this, PunchOutConfirmActivity::class.java)
        startActivity(intent)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onManualEntryClick(view: View?) {
        val intent = Intent(this, TimeCardActivity::class.java)
        startActivity(intent)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onInventoryClick(view: View?) {
        val intent = Intent(this, InventoryItemActivity::class.java)
        startActivity(intent)
    }

    @Suppress("UNUSED_PARAMETER")
    fun logout(view: View?) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // prevents back
    }

    private fun loadStatus() {

        // Instantiate the RequestQueue.
        val url = "${(application as MyApplication).baseUrl}/api/Kiosk/Punches/Current"

        val request: JsonObjectRequest = object : JsonObjectRequest(Method.GET, url, null,
            { response ->
                try {
                    // Format for parsing timestamps from server
                    val dfServer: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
                    val dfHuman: DateFormat = SimpleDateFormat("MMM dd, yyyy h:mma", Locale.ENGLISH)

                    if (response.length() > 0) {
                        val task = response.getJSONObject("task")
                        val job = task.getJSONObject("job")
                        val customer = job.getJSONObject("customer")

                        // Set color and text of status
                        textStatus!!.setTextColor(ContextCompat.getColor(baseContext, R.color.colorGreenDark))
                        textStatus!!.setText(R.string.status_punched_in)
                        textStatus!!.visibility = View.VISIBLE
                        textHello!!.visibility = View.VISIBLE

                        // Task Number and Name
                        textTask!!.text = String.format("%s - %s",
                                task.getString("number"),
                                task.getString("name"))
                        textTaskHeader!!.visibility = View.VISIBLE
                        textTask!!.visibility = View.VISIBLE

                        // Customer Number and Name
                        textCustomer!!.text = String.format("%s - %s",
                                customer.getString("number"),
                                customer.getString("name"))
                        textCustomerHeader!!.visibility = View.VISIBLE
                        textCustomer!!.visibility = View.VISIBLE

                        // Job Number and Name
                        textJob!!.text = String.format("%s - %s",
                                job.getString("number"),
                                job.getString("name"))
                        textJobHeader!!.visibility = View.VISIBLE
                        textJob!!.visibility = View.VISIBLE

                        // Format the since timestamp
                        val since = dfServer.parse(response.getString("inAt"))
                        textSince!!.text = dfHuman.format(since!!)
                        textTimeZone!!.text = response.getString("inAtTimeZone")
                        textSinceHeader!!.visibility = View.VISIBLE
                        textSince!!.visibility = View.VISIBLE
                        textTimeZone!!.visibility = View.VISIBLE

                        // Set visibility of buttons
                        buttonPunchIn!!.visibility = View.VISIBLE
                        buttonPunchOut!!.visibility = View.VISIBLE
                        buttonInventory!!.visibility = View.VISIBLE
                        buttonLogout!!.visibility = View.VISIBLE
                    } else {
                        // Set color and text of status
                        textStatus!!.setTextColor(ContextCompat.getColor(baseContext, R.color.colorRed))
                        textStatus!!.setText(R.string.status_punched_out)
                        textStatus!!.visibility = View.VISIBLE
                        textHello!!.visibility = View.VISIBLE

                        // Set visibility of buttons
                        buttonPunchIn!!.visibility = View.VISIBLE
                        buttonInventory!!.visibility = View.GONE
                        buttonLogout!!.visibility = View.VISIBLE
                    }

                    if (isTimeCardEnabled)
                    {
                        buttonManualEntry!!.visibility = View.VISIBLE
                    }

                } catch (e: JSONException) {
                    showDialog("Could not understand the response from the server, please try again.")
                } catch (e: ParseException) {
                    showDialog("Could not understand the response from the server, please try again.")
                }
            }, { error ->
                runOnUiThread {
                    val response = error.networkResponse
                    when (response.statusCode) {
                        else -> {
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

    private fun loadUser() {
        val user = (application as MyApplication).user

        textHello!!.text = String.format("Hello, %s", user.name)

        // Logout immediately if the user is not allowed to use mobile app.
        if (!user.usesMobileClock)
        {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // prevents back
        }

        // Determine if user can enter time cards.
        isTimeCardEnabled = user.usesTimesheets
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