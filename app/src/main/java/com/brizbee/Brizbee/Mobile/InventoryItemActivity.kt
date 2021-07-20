package com.brizbee.Brizbee.Mobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.google.zxing.integration.android.IntentIntegrator
import java.util.*
import kotlin.concurrent.thread

class InventoryItemActivity : AppCompatActivity() {
    private var buttonScan: Button? = null
    private var editBarCodeValue: EditText? = null

    companion object {
        val TAG = InventoryItemActivity::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory_item)

        // Get references from layouts
        buttonScan = findViewById(R.id.buttonScan)
        editBarCodeValue = findViewById<EditText>(R.id.editBarCodeValue)

        // Respond to button click
        buttonScan?.setOnClickListener { view ->

            if (view.id == R.id.buttonScan) {

                // Start scanning
                val scanIntegrator = IntentIntegrator(this)
                scanIntegrator.initiateScan()
            }
        }

        // Set focus
        if (editBarCodeValue?.requestFocus() == true) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    fun onClick(v: View) {

        // Respond to button click
        if (v.id == R.id.buttonScan) {

            // Start scanning
            val scanIntegrator = IntentIntegrator(this)
            scanIntegrator.initiateScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Retrieve the scan result
        val scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent)
        if (scanningResult != null) {
            val scanContent = scanningResult.contents
            editBarCodeValue?.setText("") // clear the text
            editBarCodeValue?.setText(scanContent) // set the scanned value
            confirm() // Verify that the task number is valid
        } else {
            showDialog("No bar code could be scanned, please try again.")
        }
    }

    override fun onStop() {
        super.onStop()
        MySingleton.getInstance(this).requestQueue.cancelAll(TAG)
    }

    fun onCancelClick(view: View?) {
        val intent = Intent(this, StatusActivity::class.java)
        startActivity(intent)
        finish() // Prevents going back
    }

    fun onContinueClick(view: View?) {
        thread(start = true) {
            confirm()
        }
    }

    private fun confirm() {
        val intent = Intent(this, InventoryQuantityActivity::class.java)

        // Instantiate the RequestQueue
        val url = String.format(
            "https://app-brizbee-prod.azurewebsites.net/api/QBDInventoryItems/Search?barCode=%s",
            editBarCodeValue?.text
        )
        val jsonRequest: JsonObjectRequest = object : JsonObjectRequest(
            Method.GET, url, null,
            Response.Listener { response ->
                val itemJSON = response.toString()

                runOnUiThread {
                    // Pass the item
                    intent.putExtra("item", itemJSON)
                    startActivity(intent)
                }
            },
            Response.ErrorListener { error ->
                val response = error.networkResponse
                when (response.statusCode) {
                    404 -> showDialog("No item matches that bar code value.")
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
                if (authExpiration != null && !authExpiration.isEmpty() && authToken != null && !authToken.isEmpty() && authUserId != null && !authUserId.isEmpty()) {
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

        // Add the request to the RequestQueue
        MySingleton.getInstance(this).addToRequestQueue(jsonRequest)
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
