package com.example.polarstarproject;

import android.Manifest;
import android.graphics.Color;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import com.example.polarstarproject.Domain.AddressGeocoding;
import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.Range;
import com.example.polarstarproject.Domain.SafeZone;
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
import java.util.Timer;
import java.util.TimerTask;

public class RangeSettingActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {
    Toolbar toolbar;
    EditText rName;
    TextView rangeAddress;
    Button btnSet, btnAdd;
    SeekBar seekBar;
    TextView tvDis;

    private DisconnectDialog disconnectDialog; //연결끊기 다이얼로그 팝업

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

    AddressGeocoding addressGeocoding; //상대 집 위치
            
    Marker counterpartyMarker; //상대방 마커
    LatLng counterpartyCurPoint; //상대방 위치

    Marker sMarker; //상대방 마커
    LatLng sPoint; //상대방 위치

    Connect myConnect;
    String counterpartyUID = "";

    String area = "";

    double searchAddressLat, searchAddressLng; //검색한 주소 위도 경도

    private static final int SEARCH_ADDRESS_ACTIVITY = 10000;

    CircleOverlay circle; //서클 오버레이
    public int rad = 0; //반경

    Timer timer; //상대방과 매칭 검사를 위한 타이머
    TimerTask timerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rangesetting);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("보호구역");

        rName = findViewById(R.id.rName);
        //rName.setText("보호구역");
        seekBar = findViewById(R.id.seekBar);
        tvDis = findViewById(R.id.tvDis);
        rangeAddress = findViewById(R.id.rangeAddress);
        btnSet = findViewById(R.id.btnSet);
        btnAdd = findViewById(R.id.btnAdd);

        btnSet.setOnClickListener(this);
        btnAdd.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        disconnectDialog = new DisconnectDialog(this);
        disconnectDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //타이틀 제거

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

        skipScreen();

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
                //reference.child("range").child(user.getUid()).child(rName.getText().toString()).child("distance").setValue(rad);

            }
        });
    }

    /////////////////////////////////////////액티비티 뒤로가기 설정////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home: { //toolbar의 back키를 눌렀을 때 동작
                Intent intent = new Intent(getApplicationContext(), SafeZoneActivity.class);
                startActivity(intent);
                finish(); //화면 이동
                timer.cancel();
                timerTask.cancel(); //타이머 종료

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() { //뒤로가기 했을 때
        Intent intent = new Intent(getApplicationContext(), SafeZoneActivity.class);
        startActivity(intent);
        finish(); //화면 이동
        timer.cancel();
        timerTask.cancel(); //타이머 종료
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
    }

    /////////////////////////////////////////연결 체크////////////////////////////////////////
    private void startDisconnectDialog(){
        RefactoringForegroundService.stopLocationService(this); //포그라운드 서비스 종료
        disconnectDialog = new DisconnectDialog(this);
        disconnectDialog.setCancelable(false);
        disconnectDialog.show();
        disconnectDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); //모서리 둥글게
    }

    /////////////////////////////////////////연결 여부 확인 후 화면 넘어가기////////////////////////////////////////
    private void skipScreen(){
        timer = new Timer();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                //3초마다 실행
                connectionCheck(); //상대방과 매칭 여부 확인
                Log.w(TAG, "돌아감");
            }
        };
        timer.schedule(timerTask,0,3000);

        classificationUser(user.getUid()); //사용자 구별
    }

    /////////////////////////////////////////연결 여부 확인////////////////////////////////////////
    private void connectionCheck(){ //firebase select 조회 함수, 내 connect 테이블 조회
        Query guardianQuery = reference.child("connect").child("guardian").orderByKey().equalTo(user.getUid()); //보호자 테이블 조회
        guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    if(myConnect.getCounterpartyCode() == null){ //상대방이 연결 끊었을 경우
                        if(! RangeSettingActivity.this.isFinishing()){ //finish 오류 방지
                            startDisconnectDialog();
                            timer.cancel();
                            timerTask.cancel(); //타이머 종료
                        }
                        Log.w(TAG, "상대 피보호자 없음");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
        Query query = reference.child("connect").child("clientage").orderByChild("myCode").equalTo(myConnect.getCounterpartyCode());
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
        Query query = reference.child("addressgeocoding").orderByKey().equalTo(counterpartyUID); //장애인 테이블 조회
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                addressGeocoding = new AddressGeocoding();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    addressGeocoding = ds.getValue(AddressGeocoding.class);
                }
                if (!dataSnapshot.exists()) {
                    Log.w(TAG, "상대방 집 위치 오류");
                }
                else {
                    counterpartyCurPoint = new LatLng(addressGeocoding.getAddressLatitude(), addressGeocoding.getAddressLongitude()); //상대 집 주소
                    cameraUpdate = CameraUpdate.scrollTo(counterpartyCurPoint)
                            .animate(CameraAnimation.Linear); //카메라 애니메이션
                    mNaverMap.moveCamera(cameraUpdate); //카메라 이동

                    if(counterpartyCurPoint != null) { //상대방 위치가 존재하면
                        if (counterpartyMarker == null) {//마커가 없었을 경우
                            counterpartyMarker = new Marker();
                            counterpartyMarker.setPosition(counterpartyCurPoint);
                            counterpartyMarker.setIcon(MarkerIcons.LIGHTBLUE);
                            counterpartyMarker.setMap(mNaverMap);
                        }
                        else if (counterpartyMarker != null) { //마커가 존재했던 경우
                            counterpartyMarker.setMap(null); //마커삭제
                            counterpartyMarker.setPosition(counterpartyCurPoint);
                            counterpartyMarker.setIcon(MarkerIcons.LIGHTBLUE);
                            counterpartyMarker.setMap(mNaverMap);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    ////////////////////////////////////임시 저장된 주소지로 이동한 마커/////////////////////////////////////////////
    private void mapMarker() {
        if(reference.child("tempRange").child(user.getUid()).orderByKey().equalTo(rName.getText().toString()) != null){
            reference.child("tempRange").child(user.getUid()).orderByKey().equalTo(rName.getText().toString()). //저장명으로 접근
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
                        if(counterpartyCurPoint != null) { // 보호구역이 존재하면
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

                            }
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
                searchAddressLng = Double.parseDouble(stringBuilder.substring(indexFirst + 5, indexLast));

                indexFirst = stringBuilder.indexOf("\"y\":\"");
                indexLast = stringBuilder.indexOf("\",\"distance\":");
                searchAddressLat = Double.parseDouble(stringBuilder.substring(indexFirst + 5, indexLast));

                //여기서 파이어베이스에 저장하고
                Range tempRange = new Range(searchAddressLat, searchAddressLng, rad);
                Log.w(TAG, "로그: "+ searchAddressLat+searchAddressLng+rad);
                reference.child("tempRange").child(user.getUid()).child(rName.getText().toString()).setValue(tempRange);
                mapMarker();

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
    public void Save(){
        Range myRange = new Range(searchAddressLat, searchAddressLng, rad);
        if (searchAddressLat != 0 && searchAddressLng != 0) { // 주소 검색 안하면 저장 안 되게
            reference.child("range").child(user.getUid()).child(rName.getText().toString()).setValue(myRange)
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) { //보호구역 설정 성공
                                Toast.makeText(getApplicationContext(), "보호구역 설정 완료", Toast.LENGTH_SHORT).show();
                            } else { //보호구역 설정 실패
                                Toast.makeText(getApplicationContext(), "보호구역 설정 실패", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

            SafeZone sZone = new SafeZone(rName.getText().toString(), rangeAddress.getText().toString(), rad);
            reference.child("safezone").child(user.getUid()).child(rName.getText().toString()).setValue(sZone)
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) { //보호구역 설정 성공
                                Toast.makeText(getApplicationContext(), "보호구역 설정 완료", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(getApplicationContext(), SafeZoneActivity.class);
                                startActivity(intent);
                            } else { //보호구역 설정 실패
                                Toast.makeText(getApplicationContext(), "보호구역 설정 실패", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

            reference.child("tempRange").child(user.getUid()).setValue(null);

        } else { //보호구역 설정 실패
            Toast.makeText(getApplicationContext(), "주소를 검색해주세요.", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAdd: //우편번호 검색
                if(rName.getText() != null && !rName.getText().toString().isEmpty()){ // 보호구역 이름 없으면 못 누름
                    Log.w(TAG, "rName: "+ rName.getText());
                    Intent i = new Intent(RangeSettingActivity.this, WebViewActivity.class);
                    startActivityForResult(i, SEARCH_ADDRESS_ACTIVITY);
                    break;
                }
                else {
                    Toast.makeText(getApplicationContext(), "보호구역 이름이 없습니다.", Toast.LENGTH_SHORT).show();//토스메세지 출력
                }

            case R.id.btnSet:
                reference.child("safezone").child(user.getUid()).child(rName.getText().toString()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        SafeZone value = snapshot.getValue(SafeZone.class);

                        if(value!=null){
                            Toast.makeText(getApplicationContext(),"저장명은 중복될 수 없습니다.",Toast.LENGTH_SHORT).show();//토스메세지 출력
                        }
                        else{
                            Save();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // 디비를 가져오던중 에러 발생 시
                        //Log.e("MainActivity", String.valueOf(databaseError.toException())); // 에러문 출력
                    }
                });
                break;


        }
    }
}
