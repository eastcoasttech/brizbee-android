//
//  InventoryItemActivity.kt
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
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import kotlin.concurrent.thread

class InventoryItemActivity : AppCompatActivity() {
    private var buttonScan: Button? = null
    private var editBarCodeValue: EditText? = null
    var progressDialog: AlertDialog? = null

    companion object {
        val TAG = InventoryItemActivity::class.qualifiedName
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanContent = result.data?.extras?.get("BarCodeValue").toString()
            editBarCodeValue?.setText(scanContent)

            // Verify that the barcode is valid.
            thread(start = true) {
                confirm()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory_item)

        // Get references from layouts.
        buttonScan = findViewById(R.id.buttonScan)
        editBarCodeValue = findViewById(R.id.editBarCodeValue)

        // Respond to button click.
        buttonScan?.setOnClickListener {
            // Start the barcode scanner.
            startForResult.launch(Intent(this, BarcodeScanActivity::class.java))
        }

        // Focus on the bar code value.
        if (editBarCodeValue?.requestFocus() == true) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    override fun onStop() {
        super.onStop()
        MySingleton.getInstance(this).requestQueue.cancelAll(TAG)
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

        // Instantiate the RequestQueue.
        val url = "${(application as MyApplication).baseUrl}/api/Kiosk/InventoryItems/Search?barCodeValue=${editBarCodeValue?.text}"

        val request: JsonObjectRequest = object : JsonObjectRequest(Method.GET, url, null,
            { response ->
                val itemJSON = response.toString()

                runOnUiThread {
                    progressDialog?.dismiss()

                    val intent = Intent(this, InventoryQuantityActivity::class.java)

                    // Pass the item
                    intent.putExtra("item", itemJSON)
                    startActivity(intent)
                }
            }, { error ->
                runOnUiThread {
                    val response = error.networkResponse
                    when (response.statusCode) {
                        404 -> {
                            progressDialog?.dismiss()
                            showDialog("No item matches that bar code value.")
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
