// MainActivity.kt
package com.example.ems

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap

import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle





import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.FirebaseApp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.view.View
import android.view.LayoutInflater
import com.example.ems.utils.PrefsManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException
import java.net.SocketTimeoutException

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var spinnerEmployee: Spinner
    private lateinit var spinnerOffice: Spinner
    private lateinit var radioGroup: RadioGroup
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvLocation: TextView
    private lateinit var btnSelfie: Button
    private lateinit var btnSubmit: Button
    private lateinit var btnRefresh: ImageButton
    private lateinit var imagePreview: ImageView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var gMap: GoogleMap
    private  lateinit var  tvdistanceInMeters :TextView
    private var selfieUri: Uri? = null
    private var selfieFilePath: String? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var internetDate = ""
    private var internetTime = ""
    private var locationName: String = ""
    private var selectedOffice = "Select Office"
    private var officeLat = 0.0
    private var officeLng = 0.0

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_PERMISSIONS = 100

    private lateinit var radioCheckin: RadioButton
    private lateinit var radioCheckout: RadioButton
    private lateinit var radioLunchStart: RadioButton
    private lateinit var radioLunchEnd: RadioButton
    private lateinit var prefs: SharedPreferences
    private val CHECKIN_TIME_SUFFIX = "checkin_time_"

    private lateinit var progressBar: ProgressBar
    private lateinit var prefsManager: PrefsManager
    
    private fun checkApiConfiguration() {
        if (!prefsManager.hasApiUrl()) {
            showApiConfigDialog()
        } else {
            // API is already configured, you can use prefsManager.apiUrl here
            initializeApp()
        }
    }
    
    private fun showApiConfigDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_api_config, null)
        val etApiUrl = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etApiUrl)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        btnSave.setOnClickListener {
            val apiUrl = etApiUrl.text.toString().trim()
            if (apiUrl.isNotEmpty() && (apiUrl.startsWith("http://") || apiUrl.startsWith("https://"))) {
                prefsManager.apiUrl = apiUrl
                dialog.dismiss()
                initializeApp()
            } else {
                etApiUrl.error = "Please enter a valid URL starting with http:// or https://"
            }
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
        
        dialog.show()
    }
    
    private fun initializeApp() {
        // Initialize views
        tvdistanceInMeters = findViewById(R.id.tvdistanceInMeters)
        spinnerEmployee = findViewById(R.id.spinnerEmployee)
        spinnerOffice = findViewById(R.id.spinnerOffice)
        radioGroup = findViewById(R.id.radioGroup)
        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)
        tvLocation = findViewById(R.id.tvLocation)
        btnSelfie = findViewById(R.id.btnSelfie)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnRefresh = findViewById(R.id.btnRefresh)
        imagePreview = findViewById(R.id.imagePreview)
        radioCheckin = findViewById(R.id.radioCheckin)
        radioCheckout = findViewById(R.id.radioCheckout)
        radioLunchStart = findViewById(R.id.radioLunchStart)
        radioLunchEnd = findViewById(R.id.radioLunchEnd)
        progressBar = findViewById(R.id.progressBar)

        // Initialize SharedPreferences for attendance type tracking
        prefs = getSharedPreferences("AttendancePrefs", MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request permissions and fetch initial data
        requestPermissions()
        fetchInternetTime()
        fetchEmployees()
        fetchOffices()

        // Set up map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up click listeners
        btnSelfie.setOnClickListener { openCamera() }
        btnSubmit.setOnClickListener { submitAttendance() }
        btnRefresh.setOnClickListener { resetForm(true) }
        
        // Call auto-checkout after employees are loaded (delayed to ensure spinner is populated)
        spinnerEmployee.postDelayed({ autoCheckoutIfNeeded() }, 2000)

        // Office spinner item selection
        spinnerOffice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedOffice = parent.getItemAtPosition(position).toString()
                if (position > 0) fetchOfficeCoordinates(selectedOffice)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Update radio button states on start
        updateRadioButtonsState()

        // Add listeners to show Toast if user taps a disabled RadioButton
        radioCheckin.setOnClickListener {
            if (!radioCheckin.isEnabled) {
                Toast.makeText(this, "You cannot select Checkin at this stage.", Toast.LENGTH_SHORT).show()
                radioGroup.clearCheck()
            }
        }
        radioCheckout.setOnClickListener {
            if (!radioCheckout.isEnabled) {
                Toast.makeText(this, "You cannot select Checkout at this stage.", Toast.LENGTH_SHORT).show()
                radioGroup.clearCheck()
            }
        }
        radioLunchStart.setOnClickListener {
            if (!radioLunchStart.isEnabled) {
                Toast.makeText(this, "You cannot select Lunch Start at this stage.", Toast.LENGTH_SHORT).show()
                radioGroup.clearCheck()
            }
        }
        radioLunchEnd.setOnClickListener {
            if (!radioLunchEnd.isEnabled) {
                Toast.makeText(this, "You cannot select Lunch End at this stage.", Toast.LENGTH_SHORT).show()
                radioGroup.clearCheck()
            }
        }
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        prefsManager = PrefsManager.getInstance(this)
//        FirebaseApp.initializeApp(this)
//
//        // Initialize SharedPreferences and FusedLocationProviderClient
//        prefs = getSharedPreferences("AttendancePrefs", MODE_PRIVATE)
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//
//        // Check if API is configured, if not show configuration dialog
//        checkApiConfiguration()
//
//        // Progress bar will be initialized in initializeApp()
//    }

    private fun fetchGoogleMapsApiKey(onComplete: (String?) -> Unit) {
        val client = OkHttpClient()
        val baseUrl = prefsManager.apiUrl ?: return onComplete(null)

        val request = Request.Builder()
            .url("$baseUrl/google")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to load map key", Toast.LENGTH_SHORT).show()
                    onComplete(null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    val key = bodyString?.let { JSONObject(it).optString("key", null) }
                    runOnUiThread {
                        onComplete(key)
                    }
                } else {
                    runOnUiThread {
                        onComplete(null)
                    }
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefsManager = PrefsManager.getInstance(this)
        FirebaseApp.initializeApp(this)

        prefs = getSharedPreferences("AttendancePrefs", MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fetchGoogleMapsApiKey { apiKey ->
            if (apiKey != null) {
                try {
                    val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                    applicationInfo.metaData.putString("com.google.android.geo.API_KEY", apiKey)
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to set map key", Toast.LENGTH_SHORT).show()
                }
            }
            checkApiConfiguration()
        }
    }




    override fun onMapReady(map: GoogleMap) {
        gMap = map
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gMap.isMyLocationEnabled = true
            getLocation()
        } else {
            requestPermissions()
        }
    }

    private fun fetchEmployees() {
        Thread {
            try {
                val client = OkHttpClient()
                val baseUrl = prefsManager.apiUrl ?: return@Thread
                val request = Request.Builder()
                    .url("$baseUrl/employees")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = JSONArray(response.body?.string())
                    val employeeList = mutableListOf("Select Employee")
                    for (i in 0 until json.length()) {
                        val name = json.getJSONObject(i).getString("name")
                        employeeList.add(name)
                    }
                    runOnUiThread {
                        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, employeeList)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerEmployee.adapter = adapter

                        spinnerEmployee.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                                updateRadioButtonsState()
                            }
                            override fun onNothingSelected(parent: AdapterView<*>) {}
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Employee fetch error: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun fetchOffices() {
        Thread {
            try {
                val client = OkHttpClient()
                val baseUrl = prefsManager.apiUrl ?: return@Thread
                val request = Request.Builder()
                    .url("$baseUrl/offices")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = JSONArray(response.body?.string())
                    val officeList = mutableListOf("Select Office")
                    for (i in 0 until json.length()) {
                        val name = json.getJSONObject(i).getString("officename")
                        officeList.add(name)
                    }
                    runOnUiThread {
                        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, officeList)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerOffice.adapter = adapter
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Office fetch error: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun fetchOfficeCoordinates(name: String) {
        Thread {
            try {
                val client = OkHttpClient()
                val baseUrl = prefsManager.apiUrl ?: return@Thread
                val request = Request.Builder()
                    .url("$baseUrl/offices")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = JSONArray(response.body?.string())
                    for (i in 0 until json.length()) {
                        val obj = json.getJSONObject(i)
                        if (obj.getString("officename") == name) {
                            officeLat = obj.getDouble("latitude")
                            officeLng = obj.getDouble("longitude")
                            calculateDistance()
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Distance calc error: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun calculateDistance() {
        val results = FloatArray(1)
        Location.distanceBetween(latitude, longitude, officeLat, officeLng, results)
        val distanceInMeters = results[0]
        runOnUiThread {
            tvdistanceInMeters.text = "Distance to Office: %.2f meters".format(distanceInMeters)
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            tvLocation.text = "Location permission not granted"
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                latitude = loc.latitude
                longitude = loc.longitude
                val pos = LatLng(latitude, longitude)
                if (::gMap.isInitialized) {
                    gMap.clear()
                    gMap.addMarker(MarkerOptions().position(pos).title("You are here"))
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                }
                tvLocation.text = "Lat: $latitude, Lng: $longitude\nFetching address..."
                getAddressFromLatLng(latitude, longitude)
            } else {
                tvLocation.text = "Location not available"
            }
        }.addOnFailureListener {
            tvLocation.text = "Failed to get location"
        }
    }

    private fun getAddressFromLatLng(lat: Double, lng: Double) {
        Thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                locationName = addresses?.firstOrNull()?.getAddressLine(0) ?: "Address not found"
            } catch (e: Exception) {
                locationName = "Address error"
            }
            runOnUiThread { updateLocationUI() }
        }.start()
    }

    private fun updateLocationUI() {
        tvLocation.text = "Lat: $latitude, Lng: $longitude\n$locationName"
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
        else
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        ActivityCompat.requestPermissions(this, perms.toTypedArray(), REQUEST_PERMISSIONS)
    }

    private fun fetchInternetTime(retryCount: Int = 3) {
        Thread {
            var attempts = 0
            var success = false
            while (attempts < retryCount && !success) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder().url("http://worldtimeapi.org/api/timezone/Asia/Kolkata").build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        response.body?.use {
                            val json = JSONObject(it.string())
                            val datetime = json.getString("datetime")
                            internetDate = datetime.substring(0, 10)
                            internetTime = datetime.substring(11, 19)
                            runOnUiThread {
                                tvDate.text = "Date: $internetDate"
                                tvTime.text = "Time: $internetTime"
                            }
                            success = true
                        }
                    }
                } catch (e: Exception) {
                    attempts++
                    Thread.sleep(1000)
                }
            }

            if (!success) {
                val now = Calendar.getInstance()
                internetDate = "%04d-%02d-%02d".format(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH))
                internetTime = "%02d:%02d:%02d".format(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND))
                runOnUiThread {
                    tvDate.text = "Date: $internetDate"
                    tvTime.text = "Time: $internetTime"
                }
            }
        }.start()
    }

    private fun openCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "Selfie")
            put(MediaStore.Images.Media.DESCRIPTION, "Attendance Selfie")
        }
        selfieUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, selfieUri)
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            try {
                // Load and resize the image to a smaller size
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                contentResolver.openInputStream(selfieUri!!)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
                
                // Calculate inSampleSize
                options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
                options.inJustDecodeBounds = false
                
                // Decode with new options
                val bitmap = contentResolver.openInputStream(selfieUri!!)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                } ?: return
                
                imagePreview.setImageBitmap(bitmap)

                // Save as WebP with 70% quality
                val file = File(cacheDir, "selfie_${System.currentTimeMillis()}.webp")
                FileOutputStream(file).use { 
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 70, it) 
                }
                selfieFilePath = file.absolutePath
                
            } catch (e: Exception) {
                Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getTodayKey(employee: String): String {
        val date = if (internetDate.isNotEmpty()) internetDate else {
            val now = Calendar.getInstance()
            "%04d-%02d-%02d".format(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH))
        }
        return "attendance_type_${employee}_$date"
    }

    private fun getTodayAttendanceType(employee: String): String? {
        return prefs.getString(getTodayKey(employee), null)
    }

    private fun setTodayAttendanceType(employee: String, type: String) {
        prefs.edit().putString(getTodayKey(employee), type).apply()
    }

    private fun setCheckinTime(employee: String, timeMillis: Long) {
        val date = if (internetDate.isNotEmpty()) internetDate else {
            val now = Calendar.getInstance()
            "%04d-%02d-%02d".format(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH))
        }
        prefs.edit().putLong(" $CHECKIN_TIME_SUFFIX${employee}_$date", timeMillis).apply()
    }

    private fun getCheckinTime(employee: String): Long {
        val date = if (internetDate.isNotEmpty()) internetDate else {
            val now = Calendar.getInstance()
            "%04d-%02d-%02d".format(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH))
        }
        return prefs.getLong(" $CHECKIN_TIME_SUFFIX${employee}_$date", 0L)
    }

    private fun updateRadioButtonsState() {
        val employee = spinnerEmployee.selectedItem?.toString() ?: ""
        if (employee == "Select Employee" || employee.isEmpty()) {
            radioCheckin.isEnabled = false
            radioCheckout.isEnabled = false
            radioLunchStart.isEnabled = false
            radioLunchEnd.isEnabled = false
            return
        }
        val type = getTodayAttendanceType(employee)
        when (type) {
            null -> { // Step 1: Only Checkin enabled
                radioCheckin.isEnabled = true
                radioCheckout.isEnabled = false
                radioLunchStart.isEnabled = false
                radioLunchEnd.isEnabled = false
            }
            "Checkin" -> { // Step 2: Lunch Start and Checkout enabled
                radioCheckin.isEnabled = false
                radioCheckout.isEnabled = true
                radioLunchStart.isEnabled = true
                radioLunchEnd.isEnabled = false
            }
            "Lunch Start" -> { // Step 3: Only Checkout and Lunch End enabled
                radioCheckin.isEnabled = false
                radioCheckout.isEnabled = true
                radioLunchStart.isEnabled = false
                radioLunchEnd.isEnabled = true
            }
            "Lunch End" -> { // Step 4: Only Checkout enabled
                radioCheckin.isEnabled = false
                radioCheckout.isEnabled = true
                radioLunchStart.isEnabled = false
                radioLunchEnd.isEnabled = false
            }
            "Checkout" -> { // Step 5: All disabled
                radioCheckin.isEnabled = false
                radioCheckout.isEnabled = false
                radioLunchStart.isEnabled = false
                radioLunchEnd.isEnabled = false
            }
        }
    }

    private fun submitAttendance() {
        val employee = spinnerEmployee.selectedItem?.toString() ?: return
        val typeId = radioGroup.checkedRadioButtonId
        val type = findViewById<RadioButton>(typeId)?.text?.toString() ?: return


        val lastType = getTodayAttendanceType(employee)
        if (lastType == type) {
            Toast.makeText(this, "Already submitted $type for today", Toast.LENGTH_SHORT).show()
            return
        }
        if (lastType == "Checkin" && type == "Checkin") {
            Toast.makeText(this, "Already checked in today", Toast.LENGTH_SHORT).show()
            return
        }
        if (lastType == "Checkout" && (type == "Checkin" || type == "Checkout")) {
            Toast.makeText(this, "Already checked out today", Toast.LENGTH_SHORT).show()
            return
        }
        if (lastType == "Lunch Start" && type == "Lunch Start") {
            Toast.makeText(this, "Already started lunch today", Toast.LENGTH_SHORT).show()
            return
        }
        if (lastType == "Lunch End" && (type == "Lunch Start" || type == "Lunch End")) {
            Toast.makeText(this, "Already ended lunch today", Toast.LENGTH_SHORT).show()
            return
        }
        if (employee == "Select Employee" || selectedOffice == "Select Office" || selfieFilePath == null) {
            Toast.makeText(this, "Please fill all fields and take selfie", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(selfieFilePath!!)
        if (!file.exists() || !file.canRead()) {
            Toast.makeText(this, "Selfie file not found or unreadable", Toast.LENGTH_SHORT).show()
            return
        }

        val baseUrl = prefsManager.apiUrl
        if (baseUrl.isNullOrEmpty()) {
            Toast.makeText(this, "API URL not configured", Toast.LENGTH_LONG).show()
            return
        }

        // Show loading state
        progressBar.visibility = View.VISIBLE
        btnSubmit.isEnabled = false

        Thread {
            try {
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                
                // Configure timeouts with shorter durations
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)  // Enable retry on failure
                    .build()

                // Split the location into latitude and longitude
                val locationParts = "$latitude,$longitude".split(",")
                val lat = if (locationParts.isNotEmpty()) locationParts[0].trim() else "0.0"
                val lng = if (locationParts.size > 1) locationParts[1].trim() else "0.0"
                
                // Build the request body with optimized settings
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("employee", employee)
                    .addFormDataPart("type", type)
                    .addFormDataPart("date", internetDate)
                    .addFormDataPart("time", internetTime)
                    .addFormDataPart("office", selectedOffice)
                    .addFormDataPart("latitude", lat)
                    .addFormDataPart("longitude", lng)
                    .addFormDataPart("location", "$latitude,$longitude")
                    // Use a fixed filename with .webp extension to match the compressed format
                    .addFormDataPart("selfie", "selfie_${System.currentTimeMillis()}.webp", 
                        file.readBytes().toRequestBody("image/webp".toMediaTypeOrNull()))
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl/attendance")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    
                    if (response.isSuccessful) {
                        try {
                            val jsonResponse = JSONObject(responseBody ?: "{}")
                            if (jsonResponse.optBoolean("success", false)) {
                                showSuccessDialog()
                                setTodayAttendanceType(employee, type) // Save type for today
                                if (type == "Checkin") {
                                    setCheckinTime(employee, System.currentTimeMillis())
                                }
                                updateRadioButtonsState() // Update UI
                                resetForm(true)
                            } else {
                                val errorMessage = jsonResponse.optString("error", "Failed to submit attendance")
                                throw Exception(errorMessage)
                            }
                        } catch (e: Exception) {
                            throw Exception("Invalid response format from server")
                        }
                    } else {
                        val errorMessage = try {
                            JSONObject(responseBody).optString("message", response.message)
                        } catch (e: Exception) {
                            response.message
                        }
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to submit attendance: $errorMessage (${response.code})",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: SocketTimeoutException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    Toast.makeText(
                        this@MainActivity,
                        "Request timed out. Please check your internet connection and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    Toast.makeText(
                        this@MainActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun showSuccessDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage("Attendance submitted successfully!")
            .setPositiveButton("OK") { _, _ ->
                // After dialog is dismissed, reset form and select next employee
                val lastEmployee = spinnerEmployee.selectedItemPosition
                resetForm(true)
                // Try to select the next employee in the list, if any
                val totalEmployees = spinnerEmployee.adapter?.count ?: 0
                if (lastEmployee + 1 < totalEmployees) {
                    spinnerEmployee.setSelection(lastEmployee + 1)
                } else {
                    spinnerEmployee.setSelection(0) // Or keep at 0 if at end
                }
            }
            .show()
    }

    private fun autoCheckoutIfNeeded() {
        val adapter = spinnerEmployee.adapter ?: return
        val allEmployees = (0 until adapter.count).map { adapter.getItem(it).toString() }
        for (employee in allEmployees) {
            if (employee == "Select Employee") continue
            val lastType = getTodayAttendanceType(employee)
            if (lastType == "Checkin") {
                val checkinTime = getCheckinTime(employee)
                if (checkinTime > 0 && System.currentTimeMillis() - checkinTime > 23 * 60 * 60 * 1000) {
                    autoSubmitCheckout(employee)
                }
            }
        }
    }

    private fun autoSubmitCheckout(employee: String) {
        // Mark as checked out in SharedPreferences
        setTodayAttendanceType(employee, "Checkout")
        runOnUiThread {
            Toast.makeText(this, "Auto-Checkout submitted for $employee after 23 hours", Toast.LENGTH_LONG).show()
        }
        // Optionally, you could also send a request to the backend here if required
    }

    private fun resetForm(fullReset: Boolean) {
        tvdistanceInMeters.text = "Distance to Office:"
        if (fullReset) {
            fetchInternetTime()
            fetchEmployees()
            fetchOffices()
            getLocation()
        }
        spinnerEmployee.setSelection(0)
        spinnerOffice.setSelection(0)
        radioGroup.clearCheck()
        imagePreview.setImageBitmap(null)
        selfieUri = null
        selfieFilePath = null
        tvLocation.text = "Location"
        updateRadioButtonsState()
    }
}
