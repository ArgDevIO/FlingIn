package argdev.io.flingin.java.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import argdev.io.flingin.R;
import argdev.io.flingin.java.models.ProfileInfo;
import argdev.io.flingin.java.models.User;
import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private RecyclerView mUsersList;

    private DatabaseReference mUsersDatabase;

    private FirebaseRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        mToolbar = findViewById(R.id.users_appBar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("All User");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        //mUsersDatabase.keepSynced(true);

        mUsersList = findViewById(R.id.users_list);
        mUsersList.setHasFixedSize(true);
        mUsersList.setLayoutManager(new LinearLayoutManager(this));


        fetch();
        adapter.startListening();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.stopListening();
    }

    private void fetch() {
        Query query = mUsersDatabase;

        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(query, new SnapshotParser<User>() {
                            @NonNull
                            @Override
                            public User parseSnapshot(@NonNull DataSnapshot snapshot) {
                                return new User(snapshot.child("profile_info").getValue(ProfileInfo.class));
                            }
                        })
                        .build();

        adapter = new FirebaseRecyclerAdapter<User, UsersViewHolder>(options) {

            @Override
            protected void onBindViewHolder(@NonNull UsersViewHolder holder, int position, @NonNull final User model) {
                Log.d("MODELUSER", "onBindViewHolder: " + model.getProfile_info());
                String username = model.getProfile_info().getUsername();
                String image = model.getProfile_info().getImage_url();
                String status = model.getProfile_info().getStatus();

                holder.setmUsername(username);
                holder.setmImage(image, getApplicationContext());
                holder.setmStatus(status);

                final String user_id = getRef(position).getKey();

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(UsersActivity.this, ProfileActivity.class);
                        intent.putExtra("USER_ID", user_id);
                        startActivity(intent);
                    }
                });
            }

            @NonNull
            @Override
            public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.all_users_single_layout, parent, false);

                return new UsersViewHolder(view);
            }
        };
        mUsersList.setAdapter(adapter);
    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder {

        View mView;
        TextView mUsername;
        TextView mStatus;
        CircleImageView mImage;

        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;
            mUsername = itemView.findViewById(R.id.all_user_single_name);
            mStatus = itemView.findViewById(R.id.all_user_single_status);
            mImage = itemView.findViewById(R.id.all_users_single_image);
        }

        public View getmView() {
            return mView;
        }

        public void setmStatus(String status) {
            mStatus.setText(status);
        }

        public void setmUsername(String username) {
            mUsername.setText(username);
        }

        public void setmImage(final String imageUri, Context context) {
            /*Glide.with(context)
                    .load(imageUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(mImage);*/
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
}
