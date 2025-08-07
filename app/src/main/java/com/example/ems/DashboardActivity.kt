package com.example.ems

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ems.utils.PrefsManager
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class DashboardActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EmployeeDetailsAdapter
    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        
        prefsManager = PrefsManager.getInstance(this)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val employee = intent.getStringExtra("employee") ?: ""
        val date = intent.getStringExtra("date") ?: ""

        if (prefsManager.hasApiUrl()) {
            fetchEmployeeDetails(employee, date)
        } else {
            Toast.makeText(this, "API URL not configured", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun fetchEmployeeDetails(employee: String, date: String) {
        val baseUrl = prefsManager.apiUrl ?: return
        val url = "$baseUrl/attendance?employee=$employee&date=$date"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@DashboardActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val details = parseDetails(body)
                    runOnUiThread {
                        adapter = EmployeeDetailsAdapter(details)
                        recyclerView.adapter = adapter
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@DashboardActivity, "Failed to fetch details", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun parseDetails(json: String?): List<EmployeeDetail> {
        val details = mutableListOf<EmployeeDetail>()
        if (json == null) return details
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            details.add(
                EmployeeDetail(
                    obj.optString("employee"),
                    obj.optString("type"),
                    obj.optString("date"),
                    obj.optString("time"),
                    obj.optString("location"),
                    obj.optString("office"),
                    obj.optString("selfie")
                )
            )
        }
        return details
    }
} 