package com.brizbee.Brizbee.Mobile

import android.content.Intent
import android.os.Bundle
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
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws

class StatusActivity : AppCompatActivity() {

    companion object {
        val TAG = StatusActivity::class.qualifiedName
    }

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
        val url = "https://app-brizbee-prod.azurewebsites.net/odata/Punches/Default.Current?\$expand=Task(\$expand=Job(\$expand=Customer))"

        val jsonRequest: JsonObjectRequest = object : JsonObjectRequest(Method.GET, url, null,
            Response.Listener { response: JSONObject ->
                try {
                    // Format for parsing timestamps from server
                    val dfServer: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
                    val dfHuman: DateFormat = SimpleDateFormat("MMM dd, yyyy h:mma", Locale.ENGLISH)
                    val value = response.getJSONArray("value")
                    if (value.length() > 0) {
                        val first = value.getJSONObject(0)
                        val task = first.getJSONObject("Task")
                        val job = task.getJSONObject("Job")
                        val customer = job.getJSONObject("Customer")

                        // Set color and text of status
                        textStatus!!.setTextColor(ContextCompat.getColor(baseContext, R.color.colorGreenDark))
                        textStatus!!.setText(R.string.status_punched_in)
                        textStatus!!.visibility = View.VISIBLE
                        textHello!!.visibility = View.VISIBLE

                        // Task Number and Name
                        textTask!!.text = String.format("%s - %s",
                                task.getString("Number"),
                                task.getString("Name"))
                        textTaskHeader!!.visibility = View.VISIBLE
                        textTask!!.visibility = View.VISIBLE

                        // Customer Number and Name
                        textCustomer!!.text = String.format("%s - %s",
                                customer.getString("Number"),
                                customer.getString("Name"))
                        textCustomerHeader!!.visibility = View.VISIBLE
                        textCustomer!!.visibility = View.VISIBLE

                        // Job Number and Name
                        textJob!!.text = String.format("%s - %s",
                                job.getString("Number"),
                                job.getString("Name"))
                        textJobHeader!!.visibility = View.VISIBLE
                        textJob!!.visibility = View.VISIBLE

                        // Format the since timestamp
                        val since = dfServer.parse(first.getString("InAt"))
                        textSince!!.text = dfHuman.format(since!!)
                        textTimeZone!!.text = first.getString("InAtTimeZone")
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
            },
            Response.ErrorListener { error: VolleyError ->
                val response = error.networkResponse
                when (response.statusCode) {
                    else -> showDialog("Could not reach the server, please try again.")
                }
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                val authExpiration = (application as MyApplication).authExpiration
                val authToken = (application as MyApplication).authToken
                val authUserId = (application as MyApplication).authUserId
                if (authExpiration != null && !authExpiration.isEmpty() && authToken != null && !authToken.isEmpty() && authUserId != null && !authUserId.isEmpty()) {
                    headers["AUTH_EXPIRATION"] = authExpiration
                    headers["AUTH_TOKEN"] = authToken
                    headers["AUTH_USER_ID"] = authUserId
                }
                return headers
            }
        }

        // Increase number of retries because we may be on a poor connection
        val socketTimeout = 10000
        val policy: RetryPolicy = DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        jsonRequest.retryPolicy = policy

        // Add the request to the RequestQueue
        jsonRequest.tag = TimeCardActivity.TAG
        MySingleton.getInstance(this).addToRequestQueue(jsonRequest)
    }

    private fun loadUser() {
        val user = (application as MyApplication).user
        if (user != null) {
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
    }

    private fun showDialog(message: String) {
        // Build a dialog with the given message to show the user
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }
}