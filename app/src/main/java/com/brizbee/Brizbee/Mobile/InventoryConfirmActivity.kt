//
//  InventoryConfirmActivity.kt
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
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import java.net.InetAddress
import java.net.URLEncoder
import java.util.*
import kotlin.concurrent.thread

class InventoryConfirmActivity : AppCompatActivity() {
    private var textInventoryConfirmQuantity: TextView? = null
    private var textInventoryConfirmName: TextView? = null
    private var item: JSONObject? = null
    private var name: String? = null
    private var quantity: String? = null
    var progressDialog: AlertDialog? = null
    var another: Boolean = false

    companion object {
        val TAG = InventoryConfirmActivity::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory_confirm)

        // Get references from layouts
        textInventoryConfirmQuantity = findViewById(R.id.textInventoryConfirmQuantity)
        textInventoryConfirmName = findViewById(R.id.textInventoryConfirmName)

        // Parse object from JSON string
        item = JSONObject(intent.getStringExtra("item")!!)
        name = item?.getString("name")
        quantity = intent.getStringExtra("quantity")

        // Set the inventory item details
        textInventoryConfirmName?.setText(name)
        textInventoryConfirmQuantity?.setText(quantity)
    }

    override fun onStop() {
        super.onStop()
        MySingleton.getInstance(this).requestQueue.cancelAll(TAG)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCancelClick(view: View?) {
        val intent = Intent(this, InventoryQuantityActivity::class.java)
        startActivity(intent)
        finish()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onContinueClick(view: View?) {
        thread(start = true) {
            confirm()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onAnotherClick(view: View?) {
        thread(start = true) {
            another = true
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

        val hostname = InetAddress.getLocalHost().hostName

        // Instantiate the RequestQueue.
        val builder = StringBuilder()
        builder.append("${(application as MyApplication).baseUrl}/api/Kiosk/InventoryItems/Consume")
            .append("?qbdInventoryItemId=${item?.getString("id")}")
            .append("&quantity=${quantity}")
            .append("&hostname=${URLEncoder.encode(hostname, "utf-8")}")

        val request: MyJsonObjectRequest = object : MyJsonObjectRequest(Method.POST, builder.toString(), null,
            { _ ->
                runOnUiThread {
                    progressDialog?.dismiss()

                    if (another == true) {
                        val intent = Intent(this, InventoryItemActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val intent = Intent(this, StatusActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }, { error ->
                runOnUiThread {
                    val response = error.networkResponse
                    when (response.statusCode) {
                        401 -> {
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