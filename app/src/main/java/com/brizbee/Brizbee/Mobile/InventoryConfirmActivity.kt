package com.brizbee.Brizbee.Mobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.util.HashMap
import kotlin.concurrent.thread

class InventoryConfirmActivity : AppCompatActivity() {
    private var textInventoryConfirmQuantity: TextView? = null
    private var textInventoryConfirmName: TextView? = null
    private var item: JSONObject? = null
    private var name: String? = null
    private var quantity: String? = null

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
        name = item?.getString("Name")
        quantity = intent.getStringExtra("quantity")

        // Set the inventory item details
        textInventoryConfirmName?.setText(name)
        textInventoryConfirmQuantity?.setText(quantity)
    }

    override fun onStop() {
        super.onStop()
        MySingleton.getInstance(this).requestQueue.cancelAll(TAG)
    }

    fun onCancelClick(view: View?) {
        val intent = Intent(this, InventoryQuantityActivity::class.java)
        startActivity(intent)
        finish() // Prevents going back
    }

    fun onContinueClick(view: View?) {
        thread(start = true) {
            confirm()
        }
    }

    private fun confirm() {

        // Instantiate the RequestQueue
        val url = String.format(
            "https://app-brizbee-prod.azurewebsites.net/api/QBDInventoryConsumptions/Consume?qbdInventoryItemId=%s&quantity=%s&hostname=%s",
            item?.getString("Id"), quantity, URLEncoder.encode("the hostname", "utf-8")
        )
        val jsonRequest: JsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, null,
            Response.Listener { response ->

                runOnUiThread {
                    val intent = Intent(this, StatusActivity::class.java)

                    // Pass the item as a string
                    val item = response.toString()
                    intent.putExtra("item", item)

                    startActivity(intent)
                    finish() // Prevents going back
                }
            },
            Response.ErrorListener { error ->
                val response = error.networkResponse

                when (response?.statusCode) {
                    401 -> showDialog("No item matches that bar code value.")
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