package com.example.polarstarproject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.app.DatePickerDialog; //달력
import android.view.ViewGroup;
import android.widget.DatePicker; //달력
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import java.text.ParseException;
import java.util.ArrayList;
import java.text.SimpleDateFormat; //달력
import java.util.Calendar; //달력
import java.util.Date;
import java.util.Locale; //달력

public class RouteActivity extends AppCompatActivity implements OnMapReadyCallback,View.OnClickListener {
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

    LinearLayout container;
    Button datePickCc;
    int year, month, day;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        container = (LinearLayout) findViewById(R.id.container);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //inflater.inflate(R.layout.activity_date_picker, container, true);
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.activity_date_picker, container, false);

        datePickCc = (Button) rootView.findViewById(R.id.datePickCc);
        datePickCc.setOnClickListener(this);

        arrayPoints = new ArrayList<>();

        MapsInitializer.initialize(this);

        // SupportMapFragment을 통해 레이아웃에 만든 fragment의 ID를 참조하고 구글맵을 호출한다.
        SupportMapFragment mapFragment = (SupportMapFragment) this.getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                counterpartyUID = ds.getKey();
                            }

                            if (counterpartyUID != null && !counterpartyUID.isEmpty()) {
                                //disabledRoute();
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

        Log.w(TAG, "실행 " + dateResult);
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

    private void drawPolyline() {
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
        switch (v.getId()) {
            case R.id.datePickCc: //날짜 버튼 클릭
                Log.w(TAG, "실행");
                disabledRoute(year, month, day); //매칭 장애인 경로 가져오기
                break;
        }
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
                    year = yy;
                    month = mm;
                    day = dd;
                }
    };
}



