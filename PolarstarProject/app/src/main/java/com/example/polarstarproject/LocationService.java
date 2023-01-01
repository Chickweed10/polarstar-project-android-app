package com.example.polarstarproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.DepartureArrivalStatus;
import com.example.polarstarproject.Domain.Disabled;
import com.example.polarstarproject.Domain.RealTimeLocation;
import com.example.polarstarproject.Domain.Route;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

//백그라운드 위치 서비스
public class LocationService extends Service {
    private static final String TAG = "LocationService";

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseUser user; //firebase 변수

    int classificationUserFlag = 0; //사용자 구별 플래그
    int count = 0; //경로 스케쥴러 카운트

    double latitude, longitude;

    Connect myConnect; //내 연결 객체

    public double distance; //거리
    private final String DEFAULT = "DEFAULT";

    RealTimeLocationActivity realTimeLocationActivity;

    Intent notificationIntent;

    public LocationService(){
        realTimeLocationActivity = new RealTimeLocationActivity();
    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            if (locationResult != null && locationResult.getLastLocation() != null) {
                latitude = locationResult.getLastLocation().getLatitude();
                longitude = locationResult.getLastLocation().getLongitude();
                Log.v(TAG, latitude + ", " + longitude);

                mAuth = FirebaseAuth.getInstance();
                user = mAuth.getCurrentUser();
                realTimeLocationActivity.realTimeDeviceLocationBackground(user, latitude, longitude); //firebase에 실시간 위치 업데이트

                classificationUserBackground(); //사용자 구별

                if(classificationUserFlag == 1){ //장애인일 경우
                    if(count == 0){
                        routeScheduler(); //경로 스케쥴러 실행
                        count++;
                    }
                }
            }
        }
    };

    /////////////////////////////////////////사용자 구별////////////////////////////////////////
    private void classificationUserBackground(){
        Query disabledQuery = reference.child("connect").child("disabled").orderByKey().equalTo(user.getUid()); //장애인 테이블 조회
        disabledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) { 
                myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){ //connect 테이블에 mycode가 비어있지 않으면 장애인 일치
                    classificationUserFlag = 1;
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

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){ //connect 테이블에 mycode가 비어있지 않으면 보호자 일치
                    classificationUserFlag = 2;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void routeScheduler(){
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() { //경로 저장용 스케쥴러
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                //20초마다 실행
                LocalDate localDate = LocalDate.now(ZoneId.of("Asia/Seoul")); //현재 날짜 구하기
                String nowDate = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                Query routeQuery = reference.child("route").child(user.getUid()).child(nowDate).limitToLast(1); //장애인 경로 테이블 최근 기록 조회
                routeQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Route route = new Route();
                        for(DataSnapshot ds : snapshot.getChildren()){
                            route = ds.getValue(Route.class);
                        }

                        if(String.format("%.3f", latitude).equals(String.format("%.3f", route.getLatitude())) == false){ //위치를 이동했을 경우에만 경로 저장
                            if(String.format("%.3f", longitude).equals(String.format("%.3f", route.getLongitude())) == false){
                                Log.w(TAG, "백그라운드 실행");
                                realTimeLocationActivity.firebaseUpdateRoute(user, latitude, longitude); //firebase에 경로 저장
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        };
        timer.schedule(timerTask,0,20000);
    }

    public void createNotificationChannel(String channelId, String channelName, int importance) { //알림 초기화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId, channelName, importance));
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @SuppressLint("MissingPermission")
    private void startLocationService() {
        String channelId = "location_notification_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent(this, RealTimeLocationActivity.class); //알림 클릭시 띄울 화면 지정
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId);
        builder.setSmallIcon(R.drawable.ic_stat_polaris_smallicon);
        builder.setContentTitle("위치 서비스");
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setContentText("실행중");
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(channelId, "Location Service", NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setDescription("This channel is used by location service");
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper());
        startForeground(Constants.LOCATION_SERVICE_ID, builder.build());
    }

    private void stopLocationService() {
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(mLocationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel(DEFAULT, "default channel", NotificationManager.IMPORTANCE_HIGH); //알림 초기화
        notificationIntent = new Intent(this, RealTimeLocationActivity.class); // 클릭시 실행할 activity를 지정
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Constants.ACTION_START_LOCATION_SERVICE)) {
                    startLocationService();
                } else if (action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)) {
                    stopLocationService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}