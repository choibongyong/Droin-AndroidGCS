package com.example.mygcs;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

import com.naver.maps.map.overlay.PolylineOverlay;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    NaverMap naverMap;

    int count_Basic =1, count_Hybrid =1, count_Navi =1, count_offCadastral =1, count_onCadastral =1;

    private int i = 0;
    private Boolean isMapLock = true;
    private int Mapclick = 1;

    private final Handler handler = new Handler();
    private Drone drone;
    private ControlTower controlTower;
    private int droneType = Type.TYPE_UNKNOWN;
    private Spinner modeSelector;
    private Marker mDroneMarker;
    private Marker mMarkerGuide; //GCS 위치 표시마커 옵션

    private Button baseAltitude;
    private Button addAltitude;
    private Button subAltitude;

    private Attitude droneYawAttitude;
    private Altitude droneAltitude;
    private double SetAltitudevalue = 4.0;
    private LatLng defaultPosition = new LatLng(35.945367, 126.682193);;
    private PolylineOverlay polyline;
    private List<LatLng> coords;

    private LatLng checklatLng;

    RecyclerView recyclerView;
    LinearLayoutManager layoutManager;
    RecyclerAdapter adapter;
    ArrayList<SingerItem> messageBundle = new ArrayList<>();

    @Override
    public void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        mDroneMarker = new Marker();
        mDroneMarker.setIconTintColor(Color.RED);
        mDroneMarker.setWidth(50);
        mDroneMarker.setHeight(80);
        mDroneMarker.setFlat(true);

        mMarkerGuide = new Marker();
        mMarkerGuide.setIconTintColor(Color.BLUE);
        mMarkerGuide.setWidth(50);
        mMarkerGuide.setHeight(80);

        polyline = new PolylineOverlay();
        coords = new ArrayList<>();

        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
                try {
                    ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
                } catch (Exception e) {
                    Log.d("myLog", "비행모드 세팅하는 부분에서 오류가 발생 했습니다.");
                    Log.d("myLog", "오류 메시지 : " + e.getMessage());
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        baseAltitude = (Button) findViewById(R.id.btnTakeoffAltitude);
        baseAltitude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                i++;
                if(i == 1) {
                    //SetAltitudevalue = Double.parseDouble(baseAltitude.getText().toString().replace("m       이륙고도", ""));
                    droneAltitude.setAltitude(SetAltitudevalue);
                    alertUser("설정높이 "+ String.format("%3.1f", SetAltitudevalue));
                }

                if(i > 1){
                    addAltitude.setVisibility(View.VISIBLE);
                    subAltitude.setVisibility(View.VISIBLE);
                }
            }
        });

        addAltitude = (Button) findViewById(R.id.btnAddAltitude);
        addAltitude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetAltitudevalue += 0.5;
                //SetAltitudevalue += Double.parseDouble(addAltitude.getText().toString().replace("+", ""));
                droneAltitude.setAltitude(SetAltitudevalue);
                alertUser("설정높이 "+ String.format("%3.1f", SetAltitudevalue));
            }
        });

        subAltitude = (Button) findViewById(R.id.btnSubAltitude);
        subAltitude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SetAltitudevalue -= 0.5;
                droneAltitude.setAltitude(SetAltitudevalue);
                alertUser("설정높이 "+ String.format("%3.1f", SetAltitudevalue));
            }
        });

        //Recycler Setting
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new RecyclerAdapter(messageBundle);

        //Naver Map Setting
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map_fragment);

        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map_fragment, mapFragment).commit();
        }
        mapFragment.getMapAsync((OnMapReadyCallback) this);
}

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }
 //=====================================================================================================

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() { alertUser("DroneKit-Android Interrupted"); }

 //=====================================================================================================

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateBattry();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.ATTITUDE_UPDATED:
                updateYaw();
                break;

            case AttributeEvent.GPS_COUNT:
                updateSatellite();
                break;

            case AttributeEvent.GPS_POSITION:
                updateDronePosition();
                Guid();
                break;

            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }

    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch(connectionStatus.getStatusCode()){
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                alertUser("Connection Failed:" + msg);
                break;
        }
    }
 //=====================================================================================================

    public void onBtnConnectTap(View view) {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
        }
    }

    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }

    public void onArmButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) { alertUser("Unable to land the vehicle."); }

                @Override
                public void onTimeout() {
                    alertUser("Unable to land the vehicle.");
                }
            });

        } else if (vehicleState.isArmed()) {
            // Take off
            AlertDialog.Builder takeoff_alert_confirm = new AlertDialog.Builder(MainActivity.this);
            takeoff_alert_confirm.setMessage("지정한 이륙고도까지 기체가 상승합니다.\n" + "이륙시키겠습니까?").setCancelable(false)
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            ControlApi.getApi(drone).takeoff(SetAltitudevalue, new AbstractCommandListener() {

                                @Override
                                public void onSuccess() { alertUser("Taking off..."); }

                                @Override
                                public void onError(int i) {
                                    alertUser("Unable to take off.");
                                }

                                @Override
                                public void onTimeout() {
                                    alertUser("Unable to take off.");
                                }
                            });
                        }
                    })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = takeoff_alert_confirm.create();
            alert.show();

        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            // Connected but not Armed
            AlertDialog.Builder arm_alert_confirm = new AlertDialog.Builder(MainActivity.this);
            arm_alert_confirm.setMessage("모터를 가동합니다.\n" + "모터를 고속으로 회전시키겠습니까?").setCancelable(false)
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            VehicleApi.getApi(drone).arm(true, false, new SimpleCommandListener() {
                                @Override
                                public void onError(int executionError) {
                                    alertUser("Unable to arm vehicle.");
                                }

                                @Override
                                public void onTimeout() {
                                    alertUser("Arming operation timed out.");
                                }
                            });
                        }
                    })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert = arm_alert_confirm.create();
            alert.show();
        }
    }

 //========================================================================================================

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.connect);
        if (isConnected) {
            connectButton.setText("Disconnect");
        } else {
            connectButton.setText("Connect");
        }
    }

    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button connectButton = (Button) findViewById(R.id.connect);
        Button armButton = (Button) findViewById(R.id.btnArm);

        if (this.drone.isConnected()) {
            connectButton.setVisibility(View.GONE);
            armButton.setVisibility(View.VISIBLE);
        } else {
            connectButton.setVisibility(View.VISIBLE);
            armButton.setVisibility(View.GONE);
            baseAltitude.setVisibility(View.GONE);
        }

        if (vehicleState.isFlying()) {
            // Land
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    protected void updateBattry() {
        TextView batteryTextView = (TextView) findViewById(R.id.batteryTextView);
        Battery droneBATTERY = this.drone.getAttribute(AttributeType.BATTERY);
        batteryTextView.setText("전압 " + String.format("%3.1f", droneBATTERY.getBatteryVoltage()) + "V");
    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText("속도 " + String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText("고도 " + String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    protected void updateYaw() {
        TextView YawTextView = (TextView) findViewById(R.id.yawTextView);
        droneYawAttitude = this.drone.getAttribute(AttributeType.ATTITUDE);
        YawTextView.setText("YAW " +String.format("%3.1f", droneYawAttitude.getYaw()) + "deg");
    }

    protected void updateSatellite() {
        TextView SatelliteTextView = (TextView) findViewById(R.id.satelliteTextView);
        Gps droneSatellite = this.drone.getAttribute(AttributeType.GPS);
        SatelliteTextView.setText("위성 " +String.format("%d", droneSatellite.getSatellitesCount()));
    }

    protected void updateDronePosition() {
        Gps droneLatLong = this.drone.getAttribute(AttributeType.GPS);
        LatLong MapDroneLatLong = droneLatLong.getPosition();
        Button MapLock = (Button) findViewById(R.id.MapLockbtn);
        Button MapMove = (Button) findViewById(R.id.MapMovebtn);

        MapLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapMove.setVisibility(View.VISIBLE);
                isMapLock = true;
            }
        });

        MapMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapLock.setVisibility(View.GONE);
                if(Mapclick++ % 2 == 0 ) {
                    MapMove.setVisibility(View.GONE);
                    MapLock.setVisibility(View.VISIBLE);
                }
                isMapLock = false;
            }
        });

        if(isMapLock){
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(MapDroneLatLong.getLatitude(),MapDroneLatLong.getLongitude()));
            naverMap.moveCamera(cameraUpdate);
        }

        try {
            mDroneMarker.setPosition(new LatLng(MapDroneLatLong.getLatitude(), MapDroneLatLong.getLongitude()));
            mDroneMarker.setAngle(180 + (float)droneYawAttitude.getYaw());
            mDroneMarker.setMap(naverMap);
        } catch (Exception e) {
            Log.d("myLog", "오류메세지: " + e.getMessage());
            mDroneMarker.setPosition(defaultPosition);
            mDroneMarker.setAngle(180 + (float)droneYawAttitude.getYaw());
            mDroneMarker.setMap(naverMap);
        }

        coords.add(new LatLng(MapDroneLatLong.getLatitude(),MapDroneLatLong.getLongitude()));
        polyline.setCoords(coords);
        polyline.setMap(naverMap);
    }

    protected void alertUser(String message) {
        messageBundle.add(new SingerItem(message));
        recyclerView.setAdapter(adapter);

    }

 //===========================================================================================================

    public boolean CheckGoal(final Drone drone, LatLng recentLatLng) {
        GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
        LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(), guidedState.getCoordinate().getLongitude());
        return target.distanceTo(recentLatLng) <= 1;
    }

    public void Guid(){
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        naverMap.setOnMapLongClickListener(new NaverMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                mMarkerGuide.setPosition(latLng);
                mMarkerGuide.setMap(naverMap);
                checklatLng = latLng;

                LatLong mGuidedPoint = new LatLong(latLng.latitude,latLng.longitude);//가이드모드 목적지 저장

                if(vehicleState.isFlying()){
                    if(!(vehicleState.getVehicleMode().equals(VehicleMode.COPTER_GUIDED))){
                        AlertDialog.Builder alt_bld = new AlertDialog.Builder(MainActivity.this);
                        alt_bld.setMessage("현재 고도를 유지하며 \n" + "목표지점까지 기체를 이동하겠습니까?").setCancelable(false)
                                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener() {
                                            @Override
                                            public void onSuccess() {
                                                ControlApi.getApi(drone).goTo(mGuidedPoint, true, null);}
                                            @Override
                                            public void onError(int i) { }
                                            @Override
                                            public void onTimeout() { }
                                        });
                                    }
                                }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alert = alt_bld.create();
                        alert.show();
                    }else{
                        VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener() {
                            @Override
                            public void onSuccess() {
                                ControlApi.getApi(drone).goTo(mGuidedPoint, true, null);}
                            @Override
                            public void onError(int i) { }
                            @Override
                            public void onTimeout() { }
                        });
                    }
                }
            }
        });

        if(CheckGoal(drone,checklatLng)){
            alertUser("목적지 근방에 도착했습니다.");
            VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_LOITER);
            mMarkerGuide.setMap(null);
        }
    }

    @Override
    public void onMapReady(@NonNull final NaverMap naverMap) {

        this.naverMap = naverMap;

        final Button button_Basic = (Button)findViewById(R.id.NormalBtn);
        final Button button_Terrain = (Button)findViewById(R.id.TerrainBtn);
        final Button button_Satellite = (Button)findViewById(R.id.SatelliteBtn);
        final Button button_OffCadastral = (Button)findViewById(R.id.OffCadastralBtn);
        final Button button_OnCadastral = (Button)findViewById(R.id.OnCadastralBtn);

        button_OffCadastral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);

                if(count_offCadastral++ % 2 == 0)button_OnCadastral.setVisibility(View.VISIBLE);
                else button_OnCadastral.setVisibility(View.GONE);
            }
        });
        button_OnCadastral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);

                if(count_onCadastral++ % 2 == 0)button_OffCadastral.setVisibility(View.VISIBLE);
                else button_OffCadastral.setVisibility(View.GONE);
            }
        });

        button_Satellite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                naverMap.setMapType(NaverMap.MapType.Satellite);

                if(count_Hybrid++ % 2 == 0){
                    button_Basic.setVisibility(view.VISIBLE);
                    button_Terrain.setVisibility(view.VISIBLE);
                }else{
                    button_Basic.setVisibility(view.GONE);
                    button_Terrain.setVisibility(view.GONE);
                }
            }
        });
        button_Terrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                naverMap.setMapType(NaverMap.MapType.Terrain);

                if(count_Navi++ % 2 == 0){
                    button_Basic.setVisibility(view.VISIBLE);
                    button_Satellite.setVisibility(view.VISIBLE);
                }else{
                    button_Basic.setVisibility(view.GONE);
                    button_Satellite.setVisibility(view.GONE);
                }
            }
        });
        button_Basic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                naverMap.setMapType(NaverMap.MapType.Basic);
                if(count_Basic++ % 2 == 0){
                    button_Satellite.setVisibility(view.GONE);
                    button_Terrain.setVisibility(view.GONE);
                }else{
                    button_Satellite.setVisibility(view.VISIBLE);
                    button_Terrain.setVisibility(view.VISIBLE);
                }
            }
        });

        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setZoomControlEnabled(false);

        InfoWindow infoWindow = new InfoWindow();
        infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(getApplicationContext()) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return (CharSequence)infoWindow.getMarker().getTag();
            }
        }); // 마커에 정보띄우기
        Overlay.OnClickListener listener = overlay -> {
            Marker marker = (Marker)overlay;

            if (marker.getInfoWindow() == null) {
                // 현재 마커에 정보 창이 열려있지 않을 경우 엶
                infoWindow.open(marker);
            } else {
                // 이미 현재 마커에 정보 창이 열려있을 경우 닫음
                infoWindow.close();
            }
            return true;
        }; //마커 on/off

        naverMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            List<Marker> coords = new ArrayList<>();
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                Marker mark = new Marker();
                mark.setPosition(latLng);
                mark.setMap(naverMap);

                Log.d("coords size : ",Integer.toString(coords.size()));

                mark.setTag("위도: " + latLng.latitude + "경도: " + latLng.longitude);

                mark.setOnClickListener(listener);
