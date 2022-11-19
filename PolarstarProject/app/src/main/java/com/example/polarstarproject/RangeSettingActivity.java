package com.example.polarstarproject;

import android.Manifest;
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
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.Disabled;
import com.example.polarstarproject.Domain.Range;
import com.example.polarstarproject.Domain.RealTimeLocation;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.CircleOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.MarkerIcons;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

public class RangeSettingActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {
    Toolbar toolbar;
    TextView rName;
    TextView rangeAddress;
    Button btnSet, btnAdd;
    SeekBar seekBar;
    TextView tvDis;

    private static final String TAG = "RangeSetting";

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseUser user; //firebase 변수

    //네이버 지도
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;

    CameraUpdate cameraUpdate; //지도 카메라

    Marker counterpartyMarker; //상대방 마커
    LatLng counterpartyCurPoint; //상대방 위치

    Connect myConnect;
    String counterpartyUID = "";

    double disabledAddressLat, disabledAddressLng; //장애인 집 주소 위도 경도

    private static final int SEARCH_ADDRESS_ACTIVITY = 10000;

    CircleOverlay circle; //서클 오버레이
    public int rad = 0; //반경

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rangesetting);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("보호구역");

        rName = findViewById(R.id.rName);
        rName.setText("보호구역");
        seekBar = findViewById(R.id.seekBar);
        tvDis = findViewById(R.id.tvDis);
        rangeAddress = findViewById(R.id.rangeAddress);
        btnSet = findViewById(R.id.btnSet);
        btnAdd = findViewById(R.id.btnAdd);

        btnSet.setOnClickListener(this);
        btnAdd.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        //네이버 지도 객체 생성
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            //mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        // getMapAsync를 호출하여 비동기로 onMapReady 콜백 메서드 호출
        // onMapReady에서 NaverMap 객체를 받음
        mapFragment.getMapAsync(this);

        // 위치를 반환하는 구현체인 FusedLocationSource 생성
        mLocationSource = new FusedLocationSource(this, PERMISSION_REQUEST_CODE);


        // 반경 거리 설정하기
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rad = seekBar.getProgress();
                tvDis.setText(String.format("%d M", rad));

                if (circle != null) { //이미 존재했던 경우
                    circle.setMap(null);
                }
                //반경 원
                circle = new CircleOverlay();
                circle.setCenter(counterpartyCurPoint);
                circle.setRadius(rad); //반경
                circle.setColor(Color.parseColor("#880000ff")); //원 내부 색
                circle.setOutlineWidth(5); //원 테두리
                circle.setOutlineColor(Color.BLUE); //원 테두리 색
                circle.setMap(mNaverMap);
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

    /////////////////////////////////////////액티비티 뒤로가기 설정////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home: { //toolbar의 back키를 눌렀을 때 동작
                Intent intent = new Intent(getApplicationContext(), RealTimeLocationActivity.class);
                startActivity(intent);
                finish(); //로그인 화면으로 이동

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() { //뒤로가기 했을 때
        Intent intent = new Intent(getApplicationContext(), RealTimeLocationActivity.class);
        startActivity(intent);
        finish(); //로그인 화면으로 이동
    }

    //////////////////////////////////////////지도 설정////////////////////////////////////////////
    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        // NaverMap 객체 받아서 NaverMap 객체에 위치 소스 지정
        mNaverMap = naverMap;
        mNaverMap.setLocationSource(mLocationSource);

        // 권한확인. 결과는 onRequestPermissionsResult 콜백 매서드 호출
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
        classificationUser(user.getUid());
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

    /////////////////////////////////////////상대방 주소 기준 위치 마커////////////////////////////////////////
    private void counterpartyMarker() {
        counterpartyCurPoint = new LatLng(RealTimeLocationActivity.disabledAddressLatitude, RealTimeLocationActivity.disabledAddressLongitude);
        cameraUpdate = CameraUpdate.scrollTo(counterpartyCurPoint)
                .animate(CameraAnimation.Linear); //카메라 애니메이션
        mNaverMap.moveCamera(cameraUpdate);
        Log.w(TAG, "첫 카메라 위치 "+ counterpartyCurPoint);
        if(counterpartyCurPoint != null) { //상대방 위치가 존재하면
            if (counterpartyMarker == null) {//마커가 없었을 경우
                counterpartyMarker = new Marker();
                counterpartyMarker.setPosition(counterpartyCurPoint);
                counterpartyMarker.setIcon(MarkerIcons.LIGHTBLUE);
                counterpartyMarker.setMap(mNaverMap);
                Log.w(TAG, "첫 마커 위치 "+ counterpartyCurPoint);
            }
            else if (counterpartyMarker != null) { //마커가 존재했던 경우
                counterpartyMarker.setMap(null); //마커삭제
                counterpartyMarker.setPosition(counterpartyCurPoint);
                counterpartyMarker.setIcon(MarkerIcons.LIGHTBLUE);
                counterpartyMarker.setMap(mNaverMap);
            }
        }
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
                    }
                    else {
                        counterpartyCurPoint = new LatLng(myRangeP.latitude, myRangeP.longitude);
                        cameraUpdate = CameraUpdate.scrollTo(counterpartyCurPoint)
                                .animate(CameraAnimation.Linear); //카메라 애니메이션
                        mNaverMap.moveCamera(cameraUpdate);
                        Log.w(TAG, "첫 카메라 위치 "+ counterpartyCurPoint);
                        if(counterpartyCurPoint != null) { //상대방 위치가 존재하면
                            if (counterpartyMarker == null) {//마커가 없었을 경우
                                counterpartyMarker = new Marker();
                                counterpartyMarker.setPosition(counterpartyCurPoint);
                                counterpartyMarker.setMap(mNaverMap);
                                Log.w(TAG, "첫 마커 위치 "+ counterpartyCurPoint);
                            }
                            else if (counterpartyMarker != null) { //마커가 존재했던 경우
                                counterpartyMarker.setMap(null); //마커삭제
                                counterpartyMarker.setPosition(counterpartyCurPoint);
                                counterpartyMarker.setMap(mNaverMap);

                                //cir.radius(0);
                                if (circle != null) { //이미 존재했던 경우
                                    circle.setMap(null);
                                }
                                //반경 원
                                circle = new CircleOverlay();
                                circle.setCenter(counterpartyCurPoint);
                                circle.setRadius(myRangeP.getDis()); //반경
                                circle.setColor(Color.parseColor("#880000ff")); //원 내부 색
                                circle.setOutlineWidth(5); //원 테두리
                                circle.setOutlineColor(Color.BLUE); //원 테두리 색
                                circle.setMap(mNaverMap);
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
