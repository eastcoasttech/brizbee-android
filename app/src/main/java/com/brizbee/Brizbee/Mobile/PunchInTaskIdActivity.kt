//
//  PunchInTaskIdActivity.kt
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

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.EditText
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONException
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ExperimentalGetImage
import com.android.volley.*
import java.util.HashMap
import kotlin.concurrent.thread

@ExperimentalGetImage
class PunchInTaskIdActivity : AppCompatActivity() {
    private var buttonScan: Button? = null
    private var editTaskNumber: EditText? = null
    var progressDialog: AlertDialog? = null

    companion object {
        val TAG = PunchInTaskIdActivity::class.qualifiedName
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanContent = result.data?.extras?.get("BarCodeValue").toString()
            editTaskNumber?.setText(scanContent)

            // Verify that the barcode is valid.
            thread(start = true) {
                confirm()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_punch_in_task_id)

        // Get references from layouts.
        buttonScan = findViewById(R.id.buttonScan)
        editTaskNumber = findViewById(R.id.editTaskNumber)

        // Respond to button click.
        buttonScan?.setOnClickListener {
            // Start the barcode scanner.
            startForResult.launch(Intent(this, BarcodeScanActivity::class.java))
        }

        // Focus on the task number.
        if (editTaskNumber?.requestFocus() == true) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
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
            confirm()
        }
    }

    private fun confirm() {
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

        // Lookup the task number.
        val editTaskNumber = findViewById<EditText>(R.id.editTaskNumber)

        // Instantiate the RequestQueue.
        val url = "${(application as MyApplication).baseUrl}/api/Kiosk/SearchTasks?taskNumber=${editTaskNumber.text}"

        val request: JsonObjectRequest = object : JsonObjectRequest(Method.GET, url, null,
            { response ->
                runOnUiThread {
                    try {
                        if (response.getJSONObject("job").getString("status").uppercase() != "OPEN") {
                            // Notify the user that the project is not open
                            showDialog("The project for that task number is not open, please try again.")
                            progressDialog?.dismiss()
                        } else {
                            val intent = Intent(this, PunchInConfirmActivity::class.java)

                            // Pass the task as a string
                            intent.putExtra("task", response.toString())
                            startActivity(intent)
                        }
                    } catch (e: JSONException) {
                        runOnUiThread {
                            progressDialog?.dismiss()
                            showDialog("Could not understand the response from the server, please try again.")
                        }
                    }
                }
            }, { error ->
                runOnUiThread {
                    val response = error.networkResponse
                    when (response.statusCode) {
                        404 -> {
                            progressDialog?.dismiss()
                            showDialog("That's not a valid task number, please try again.")
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
