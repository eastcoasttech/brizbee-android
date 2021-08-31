//
//  TimeCardActivity.kt
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

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.*
import com.android.volley.toolbox.JsonArrayRequest
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
import kotlin.concurrent.thread

class TimeCardActivity : AppCompatActivity() {
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
    var progressDialog: AlertDialog? = null

    companion object {
        val TAG = TimeCardActivity::class.qualifiedName
    }

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

        // Refresh the customers, projects, then tasks.
        thread(start = true) {
            refreshCustomers()
        }

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
        thread(start = true) {
            save()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCancelClick(view: View?) {
        val intent = Intent(this, StatusActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun refreshCustomers() {
        // Instantiate the RequestQueue.
        val url = "${(application as MyApplication).baseUrl}/api/Kiosk/Customers"

        val request: JsonArrayRequest = object : JsonArrayRequest(Method.GET, url, null,
            { response ->
                try {
                    // Format for parsing timestamps from server
                    val dfServer: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.ENGLISH)

                    // Build a customer object for each item in the response
                    val customers: MutableList<Customer> = ArrayList()
                    for (i in 0 until response.length()) {
                        val j = response.getJSONObject(i)
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
            }, { error ->
                val response = error.networkResponse
                when (response.statusCode) {
                    else -> showDialog("Could not reach the server, please try again.")
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

    private fun refreshJobs() {
        // Instantiate the RequestQueue.
        val builder = StringBuilder()
        builder.append("${(application as MyApplication).baseUrl}/api/Kiosk/Projects")
            .append("?customerId=${selectedCustomer!!.id}")

        val request: JsonArrayRequest = object : JsonArrayRequest(Method.GET, builder.toString(), null,
            { response ->
                try {
                    // Format for parsing timestamps from server
                    val dfServer: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.ENGLISH)

                    // Build a job object for each item in the response
                    val jobs: MutableList<Job> = ArrayList()
                    for (i in 0 until response.length()) {
                        val j = response.getJSONObject(i)
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
            }, { error ->
                val response = error.networkResponse
                when (response.statusCode) {
                    else -> showDialog("Could not reach the server, please try again.")
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

    private fun refreshTasks() {
        // Instantiate the RequestQueue.
        val builder = StringBuilder()
        builder.append("${(application as MyApplication).baseUrl}/api/Kiosk/Tasks")
            .append("?projectId=${selectedJob!!.id}")

        val request: JsonArrayRequest = object : JsonArrayRequest(Method.GET, builder.toString(), null,
            { response ->
                try {
                    // Format for parsing timestamps from server
                    val dfServer: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.ENGLISH)

                    // Build a task object for each item in the response
                    val tasks: MutableList<Task> = ArrayList()
                    for (i in 0 until response.length()) {
                        val j = response.getJSONObject(i)
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
            }, { error ->
                val response = error.networkResponse
                when (response.statusCode) {
                    else -> showDialog("Could not reach the server, please try again.")
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

    private fun save() {
        runOnUiThread {
            // Prepare a progress dialog.
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Working")
            builder.setCancelable(false)
            progressDialog = builder.create()
            progressDialog?.setCancelable(false)
            progressDialog?.setCanceledOnTouchOutside(false)
            progressDialog?.show()
        }

        val minutes = selectedMinute + selectedHour * 60
        val enteredAt = editDate?.text

        val intent = Intent(this, StatusActivity::class.java)

        // Instantiate the RequestQueue.
        val builder = StringBuilder()
        builder.append("${(application as MyApplication).baseUrl}/api/Kiosk/Timecard")
            .append("?taskId=${selectedTask!!.id}")
            .append("&minutes=${minutes}")
            .append("&enteredAt=${enteredAt}")

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
                        400 -> {
                            progressDialog?.dismiss()
                            val data = JSONObject(String(response.data))
                            showDialog(data.getString("Message"))
                        }
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