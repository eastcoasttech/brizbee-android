package com.brizbee.Brizbee.Mobile

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Log.WARN
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.brizbee.Brizbee.Mobile.models.Customer
import com.brizbee.Brizbee.Mobile.models.Job
import com.brizbee.Brizbee.Mobile.models.Task
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class TimeCardActivity : AppCompatActivity() {

    companion object {
        val TAG = TimeCardActivity::class.qualifiedName
    }

    private var editDate: EditText? = null
    private var spinnerMCustomer: Spinner? = null
    private var spinnerMJob: Spinner? = null
    private var spinnerMTask: Spinner? = null
    private var spinnerHours: Spinner? = null
    private var spinnerMinutes: Spinner? = null
    private var selectedCustomer: Customer? = null
    private var selectedJob: Job? = null
    private var selectedTask: Task? = null
    private var selectedHour = 0
    private var selectedMinute = 0
    private var datePicker: DatePickerDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_card)

        // Populate hours
        val hours = arrayOfNulls<Int>(24)
        Arrays.setAll(hours) { index: Int -> 0 + index }

        // Populate minutes
        val minutes = arrayOfNulls<Int>(60)
        Arrays.setAll(minutes) { index: Int -> 0 + index }

        // Get references from layouts
        spinnerMCustomer = findViewById(R.id.spinnerMCustomer)
        spinnerMJob = findViewById(R.id.spinnerMJob)
        spinnerMTask = findViewById(R.id.spinnerMTask)
        spinnerHours = findViewById(R.id.spinnerHours)
        spinnerMinutes = findViewById(R.id.spinnerMinutes)
        editDate = findViewById(R.id.editDate)

        // Configure the hours spinner
        spinnerHours!!.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View,
                                        position: Int, id: Long) {
                // Store the selected item
                val sid = spinnerHours!!.selectedItemPosition
                selectedHour = hours[sid]!!
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
        val hourAdapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, hours)
        spinnerHours!!.adapter = hourAdapter

        // Configure the minutes spinner
        spinnerMinutes!!.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View,
                                        position: Int, id: Long) {
                // Store the selected item
                val sid = spinnerMinutes!!.selectedItemPosition
                selectedMinute = minutes[sid]!!
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
        val minuteAdapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, minutes)
        spinnerMinutes!!.adapter = minuteAdapter

        // Set the default date to today
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        editDate!!.setText(dateFormatter.format(Date().time))

        // Configure date picker
        val newCalendar = Calendar.getInstance()
        datePicker = DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->
            val newDate = Calendar.getInstance()
            newDate[year, monthOfYear] = dayOfMonth
            editDate!!.setText(dateFormatter.format(newDate.time))
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH))

        editDate!!.setOnClickListener { datePicker!!.show() }

        // Refresh the customer list
        refreshCustomers()

        // Focus on the hours
        spinnerHours!!.clearFocus()
        spinnerHours!!.requestFocus()
    }

    override fun onStop() {
        super.onStop()
        MySingleton.getInstance(this).requestQueue.cancelAll(TAG)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onContinueClick(view: View?) {
        save()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCancelClick(view: View?) {
        // Take the user to the status activity
        val intent = Intent(this, StatusActivity::class.java)
        startActivity(intent)
        finish() // prevents going back
    }

    private fun refreshCustomers() {
        val url = "https://app-brizbee-prod.azurewebsites.net/odata/Customers?\$count=true&\$orderby=Number&\$top=20&\$skip=0"

        val jsonRequest: JsonObjectRequest = object : JsonObjectRequest(Method.GET, url, null,
                Response.Listener { response: JSONObject ->
                    try {
                        // Format for parsing timestamps from server
                        val dfServer: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'", Locale.ENGLISH)

                        // Build a customer object for each item in the response
                        val customers: MutableList<Customer> = ArrayList()
                        val value = response.getJSONArray("value")
                        for (i in 0 until value.length()) {
                            val j = value.getJSONObject(i)
                            val customer = Customer()
                            customer.name = j.getString("Name")
                            customer.number = j.getString("Number")
                            customer.id = j.getInt("Id")
                            val createdAt = dfServer.parse(j.getString("CreatedAt"))
                            customer.createdAt = createdAt
                            customers.add(customer)
                        }

                        // Update the list of customers and configure the spinner
                        val adapter = ArrayAdapter(this,
                                android.R.layout.simple_spinner_dropdown_item, customers)
                        spinnerMCustomer!!.adapter = adapter
                        spinnerMCustomer!!.onItemSelectedListener = object : OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View,
                                                        position: Int, id: Long) {
                                // Store the selected item
                                val sid = spinnerMCustomer!!.selectedItemPosition
                                selectedCustomer = customers[sid]
                                Log.i(TAG, selectedCustomer!!.name)

                                // Refresh job list
                                refreshJobs()
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {
                                // Do nothing
                            }
                        }
                    } catch (e: JSONException) {
                        showDialog("Could not understand the response from the server, please try again.")
                    } catch (e: ParseException) {
                        showDialog("Could not understand the response from the server, please try again.")
                    }
                }, Response.ErrorListener { error: VolleyError ->
            val response = error.networkResponse
            when (response.statusCode) {
                else -> showDialog("Could not reach the server, please try again.")
            }
        }
        ) {
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

        // Increase number of retries because we may be on a poor connection
        val socketTimeout = 10000
        val policy: RetryPolicy = DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        jsonRequest.retryPolicy = policy

        // Add the request to the RequestQueue
        jsonRequest.tag = TAG
        MySingleton.getInstance(this).addToRequestQueue(jsonRequest)
    }

    private fun refreshJobs() {
        val url = "https://app-brizbee-prod.azurewebsites.net/odata/Jobs?\$count=true&\$orderby=Number&\$top=20&\$skip=0&\$filter=CustomerId eq " + selectedCustomer!!.id

        val jsonRequest: JsonObjectRequest = object : JsonObjectRequest(Method.GET, url, null,
                Response.Listener { response: JSONObject ->
                    try {
                        // Format for parsing timestamps from server
                        val dfServer: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'", Locale.ENGLISH)

                        // Build a job object for each item in the response
                        val jobs: MutableList<Job> = ArrayList()
                        val value = response.getJSONArray("value")
                        for (i in 0 until value.length()) {
                            val j = value.getJSONObject(i)
                            val job = Job()
                            job.name = j.getString("Name")
                            job.number = j.getString("Number")
                            job.id = j.getInt("Id")
                            val createdAt = dfServer.parse(j.getString("CreatedAt"))
                            job.createdAt = createdAt
                            jobs.add(job)
                        }

                        // Update the list of jobs and configure the spinner
                        val adapter = ArrayAdapter(this,
                                android.R.layout.simple_spinner_dropdown_item, jobs)
                        spinnerMJob!!.adapter = adapter
                        spinnerMJob!!.onItemSelectedListener = object : OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View,
                                                        position: Int, id: Long) {
                                // Store the selected item
                                val sid = spinnerMJob!!.selectedItemPosition
                                selectedJob = jobs[sid]
                                Log.i(TAG, selectedJob!!.name)

                                // Refresh task list
                                refreshTasks()
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {
                                // Do nothing
                            }
                        }
                    } catch (e: JSONException) {
                        showDialog("Could not understand the response from the server, please try again.")
                    } catch (e: ParseException) {
                        showDialog("Could not understand the response from the server, please try again.")
                    }
                }, Response.ErrorListener { error: VolleyError ->
            val response = error.networkResponse
            when (response.statusCode) {
                else -> showDialog("Could not reach the server, please try again.")
            }
        }
        ) {
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

        // Increase number of retries because we may be on a poor connection
        val socketTimeout = 10000
        val policy: RetryPolicy = DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        jsonRequest.retryPolicy = policy

        // Add the request to the RequestQueue
        jsonRequest.tag = TAG
        MySingleton.getInstance(this).addToRequestQueue(jsonRequest)
    }

    private fun refreshTasks() {
        val url = "https://app-brizbee-prod.azurewebsites.net/odata/Tasks?\$count=true&\$orderby=Number&\$top=20&\$skip=0&\$filter=JobId eq " + selectedJob!!.id

        val jsonRequest: JsonObjectRequest = object : JsonObjectRequest(Method.GET, url, null,
                Response.Listener { response: JSONObject ->
                    try {
                        // Format for parsing timestamps from server
                        val dfServer: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'", Locale.ENGLISH)

                        // Build a task object for each item in the response
                        val tasks: MutableList<Task> = ArrayList()
                        val value = response.getJSONArray("value")
                        for (i in 0 until value.length()) {
                            val j = value.getJSONObject(i)
                            val task = Task()
                            task.name = j.getString("Name")
                            task.number = j.getString("Number")
                            task.id = j.getInt("Id")
                            val createdAt = dfServer.parse(j.getString("CreatedAt"))
                            task.createdAt = createdAt
                            tasks.add(task)
                        }

                        // Update the list of tasks and configure the spinner
                        val adapter = ArrayAdapter(this,
                                android.R.layout.simple_spinner_dropdown_item, tasks)
                        spinnerMTask!!.adapter = adapter
                        spinnerMTask!!.onItemSelectedListener = object : OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View,
                                                        position: Int, id: Long) {
                                // Store the selected item
                                val sid = spinnerMTask!!.selectedItemPosition
                                selectedTask = tasks[sid]
                                Log.i(TAG, selectedTask!!.name)
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {
                                // Do nothing
                            }
                        }
                    } catch (e: JSONException) {
                        showDialog("Could not understand the response from the server, please try again.")
                    } catch (e: ParseException) {
                        showDialog("Could not understand the response from the server, please try again.")
                    }
                }, Response.ErrorListener { error: VolleyError ->
            val response = error.networkResponse
            when (response.statusCode) {
                else -> showDialog("Could not reach the server, please try again.")
            }
        }
        ) {
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

        // Increase number of retries because we may be on a poor connection
        val socketTimeout = 10000
        val policy: RetryPolicy = DefaultRetryPolicy(socketTimeout, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        jsonRequest.retryPolicy = policy

        // Add the request to the RequestQueue
        jsonRequest.tag = TAG
        MySingleton.getInstance(this).addToRequestQueue(jsonRequest)
    }

    private fun save() {
        val intent = Intent(this, StatusActivity::class.java)
        val minutes = selectedMinute + selectedHour * 60
        val enteredAt = editDate?.text

        val url = "https://app-brizbee-prod.azurewebsites.net/odata/TimesheetEntries/Default.Add"
        val jsonBody = JSONObject()
        try {
            jsonBody.put("TaskId", selectedTask!!.id)
            jsonBody.put("Minutes", minutes)
            jsonBody.put("EnteredAt", enteredAt)
            jsonBody.put("Notes", "")
        } catch (e: JSONException) {
            showDialog("Could not prepare the request to the server, please try again.")
        }
        val jsonRequest: JsonObjectRequest = object : JsonObjectRequest(Method.POST, url, jsonBody,
            Response.Listener {
                // Take the user to the status activity
                startActivity(intent)
                finish() // prevents going back
            }, Response.ErrorListener { error ->
                val response = error.networkResponse
                when (response.statusCode) {
                    else -> {
                        val content = String(response.data)
                        Log.w(TAG, content)
                        showDialog("Could not reach the server, please try again.")
                    }
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
                if (authExpiration != null && authExpiration.isNotEmpty() && authToken != null && authToken.isNotEmpty() && authUserId != null && authUserId.isNotEmpty()) {
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
        jsonRequest.tag = TAG
        MySingleton.getInstance(this).addToRequestQueue(jsonRequest)
    }

    private fun showDialog(message: String) {
        // Build a dialog with the given message to show the user
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }
}