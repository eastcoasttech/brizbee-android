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
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.EditText
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONException
import kotlin.Throws
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.android.volley.*
import java.util.HashMap

class PunchInTaskIdActivity : AppCompatActivity() {
    private var buttonScan: Button? = null
    private var editTaskNumber: EditText? = null

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanContent = result.data?.extras?.get("BarCodeValue").toString()
            editTaskNumber?.setText(scanContent)

            // Verify that the barcode is valid.
            confirm()
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

        // Prevent going back.
        finish()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onContinueClick(view: View?) {
        confirm()
    }

    private fun confirm() {
        // Lookup the task number.
        val intent = Intent(this, PunchInConfirmActivity::class.java)
        val editTaskNumber = findViewById<EditText>(R.id.editTaskNumber)

        // Instantiate the RequestQueue
        val url = String.format(
            "https://app-brizbee-prod.azurewebsites.net/odata/Tasks/Default.Search(Number='%s')",
            editTaskNumber.text
        )
        val jsonRequest: JsonObjectRequest = object : JsonObjectRequest(
            Method.GET, url, null, Response.Listener { response ->
                try {
                    if (response == null)
                        // Notify the user that the task number does not exist
                        showDialog("That's not a valid task number, please try again.")

                    // Pass the task as a string
                    intent.putExtra("task", response.toString())
                    startActivity(intent)
                } catch (e: JSONException) {
                    showDialog("Could not understand the response from the server, please try again.")
                }
            },
            Response.ErrorListener { error ->
                val response = error.networkResponse
                when (response.statusCode) {
                    else -> showDialog("Could not reach the server, please try again.")
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
        val builder = AlertDialog.Builder(this)
        builder
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }
}
