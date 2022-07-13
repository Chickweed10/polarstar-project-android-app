package com.example.polarstarproject;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.polarstarproject.Domain.RealTimeLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RealTimeLocationActivity extends AppCompatActivity implements OnMapReadyCallback {
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseUser user; //firebase 변수
    
    private static final String TAG = "RealTimeLocation";
    private GoogleMap map;
    private CameraPosition cameraPosition;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private final LatLng defaultLocation = new LatLng(37.56, 126.97);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    private Location lastKnownLocation;

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    Marker myMarker;
    MarkerOptions myLocationMarker;

    LocationManager manager;
    GPSListener gpsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        setContentView(R.layout.activity_realtime_location);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsListener = new GPSListener();

        try {
            MapsInitializer.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) { //활동 일시중지 시, 상태저장
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(GoogleMap map) { //첫 시작 시, map 준비
        this.map = map;

        getLocationPermission(); //위치 권한 설정
        updateLocationUI(); //UI 업데이트
        defaultDeviceLocation(); //초기 위치 설정
    }

    private void getLocationPermission() { //위치 권한 확인 및 설정
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        }
        else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) { //권한 요청 결과 처리
        locationPermissionGranted = false;
        if (requestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        }
        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        updateLocationUI();
    }

    @SuppressLint("MissingPermission")
    private void updateLocationUI() { //권한에 따른 UI 내 위치 이동 버튼 업데이트
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true); //내위치 버튼
                map.getUiSettings().setMyLocationButtonEnabled(true);
            }
            else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void defaultDeviceLocation() { //권한에 따른 초기 위치 받아오기
        try {
            if (locationPermissionGranted) { //위치 권한 있을 경우
                @SuppressLint("MissingPermission") Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) { //실시간 위치 조회 성공
                                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                    LatLng curPoint = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(curPoint, DEFAULT_ZOOM)); //최근 위치로 카메라 이동

                                    defaultMyMarker(curPoint); //초기 마커 설정
                                    realTimeDeviceLocation(); //실시간 위치 추적 시작
                                }
                                else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                                    LatLng curPoint = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(curPoint, DEFAULT_ZOOM)); //최근 위치로 카메라 이동

                                    defaultMyMarker(curPoint); //초기 마커 설정
                                    realTimeDeviceLocation(); //실시간 위치 추적 시작
                                }
                            }
                        }
                        else { //최근 위치 조회 실패 시, 디폴트 위치로 카메라 이동
                            Log.d(TAG, "실시간 위치 조회 실패. 디폴트 위치 사용.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    public void defaultMyMarker(LatLng curPoint){ //디폴트 마커 설정
        if (myLocationMarker == null) {
            myLocationMarker = new MarkerOptions();
            myLocationMarker.position(curPoint);

            int height = 300;
            int width = 300;
            BitmapDrawable bitmapdraw=(BitmapDrawable)getResources().getDrawable((R.drawable.my_gps));
            Bitmap b=bitmapdraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false); //마커 크기설정

            myLocationMarker.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));
            myMarker = map.addMarker(myLocationMarker);
        }
        else {
            myMarker.remove(); // 마커삭제
            myLocationMarker.position(curPoint);
            myMarker = map.addMarker(myLocationMarker);
        }
    }

    @SuppressLint("MissingPermission")
    public void realTimeDeviceLocation() {
        try {
            Location location = null;

            long minTime = 0;        // 0초마다 갱신 - 바로바로 갱신
            float minDistance = 0;

            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    showCurrentLocation(latitude, longitude);
                }

                //위치 요청하기
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, gpsListener);
            }
            else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    showCurrentLocation(latitude,longitude);
                }

                //위치 요청하기
                manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, gpsListener);
            }

        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void showCurrentLocation(double latitude, double longitude) {
        LatLng curPoint = new LatLng(latitude, longitude);
        showMyLocationMarker(curPoint);
    }

    class GPSListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) { // 위치 변경 시 호출
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            LatLng curPoint = new LatLng(latitude, longitude);

            showMyLocationMarker(curPoint);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
        @Override
        public void onProviderEnabled(String provider) {

        }
        @Override
        public void onProviderDisabled(String provider) { // 위치 접근불가 시 호출
            //firebase에 특정 값 넣어서 보호자에게 추적 불가 알림 뜨게 하기
        }
    }

    private void showMyLocationMarker(LatLng curPoint) { //마커 설정
        if (myLocationMarker == null) { //마커가 없었을 경우
            myLocationMarker.position(curPoint);
            myMarker = map.addMarker(myLocationMarker);
        }
        else { //마커가 존재했던 경우
            myMarker.remove(); // 마커삭제
            myLocationMarker.position(curPoint);
            myMarker = map.addMarker(myLocationMarker);
        }
    }

    private void firebaseUpdateLocation(double latitude, double longitude) { //firebase에 실시간 위치 저장
        RealTimeLocation realTimeLocation = new RealTimeLocation(latitude,longitude);

        reference.child("realtimelocation").child(user.getUid()).setValue(realTimeLocation)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Write was successful!
                        Log.d(TAG,"firebase 저장 성공");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Write failed
                        Log.d(TAG,"firebase 저장 실패");
                    }
                });

    }
}
