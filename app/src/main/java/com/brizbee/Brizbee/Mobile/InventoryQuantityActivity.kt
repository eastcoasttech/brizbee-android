//
//  InventoryQuantityActivity.kt
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

            val name = item?.getString("name")
            textInventoryItemName?.setText(name)

            // Check for a base unit
            if (item?.has("baseUnitName") == true) {
                val baseUnitName = item?.getString("baseUnitName")
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
        finish()
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