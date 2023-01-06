package com.example.polarstarproject;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.example.polarstarproject.Domain.AddressGeocoding;
import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.DepartureArrivalStatus;
import com.example.polarstarproject.Domain.Disabled;
import com.example.polarstarproject.Domain.EmailVerified;
import com.example.polarstarproject.Domain.Guardian;
import com.example.polarstarproject.Domain.InOutStatus;
import com.example.polarstarproject.Domain.Range;
import com.example.polarstarproject.Domain.RealTimeLocation;
import com.example.polarstarproject.Domain.Route;
import com.example.polarstarproject.Domain.TrackingStatus;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

//실시간 위치
public class RealTimeLocationActivity extends AppCompatActivity implements OnMapReadyCallback {
    //프로젝트에서 우클릭 이 디바이스 항상 켜놓기 누름
    Toolbar toolbar;
    DrawerLayout drawerLayout;
    NavigationView navigationView; //네비게이션 바
    private AuthorityDialog authorityDialog; //권한 다이얼로그 팝업
    private DisconnectDialog disconnectDialog; //연결끊기 다이얼로그 팝업
    private WarningDialog terminationDialog; //앱 종료 다이얼로그 팝업

    private static final String TAG = "RealTimeLocation";

    Button itemMyinfo, itemOtherinfo, itemRoute, itemRange, itemSetting, itemManual;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    //firebase 변수

    //네이버 지도
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;

    Marker myMarker; //내 마커
    Marker counterpartyMarker; //상대방 마커

    LatLng counterpartyCurPoint; //상대방 위치

    CameraUpdate cameraUpdate; //지도 카메라
    int cameraCnt = 0; //카메라 이동 제어 위한 카운트
    int sCount = 0; //보호구역 개수
    int outCount = 0; //보호구역 이탈 개수

    Connect myConnect;
    String counterpartyUID = "";
    public int classificationUserFlag = 0, count;//장애인 보호자 구별 (0: 기본값, 1: 장애인, 2: 보호자), 스케줄러 호출용 카운트

    AddressGeocoding addressGeocoding;
    public static double disabledAddressLatitude, disabledAddressLongitude;  //피보호자 집 주소 지오코딩
    public double distance; //거리
    private final double DEFAULTDISTANCE= 1; //출도착 거리 기준
    private final String DEFAULT = "DEFAULT";
    public boolean inFlag, outFlag = false; //복귀, 이탈 플래그
    DepartureArrivalStatus departureArrivalStatus; //출도착 플래그 변수

    public LocationManager manager; //GPS 위치 권한

    int permissionFlag = 0; //위치 권한 플래그

    String counterpartyName; //상대방 이름
    Intent notificationIntent;

