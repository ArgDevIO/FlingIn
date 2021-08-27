package argdev.io.flingin.java.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.model.Distance;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import argdev.io.flingin.R;
import argdev.io.flingin.java.models.ClusterMarker;
import argdev.io.flingin.java.models.Friend;
import argdev.io.flingin.java.services.GPS_Service;
import argdev.io.flingin.java.utils.GoogleMapsUtils;
import argdev.io.flingin.java.utils.MyClusterManagerRenderer;
import de.hdodenhof.circleimageview.CircleImageView;

public class MapsActivity extends AppCompatActivity {

    private static final String TAG = "MapsActivity_DEBUG";

    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 19.5f;
    private static final int PROXIMITY_RADIUS = 5000;

    //Maps
    private GoogleMap mMap;
    private Boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private BroadcastReceiver broadcastReceiver;
    private LatLng currentLocationLtdLnd;

    private boolean followMyLocation;

    private LocationListener locationListener;
    private LocationManager locationManager;

    private FloatingActionButton challengeSendBtn;

    private boolean fromChallengeReceived;

    private LinearLayout challengesMapSwitchWrapper;
    private Switch challengesMapSwitchBtn;

    private GoogleMapsUtils googleMapsUtils;

    //region vars used inside Dialog
    private boolean dialogOpen = false;
    private TextSwitcher textDaysSwitcher;
    private TextView textDays;

    //Recycler View Friend
    private RecyclerView mFriendsList;
    private FirebaseRecyclerAdapter myFriendsAdapter;

    //Firebase
    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mUsersDatabase;
    private DatabaseReference mRootRef;
    private FirebaseAuth mAuth;
    private String mCurrent_user_id;

    private ArrayList<String> selectedUsers;
    private HashMap<String, String> usersWithIDs;
    private HorizontalScrollView scrollView;
    private TextView selectedUsersTxt;
    private RelativeLayout selectedUsersNav;

    //ChallengeReceivedVars
    private String challengeID;
    private Double challengeLatitude;
    private Double challengeLongitude;
    private String challengeFromID;
    private String challengeUserName;
    private String challengeImageURL;

    private Button challengeCompleteBtn;

    private ClusterManager<ClusterMarker> mClusterManager;
    private MyClusterManagerRenderer mClusterManagerRenderer;
    private ArrayList<ClusterMarker> mClusterMarkers = new ArrayList<>();

