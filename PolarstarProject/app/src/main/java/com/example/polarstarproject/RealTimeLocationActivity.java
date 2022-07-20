package com.example.polarstarproject;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.Disabled;
import com.example.polarstarproject.Domain.RealTimeLocation;
import com.example.polarstarproject.Domain.Route;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RealTimeLocationActivity extends AppCompatActivity implements OnMapReadyCallback { //프로젝트에서 우클릭 이 디바이스 항상 켜놓기 누름
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
    MarkerOptions myLocationMarker; //내 위치 마커
    Marker counterpartyMarker;
    MarkerOptions counterpartyLocationMarker; //상대방 위치 마커

    LatLng counterpartyCurPoint; //상대방 위치

    LocationManager manager;
    GPSListener gpsListener;

    Connect myConnect;
    String counterpartyUID = "";
    int classificationUserFlag = 0, count;//장애인 보호자 구별 (0: 기본값, 1: 장애인, 2: 보호자), 스케줄러 호출용 카운트
    double routeLatitude, routeLongitude; //장애인 경로 저장

    double disabledAddressLatitude, disabledAddressLongitude; //장애인 집 주소 위도 경도
    double distance;
    private final double DEFAULTDISTANCE= 1;
    private final String DEFAULT = "DEFAULT";
    int departureFlag,arrivalFlag = 0; //출발, 도착 플래그 (0: 기본값, 1: 출발, 도착)
    String counterpartyName; //상대방 이름
    Intent notificationIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        setContentView(R.layout.activity_realtime_location);

        MapsInitializer.initialize(this);
        count = 0; //카운트 초기화
        
        createNotificationChannel(DEFAULT, "default channel", NotificationManager.IMPORTANCE_HIGH); //알림 초기화
        notificationIntent = new Intent(this, RealTimeLocationActivity.class);       // 클릭시 실행할 activity를 지정
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
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

        counterpartyLocationScheduler();
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
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) { //실시간 위치 조회 성공
                                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                    LatLng curPoint = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(curPoint, DEFAULT_ZOOM)); //최근 위치로 카메라 이동

                                    firebaseUpdateLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()); //firebase에 실시간 위치 저장
                                    defaultMyMarker(curPoint); //초기 마커 설정
                                    realTimeDeviceLocation(); //실시간 위치 추적 시작
                                }
                                else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                                    LatLng curPoint = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(curPoint, DEFAULT_ZOOM)); //최근 위치로 카메라 이동

                                    firebaseUpdateLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()); //firebase에 실시간 위치 저장
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
        else if (myLocationMarker != null){
            myMarker.remove(); // 마커삭제
            myLocationMarker.position(curPoint);
            myMarker = map.addMarker(myLocationMarker);
        }

        else if (counterpartyMarker != null){
            counterpartyMarker.remove(); // 마커삭제
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
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
                    LatLng curPoint = new LatLng(latitude, longitude);

                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(curPoint, DEFAULT_ZOOM));
                    firebaseUpdateLocation(latitude, longitude); //firebase 실시간 위치 저장
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
                    LatLng curPoint = new LatLng(latitude, longitude);

                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(curPoint, DEFAULT_ZOOM));
                    firebaseUpdateLocation(latitude, longitude); //firebase 실시간 위치 저장
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
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onLocationChanged(Location location) { // 위치 변경 시 호출
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            LatLng curPoint = new LatLng(latitude, longitude);

            firebaseUpdateLocation(latitude, longitude); //firebase 실시간 위치 저장
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
        routeLatitude = latitude;
        routeLongitude = longitude;

        Log.d(TAG,"실시간 " +user.getUid() + " 위치: " + latitude + " " + longitude);
        RealTimeLocation realTimeLocation = new RealTimeLocation(latitude,longitude);

        reference.child("realtimelocation").child(user.getUid()).setValue(realTimeLocation)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Write failed
                        Log.d(TAG,"firebase 실시간 위치 저장 실패");
                    }
                });
    }

    private void routeScheduler(){ //경로 저장용 스케쥴러
        Log.d(TAG,"경로 저장용 스케쥴러 실행");
        if(classificationUserFlag == 1){
            Timer timer = new Timer();

            TimerTask timerTask = new TimerTask() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void run() {
                    //10초마다 실행
                    firebaseUpdateRoute(routeLatitude, routeLongitude);
                }
            };
            timer.schedule(timerTask,0,10000);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void firebaseUpdateRoute(double latitude, double longitude) { //firebase에 경로용 위치 저장
        LocalTime localTime = LocalTime.now(ZoneId.of("Asia/Seoul"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String nowTime = localTime.format(formatter); //현재 시간 구하기

        Route route = new Route(nowTime, latitude,longitude);

        LocalDate localDate = LocalDate.now(ZoneId.of("Asia/Seoul")); //현재 날짜 구하기
        String nowDate = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        reference.child("route").child(user.getUid()).child(nowDate).child(nowTime).setValue(route)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Write failed
                        Log.d(TAG,"firebase 경로용 위치 저장 실패");
                    }
                });
    }



    /////////////////////////////////////////상대방 위치////////////////////////////////////////
    private void counterpartyLocationScheduler(){ //1초마다 상대방 DB 검사 후, 위치 띄우기
        Timer timer = new Timer();

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                //1초마다 실행
                classificationUser(user.getUid());
            }
        };
        timer.schedule(timerTask,0,1000);
    }

    /////////////////////////////////////////사용자 구별////////////////////////////////////////
    private void classificationUser(String uid){ //firebase select 조회 함수, 내 connect 테이블 조회
        Query disabledQuery = reference.child("connect").child("disabled").orderByKey().equalTo(uid); //장애인 테이블 조회
        disabledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    classificationUserFlag = 1;
                    if(count == 0){
                        routeScheduler(); //장애인 경로 저장 함수 호출
                    }
                    count++;
                    getOtherUID();
                }
                else {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        Query guardianQuery = reference.child("connect").child("guardian").orderByKey().equalTo(uid); //보호자 테이블 조회
        guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    classificationUserFlag = 2;
                    getOtherUID();
                }
                else {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    /////////////////////////////////////////상대방 UID 가져오기////////////////////////////////////////
    private void getOtherUID(){
        if(classificationUserFlag == 1) { //내가 장애인이고, 상대방이 보호자일 경우
            Query query = reference.child("connect").child("guardian").orderByChild("myCode").equalTo(myConnect.getCounterpartyCode());
            query.addListenerForSingleValueEvent(new ValueEventListener() { //보호자 코드로 보호자 uid 가져오기
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        counterpartyUID = ds.getKey();
                    }

                    if(counterpartyUID != null  && !counterpartyUID.isEmpty()){
                        counterpartyMarker();
                    }
                    else {
                        Toast.makeText(RealTimeLocationActivity.this, "오류", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "상대방 인적사항 확인 오류");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else if(classificationUserFlag == 2) { //내가 보호자고, 상대방이 장애인일 경우
            Query query = reference.child("connect").child("disabled").orderByChild("myCode").equalTo(myConnect.getCounterpartyCode());
            query.addListenerForSingleValueEvent(new ValueEventListener() { //장애인 코드로 장애인 uid 가져오기
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        counterpartyUID = ds.getKey();
                    }

                    if(counterpartyUID != null  && !counterpartyUID.isEmpty()){
                        counterpartyMarker();
                    }
                    else {
                        Toast.makeText(RealTimeLocationActivity.this, "오류", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "상대방 인적사항 확인 오류");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else { //올바르지 않은 사용자
            Log.w(TAG, "상대방 인적사항 확인 오류");
        }
    }

    /////////////////////////////////////////실시간 위치 마커////////////////////////////////////////
    private void counterpartyMarker() {
        reference.child("realtimelocation").orderByKey().equalTo(counterpartyUID). //상대방 실시간 위치 검색
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                RealTimeLocation realTimeLocation = new RealTimeLocation();
                for(DataSnapshot ds : snapshot.getChildren()){
                    realTimeLocation = ds.getValue(RealTimeLocation.class);
                }
                if (!snapshot.exists()) {
                    Log.w(TAG, "상대방 실시간 위치 오류");
                }
                else {
                    counterpartyCurPoint = new LatLng(realTimeLocation.latitude, realTimeLocation.longitude);
                    return;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        if(counterpartyCurPoint != null){
            if (counterpartyLocationMarker == null) { //마커가 없었을 경우
                counterpartyLocationMarker = new MarkerOptions();
                counterpartyLocationMarker.position(counterpartyCurPoint);

                int height = 300;
                int width = 300;
                BitmapDrawable bitmapdraw=(BitmapDrawable)getResources().getDrawable((R.drawable.other_gps));
                Bitmap b=bitmapdraw.getBitmap();
                Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false); //마커 크기설정

                counterpartyLocationMarker.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));
                counterpartyMarker = map.addMarker(counterpartyLocationMarker);
            }
            else if(counterpartyLocationMarker != null){ //마커가 존재했던 경우
                counterpartyMarker.remove(); // 마커삭제
                counterpartyLocationMarker.position(counterpartyCurPoint);
                counterpartyMarker = map.addMarker(counterpartyLocationMarker);
            }

            if(classificationUserFlag == 2){ //보호자일 경우
                reference.child("disabled").orderByKey().equalTo(counterpartyUID). //상대방 이름 가져오기
                        addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Disabled disabled = new Disabled();
                        for(DataSnapshot ds : snapshot.getChildren()){
                            disabled = ds.getValue(Disabled.class);
                        }
                        if (disabled.getName()!= null && !disabled.getName().isEmpty()) {
                            counterpartyName = disabled.getName();
                            departureArrivalNotification(); //장애인 출도착 알림
                        }
                        else {
                            Log.w(TAG, "상대방 이름 불러오기 오류");
                            return;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }
    }

    /////////////////////////////////////////장애인 집 출발&도착 알림////////////////////////////////////////
    private void departureArrivalNotification(){
        reference.child("disabled").child(counterpartyUID).orderByKey().equalTo("address"). //장애인 집 주소 가져오기
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String address = null;
                for(DataSnapshot ds : snapshot.getChildren()){
                    address = ds.getValue().toString();
                }
                if (!snapshot.exists()) {
                    Log.w(TAG, "장애인 집 주소 오류");
                }
                else { //장애인 집 주소 받아오면
                    String finalAddress = address.substring(7);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            geoCoding(finalAddress);
                        }
                    }).start();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void geoCoding(String address) {
        try{
            BufferedReader bufferedReader;
            StringBuilder stringBuilder = new StringBuilder();

            String query = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=" + URLEncoder.encode(address, "UTF-8");
            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            if(conn != null) {
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", BuildConfig.CLIENT_ID);
                conn.setRequestProperty("X-NCP-APIGW-API-KEY", BuildConfig.CLIENT_SECRET);
                conn.setDoInput(true);

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    bufferedReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }

                int indexFirst;
                int indexLast;

                indexFirst = stringBuilder.indexOf("\"x\":\"");
                indexLast = stringBuilder.indexOf("\",\"y\":");
                disabledAddressLongitude = Double.parseDouble(stringBuilder.substring(indexFirst + 5, indexLast));

                indexFirst = stringBuilder.indexOf("\"y\":\"");
                indexLast = stringBuilder.indexOf("\",\"distance\":");
                disabledAddressLatitude = Double.parseDouble(stringBuilder.substring(indexFirst + 5, indexLast));

                bufferedReader.close();
                conn.disconnect();
        }
    } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //경도(longitude)가 X, 위도(latitude)가 Y
        distance = Math.sqrt(((counterpartyCurPoint.longitude-disabledAddressLongitude)*(counterpartyCurPoint.longitude-disabledAddressLongitude))+((counterpartyCurPoint.latitude-disabledAddressLatitude)*(counterpartyCurPoint.latitude-disabledAddressLatitude)));

        if(disabledAddressLatitude != 0.0 && disabledAddressLongitude != 0.0){
            if(departureFlag == 0){ //아직 출발 안했을 경우
                if(distance*1000 > DEFAULTDISTANCE) {
                    departureNotification(DEFAULT, 1); //출발 알림 울리기
                    departureFlag = 1; //출발함
                    arrivalFlag = 0; //도착 플래그 초기화
                }
            }

            if(arrivalFlag == 0){ //아직 도착안했을 경우
                if(departureFlag == 1){ //출발함
                    if(distance*1000 < DEFAULTDISTANCE) {
                        arrivalNotification(DEFAULT, 2); //도착 알림 울리기
                        arrivalFlag = 1; //도착함
                        departureFlag = 0; //출발 플래그 초기화
                    }
                }
            }
        }
    }

    private void createNotificationChannel(String channelId, String channelName, int importance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId, channelName, importance));
        }
    }

    private void departureNotification(String channelId, int id) { //출발 알림
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.polaris_roughly) //알림 이미지
                .setContentTitle("북극성")
                .setContentText(counterpartyName + "님이 집에서 출발하였습니다.")
                .setContentIntent(pendingIntent)    // 클릭시 설정된 PendingIntent가 실행된다
                .setAutoCancel(true)                // true이면 클릭시 알림이 삭제된다
                //.setTimeoutAfter(1000)
                //.setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    private void arrivalNotification(String channelId, int id) { //도착 알림
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.polaris_roughly) //알림 이미지
                .setContentTitle("북극성")
                .setContentText(counterpartyName + "님이 집으로 도착하였습니다.")
                .setContentIntent(pendingIntent)    // 클릭시 설정된 PendingIntent가 실행된다
                .setAutoCancel(true)                // true이면 클릭시 알림이 삭제된다
                //.setTimeoutAfter(1000)
                //.setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }


}