    Timer timer; //상대방 위치 검색을 위한 타이머
    TimerTask timerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime_location);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //다이얼로그 초기 설정
        authorityDialog = new AuthorityDialog(this, null);
        authorityDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //타이틀 제거

        disconnectDialog = new DisconnectDialog(this);
        disconnectDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //타이틀 제거

        //다이얼로그 밖 화면 흐리게
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.8f;
        getWindow().setAttributes(layoutParams);

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

        classificationUser(user.getUid()); //상대방 위치 띄우기

        ///////////////////////////////툴바 & 네비게이션 바////////////////////////////////
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //뒤로가기
        getSupportActionBar().setTitle("");

        Resources res = getResources();
        Drawable drawable = res.getDrawable(R.drawable.ic_menu, getTheme()); //Vector Asset 렌더링
        getSupportActionBar().setHomeAsUpIndicator(drawable); //왼쪽 상단 버튼 아이콘 지정

        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        navigationView = (NavigationView)findViewById(R.id.navigation_view);
        navigationView.setItemIconTintList(null); //설정 안하면 회색

        View headerView = navigationView.getHeaderView(0);
        ImageView headerViewImageContent = (ImageView) headerView.findViewById(R.id.iv_image); //네비게이션 바 프로필 사진
        TextView headerViewNameContent = (TextView) headerView.findViewById(R.id.tv_name); //네비게이션 바 프로필 이름
        TextView headerViewEmailContent = (TextView) headerView.findViewById(R.id.Edit_UserEmail); //네비게이션 바 프로필 이메일
        
        //네비게이션 콘텐츠
        itemMyinfo = (Button)headerView.findViewById(R.id.item_myinfo);
        itemOtherinfo = (Button)headerView.findViewById(R.id.item_otherinfo);
        itemRoute = (Button)headerView.findViewById(R.id.item_route);
        itemRange = (Button)headerView.findViewById(R.id.item_range);
        itemSetting = (Button)headerView.findViewById(R.id.item_setting);
        itemManual = (Button)headerView.findViewById(R.id.item_manual);

        //네비게이션 바 프로필 사진 띄우기
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference myPro = storageRef.child("profile").child(user.getUid());
        if (myPro != null) {
            myPro.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    //이미지 로드 성공시
                    Glide.with(headerView).load(uri).into(headerViewImageContent);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    StorageReference defaultPro = storageRef.child("profile").child("default.png");
                    defaultPro.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            //이미지 로드 성공시
                            Glide.with(headerView).load(uri).into(headerViewImageContent);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //이미지 로드 실패시
                            Log.w(TAG, "이미지 로드 실패");
                        }
                    });
                }
            });
        }

        //네비게이션 바 이름, 이메일 띄우기
        reference.child("disabled").child(user.getUid()).addValueEventListener(new ValueEventListener() { //장애인 테이블 조회
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Disabled disabled = snapshot.getValue(Disabled.class);
                if(disabled == null){ //보호자일 경우
                    reference.child("guardian").child(user.getUid()).addValueEventListener(new ValueEventListener() { //보호자 테이블 조회
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Guardian guardian = snapshot.getValue(Guardian.class);
                            if(guardian != null){
                                headerViewNameContent.setText(guardian.getName());
                                headerViewEmailContent.setText(guardian.getEmail());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.w(TAG, "사용자 오류");
                        }
                    });
                }
                else{
                    headerViewNameContent.setText(disabled.getName());
                    headerViewEmailContent.setText(disabled.getEmail());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { //참조에 액세스 할 수 없을 때 호출
                Toast.makeText(getApplicationContext(),"데이터를 가져오는데 실패했습니다" , Toast.LENGTH_SHORT).show();
            }
        });

        //네비게이션 버튼 이동
        itemMyinfo.setOnClickListener(new View.OnClickListener() { //내 정보
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.item_myinfo: //내 정보
                        if (classificationUserFlag == 1) { //장애인일 경우
                            connectionCheck(Myinfo_DuserActivity.class);

                        } else if (classificationUserFlag == 2) { //보호자일 경우
                            connectionCheck(Myinfo_Duser_nActivity.class);
                        }
                        break;
                }
            }
        });
        itemOtherinfo.setOnClickListener(new View.OnClickListener() { //상대 정보
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.item_otherinfo: //내 정보
                        if(classificationUserFlag == 1){ //장애인일 경우
                            connectionCheck(OtherInformationGuardianCheckActivity.class);
                        }
                        else if(classificationUserFlag == 2){ //보호자일 경우
                            connectionCheck(OtherInformationDisableCheckActivity.class);
                        }
                        break;
                }
            }
        });
        itemRoute.setOnClickListener(new View.OnClickListener() { //경로
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.item_route: //내 정보
                        if(classificationUserFlag == 1){ //장애인일 경우
                            connectionCheck(null);
                        }
                        else if(classificationUserFlag == 2){ //보호자일 경우
                            connectionCheck(RouteActivity.class);
                        }

                        break;
                }
            }
        });
        itemRange.setOnClickListener(new View.OnClickListener() { //보호구역
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.item_range: //내 정보
                        if(classificationUserFlag == 1){ //장애인일 경우
                            connectionCheck(null);
                        }
                        else if(classificationUserFlag == 2){ //보호자일 경우
                            connectionCheck(SafeZoneActivity.class);
                        }
                        break;
                }
            }
        });
        itemSetting.setOnClickListener(new View.OnClickListener() { //설정
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.item_setting: //내 정보
                        Intent settingIntent = new Intent(getApplicationContext(), MenuSettingActivity.class);
                        startActivity(settingIntent);
                        finish(); //설정 화면으로 이동
                        break;
                }
            }
        });
        itemManual.setOnClickListener(new View.OnClickListener() { //메뉴얼
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.item_manual: //내 정보
                        Intent settingIntent = new Intent(getApplicationContext(), ManualActivity.class);
                        startActivity(settingIntent);
                        finish(); //메뉴얼 화면으로 이동
                        break;
                }
            }
        });

        RefactoringForegroundService.startLocationService(this); //포그라운드 서비스 실행

        count = 0; //카운트 초기화

        createNotificationChannel(DEFAULT, "default channel", NotificationManager.IMPORTANCE_HIGH); //알림 초기화
        setNotificationIntent();
    }

    private void startAuthorityDialog(){
        authorityDialog = new AuthorityDialog(this, "접근 권한이 없습니다.");
        authorityDialog.setCancelable(false);
        authorityDialog.show();
        authorityDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); //모서리 둥글게
    }

    //권한 설정
    public void onCheckPermission() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isPowersaveMode = powerManager.isPowerSaveMode();

        if ( //권한이 모두 있는 경우
            //위치 접근 권한
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                        //카메라 접근 권한
                        && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        //저장소 접근 권한
                        && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        //절전모드
                        && isPowersaveMode == false)
        {

        }
        else { //하나라도 없는 경우
            Intent intent = new Intent(RealTimeLocationActivity.this, PermissionActivity.class);
            intent.putExtra("skipIntent", 2);
            startActivity(intent);
            finish();
        }
    }

    /////////////////////////////////////////네비게이션 바 설정////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:{ // 왼쪽 상단 버튼 눌렀을 때
                drawerLayout.openDrawer(GravityCompat.START);

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() { //뒤로가기 했을 때
        terminationDialog = new WarningDialog(RealTimeLocationActivity.this, "북극성 앱을 종료하시겠습니까?");
        terminationDialog.show(); // 다이얼로그 띄우기
        terminationDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); //모서리 둥글게

        //취소 버튼
        Button btnCancle = terminationDialog.findViewById(R.id.btn_cancle);
        btnCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 원하는 기능 구현
                terminationDialog.dismiss(); // 다이얼로그 닫기
            }
        });

        //확인 버튼
        Button btnOk = terminationDialog.findViewById(R.id.btn_ok);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 원하는 기능 구현
                terminationDialog.dismiss(); //다이얼로그 닫기
                moveTaskToBack(true); //태스크를 백그라운드로 이동
                finishAffinity(); //스택 비우기
                finish(); //액티비티 종료
            }
        });
    }

    /////////////////////////////////////////알림 화면 설정////////////////////////////////////////
    public void setNotificationIntent(){
        notificationIntent = new Intent(RealTimeLocationActivity.this, RealTimeLocationActivity.class); // 클릭시 실행할 activity를 지정
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    //////////////////////////////////////////지도 설정////////////////////////////////////////////
    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        // NaverMap 객체 받아서 NaverMap 객체에 위치 소스 지정
        mNaverMap = naverMap;
        mNaverMap.setLocationSource(mLocationSource);

        UiSettings uiSettings = mNaverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true); //현재 위치 버튼 활성화

        mNaverMap.addOnLocationChangeListener(new NaverMap.OnLocationChangeListener() {
            @Override
            public void onLocationChange(@NonNull Location location) {
                // 지도상에 마커 표시
                if(myMarker != null){
                    myMarker.setMap(null);
                }
                myMarker = new Marker();
                myMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
                myMarker.setZIndex(100); //Z 인덱스 설정해서 마커끼리 겹칠때 오류 방지
                myMarker.setMap(naverMap);

                if(cameraCnt == 0){ //초반 카메라 설정 (카메라 계속 따라오지 않게)
                    cameraCnt++;
                    cameraUpdate = CameraUpdate.scrollTo(new LatLng(location.getLatitude(), location.getLongitude()))
                            .animate(CameraAnimation.Linear);
                    //카메라 애니메이션
                    mNaverMap.moveCamera(cameraUpdate);
                }
                firebaseUpdateLocation(user, location.getLatitude(), location.getLongitude());
            }
        });

        // 권한확인. 결과는 onRequestPermissionsResult 콜백 매서드 호출
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(mLocationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)){
            if(!mLocationSource.isActivated()){
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.None);
                return;
            }
            else{
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    protected void onStart(){ //Activity가 사용자에게 보여지면
        super.onStart();
        onCheckPermission(); //권한 체크

        //이메일 유효성 검사
        if(user.isEmailVerified()) {
            EmailVerified emailVerified = new EmailVerified(true);
            reference.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 true

            Log.d(TAG, "메일 인증 성공");
        }
        else{
            EmailVerified emailVerified = new EmailVerified(false);
            reference.child("emailverified").child(user.getUid()).setValue(emailVerified); //이메일 유효성 false

            //Toast.makeText(DisabledRealTimeLocationActivity.this, "이메일 인증이 필요합니다.", Toast.LENGTH_SHORT).show(); //이메일 인증 요구 토스트 알림

            Log.d(TAG, "메일 인증 실패");
        }
    }


    public void realTimeDeviceLocationBackground(FirebaseUser user, double latitude, double longitude) { //백그라운드 실시간 위치 갱신
        Log.w (TAG, "백그라운드 실시간 위치 갱신");
        firebaseUpdateLocation(user, latitude, longitude); //firebase 실시간 위치 저장
    }

    private void firebaseUpdateLocation(FirebaseUser user, double latitude, double longitude) { //firebase에 실시간 위치 저장
        RealTimeLocation realTimeLocation = new RealTimeLocation(latitude,longitude);

        Log.w(TAG, "firebaseUpdate " + user.getUid()+ " : " + latitude + ", " + longitude);
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void firebaseUpdateRoute(FirebaseUser user, double latitude, double longitude) { //firebase에 경로용 위치 저장
        if(latitude != 0 && longitude != 0){
            LocalTime localTime = LocalTime.now(ZoneId.of("Asia/Seoul"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String nowTime = localTime.format(formatter); //현재 시간 구하기

            Log.w(TAG, "경로 위치: " + latitude + ", " + longitude);
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
    }

    /////////////////////////////////////////경로 삭제 스케쥴러////////////////////////////////////////
    public void routeDelete(){
        //30일 이전 기록은 자동으로 삭제 되도록
    }

    /////////////////////////////////////////상대방 위치////////////////////////////////////////
    public void counterpartyLocationScheduler(){ //20초마다 상대방 DB 검사 후, 위치 띄우기
        timer = new Timer();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                //20초마다 실행
                classificationUser(user.getUid());
            }
        };
        timer.schedule(timerTask,0,20000);
    }

    private void startDisconnectDialog(){
        RefactoringForegroundService.stopLocationService(this); //포그라운드 서비스 종료
        timer.cancel();
        timerTask.cancel(); //타이머 종료
        
        disconnectDialog = new DisconnectDialog(this);
        disconnectDialog.setCancelable(false);
        disconnectDialog.show();
        disconnectDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); //모서리 둥글게
    }

    /////////////////////////////////////////사용자 구별////////////////////////////////////////
    private void connectionCheck(Class skipClass){ //firebase select 조회 함수, 내 connect 테이블 조회
        Query disabledQuery = reference.child("connect").child("disabled").orderByKey().equalTo(user.getUid()); //장애인 테이블 조회
        disabledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    if(myConnect.getCounterpartyCode() == null){ //상대방이 연결 끊었을 경우
                        if(! RealTimeLocationActivity.this.isFinishing()){ //finish 오류 방지
                            startDisconnectDialog();
                        }
                        Log.w(TAG, "상대 보호자 없음");
                    }
                    else {
                        if(skipClass == null){
                            startAuthorityDialog();
                        }
                        else{
                            Intent otherInfoIntent = new Intent(getApplicationContext(), skipClass);
                            startActivity(otherInfoIntent);
                            finish(); //보호구역 화면으로 이동
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

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
                        if(! RealTimeLocationActivity.this.isFinishing()){ //finish 오류 방지
                            startDisconnectDialog();
                        }
                        Log.w(TAG, "상대 피보호자 없음");
                    }
                    else {
                        Intent otherInfoIntent = new Intent(getApplicationContext(), skipClass);
                        startActivity(otherInfoIntent);
                        finish(); //보호구역 화면으로 이동
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

                    if(myConnect.getCounterpartyCode() == null){ //상대방이 탈퇴할 경우
                        if(! RealTimeLocationActivity.this.isFinishing()){ //finish 오류 방지
                            startDisconnectDialog();
                        }
                        Log.w(TAG, "상대 보호자 없음");
                    }

                    if(count == 0){
                        routeDelete(); //30일 이전 피보호자 경로 삭제
                    }
                    count++;
                    getOtherUID();

                    if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { //위치 권한 없는 경우
                        TrackingStatus trackingStatus = new TrackingStatus(false);
                        reference.child("trackingstatus").child(user.getUid()).setValue(trackingStatus);
                    }

                    else {
                        TrackingStatus trackingStatus = new TrackingStatus(true); //위치 권한 있는 경우
                        reference.child("trackingstatus").child(user.getUid()).setValue(trackingStatus);
                    }
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

                    if(myConnect.getCounterpartyCode() == null){ //상대방이 탈퇴할 경우
                        if(! RealTimeLocationActivity.this.isFinishing()){ //finish 오류 방지
                            startDisconnectDialog();
                        }
                        Log.w(TAG, "상대 피보호자 없음");
                    }

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
        if(classificationUserFlag == 1) { //내가 피보호자고, 상대방이 보호자일 경우
            Query query = reference.child("connect").child("guardian").orderByChild("myCode").equalTo(myConnect.getCounterpartyCode());
            query.addListenerForSingleValueEvent(new ValueEventListener() { //보호자 코드로 보호자 uid 가져오기
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        counterpartyUID = ds.getKey();
                    }

                    if(counterpartyUID != null  && !counterpartyUID.isEmpty()){
                        counterpartyMarker(); //실시간 위치 마커
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else if(classificationUserFlag == 2) { //내가 보호자고, 상대방이 피보호자일 경우
            Query query = reference.child("connect").child("disabled").orderByChild("myCode").equalTo(myConnect.getCounterpartyCode());
            query.addListenerForSingleValueEvent(new ValueEventListener() { //피보호자 코드로 장애인 uid 가져오기
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        counterpartyUID = ds.getKey();
                    }

                    if(counterpartyUID != null  && !counterpartyUID.isEmpty()){
                        Query clientageQuery = reference.child("addressgeocoding").orderByKey().equalTo(counterpartyUID); //피보호자 주소 지오코딩 테이블 조회
                        clientageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                addressGeocoding = new AddressGeocoding();
                                for(DataSnapshot ds : dataSnapshot.getChildren()){
                                    addressGeocoding = ds.getValue(AddressGeocoding.class);
                                }

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                        counterpartyMarker();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Intent intent = new Intent(getApplicationContext(), ConnectActivity.class); //연결 화면 넘어가기
                    startActivity(intent);
                    finish();
                    Log.w(TAG, "상대 피보호자 없음");
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

                    if(counterpartyCurPoint != null) { //상대방 위치가 존재하면
                        if(counterpartyMarker == null){//마커가 없었을 경우
                            counterpartyMarker = new Marker();
                            counterpartyMarker.setPosition(counterpartyCurPoint);
                            counterpartyMarker.setIcon(MarkerIcons.LIGHTBLUE);
                            counterpartyMarker.setZIndex(0); //Z 인덱스 설정해서 마커끼리 겹칠때 오류 방지
                            counterpartyMarker.setHideCollidedMarkers(true); //마커 겹치면 숨기기
                            counterpartyMarker.setMap(mNaverMap);
                            counterpartyLocationScheduler(); //상대 위치 스케쥴러 실행
                        }
                        else if(counterpartyMarker != null) { //마커가 존재했던 경우
                            counterpartyMarker.setMap(null); //마커삭제
                            counterpartyMarker.setPosition(counterpartyCurPoint);
                            counterpartyMarker.setIcon(MarkerIcons.LIGHTBLUE);
                            counterpartyMarker.setZIndex(0); //Z 인덱스 설정해서 마커끼리 겹칠때 오류 방지
                            counterpartyMarker.setHideCollidedMarkers(true); //마커 겹치면 숨기기
                            counterpartyMarker.setMap(mNaverMap);
                        }

                        if(classificationUserFlag == 2){ //보호자일 경우 //////////////////////////////장애인 위치 실시간으로 가져오는데
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
                                        departureArrivalNotification(); //출도착 알림
                                        trackingStatusCheck(); //추적불가 알림
                                        inOutCheck(); // 복귀이탈 알림
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

                    return;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void departureArrivalNotification(){ //출도착 알림
        Query query = reference.child("departurearrivalstatus").orderByKey().equalTo(counterpartyUID); //출도착 플래그 테이블 조회
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                departureArrivalStatus = new DepartureArrivalStatus();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    departureArrivalStatus = ds.getValue(DepartureArrivalStatus.class); //값 가져오기
                }
                if(departureArrivalStatus != null){
                    departureArrivalCheck(); //출발 도착 판단 후 알림
                }
                else {

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void departureArrivalCheck() { //출발 도착 판단 후 알림
        //경도(longitude)가 X, 위도(latitude)가 Y
        disabledAddressLatitude = addressGeocoding.getAddressLatitude();
        disabledAddressLongitude = addressGeocoding.getAddressLongitude();

        distance = Math.sqrt(((counterpartyCurPoint.longitude-disabledAddressLongitude)*(counterpartyCurPoint.longitude-disabledAddressLongitude))+((counterpartyCurPoint.latitude-disabledAddressLatitude)*(counterpartyCurPoint.latitude-disabledAddressLatitude)));

        if(disabledAddressLatitude != 0.0 && disabledAddressLongitude != 0.0){
            if(!departureArrivalStatus.departureStatus){ //아직 출발 안했을 경우 (집 출발 알림)
                if(distance*1000 > DEFAULTDISTANCE) { //1000곱하면 단위가 미터임
                    if(counterpartyCurPoint.longitude != -122.0840064 && counterpartyCurPoint.latitude != 37.4219965){
                        DepartureArrivalStatus departureArrivalStatus = new DepartureArrivalStatus(true, false); //출발 true, 도착 플래그 초기화
                        reference.child("departurearrivalstatus").child(counterpartyUID).setValue(departureArrivalStatus); //출도착 플래그 초기화
                        departureNotification(DEFAULT, 1); //출발 알림 울리기
                    }
                }
            }

            if(!departureArrivalStatus.arrivalStatus){ //아직 도착안했을 경우 (집 도착 알림)
                if(departureArrivalStatus.getDepartureStatus()){ //출발함
                    if(distance*1000 < DEFAULTDISTANCE) {
                        if(counterpartyCurPoint.longitude != -122.0840064 && counterpartyCurPoint.latitude != 37.4219965){
                            DepartureArrivalStatus departureArrivalStatus = new DepartureArrivalStatus(false, true); //도착 true, 출발 플래그 초기화
                            reference.child("departurearrivalstatus").child(counterpartyUID).setValue(departureArrivalStatus); //출도착 플래그 초기화
                            arrivalNotification(DEFAULT, 2); //도착 알림 울리기
                        }
                    }
                }
            }
        }
    }
    /////////////////////////////////////////장애인 추적불가 알림////////////////////////////////////////
    private void trackingStatusCheck() {
        Query disabledQuery = reference.child("trackingstatus").orderByKey().equalTo(counterpartyUID); //추적불가 상태 검사
        disabledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                TrackingStatus trackingStatus = new TrackingStatus();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    trackingStatus = ds.getValue(TrackingStatus.class);
                }

                if(!trackingStatus.getStatus()){ //추적 불가 상태
                    if(permissionFlag == 0){
                        trackingImpossibleNotification(DEFAULT, 3);
                        permissionFlag = 1;
                    }
                }
                else { //추적 가능
                    if(permissionFlag == 1){
                        trackingPossibleNotification(DEFAULT, 4);
                        permissionFlag = 0;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void inOutCheck(){ //출발 도착 판단 후 알림
        Query query = reference.child("inoutstatus").orderByKey().equalTo(counterpartyUID);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                InOutStatus inOutStatus = new InOutStatus();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    inOutStatus = ds.getValue(InOutStatus.class);
                }

                if(inOutStatus != null){
                    outFlag = inOutStatus.outStatus;
                    inFlag = inOutStatus.inStatus; //값 집어넣기
                }
                else { //추적 가능

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        alertNotification();
    }

    ////거리계산해서 보호구역 벗어나면 알림 항수 호출하는 메소드 만들기
    private void alertNotification(){
        sCount=0; //보호구역 개수
        outCount=0; // 이탈 횟수

        if(reference.child("range").child(user.getUid()).orderByKey() != null){
            reference.child("range").child(user.getUid()).orderByKey(). //장애인 집 주소 가져오기
                    addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Range myRangeP = new Range();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        sCount++;
                        myRangeP = ds.getValue(Range.class);

                        if (!snapshot.exists()) {
                            Log.w(TAG, "보호구역 가져오기 오류");
                        }
                        else {
                            double sDis = myRangeP.distance;
                            //경도(longitude)가 X, 위도(latitude)가 Y
                            //double nDis = Math.sqrt(((counterpartyCurPoint.longitude-myRangeP.longitude)*(counterpartyCurPoint.longitude-myRangeP.longitude))+((counterpartyCurPoint.latitude-myRangeP.latitude)*(counterpartyCurPoint.latitude-myRangeP.latitude)));
                            double nDis = cDistance(counterpartyCurPoint.latitude, counterpartyCurPoint.longitude, myRangeP.latitude, myRangeP.longitude);
                            Log.w(TAG, "현재거리: " + nDis + "세팅거리: "+sDis);

                            if(myRangeP.latitude != 0.0 && myRangeP.longitude != 0.0){
                                if (nDis > sDis) { //1000곱하면 단위가 미터임
                                    outCount++;
                                }
                            }
                        }
                    }

                    if(outFlag==false) { //아직 이탈 안했을 경우
                        if (outCount == sCount) { //
                            outNotification(DEFAULT, 5);//이탈 알림 울리기

                            InOutStatus inOutStatus = new InOutStatus(true, false); //이탈 true, 복귀 플래그 초기화
                            reference.child("inoutstatus").child(counterpartyUID).setValue(inOutStatus); //이탈복귀 플래그 초기화
                        }
                    }
                    if(inFlag==false){
                        if (outFlag==true) { //아직 안 돌아간 경우
                            if (outCount != sCount) {
                                inNotification(DEFAULT, 6); //도착 알림 울리기
                                InOutStatus inOutStatus = new InOutStatus(false, true); //복귀 true, 이탈 플래그 초기화
                                reference.child("inoutstatus").child(counterpartyUID).setValue(inOutStatus); //이탈복귀 플래그 초기화
                            }
                        }
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    /////////////////////////////////////////알림////////////////////////////////////////
    public void createNotificationChannel(String channelId, String channelName, int importance) { //알림 초기화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId, channelName, importance));
        }
    }

    public void departureNotification(String channelId, int id) { //출발 알림
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_stat_polaris_smallicon) //알림 이미지
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

    public void arrivalNotification(String channelId, int id) { //도착 알림
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_stat_polaris_smallicon) //알림 이미지
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
    ////범위 이탈 알림 만들기
    private void outNotification(String channelId, int id) { //이탈 알림
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_stat_polaris_smallicon) //알림 이미지
                .setContentTitle("북극성")
                .setContentText(counterpartyName + "님이 보호구역을 벗어났습니다.")
                .setContentIntent(pendingIntent)    // 클릭시 설정된 PendingIntent가 실행된다
                .setAutoCancel(true)                // true이면 클릭시 알림이 삭제된다
                //.setTimeoutAfter(1000)
                //.setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }
    private void inNotification(String channelId, int id) { //돌아옴 알림
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_stat_polaris_smallicon) //알림 이미지
                .setContentTitle("북극성")
                .setContentText(counterpartyName + "님이 보호구역에 들어왔습니다.")
                .setContentIntent(pendingIntent)    // 클릭시 설정된 PendingIntent가 실행된다
                .setAutoCancel(true)                // true이면 클릭시 알림이 삭제된다
                //.setTimeoutAfter(1000)
                //.setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    private void trackingImpossibleNotification(String channelId, int id){ //추적 불가 알림
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_stat_polaris_smallicon) //알림 이미지
                .setContentTitle("북극성")
                .setContentText(counterpartyName + "님이 위치(GPS) 사용을 중단했습니다.")
                .setContentIntent(pendingIntent)    // 클릭시 설정된 PendingIntent가 실행된다
                .setAutoCancel(true)                // true이면 클릭시 알림이 삭제된다
                //.setTimeoutAfter(1000)
                //.setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    private void trackingPossibleNotification(String channelId, int id){ //추적 가능 알림
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_stat_polaris_smallicon) //알림 이미지
                .setContentTitle("북극성")
                .setContentText(counterpartyName + "님이 위치(GPS) 사용을 허용했습니다.")
                .setContentIntent(pendingIntent)    // 클릭시 설정된 PendingIntent가 실행된다
                .setAutoCancel(true)                // true이면 클릭시 알림이 삭제된다
                //.setTimeoutAfter(1000)
                //.setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    /////////////////////////////////////////범위 설정 거리 계산////////////////////////////////////////
    // 두 좌표 사이의 거리 계산 함수
    private static double cDistance(double lat1, double lon1, double lat2, double lon2){
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))* Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1))*Math.cos(deg2rad(lat2))*Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60*1.1515*1609.344;

        return dist; //단위 meter
    }
    //10진수를 radian(라디안)으로 변환
    private static double deg2rad(double deg){
        return (deg * Math.PI/180.0);
    }
    //radian(라디안)을 10진수로 변환
    private static double rad2deg(double rad){
        return (rad * 180 / Math.PI);
    }
}