package argdev.io.flingin.java.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.ArrayList;
import java.util.HashMap;

import argdev.io.flingin.R;
import argdev.io.flingin.java.fragments.ChallengesFragment;
import argdev.io.flingin.java.fragments.FriendsFragment;
import argdev.io.flingin.java.fragments.ProfileFragment;
import argdev.io.flingin.kotlin.LoginRegisterActivity;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity-Debug";

    private BottomNavigationView mMainNav;
    private FrameLayout mMainFrame;

    private GoogleSignInClient mGoogleSignInClient;

    private ProfileFragment profileFragment;
    private ChallengesFragment challengesFragment;
    private FriendsFragment friendsFragment;

    private FirebaseAuth mAuth;
    private DatabaseReference mUserOnlineFieldDatabase;

    private int currentFragment = 2;

    private static HashMap<String, String> savedEditText;

    private static Parcelable chatMsgsRecyclerViewState;

    private boolean finished = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        savedEditText = new HashMap<>();
        chatMsgsRecyclerViewState = null;

        Log.d("MAIN-Activityy", "onCreate: CREATEDD");

        String intentExtra = getIntent().getStringExtra("FromLogin");
        if (intentExtra != null) Log.d("MAIN-Activityy", "onCreate: " + intentExtra);

        finished = false;

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            mUserOnlineFieldDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(mAuth.getCurrentUser().getUid()).child("online");
        }
        initializeGoogle();

        mMainFrame = findViewById(R.id.main_frame);
        mMainNav = findViewById(R.id.main_nav);

        friendsFragment = new FriendsFragment();
        profileFragment = new ProfileFragment();
        challengesFragment = new ChallengesFragment();

        setFragment(profileFragment, 2);

        mMainNav.setSelectedItemId(R.id.nav_profile);
        mMainNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.nav_friends: {
                        setFragment(friendsFragment, 1);
                        return true;
                    }

                    case R.id.nav_profile: {
                        setFragment(profileFragment, 2);
                        return true;
                    }

                    case R.id.nav_challenges: {
                        setFragment(challengesFragment, 3);
                        return true;
                    }

                    default:
                        return false;

                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        clear_pref();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null)
            mUserOnlineFieldDatabase.setValue("true");
    }

    public void logoutMain() {

        finished = true;
        mUserOnlineFieldDatabase.setValue(System.currentTimeMillis());

        String providerId = FirebaseAuth.getInstance().getCurrentUser().getProviderData().get(1).getProviderId();

        if (providerId.equals("facebook.com")) {
            FirebaseAuth.getInstance().signOut();
            LoginManager.getInstance().logOut();
        } else if (providerId.equals("google.com")) {
            FirebaseAuth.getInstance().signOut();
            mGoogleSignInClient.signOut().addOnCompleteListener(this,
                    new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                        }
                    });
        } else {
            FirebaseAuth.getInstance().signOut();
        }

        Intent intent = new Intent(MainActivity.this, LoginRegisterActivity.class);
        startActivity(intent);
        finish();
    }

    private void initializeGoogle() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setFragment(Fragment fragment, int i) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Log.d("FRAGMENT_CRASH", "setFragment: ");
        if (currentFragment < i) {
            if (Math.abs(currentFragment - i) > 1) {
                fragmentTransaction.setCustomAnimations(R.anim.enter_left_to_right, R.anim.exit_left_to_right,
                        R.anim.enter_right_to_left, R.anim.exit_right_to_left);
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                currentFragment = i;
            } else {
                fragmentTransaction.setCustomAnimations(R.anim.enter_right_to_left, R.anim.exit_right_to_left,
                        R.anim.enter_left_to_right, R.anim.exit_left_to_right);
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                currentFragment = i;
            }
        } else {
            if (Math.abs(currentFragment - i) > 1) {
                fragmentTransaction.setCustomAnimations(R.anim.enter_right_to_left, R.anim.exit_right_to_left,
                        R.anim.enter_left_to_right, R.anim.exit_left_to_right);
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                currentFragment = i;
            } else {
                fragmentTransaction.setCustomAnimations(R.anim.enter_left_to_right, R.anim.exit_left_to_right,
                        R.anim.enter_right_to_left, R.anim.exit_right_to_left);
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                currentFragment = i;
            }
        }

        fragmentTransaction
                .addToBackStack(null)
                .replace(R.id.main_frame, fragment)
                .commit();
    }

    private void clear_pref() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear().apply();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        mMainNav.setSelectedItemId(R.id.nav_profile);
        setFragment(profileFragment, 2);
    }

    public static HashMap<String, String> getSavedEditText() {
        return savedEditText;
    }

}
