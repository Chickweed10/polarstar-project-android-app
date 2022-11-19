package com.example.polarstarproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.app.DatePickerDialog; //달력
import android.widget.DatePicker; //달력
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.Route;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.GeometryUtils;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.text.SimpleDateFormat; //달력
import java.util.Calendar; //달력
import java.util.Date;
import java.util.Locale; //달력

public class RouteActivity extends AppCompatActivity implements OnMapReadyCallback {
    Toolbar toolbar;

    private static final String TAG = "Route";

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

    ArrayList<LatLng> arrayPoints; //위치 경로 리스트
    PathOverlay path = new PathOverlay(); //경로선

    CameraUpdate cameraUpdate; //지도 카메라

    Connect myConnect;
    String counterpartyUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("위치 기록");

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        arrayPoints = new ArrayList<>(); //위치 경로 리스트

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

        /////////////////////////////////////////달력///////////////////////////////////
        EditText et_Date = (EditText) findViewById(R.id.Date);
        Calendar cal = Calendar.getInstance();
        et_Date.setText(cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE));
        // Calendar cal~ et_Date.setText, 날짜를 출력하는 EditText에 오늘 날짜 설정.
        et_Date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(RouteActivity.this, mDatePicker, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH)).show();
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
        mNaverMap = naverMap;
        mNaverMap.setLocationSource(mLocationSource);

        getOtherUID(); //상대방 UID 가져오기
    }
    

    /////////////////////////////////////////상대방 UID 가져오기////////////////////////////////////////
    private void getOtherUID() {
        Query guardianQuery = reference.child("connect").child("guardian").orderByKey().equalTo(user.getUid()); //보호자 테이블 조회
        guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                myConnect = new Connect();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    myConnect = ds.getValue(Connect.class);
                }

                if (myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()) {
                    Query query = reference.child("connect").child("disabled").orderByChild("myCode").equalTo(myConnect.getCounterpartyCode());
                    query.addListenerForSingleValueEvent(new ValueEventListener() { //보호자와 매칭된 장애인 uid 가져오기
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                counterpartyUID = ds.getKey();
                            }

                            if (counterpartyUID != null && !counterpartyUID.isEmpty()) {
                                LocalDate localDate = LocalDate.now(ZoneId.of("Asia/Seoul")); //현재 날짜 구하기
                                String nowDate = localDate.format(DateTimeFormatter.ofPattern("yyyy-M-d"));
                                String[] arrayDate = nowDate.split("-"); //'-'를 기준으로 문자열 쪼개기
                                
                                int year = Integer.parseInt(arrayDate[0]); //년도
                                int month = Integer.parseInt(arrayDate[1]); //월
                                int day = Integer.parseInt(arrayDate[2]); //일
                                
                                disabledRoute(year, month-1, day);
                                Log.w(TAG, "날짜: " + year + " " + month + " " + day);
                            } else {
                                Toast.makeText(RouteActivity.this, "오류", Toast.LENGTH_SHORT).show();
                                Log.w(TAG, "상대방 인적사항 확인 오류");
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                } else {
                    Log.w(TAG, "내 인적사항 확인 오류");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /////////////////////////////////////////경로 그리기////////////////////////////////////////
    private void disabledRoute(int year, int month, int day) {
        Date date = null;

        String dateString = String.format("%d-%d-%d", year, month + 1, day);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-M-d");
        try {
            date = formatter.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateResult = transFormat.format(date);

        Query routeQuery = reference.child("route").child(counterpartyUID).orderByKey().equalTo(dateResult);
        routeQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Route route = new Route();

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    for (DataSnapshot ds2 : ds.getChildren()) {
                        route = ds2.getValue(Route.class);
                        LatLng latLng = new LatLng(route.getLatitude(), route.getLongitude());

                        arrayPoints.add(latLng);
                    }
                }

                if (route.getNowTime() != null) {
                    drawPolyline();
                } else {
                    Log.w(TAG, "경로 없음");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void drawPolyline() { //경로선 그리기
        //카메라 설정
        CameraPosition cameraPosition;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(int i=0; i<arrayPoints.size(); i++){
            builder.include(arrayPoints.get(i));
        }

        LatLngBounds bounds = builder.build();
        cameraUpdate = CameraUpdate.fitBounds(bounds, 100); //경로 다 들어오게 카메라 이동
        mNaverMap.moveCamera(cameraUpdate);

        //경로선 설정
        int width = getResources().getDimensionPixelSize(R.dimen.path_overlay_width);

        path = new PathOverlay();
        path.setCoords(arrayPoints);
        path.setWidth(width); //경로선 두께
        path.setOutlineWidth(0); //경로선 테두리 두께
        path.setColor(Color.BLUE); //경로선 색상
        path.setPatternImage(OverlayImage.fromResource(R.drawable.path_pattern)); //경로선 패턴
        path.setPatternInterval(getResources().getDimensionPixelSize(R.dimen.overlay_pattern_interval)); //패턴 간격
        path.setMap(mNaverMap);
    }

    //////////////////////////////////////// 달력 ///////////////////////////////////
    Calendar mCalendar = Calendar.getInstance();

    DatePickerDialog.OnDateSetListener mDatePicker = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, month);
            mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel();

            arrayPoints.clear();
            path.setMap(null);
            
            disabledRoute(year, month, dayOfMonth);
        }
    };

    public void mOnClick_DatePick(View view) {
        // DatePicker가 처음 떴을 때, 오늘 날짜가 보이도록 설정.
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, mDateSet, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE)).show();
    }

    private void updateLabel() {
        String myFormat = "yyyy/MM/dd";    // 출력형식   1900/12/31
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.KOREA);

        EditText et_date = (EditText) findViewById(R.id.Date);
        et_date.setText(sdf.format(mCalendar.getTime()));
    }

    DatePickerDialog.OnDateSetListener mDateSet =
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int yy, int mm, int dd) {
                    // DatePicker에서 선택한 날짜를 EditText에 설정
                    TextView tv = findViewById(R.id.Date);
                    tv.setText(String.format("%d-%d-%d", yy, mm + 1, dd));
                }
    };
}



