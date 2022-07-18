package com.example.polarstarproject;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.polarstarproject.Domain.Connect;
import com.example.polarstarproject.Domain.Route;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class RouteActivity extends AppCompatActivity implements OnMapReadyCallback,View.OnClickListener{
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference reference = database.getReference();
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private static final String TAG = "Route";

    private GoogleMap map;
    private final LatLng defaultLocation = new LatLng(37.56, 126.97);
    private static final int DEFAULT_ZOOM = 15;

    ArrayList<LatLng> arrayPoints;

    Connect myConnect;
    String counterpartyUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        arrayPoints = new ArrayList<>();

        MapsInitializer.initialize(this);

        SupportMapFragment mapFragment = (SupportMapFragment) this.getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));

        getOtherUID();
    }

    /////////////////////////////////////////상대방 UID 가져오기////////////////////////////////////////
    private void getOtherUID(){
        Query guardianQuery = reference.child("connect").child("guardian").orderByKey().equalTo(user.getUid()); //보호자 테이블 조회
        guardianQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                myConnect = new Connect();
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    myConnect = ds.getValue(Connect.class);
                }

                if(myConnect.getMyCode() != null && !myConnect.getMyCode().isEmpty()){
                    Query query = reference.child("connect").child("disabled").orderByChild("myCode").equalTo(myConnect.getCounterpartyCode());
                    query.addListenerForSingleValueEvent(new ValueEventListener() { //보호자와 매칭된 장애인 uid 가져오기
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            for(DataSnapshot ds : dataSnapshot.getChildren()){
                                counterpartyUID = ds.getKey();
                            }

                            if(counterpartyUID != null && !counterpartyUID.isEmpty()){
                                disabledRoute();
                            }
                            else {
                                Toast.makeText(RouteActivity.this, "오류", Toast.LENGTH_SHORT).show();
                                Log.w(TAG, "상대방 인적사항 확인 오류");
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
                else {
                    Log.w(TAG, "내 인적사항 확인 오류");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /////////////////////////////////////////경로 그리기////////////////////////////////////////
    private void disabledRoute(){
        Query routeQuery = reference.child("route").child(counterpartyUID).orderByKey().equalTo("2022-07-19");
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

    private void drawPolyline(){
        int size = arrayPoints.size() / 2;
        LatLng routeLocation = new LatLng(arrayPoints.get(size).latitude, arrayPoints.get(size).longitude);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(routeLocation, 12));
        //경로 그려진 구역에 카메라 포커싱 잡아보기

        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.RED);
        polylineOptions.width(8);
        polylineOptions.addAll(arrayPoints);
        polylineOptions.startCap(new RoundCap());
        polylineOptions.endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow), 150));

        map.addPolyline(polylineOptions);
    }


    @Override
    public void onClick(View v) {
        /*switch (v.getId()) {
            case R.id.: //날짜 버튼 클릭
                getOtherUID(); //매칭 장애인 경로 가져오기
                break;*/
    }
}