//                MyAsyncTask myAsyncTask = new MyAsyncTask();
//                myAsyncTask.execute(latLng);
            }
        });
    }
}

//
//    public class MyAsyncTask extends AsyncTask<LatLng,String, String> {
//
//        @Override
//        protected String doInBackground(LatLng... latLngs) {
//
//            String strCoord = String.valueOf(latLngs[0].longitude) + "," + String.valueOf(latLngs[0].latitude);
//            Log.d("myLog", strCoord);
//
//            StringBuilder sb = new StringBuilder();
//            StringBuilder urlBuilder =
//                    new StringBuilder("https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?request=coordsToaddr&coords=" +strCoord+ "&sourcecrs=epsg:4326&output=json&orders=addr");
//            try{
//                URL url = new URL(urlBuilder.toString());
//                HttpsURLConnection http = (HttpsURLConnection)url.openConnection();
//                http.setRequestProperty("Content-type", "application/json");
//                http.getRequestProperty("X-NCP-APIGW-API-KEY-ID:{b2swhahkzz}");
//                http.getRequestProperty("X-NCP-APIGW-API-KEY:{p4QGLMdzuXMdbK8BBCgIZHVo7MKKtc6mK5k6ScaI}");
//                http.setRequestMethod("GET");
//                http.connect();
//
//                InputStreamReader in = new InputStreamReader(http.getInputStream(),"utf-8");
//                BufferedReader rd;
////                Log.d("getResponseCode: ", http.getResponseCode()+"");
//                if(http.getResponseCode() >=200 && http.getResponseCode() <= 300){
//                    rd = new BufferedReader(in);
//                } else {
//                    rd = new BufferedReader(in);
//                }
//
//                String line;
//                while ((line = rd.readLine()) != null){
//                    sb.append(line).append("\n");
//                }
//
//                JsonParser parser = new JsonParser();
//                JsonObject jsonObject;
//                JsonObject jsonObject2;
//                String x = "";
//                String y = "";
//
//                jsonObject = (JsonObject) parser.parse(sb.toString());
//                JsonArray jsonArray = (JsonArray) jsonObject.get("results");
//                Log.d("myLog3", jsonArray.toString());
//
//                for(int i=0;i<jsonArray.size();i++){
//                    jsonObject2 = (JsonObject)jsonArray.get(i);
//
//                    Log.d("myLog2", jsonObject2.toString());
//                    //jsonObject2 = jsonArray.json();
//                    if(null != jsonObject2.get("x")){
//                        x = (String) jsonObject2.get("x").toString();
//                    }
//                    if(null != jsonObject2.get("y")){
//                        x = (String) jsonObject2.get("y").toString();
//                    }
//                }
//                rd.close();
//                in.close();
//                http.disconnect();
//
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            } catch (ProtocolException e) {
//                e.printStackTrace();
//            } catch (MalformedURLException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//    }
//============================================================================================================