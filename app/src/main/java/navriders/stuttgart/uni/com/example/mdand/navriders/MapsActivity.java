package navriders.stuttgart.uni.com.example.mdand.navriders;

import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;

/**
 * Created by mdand on 6/12/2017.
 */

public class MapsActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, RoutingListener {

    private TextView mTextMessage;
    protected GoogleMap map;
    protected LatLng start;
    protected LatLng end;
    @Optional @InjectView(R.id.start)
    AutoCompleteTextView starting;
    @Optional @InjectView(R.id.destination)
    AutoCompleteTextView destination;
    @Optional @InjectView(R.id.send)
    ImageView send;
    private static final String LOG_TAG = "MyActivity";
    private RelativeLayout directionView;
    protected GoogleApiClient mGoogleApiClient;
    private PlaceArrayAdapter mAdapter;
    private ProgressDialog progressDialog;
    private List<Polyline> polylines ;
    Location currPosition;
    private List<Location> routePoints = new ArrayList<>();
    private List<String> turningPoints = new ArrayList<>();
    private final Marker[] marker = new Marker[1];
    private static final int[] COLORS = new int[]{R.color.primary_dark,R.color.primary,R.color.primary_light,R.color.accent,R.color.primary_dark_material_light};


    private static final LatLngBounds BOUNDS_JAMAICA= new LatLngBounds(new LatLng(-57.965341647205726, 144.9987719580531),
            new LatLng(72.77492067739843, -9.998857788741589));

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    //mTextMessage.setText(R.string.title_home);
                    startActivity(new Intent(MapsActivity.this, MainActivity.class));
                    return true;
                case R.id.navigation_dashboard:
                    //mTextMessage.setText(R.string.title_dashboard);
                    startActivity(new Intent(MapsActivity.this, BLEConnectionManager.class));
                    return true;
                case R.id.navigation_notifications:
                    startActivity(new Intent(MapsActivity.this, MapsActivity.class));
                    //mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }

    };


    @Override
    public void onStop() {
        super.onStop();
        finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        directionView = (RelativeLayout) findViewById(R.id.directionView);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        ButterKnife.inject(this);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        polylines = new ArrayList<>();
        mGoogleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, 0, this)
                .addApi(Places.GEO_DATA_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        MapsInitializer.initialize(this);
        mGoogleApiClient.connect();

        //SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        //if (mapFragment == null) {
        //    mapFragment = SupportMapFragment.newInstance();
        //    getSupportFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
       // }
        //mapFragment.getMapAsync(this);

        map = getMapFragment().getMap();

        mAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1, BOUNDS_JAMAICA, null);
        mAdapter.setGoogleApiClient(mGoogleApiClient);


        /*
        * Updates the bounds being used by the auto complete adapter based on the position of the
        * map.
        * */
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
                //mAdapter.setBounds(bounds);
            }
        });


        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(18.013610, -77.498803));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);

        map.moveCamera(center);
        map.animateCamera(zoom);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 5000, 0,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {

                            CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(),location.getLongitude()));
                            CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);

                            LatLng latitudeLongitude = new LatLng(getCurrentLatitude(location), getCurrentLongitude(location));

                            Location newlocation = new Location("current");
                            newlocation.setLatitude(getCurrentLatitude(location));
                            newlocation.setLongitude(getCurrentLongitude(location));
                            currPosition = newlocation;
                            routeTurnAlert(currPosition);

                            if(marker[0] != null) {
                                marker[0].remove();
                            }
                            marker[0] = map.addMarker(new MarkerOptions().position(latitudeLongitude).title("currentLoc"));

                            map.moveCamera(center);
                            map.animateCamera(zoom);
                            map.setMyLocationEnabled(true);
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {

                        }

                        @Override
                        public void onProviderEnabled(String provider) {

                        }

                        @Override
                        public void onProviderDisabled(String provider) {

                        }
                    });
        }catch (SecurityException e) {
            Toast.makeText(getApplicationContext(), "GPS Permission Error", Toast.LENGTH_LONG).show();
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    3000, 0, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(),location.getLongitude()));
                            CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);

                            LatLng latitudeLongitude = new LatLng(getCurrentLatitude(location), getCurrentLongitude(location));
                            System.out.println(latitudeLongitude.latitude);

                            Location newlocation = new Location("current");
                            newlocation.setLatitude(getCurrentLatitude(location));
                            newlocation.setLongitude(getCurrentLongitude(location));
                            currPosition = newlocation;
                            routeTurnAlert(currPosition);

                            if(marker[0] != null) {
                                marker[0].remove();
                            }
                            marker[0] = map.addMarker(new MarkerOptions().position(latitudeLongitude).title("currentLoc"));

                            map.moveCamera(center);
                            map.animateCamera(zoom);
                            map.setMyLocationEnabled(true);

                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {

                        }

                        @Override
                        public void onProviderEnabled(String provider) {

                        }

                        @Override
                        public void onProviderDisabled(String provider) {

                        }
                    });
        }catch (SecurityException e) {
            Toast.makeText(getApplicationContext(), "GPS Permission Error", Toast.LENGTH_LONG).show();
        }






        /*
        * Adds auto complete adapter to both auto complete
        * text views.
        * */
        starting.setAdapter(mAdapter);
        destination.setAdapter(mAdapter);


        /*
        * Sets the start and destination points based on the values selected
        * from the autocomplete text views.
        * */

        starting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final PlaceArrayAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);
                Log.i(LOG_TAG, "Autocomplete item selected: " + item.description);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
              details about the place.
              */
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            // Request did not complete successfully
                            Log.e(LOG_TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                            places.release();
                            return;
                        }
                        // Get the Place object from the buffer.
                        final Place place = places.get(0);

                        start=place.getLatLng();
                    }
                });

            }
        });
        destination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final PlaceArrayAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);
                Log.i(LOG_TAG, "Autocomplete item selected: " + item.description);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
              details about the place.
              */
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            // Request did not complete successfully
                            Log.e(LOG_TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                            places.release();
                            return;
                        }
                        // Get the Place object from the buffer.
                        final Place place = places.get(0);

                        end=place.getLatLng();
                    }
                });

            }
        });

        /*
        These text watchers set the start and end points to null because once there's
        * a change after a value has been selected from the dropdown
        * then the value has to reselected from dropdown to get
        * the correct location.
        * */
        starting.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int startNum, int before, int count) {
                if (start != null) {
                    start = null;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        destination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {


                if(end!=null)
                {
                    end=null;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @OnClick(R.id.send)
    public void sendRequest()
    {
        if(Util.Operations.isOnline(this))
        {
            route();
        }
        else
        {
            Toast.makeText(this,"No internet connectivity",Toast.LENGTH_SHORT).show();
        }
    }

    public void route()
    {
        if(start==null || end==null)
        {
            if(start==null)
            {
                if(starting.getText().length()>0)
                {
                    starting.setError("Choose location from dropdown.");
                }
                else
                {
                    Toast.makeText(this,"Please choose a starting point.",Toast.LENGTH_SHORT).show();
                }
            }
            if(end==null)
            {
                if(destination.getText().length()>0)
                {
                    destination.setError("Choose location from dropdown.");
                }
                else
                {
                    Toast.makeText(this,"Please choose a destination.",Toast.LENGTH_SHORT).show();
                }
            }
        }
        else
        {
            progressDialog = ProgressDialog.show(this, "Please wait.",
                    "Fetching route information.", true);
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.BIKING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(start, end)
                    .build();
            routing.execute();
        }
    }



    public void onRoutingFailure(RouteException e) {
        // The Routing request failed
        progressDialog.dismiss();
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }


    public void onRoutingStart() {
        // The Routing Request starts
    }


    public void onRoutingSuccess(List<Route> route, int shortestRouteIndex)
    {
        progressDialog.dismiss();
        CameraUpdate center = CameraUpdateFactory.newLatLng(start);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);

        map.moveCamera(center);


        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            if (i == shortestRouteIndex) {
                //In case of more than 5 alternative routes
                int colorIndex = i % COLORS.length;
                Log.d("Color", String.valueOf(colorIndex));

                PolylineOptions polyOptions = new PolylineOptions();
                polyOptions.color(getResources().getColor(R.color.colorAccent));
                polyOptions.width(10 + i * 3);
                polyOptions.addAll(route.get(i).getPoints());
                for (int j = 0 ; j<route.get(i).getPoints().size();j++) {
//                    Log.d("RouteInfo", String.valueOf(route.get(i).getPoints().get(j)) + "\n");
                    LatLng LatLong = route.get(i).getPoints().get(j);
//                    LatLog = LatLog.replaceAll("\\p{P}","");
//                    String[] PointsArray = LatLog.split(",");
                    Location newLocation = new Location("new Location");
                    newLocation.setLatitude(LatLong.latitude);
                    newLocation.setLongitude(LatLong.longitude);
                    Log.d("RouteInfo", String.valueOf(currPosition.distanceTo(newLocation)));

                }

                int sizeOfGetPoints = route.get(i).getPoints().size();
//                for (int j=0; j<sizeOfGetPoints; j++) {
////                routePoints.addAll(route.get(i).getPoints().indexOf(j));
//                    Location newlocation = new Location("current");
//                    newlocation.setLatitude(route.get(i).getLatLgnBounds();
//                    newlocation.setLongitude(getCurrentLongitude(location));
//                    Log.d("RouteInfo", String.valueOf(currPosition.distanceTo((Location) route.get(i).getPoints().get(j))));
//                }
                Polyline polyline = map.addPolyline(polyOptions);
                polylines.add(polyline);

                if(routePoints == null) {
                    // do nothing
                } else {
                    routePoints.clear();
                }


                for (int j = 0; j < route.get(i).getSegments().size(); j++) {
                    Log.d("segment", String.valueOf(route.get(i).getSegments().get(j).getInstruction()));
                    Log.d("segment", String.valueOf(route.get(i).getSegments().get(j).getManeuver()));

                    Location newLocation = new Location("new Location");
                    newLocation.setLatitude(route.get(i).getSegments().get(j).startPoint().latitude);
                    newLocation.setLongitude(route.get(i).getSegments().get(j).startPoint().longitude);
                    Log.d("RouteInfo", String.valueOf(currPosition.distanceTo(newLocation)));

                    routePoints.add(newLocation);
                    if (currPosition.distanceTo(newLocation)> 10.0 && currPosition.distanceTo(newLocation)< 1000.0 ){
                        if( route.get(i).getSegments().get(j).getManeuver()!= null) {
                            if (route.get(i).getSegments().get(j).getManeuver().equalsIgnoreCase("turn-right")) {
                                //Toast.makeText(getApplicationContext(), "Turn Right", Toast.LENGTH_LONG).show();
                                Log.d("segment", "right");
                                turningPoints.add("Right");

                            }else if (route.get(i).getSegments().get(j).getManeuver().equalsIgnoreCase("turn-left")) {
                                //Toast.makeText(getApplicationContext(), "Turn left", Toast.LENGTH_LONG).show();
                                Log.d("segment", "left");
                                turningPoints.add("Left");
                            }else {
                                turningPoints.add("");
                                //Toast.makeText(getApplicationContext(), "Blank", Toast.LENGTH_LONG).show();
                            }


                            }

                    }

                }

                Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
                break;
            }

        }

        // Start marker
        MarkerOptions options = new MarkerOptions();
        options.position(start);
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue));
        map.addMarker(options);

        // End marker
        options = new MarkerOptions();
        options.position(end);
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green));
        map.addMarker(options);

        directionView.setVisibility(View.GONE);

    }


    public void onRoutingCancelled() {
        Log.i(LOG_TAG, "Routing was cancelled.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.v(LOG_TAG,connectionResult.toString());
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public MapFragment getMapFragment() {
        FragmentManager fm = null;

        Log.d("MapsAct", "sdk: " + Build.VERSION.SDK_INT);
        Log.d("MapsAct", "release: " + Build.VERSION.RELEASE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.d("MapsAct", "using getFragmentManager");
            fm = getFragmentManager();
        } else {
            Log.d("MapsAct", "using getChildFragmentManager");
            fm = getFragmentManager();
        }

        return (MapFragment) fm.findFragmentById(R.id.map);
    }

    private double getCurrentLatitude(Location location) {
        double latitude = location.getLatitude();
        return latitude;
    }

    private double getCurrentLongitude(Location location) {
        double longitude = location.getLongitude();
        return longitude;
    }

    public void routeTurnAlert(Location currLoc) {
        BLEConnectionManager ble = new BLEConnectionManager();

        if(routePoints == null) {

        } else {

        for(int i = 0; i < routePoints.size(); i++) {
            Location newLocation = new Location("new Location");
            newLocation.setLatitude(routePoints.get(i).getLatitude());
            newLocation.setLongitude(routePoints.get(i).getLongitude());

            float distance = currLoc.distanceTo(newLocation);
            //Toast.makeText(getApplicationContext(),"Distance:" + String.valueOf(distance), Toast.LENGTH_LONG).show();
            if((distance <= 20.00)) {

                String turn = turningPoints.get(i);
                if (turn.equalsIgnoreCase("Right")) {
                    Toast.makeText(getApplicationContext(), "Turn Right", Toast.LENGTH_LONG).show();
                    //ble.setVibrationDevice2("on");
                }else if (turn.equalsIgnoreCase("Left")) {
                    Toast.makeText(getApplicationContext(), "Turn left", Toast.LENGTH_LONG).show();
                    //ble.setVibrationDevice("on");
                }else {
                    Toast.makeText(getApplicationContext(), "Blank", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    }


}