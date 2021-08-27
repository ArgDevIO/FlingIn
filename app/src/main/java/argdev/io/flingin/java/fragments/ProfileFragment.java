package argdev.io.flingin.java.fragments;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.util.Objects;
import java.util.UUID;

import argdev.io.flingin.R;
import argdev.io.flingin.java.FlingIn;
import argdev.io.flingin.java.activities.MainActivity;
import argdev.io.flingin.java.activities.UsersActivity;
import argdev.io.flingin.java.utils.FirebaseDBCallback;
import argdev.io.flingin.java.utils.ImagePicker;
import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    private static String TAG = "ProfileFragment-Logs";

    private ProgressDialog mProgressDialog;

    private CircleImageView circleImageView;
    private EditText profileName;
    private EditText profileStatus;
    private TextView scoreTxt;
    private ImageButton btnLogOut;
    private ImageButton btnEdit;
    private ImageButton btnDone;
    private Button btnChangeImage;

    private Bitmap profileImage;

    private ImageView pictureAddIcon;

    private Button mAllUsers;


    private static String nameStatic;
    private static String pictureStatic;
    private static String profileStatusStatic;
    private static String score;


    private static DatabaseReference mUserInfoDatabase;
    private static FirebaseUser mCurrentUser;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        scoreTxt = v.findViewById(R.id.profile_score_txt);

        circleImageView = v.findViewById(R.id.profile_photo);
        pictureAddIcon = v.findViewById(R.id.profile_pictureAddIcon);
        profileName = v.findViewById(R.id.profile_name);
        profileStatus = v.findViewById(R.id.profile_status);
        btnLogOut = v.findViewById(R.id.profile_btn_logout);
        btnEdit = v.findViewById(R.id.profile_btn_edit);
        btnDone = v.findViewById(R.id.profile_btn_done);
        btnChangeImage = v.findViewById(R.id.profile_btn_changeImage);

        //Remove default saveOnInstance from EditTexts
        profileName.setSaveEnabled(false);
        profileStatus.setSaveEnabled(false);

        hideEditMode();
        fetchScore();

        // Update profile View
        Log.d(TAG, "onCreateView: " + nameStatic);
        profileName.setText(nameStatic);

        if (profileStatusStatic != null) {
            if (!profileStatusStatic.isEmpty()) {
                profileStatus.setText(profileStatusStatic);
            }
        }

        if (pictureStatic != null) {
            loadImage();
        }

        setCustomFonts();
        setOnClickListeners();

        Log.d("FRAGMENT_CRASH", "onCreateView: " + isAdded());

        return v;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView: " + nameStatic);
        if (FlingIn.inEditingMode) {
            profileName.setText(nameStatic);
        }
        super.onDestroyView();
    }

    private void fetchScore() {
        DatabaseReference scoreDB = FirebaseDatabase.getInstance().getReference().child("users").child(mCurrentUser.getUid())
                .child("score");
        scoreDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    String tmpScore = dataSnapshot.getValue().toString();
                    scoreTxt.setText(tmpScore);
                } else {
                    scoreTxt.setText(Integer.toString(0));
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public static void fetchDataFromDB(final FirebaseDBCallback callback) {
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        final String current_uid = mCurrentUser.getUid();

        mUserInfoDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(current_uid).child("profile_info");
        mUserInfoDatabase.keepSynced(true);
        mUserInfoDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChildren()) {
                    Log.d(TAG, "Data is changingg");

                    Object oImage = dataSnapshot.child("image_url").getValue();
                    Object oUserName = dataSnapshot.child("username").getValue();
                    Object oProfileStatus = dataSnapshot.child("status").getValue();

                    if (oImage != null) {
                        pictureStatic = oImage.toString();
                    }

                    if (oUserName != null) {
                        nameStatic = oUserName.toString();
                    }

                    if (oProfileStatus != null) {
                        profileStatusStatic = oProfileStatus.toString();
                    }

                    Log.d(TAG, "onDataChange: singleToneFetch:: " + FlingIn.getSingleToneFetchProfile());
                    if (!FlingIn.getSingleToneFetchProfile()) {
                        FlingIn.setSingleToneFetchProfile(true);
                        callback.onCallback(true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void logout() {
        FlingIn.setSingleToneFetchProfile(false);
        ((MainActivity) getActivity()).logoutMain();
    }

    private void hideEditMode() {
        btnChangeImage.setVisibility(View.GONE);
        btnDone.setVisibility(View.GONE);
        pictureAddIcon.setVisibility(View.GONE);

        profileName.setFocusable(false);
        profileName.setFocusableInTouchMode(false);
        profileName.setClickable(false);

        profileStatus.setFocusable(false);
        profileStatus.setFocusableInTouchMode(false);
        profileStatus.setClickable(false);
    }

    private void disableEditMode() {
        Log.d(TAG, "disableEditMode: " + nameStatic);
        if (profileImage != null) {
            initializeProgressDialog("Updating profile...",
                    "Please wait while your profile is being updated");
            uploadImage(profileImage);
        }

        if (!profileName.getText().toString().equals(nameStatic)) {
            updateUsername(profileName.getText().toString());
        }

        if (!profileStatus.getText().toString().equals(profileStatusStatic)) {
            String newStatus = profileStatus.getText().toString();
            String formattedStatus;
            if (newStatus.charAt(0) == '"' && newStatus.charAt(newStatus.length() - 1) != '"')
                formattedStatus = newStatus + '"';
            else if (newStatus.charAt(0) != '"' && newStatus.charAt(newStatus.length() - 1) == '"')
                formattedStatus = '"' + newStatus;
            else if (newStatus.charAt(0) != '"' && newStatus.charAt(newStatus.length() - 1) != '"')
                formattedStatus = '"' + newStatus + '"';
            else
                formattedStatus = newStatus;
            updateStatus(formattedStatus);
        }

        profileName.setFocusable(false);
        profileName.setFocusableInTouchMode(false);
        profileName.setClickable(false);
        profileName.setBackgroundResource(R.drawable.rounded_profile_fields);
        profileName.setPaintFlags(0);

        profileStatus.setFocusable(false);
        profileStatus.setFocusableInTouchMode(false);
        profileStatus.setClickable(false);
        profileStatus.setBackgroundColor(Color.TRANSPARENT);
        profileStatus.setPaintFlags(0);
        profileStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.gunmetal5));

        pictureAddIcon.setVisibility(View.GONE);

        btnChangeImage.setVisibility(View.GONE);
        btnDone.setVisibility(View.GONE);
        btnEdit.setVisibility(View.VISIBLE);
        hideKeyboard();
    }

    private void enableEditMode() {
        profileName.setFocusable(true);
        profileName.setFocusableInTouchMode(true);
        profileName.setClickable(true);
        profileName.setBackgroundResource(R.drawable.rounded_btn);
        profileName.setPaintFlags(profileName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        profileStatus.setFocusable(true);
        profileStatus.setFocusableInTouchMode(true);
        profileStatus.setClickable(true);
        profileStatus.setBackgroundResource(R.drawable.rounded_btn);
        profileStatus.setPadding(10, 0, 10, 0);
        profileStatus.setPaintFlags(profileStatus.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        profileStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.lightCyan3));

        pictureAddIcon.setVisibility(View.VISIBLE);

        btnChangeImage.setVisibility(View.VISIBLE);
        btnDone.setVisibility(View.VISIBLE);
        btnEdit.setVisibility(View.GONE);
    }

    private void setOnClickListeners() {
        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FlingIn.setInEditingMode(true);
                enableEditMode();
            }
        });
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FlingIn.setInEditingMode(false);
                disableEditMode();
            }
        });
        btnChangeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPhoto();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            Uri uriImage;
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == Activity.RESULT_OK) {
                uriImage = result.getUri();
                Bitmap bmp = ImagePicker.getImageResized(getContext(), uriImage);
                profileImage = bmp;
                circleImageView.setImageBitmap(bmp);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(getContext(), "Couldn't select image", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateStatus(String newStatus) {
        profileStatus.setText(newStatus);
        if (mUserInfoDatabase == null) initializeUserDatabase();

        mUserInfoDatabase.child("status").setValue(newStatus).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (mProgressDialog != null) mProgressDialog.dismiss();
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUsername(String newUsername) {
        profileName.setText(newUsername);
        if (mUserInfoDatabase == null) initializeUserDatabase();

        mUserInfoDatabase.child("username").setValue(newUsername).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (mProgressDialog != null) mProgressDialog.dismiss();
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadImage() {
        Picasso.get().load(pictureStatic).networkPolicy(NetworkPolicy.OFFLINE)
                .placeholder(R.drawable.default_profile)
                .into(circleImageView, new Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError(Exception e) {
                        Picasso.get().load(pictureStatic).placeholder(R.drawable.default_profile)
                                .into(circleImageView);
                    }
                });
    }

    private void uploadImage(Bitmap bmp) {
        deleteOldImage();
        String uid;
        if (mUserInfoDatabase == null) uid = initializeUserDatabase();
        else uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        String filename = UUID.randomUUID().toString();
        final StorageReference imagesStorageRef = FirebaseStorage.getInstance().getReference("/users/" + uid + "/profile_image/" + filename);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 40, stream);
        byte[] imageData = stream.toByteArray();

        imagesStorageRef.putBytes(imageData)
                .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            imagesStorageRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (task.isSuccessful()) {
                                        pictureStatic = task.getResult().toString();
                                        mUserInfoDatabase.child("image_url").setValue(task.getResult().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Picasso.get().load(pictureStatic).networkPolicy(NetworkPolicy.OFFLINE)
                                                            .placeholder(R.drawable.default_profile)
                                                            .into(circleImageView, new Callback() {
                                                                @Override
                                                                public void onSuccess() {
                                                                    mProgressDialog.dismiss();
                                                                }

                                                                @Override
                                                                public void onError(Exception e) {
                                                                    Picasso.get().load(pictureStatic).placeholder(R.drawable.default_profile)
                                                                            .into(circleImageView, new Callback() {
                                                                                @Override
                                                                                public void onSuccess() {
                                                                                    mProgressDialog.dismiss();
                                                                                }

                                                                                @Override
                                                                                public void onError(Exception e) {

                                                                                }
                                                                            });
                                                                }
                                                            });
                                                } else {
                                                    mProgressDialog.dismiss();
                                                    Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                                    } else {
                                        mProgressDialog.dismiss();
                                        Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        } else {
                            mProgressDialog.dismiss();
                            Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void deleteOldImage() {
        if (pictureStatic.contains("firebasestorage.googleapis.com") && !pictureStatic.contains("default_image.jpg"))
            new DeleteImageAsync().doInBackground(pictureStatic, TAG);
    }

    private String initializeUserDatabase() {
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        final String current_uid = mCurrentUser.getUid();
        mUserInfoDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(current_uid).child("profile_info");
        mUserInfoDatabase.keepSynced(true);
        return current_uid;
    }

    private void initializeProgressDialog(String title, String msg) {
        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(msg);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    private void selectPhoto() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .setCropShape(CropImageView.CropShape.OVAL)
                .start(getContext(), ProfileFragment.this);
    }

    private void setCustomFonts() {
        Typeface typeface = Typeface.createFromAsset(Objects.requireNonNull(getActivity()).getAssets(), "fonts/Tumbly.otf");

        profileName.setTypeface(typeface);
    }

}

class DeleteImageAsync extends AsyncTask<String, Void, Integer> {

    @Override
    protected Integer doInBackground(String... strings) {

        String pictureStatic = strings[0];
        final String TAG = strings[1];

        StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(pictureStatic);
        imageRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "onDeleteComplete: Successfully deleted image!");
                } else {
                    Log.e(TAG, "onDeleteComplete: Failed, reason -> " + task.getException().getMessage());
                }
            }
        });
        return 0;
    }
}