            /*mapIntent.putExtra("challenges_received", true);
                                mapIntent.putExtra("latitude", model.getLatitude());
                                mapIntent.putExtra("longitude", model.getLongitude());
                                mapIntent.putExtra("from_name", userName);
                                mapIntent.putExtra("from_image", userImage);*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        googleMapsUtils = new GoogleMapsUtils(this);
        googleMapsUtils.getLocationPermission();

        challengeCompleteBtn = findViewById(R.id.challenge_complete_btn);

        challengesMapSwitchWrapper = findViewById(R.id.challenges_map_switch_wrapper);
        challengesMapSwitchBtn = findViewById(R.id.challenges_map_current_location_follow_switch);


        fromChallengeReceived = getIntent().getBooleanExtra("challenges_received", false);

        if (fromChallengeReceived) {
            challengeID = getIntent().getStringExtra("challenge_id");
            challengeLatitude = getIntent().getDoubleExtra("latitude", 0);
            challengeLongitude = getIntent().getDoubleExtra("longitude", 0);
            challengeFromID = getIntent().getStringExtra("from_id");
            challengeUserName = getIntent().getStringExtra("from_name");
            challengeImageURL = getIntent().getStringExtra("from_image");
        }


        //region Firebase
        mAuth = FirebaseAuth.getInstance();
        mCurrent_user_id = mAuth.getCurrentUser().getUid();
        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("friends").child(mCurrent_user_id);
        mFriendsDatabase.keepSynced(true);
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        mUsersDatabase.keepSynced(true);
        mRootRef = FirebaseDatabase.getInstance().getReference();
        //endregion

        challengeSendBtn = findViewById(R.id.challenge_create_send_btn);
        challengeSendBtn.setOnClickListener(view -> openSendToDialog());
        challengeSendBtn.setAlpha(0.0f);
        challengeSendBtn.hide();

        challengeCompleteBtn.setOnClickListener(view -> {
            Toast.makeText(this, "Challenge Completed! +5pts", Toast.LENGTH_LONG).show();
            String current_user_ref = "challenges/received/" + mCurrent_user_id;
            String sendTo_user_ref = "challenges/sent/" + challengeFromID;

            Map challengesMap = new HashMap();
            challengesMap.put(current_user_ref + "/" + challengeID, null);
            challengesMap.put(sendTo_user_ref + "/" + challengeID, null);

            mRootRef.updateChildren(challengesMap, (databaseError, databaseReference) -> {
                if (databaseError != null) {
                    Toast.makeText(MapsActivity.this, "::Couldn't send challenge::\n" + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    // Update Scores

                    mRootRef.child("users").child(mCurrent_user_id).child("score")
                            .runTransaction(new Transaction.Handler() {
                                @NonNull
                                @Override
                                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                                    Long currentValue = mutableData.getValue(Long.class);
                                    if (currentValue == null) {
                                        mutableData.setValue(5);
                                    } else {
                                        mutableData.setValue(currentValue + 5);
                                    }

                                    return Transaction.success(mutableData);
                                }

                                @Override
                                public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {

                                }
                            });
                    MapsActivity.this.finish();
                }
            });
        });

        getLocationPermission();
        //startGPS_Service();
        if (fromChallengeReceived)
            checkIfInChallengeRadius(currentLocationLtdLnd);
        listenForLocationChanges();
    }

    private void openSendToDialog() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.challenge_send_to_layout, null);

        mBuilder.setView(dialogView);

        AlertDialog dialog = mBuilder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.show();

        dialogOpen = true;

        dialog.setOnDismissListener(dialogInterface -> {
            dialogOpen = false;
            challengeSendBtn.show();
        });

        challengeSendBtn.hide();
        selectedUsers = new ArrayList<>();
        usersWithIDs = new HashMap<>();

        selectedUsersTxt = dialogView.findViewById(R.id.challenge_send_to_selected_users_txt);
        textDaysSwitcher = dialogView.findViewById(R.id.challenge_send_to_days_picker_daysTxt);
        scrollView = dialogView.findViewById(R.id.challenge_send_to_selected_users_txt_scroll);
        selectedUsersNav = dialogView.findViewById(R.id.challenge_send_to_bottom_nav);

        NumberPicker numberPicker = dialogView.findViewById(R.id.challenge_send_to_days_picker);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(999);
        numberPicker.setWrapSelectorWheel(false);

        // Friend RecyclerView
        mFriendsList = dialogView.findViewById(R.id.challenge_send_to_friends_list);
        mFriendsList.setHasFixedSize(true);
        mFriendsList.setLayoutManager(new LinearLayoutManager(this));
        mFriendsList.addItemDecoration(new DividerItemDecoration(mFriendsList.getContext(), DividerItemDecoration.VERTICAL));

        //region NumberPicker
        String[] textToShow = {"day", "days"};
        numberPicker.setOnValueChangedListener((numberPicker1, i, i1) -> {
            if (numberPicker1.getValue() == 2) {
                textDaysSwitcher.setText(textToShow[1]);
            } else if (numberPicker1.getValue() == 1)
                textDaysSwitcher.setText(textToShow[0]);
        });
        textDaysSwitcher.setFactory(() -> {
            textDays = new TextView(MapsActivity.this);
            textDays.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            textDays.setTextColor(ContextCompat.getColor(this, R.color.paleCerulean2));
            textDays.setTypeface(Typeface.MONOSPACE);
            final float scale = this.getResources().getDisplayMetrics().density;
            int pixels = (int) (50 * scale + 0.5f);
            textDays.setWidth(pixels);
            return textDays;
        });
        textDaysSwitcher.setText(textToShow[0]);
        //endregion

        fetch();
        myFriendsAdapter.startListening();

        ImageButton sendChallengeBtn = dialogView.findViewById(R.id.challenge_send_to_btn);
        sendChallengeBtn.setOnClickListener(view -> {
            for (String user : selectedUsers) {
                String userID = usersWithIDs.get(user);
                sendChallengeToUser(userID, (long)numberPicker.getValue(), currentLocationLtdLnd);
            }
            String toastMsg;
            if (selectedUsers.size() > 1)
                toastMsg = selectedUsers.size() + " challenges sent successfully!";
            else
                toastMsg = "1 challenge sent successfully!";

            Toast.makeText(MapsActivity.this, toastMsg, Toast.LENGTH_LONG).show();
            MapsActivity.this.finish();
        });
    }

    private void sendChallengeToUser(String sendToUserID, Long daysToDeadline, LatLng currentLocationLtdLnd) {
        String deadlineDate = LocalDate.now().plusDays(daysToDeadline).toString();

        String current_user_ref = "challenges/sent/" + mCurrent_user_id;
        String sendTo_user_ref = "challenges/received/" + sendToUserID;

        String challenge_id = mRootRef.child("challenges").child("sent").child(mCurrent_user_id).push().getKey();

        Map challengeSentMap = new HashMap();
        challengeSentMap.put("to", sendToUserID);
        challengeSentMap.put("type", "location");
        challengeSentMap.put("latitude", currentLocationLtdLnd.latitude);
        challengeSentMap.put("longitude", currentLocationLtdLnd.longitude);
        challengeSentMap.put("deadline", deadlineDate);

        Map challengeReceivedMap = new HashMap();
        challengeReceivedMap.put("from", mCurrent_user_id);
        challengeReceivedMap.put("type", "location");
        challengeReceivedMap.put("latitude", currentLocationLtdLnd.latitude);
        challengeReceivedMap.put("longitude", currentLocationLtdLnd.longitude);
        challengeReceivedMap.put("deadline", deadlineDate);

        Map challengesMap = new HashMap();
        challengesMap.put(current_user_ref + "/" + challenge_id, challengeSentMap);
        challengesMap.put(sendTo_user_ref + "/" + challenge_id, challengeReceivedMap);

        mRootRef.updateChildren(challengesMap, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                Toast.makeText(MapsActivity.this, "::Couldn't send challenge::\n" + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (dialogOpen)
            myFriendsAdapter.startListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    getDeviceLocation();
                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter("location_update"));*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stopGPS_Service();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (dialogOpen)
            myFriendsAdapter.stopListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    public void startGPS_Service() {
        Intent i = new Intent(getApplicationContext(), GPS_Service.class);
        startService(i);
    }

    public void stopGPS_Service() {
        Intent i = new Intent(getApplicationContext(), GPS_Service.class);
        stopService(i);
    }

    private void fetch() {
        Query query = mFriendsDatabase;

        FirebaseRecyclerOptions<Friend> options =
                new FirebaseRecyclerOptions.Builder<Friend>()
                        .setQuery(query, snapshot -> new Friend(snapshot.child("date").getValue().toString()))
                        .build();

        myFriendsAdapter = new FirebaseRecyclerAdapter<Friend, FriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendsViewHolder holder, final int position, @NonNull final Friend model) {

                final String user_id = getRef(position).getKey();

                mUsersDatabase.child(user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("profile_info").child("username").getValue().toString();
                        final String userImage = dataSnapshot.child("profile_info").child("image_url").getValue().toString();

                        holder.setName(userName);
                        holder.setImage(userImage);

                        holder.mView.setOnClickListener(view -> {
                            if (holder.isSelected()) {
                                holder.setSelected(false);
                                holder.selectedIcon.setVisibility(View.INVISIBLE);
                                holder.userNameView.setTextColor(ContextCompat.getColor(MapsActivity.this, R.color.gunmetal5));
                                selectedUsers.remove(userName);
                                usersWithIDs.remove(userName);
                                if (selectedUsers.size() > 0)
                                    selectedUsersTxt.setText(selectedUsers.toString().substring(1, selectedUsers.toString().length() - 1));
                                else {
                                    selectedUsersNav.animate()
                                            .alpha(0.0f)
                                            .setDuration(300);
                                    selectedUsersTxt.setText("");
                                }
                            } else {
                                holder.setSelected(true);
                                holder.selectedIcon.setVisibility(View.VISIBLE);
                                holder.userNameView.setTextColor(ContextCompat.getColor(MapsActivity.this, R.color.lightRedOchre4));
                                selectedUsers.add(userName);
                                usersWithIDs.put(userName, user_id);
                                if (selectedUsers.size() == 1)
                                    selectedUsersNav.animate().alpha(1.0f).setDuration(300);
                                selectedUsersTxt.setText(selectedUsers.toString().substring(1, selectedUsers.toString().length() - 1));
                            }
                            scrollView.post(() -> {
                                scrollView.fullScroll(View.FOCUS_RIGHT);
                            });
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            }

            @NonNull
            @Override
            public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.challenge_send_to_friends_list_item, parent, false);

                return new FriendsViewHolder(view);
            }
        };
        mFriendsList.setAdapter(myFriendsAdapter);
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder {

        View mView;
        boolean selected;
        RelativeLayout wrapper;
        TextView userNameView;
        CircleImageView mImage;
        ImageView selectedIcon;

        public FriendsViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
            wrapper = mView.findViewById(R.id.challenge_send_to_friends_item_wrapper);
            userNameView = mView.findViewById(R.id.challenge_send_to_friends_item_username);
            mImage = mView.findViewById(R.id.challenge_send_to_friends_item_image);
            selectedIcon = mView.findViewById(R.id.challenge_send_to_friends_item_ic_selected);
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public void setName(String name) {
            userNameView.setText(name);
        }

        public String getName() {
            TextView userNameView = mView.findViewById(R.id.user_single_name);
            return userNameView.getText().toString();
        }

        public void setImage(final String imageUri) {

            Picasso.get().load(imageUri).networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.drawable.default_profile)
                    .into(mImage, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(imageUri).placeholder(R.drawable.default_profile)
                                    .into(mImage);
                        }
                    });
        }

    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};

        if (isLocationEnabled(this)) {
            Log.d(TAG, "getLocationPermission: isEnabled? " + isLocationEnabled(this));
            if (ContextCompat
                    .checkSelfPermission(
                            this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            Toast.makeText(this, "Please, enable location services!", Toast.LENGTH_LONG).show();
            Intent locationSettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(locationSettings);
            finish();
        }
    }

    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getView().setBackgroundColor(Color.BLACK);
        mapFragment.getMapAsync((googleMap -> {
            Log.d(TAG, "onMapReady: Map is ready, initializing map!");
            mMap = googleMap;
            mMap.setMapStyle(new MapStyleOptions(getResources().getString(R.string.map_style_night)));
            mMap.setBuildingsEnabled(true);
            mMap.setIndoorEnabled(true);

            if (mLocationPermissionGranted) {
                getDeviceLocation();

                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(true);

                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    LottieAnimationView lottieAnimationView = findViewById(R.id.lottie_world_animation);
                    ImageView splashImg = findViewById(R.id.challenge_create_map_splash);
                    splashImg.animate().alpha(0.0f).setDuration(1500);
                    lottieAnimationView.animate().alpha(0.0f).setDuration(1500);
                    lottieAnimationView.pauseAnimation();
                    lottieAnimationView.setVisibility(View.GONE);

                    challengesMapSwitchWrapper.setVisibility(View.VISIBLE);
                    challengesMapSwitchWrapper.animate().alpha(1.0f).setDuration(1500);

                    if (!fromChallengeReceived) {
                        challengeSendBtn.show();
                        challengeSendBtn.animate().alpha(1.0f).setDuration(1600);
                    }

                    if (fromChallengeReceived)
                        addMapMarkers();
                }, 2800);
            }
        }));
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the device current location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionGranted) {
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Location currentLocation = (Location) task.getResult();
                        double ltd = currentLocation.getLatitude();
                        double lnd = currentLocation.getLongitude();

                        currentLocationLtdLnd = new LatLng(ltd, lnd);
                        if (!fromChallengeReceived)
                            moveCamera(currentLocationLtdLnd, DEFAULT_ZOOM);
                    }
                });
            } else {
                Log.d(TAG, "onComplete: current location is null");
            }
        } catch (SecurityException e) {
            Log.d(TAG, "getDeviceLocation: SecurityException " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", long: " + latLng.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        mLocationPermissionGranted = false;
                        Log.d(TAG, "onRequestPermissionsResult: permission failed!");
                        return;
                    }
                }
                mLocationPermissionGranted = true;
                Log.d(TAG, "onRequestPermissionsResult: permission granted!");
                //initialize our map
                initMap();
            }
        }
    }

    private boolean isLocationEnabled(Context context) {
        int locationMode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            locationMode = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.LOCATION_MODE,
                    0
            );
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }
        return false;
    }

    private void addMapMarkers(){

        if(mMap != null){

            if(mClusterManager == null){
                mClusterManager = new ClusterManager<>(this, mMap);
            }
            if(mClusterManagerRenderer == null){
                mClusterManagerRenderer = new MyClusterManagerRenderer(
                        this,
                        mMap,
                        mClusterManager
                );
                mClusterManager.setRenderer(mClusterManagerRenderer);
            }

            try{
                String snippet = "CHALLENGE FROM";
                String imageUrl = challengeImageURL;

                ClusterMarker newClusterMarker = new ClusterMarker(
                        new LatLng(challengeLatitude, challengeLongitude),
                        challengeUserName,
                        snippet,
                        imageUrl
                );
                mClusterManager.addItem(newClusterMarker);
                mClusterMarkers.add(newClusterMarker);

            }catch (NullPointerException e){
                Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage() );
            }
            mClusterManager.cluster();

            moveCamera(new LatLng(challengeLatitude, challengeLongitude), DEFAULT_ZOOM);
        }
    }

    private void listenForLocationChanges() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (challengesMapSwitchBtn.isChecked()) {
                    moveCamera(new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM);
                }
                if (fromChallengeReceived) {
                    checkIfInChallengeRadius(new LatLng(location.getLatitude(), location.getLongitude()));
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        };

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 8000, 2, locationListener);
    }

    private void checkIfInChallengeRadius(LatLng current) {
        googleMapsUtils.getDeviceLocation();

        new Handler().postDelayed(() -> {
            googleMapsUtils.calculateDirections(challengeLatitude, challengeLongitude, current);
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                Distance mDistance = googleMapsUtils.getmDistance();
                if (mDistance.inMeters < 25) {
                    challengeCompleteBtn.setVisibility(View.VISIBLE);
                    challengeCompleteBtn.animate().alpha(1.0f).setDuration(400);
                } else {
                    challengeCompleteBtn.animate().alpha(0.0f).setDuration(400);
                    challengeCompleteBtn.setVisibility(View.GONE);
                }
            }, 1000);
        }, 4000);

    }
}
