package com.test.fakelocationapp

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.test.fakelocationapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mockLocationService: MockLocationService
    private var isBound = false
    private lateinit var map: GoogleMap
    private var currentMarker: Marker? = null
    private var latLng: LatLng? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var isStart = true

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bind to the MockLocationService
        val intent = Intent(this, MockLocationService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


        val searchButton = findViewById<Button>(R.id.searchButton)
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        val start = findViewById<Button>(R.id.toggleMockButton)

        searchButton.setOnClickListener {
            val cityText = searchEditText.text.toString().trim()
            getCityLocation(cityText)
        }

        start.setOnClickListener {
            if (isStart){
                isStart = false
                toggleMockLocation()
                binding.toggleMockButton.text = "Stop Mocking"
            }else{
                isStart = true
                toggleMockLocation()
                binding.toggleMockButton.text = "Start Mocking"

            }

        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun getCityLocation(cityText: String) {
        val geocoder = Geocoder(this)
        val location = geocoder.getFromLocationName(cityText, 1)
        val address = location?.get(0)
        latLng = LatLng(address?.latitude ?: 0.0, address?.longitude ?: 0.0)

        currentMarker?.remove()
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng!!, 15f))
        currentMarker = map.addMarker(MarkerOptions().position(latLng!!))

    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.isMyLocationEnabled = true

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationProviderClient.lastLocation.addOnCompleteListener { locationTask ->
            val location = locationTask.result
            if (location != null) {
                val homeLatLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, 15f))
                currentMarker = map.addMarker(MarkerOptions().position(homeLatLng))

                setMapClick(map)
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setMapClick(map: GoogleMap) {
        map.setOnMapClickListener { latLng ->
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            currentMarker?.remove()
            currentMarker = map.addMarker(MarkerOptions().position(latLng))
            this.latLng = latLng
            Toast.makeText(
                this,
                "Latitude: ${latLng.latitude}, Longitude: ${latLng.longitude}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MockLocationService.MockLocationBinder
            mockLocationService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    private fun toggleMockLocation() {
        if (latLng != null) {
            Log.d("MainActivity", "LatLng: $latLng")
            mockLocationService.latLng = latLng!!
            mockLocationService.toggleMocking() // Start/Stop Mocking
        } else {
            Toast.makeText(this, "No location selected for mocking", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, you can now access location
            } else {
                Toast.makeText(this, "Location permissions are required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}
