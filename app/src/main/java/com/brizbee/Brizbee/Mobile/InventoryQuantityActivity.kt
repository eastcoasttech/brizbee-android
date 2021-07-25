package com.brizbee.Brizbee.Mobile

import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject

class InventoryQuantityActivity : AppCompatActivity() {
    private var editQuantity: EditText? = null
    private var textInventoryItemName: TextView? = null
    private var textInventoryItemBaseUnitName: TextView? = null
    private var item: JSONObject? = null
    private var itemJSON: String? = null

    companion object {
        val TAG = InventoryQuantityActivity::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory_quantity)

        // Get references from layouts
        editQuantity = findViewById(R.id.editQuantity)
        textInventoryItemName = findViewById(R.id.textInventoryItemName)

        // Parse object from JSON string
        itemJSON = intent.getStringExtra("item")
        item = JSONObject(itemJSON!!)

        // Set the inventory item details
        try {

            val name = item?.getString("Name")
            textInventoryItemName?.setText(name)

            // Check for a base unit
            if (item?.has("BaseUnitName") == true) {
                val baseUnitName = item?.getString("BaseUnitName")
                textInventoryItemBaseUnitName?.setText(baseUnitName)

                // Display the base unit
                textInventoryItemBaseUnitName?.visibility = View.VISIBLE
            }
        } catch (ex: JSONException) {
            // Go back
            val intent = Intent(this, InventoryItemActivity::class.java)
            startActivity(intent)
            finish() // Prevents going back
        }

        // Set focus
        if (editQuantity?.requestFocus() == true) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    override fun onStop() {
        super.onStop()
        MySingleton.getInstance(this).requestQueue.cancelAll(TAG)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCancelClick(view: View?) {
        val intent = Intent(this, InventoryItemActivity::class.java)
        startActivity(intent)
        finish() // Prevents going back
    }

    @Suppress("UNUSED_PARAMETER")
    fun onContinueClick(view: View?) {
        val intent = Intent(this, InventoryConfirmActivity::class.java)

        val quantity = editQuantity?.text.toString()

        // Pass the item and quantity
        intent.putExtra("item", itemJSON)
        intent.putExtra("quantity", quantity)
        startActivity(intent)
    }
}