package com.example.polarstarproject;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.Range;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
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
import java.util.Timer;
import java.util.TimerTask;

public class RangeSettingActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseUser user; //firebase 변수

    private static final String TAG = "RangeSetting";
    private GoogleMap map;
    private CameraPosition cameraPosition;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private final LatLng defaultLocation = new LatLng(37.56, 126.97);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    public boolean locationPermissionGranted;

    private Location lastKnownLocation;

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private static final int SEARCH_ADDRESS_ACTIVITY = 10000;

    Marker counterpartyMarker;
    MarkerOptions counterpartyLocationMarker;

    Circle counterpartyCir;
    CircleOptions cir;
    public int rad = 0;

    LatLng counterpartyCurPoint;
    public LatLng rPoint;

    LocationManager manager;

    Connect myConnect;
    String counterpartyUID = "";

    double disabledAddressLat, disabledAddressLng; //장애인 집 주소 위도 경도

    EditText rName;
    TextView rangeAddress;
    Button btnSet, btnAdd;
    SeekBar seekBar;
    TextView tvDis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rangesetting);

        rName = findViewById(R.id.rName);
        rName.setText("집");
        seekBar = findViewById(R.id.seekBar);
        tvDis = findViewById(R.id.tvDis);
        rangeAddress = findViewById(R.id.rangeAddress);
        btnSet = findViewById(R.id.btnSet);
        btnAdd = findViewById(R.id.btnAdd);

        btnSet.setOnClickListener(this);
        btnAdd.setOnClickListener(this);

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        MapsInitializer.initialize(this);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            MapsInitializer.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.gMap);
        mapFragment.getMapAsync(this);

        // 반경 거리 설정하기
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rad = seekBar.getProgress();
                tvDis.setText(String.format("%d M", rad));

                if(counterpartyCir != null){ //이미 존재했던 경우
                    counterpartyCir.remove();
                }
                //반경 원
                cir = new CircleOptions().center(counterpartyCurPoint) //원점
                        .radius(rad) //반지름 단위 = 미터
                        .strokeWidth(0f) //선너비 0f=선없음
                        .fillColor(Color.parseColor("#880000ff")); //배경색
                counterpartyCir = map.addCircle(cir);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                reference.child("range").child(user.getUid()).child(rName.getText().toString()).child("distance").setValue(rad);

            }
        });
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
        classificationUser(user.getUid()); //상대방 마지막 위치 띄우기
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
    }


    /////////////////////////////////////////사용자 구별////////////////////////////////////////
    private void classificationUser(String uid){ //firebase select 조회 함수, 내 connect 테이블 조회
        Query guardianQuery = reference.child("connect").child("guardian").orderByKey().equalTo(uid); //보호자 테이블 조회
        guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
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
                    Toast.makeText(RangeSettingActivity.this, "오류", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "상대방 인적사항 확인 오류");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(counterpartyCurPoint, DEFAULT_ZOOM)); //최근 위치로 카메라 이동
                    Log.w(TAG, "첫 카메라 위치 "+ counterpartyCurPoint);
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
                            Log.w(TAG, "첫 마커 위치 "+ counterpartyCurPoint);

                        }
                        else if(counterpartyLocationMarker != null){ //마커가 존재했던 경우
                            counterpartyMarker.remove(); // 마커삭제
                            counterpartyLocationMarker.position(counterpartyCurPoint);
                            counterpartyMarker = map.addMarker(counterpartyLocationMarker);
                        }
                    }
                    return;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    ////////////////////////////////////검색한 주소지로 이동한 마커/////////////////////////////////////////////
    private void mapMarker() {
        if(reference.child("range").child(user.getUid()).orderByKey().equalTo(rName.getText().toString()) != null){
            reference.child("range").child(user.getUid()).orderByKey().equalTo(rName.getText().toString()). //저장명으로 접근
                    addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Range myRangeP = new Range();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        myRangeP = ds.getValue(Range.class);
                    }
                    if (!snapshot.exists()) {
                        Log.w(TAG, "상대방 실시간 위치 오류");
                    } else {
                        counterpartyCurPoint = new LatLng(myRangeP.latitude, myRangeP.longitude);
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(counterpartyCurPoint, DEFAULT_ZOOM)); //최근 위치로 카메라 이동
                        Log.w(TAG, "카메라 위치 " + counterpartyCurPoint);
                        if (counterpartyCurPoint != null) {
                            if (counterpartyLocationMarker == null) { //마커가 없었을 경우
                                counterpartyLocationMarker = new MarkerOptions();
                                counterpartyLocationMarker.position(counterpartyCurPoint);

                                int height = 300;
                                int width = 300;
                                BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable((R.drawable.other_gps));
                                Bitmap b = bitmapdraw.getBitmap();
                                Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false); //마커 크기설정

                                counterpartyLocationMarker.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));
                                counterpartyMarker = map.addMarker(counterpartyLocationMarker);
                                Log.w(TAG, "첫 마커 위치 " + counterpartyCurPoint);

                            } else if (counterpartyLocationMarker != null) { //마커가 존재했던 경우
                                counterpartyMarker.remove(); // 마커삭제
                                counterpartyLocationMarker.position(counterpartyCurPoint);
                                counterpartyMarker = map.addMarker(counterpartyLocationMarker);

                                //cir.radius(0);
                                cir = new CircleOptions().center(counterpartyCurPoint) //원점
                                        .radius(myRangeP.getDis()) //반지름 단위 = 미터
                                        .strokeWidth(0f) //선너비 0f=선없음
                                        .fillColor(Color.parseColor("#880000ff")); //배경색
                                counterpartyCir = map.addCircle(cir);
                            }
                        }
                        return;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }
    /////////////////////////////////////////////////////////////////////////////////////


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == SEARCH_ADDRESS_ACTIVITY) { //우편번호
            if (resultCode == RESULT_OK) {
                String data = intent.getExtras().getString("data");
                if(data != null) {
                    rangeAddress.setText(data);
                    new Thread(() -> {
                        geoC(data.substring(7));
                        //여기서 파이어베이스에 저장하고
                        Range myRange = new Range(disabledAddressLat, disabledAddressLng, rad);
                        Log.w(TAG, "로그: "+ disabledAddressLat+disabledAddressLng+rad);
                        reference.child("range").child(user.getUid()).child(rName.getText().toString()).setValue(myRange);
                        // 밑 메소드 뺴고 카메라 이동해보기
                        mapMarker();
                    }).start();
                }
            }
        }
    }

    public void geoC(String address) { //주소를 위도 경도로 바꿔줌
        try {
            BufferedReader bufferedReader;
            StringBuilder stringBuilder = new StringBuilder();

            String query = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=" + URLEncoder.encode(address, "UTF-8");
            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            if (conn != null) {
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
                disabledAddressLng = Double.parseDouble(stringBuilder.substring(indexFirst + 5, indexLast));

                indexFirst = stringBuilder.indexOf("\"y\":\"");
                indexLast = stringBuilder.indexOf("\",\"distance\":");
                disabledAddressLat = Double.parseDouble(stringBuilder.substring(indexFirst + 5, indexLast));

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
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAdd: //우편번호 검색
                Intent i = new Intent(RangeSettingActivity.this, WebViewActivity.class);
                startActivityForResult(i, SEARCH_ADDRESS_ACTIVITY);
                break;

            case R.id.btnSet:
                Range myRange = new Range(disabledAddressLat, disabledAddressLng, rad);
                reference.child("range").child(user.getUid()).child(rName.getText().toString()).setValue(myRange);

                break;
        }

    }
}
