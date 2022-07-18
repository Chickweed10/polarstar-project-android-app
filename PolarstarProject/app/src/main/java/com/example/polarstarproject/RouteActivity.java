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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.polarstarproject.Domain.Route;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

        LocalDate localDate = LocalDate.now(ZoneId.of("Asia/Seoul")); //현재 날짜 구하기
        String nowDate = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Query routeQuery = reference.child("route").child(user.getUid()).orderByKey().equalTo("2022-07-19");
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
        polylineOptions.width(5);
        polylineOptions.addAll(arrayPoints);

        map.addPolyline(polylineOptions);
    }


    @Override
    public void onClick(View view) {

    }
}
