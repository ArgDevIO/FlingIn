package argdev.io.flingin.java.activities;


import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import argdev.io.flingin.R;
import argdev.io.flingin.java.adapters.MessageAdapter;
import argdev.io.flingin.java.models.Message;
import argdev.io.flingin.java.utils.GetTimeAgo;
import argdev.io.flingin.java.utils.ImagePicker;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    public static final int TOTAL_ITEMS_TO_LOAD = 10;
    private int mCurrentPage = 1;

    public static final int GALLERY_PICK = 1002;

    public static final String BUNDLE_RECYCLER_LAYOUT = "chatActivity.recycler.layout";

    private String mChatUser;
    private String userName;
    private Toolbar mChatToolBar;

    private DatabaseReference mRootRef;
    private DatabaseReference mChatRef;
    private DatabaseReference mMessagesRef;
    private DatabaseReference mTypingFeature;

    private StorageReference mImageStorage;

    private TextView mTitleView;
    private TextView mLastSeenView;
    private CircleImageView mProfileImage;

    private ImageButton mChatAddBtn;
    private ImageButton mChatSendBtn;
    private EditText mChatMsgView;

    private RecyclerView mMessagesList;

    private FirebaseAuth mAuth;
    private String mCurrentUserId;

    private final List<Message> messageList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter mAdapter;

    private ProgressDialog mProgressDialog;

    private boolean firstInit;

    private Map<Boolean, Boolean> cacheOnce;

    //Solution For Recycler
    private int itemPos = 0;
    private String mLastKey = "";
    private String mPrevKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        cacheOnce = new ConcurrentHashMap<>();
        firstInit = false;

        //region ------ Getting Data From Intent ------
        mChatUser = getIntent().getStringExtra("USER_ID");
        userName = getIntent().getStringExtra("USER_NAME");
        final String profileImage = getIntent().getStringExtra("USER_IMAGE");
        int areFriends = getIntent().getIntExtra("ARE_FRIENDS", 0);
        //endregion

        //region ------ Firebase ------
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mCurrentUserId = mAuth.getCurrentUser().getUid();
        mChatRef = FirebaseDatabase.getInstance().getReference().child("chat").child(mCurrentUserId);
        //mChatRef.keepSynced(true);
        mMessagesRef = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrentUserId).child(mChatUser);
        mTypingFeature = mChatRef.child(mChatUser).child("typing");

        mImageStorage = FirebaseStorage.getInstance().getReference().child("message_images");
        //endregion

        //region ------ Toolbar with Custom ActionBar ------
        mChatToolBar = findViewById(R.id.chat_app_bar);
        setSupportActionBar(mChatToolBar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        mChatToolBar.setContentInsetStartWithNavigation(0);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.chat_custom_bar, null);
        actionBar.setCustomView(action_bar_view);
        //endregion

        //region ------ Initializing custom bar views ------
        mTitleView = findViewById(R.id.custom_bar_title);
        mLastSeenView = findViewById(R.id.custom_bar_last_seen);
        mProfileImage = findViewById(R.id.custom_bar_image);

        mChatAddBtn = findViewById(R.id.chat_add_btn);
        mChatSendBtn = findViewById(R.id.chat_send_msg_btn);
        mChatMsgView = findViewById(R.id.chat_message_view);
        //endregion


        // Set focus to message input field and hide keyboard
        mChatMsgView.requestFocus();
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        mChatMsgView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (mChatMsgView.getText().length() == 0) {
                    mTypingFeature.setValue("no");
                } else if (mChatMsgView.getText().length() > 0) {
                    mTypingFeature.setValue("typing");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        loadSavedPreferences();

        // Initialize RecyclerView from View
        mMessagesList = findViewById(R.id.messages_list);

        // region ------ RecyclerView -------
        mLinearLayout = new LinearLayoutManager(this);
        //mLinearLayout.setSmoothScrollbarEnabled(true);
        mAdapter = new MessageAdapter(messageList, getApplicationContext(), mLinearLayout, mMessagesList);

        mMessagesList.setHasFixedSize(true);
        mMessagesList.setItemViewCacheSize(20);
        mMessagesList.setDrawingCacheEnabled(true);
        mMessagesList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        mMessagesList.setLayoutManager(mLinearLayout);
        ((DefaultItemAnimator) mMessagesList.getItemAnimator()).setSupportsChangeAnimations(false);

        mMessagesList.setAdapter(mAdapter);
        //endregion

        loadMessages();

        //region ------ Load Username, ProfileImage and LastSeen ------
        mTitleView.setText(userName);
        loadImage(profileImage, mProfileImage);
        //endregion

        DatabaseReference userOnlineRef = mRootRef.child("users").child(mChatUser);
        userOnlineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final String online = dataSnapshot.child("online").getValue().toString();

                DatabaseReference chatTypingRef = FirebaseDatabase.getInstance().getReference()
                        .child("chat").child(mChatUser).child(mCurrentUserId).child("typing");

                chatTypingRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.getValue() != null) {
                            String typing = dataSnapshot.getValue().toString();
                            if (typing.equals("typing")) {
                                mLastSeenView.setText("typing...");
                            } else if (online.equals("true")) {
                                mLastSeenView.setText(getResources().getString(R.string.online_string));
                            } else {
                                long lastTime = Long.parseLong(online);
                                String lastTimeSeen = "last seen " + GetTimeAgo.getTimeAgo(lastTime);
                                mLastSeenView.setText(lastTimeSeen);
                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        //region ------ Create Chat on Database ------
        mRootRef.child("chat").child(mCurrentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(mChatUser)) {

                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("unread", 0);
                    chatAddMap.put("typing", "no");
                    chatAddMap.put("timestamp", System.currentTimeMillis());

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("chat/" + mCurrentUserId + "/" + mChatUser, chatAddMap);
                    chatUserMap.put("chat/" + mChatUser + "/" + mCurrentUserId, chatAddMap);

                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Log.d("CHAT_LOG", "onComplete: ERROR:" + databaseError.getMessage());
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        //endregion

        mRootRef.child("chat").child(mCurrentUserId).child(mChatUser).child("seen").setValue(true);
        mRootRef.child("chat").child(mCurrentUserId).child(mChatUser).child("unread").setValue(0);


        initClickListeners();

        KeyboardVisibilityEvent.setEventListener(
                this,
                isOpen -> mMessagesList.postDelayed(() -> {
                    if (messageList.size() > 1)
                        mMessagesList.smoothScrollToPosition(messageList.size() - 1);
                }, 100));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!ChatActivity.this.isFinishing())
            checkForFriendsAndDisplayAlert(userName);
    }

    private void checkForFriendsAndDisplayAlert(String userName) {
        mRootRef.child("friends").child(mCurrentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean areFriends = dataSnapshot.hasChild(mChatUser);

                if (!ChatActivity.this.getWindow().getDecorView().getRootView().isShown())
                    return;

                if (!areFriends) {
                    new AlertDialog.Builder(ChatActivity.this)
                            .setTitle("Not Friend...")
                            .setMessage("Looks like you're no longer friends with " + userName + ", add this user again to friend list?")
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                                Intent profileIntent = new Intent(ChatActivity.this, ProfileActivity.class);

                                View profileImgShared = mProfileImage;
                                View profileNameShared = mTitleView;
                                String imgTransition = "profile_picture";
                                String nameTransition = "profile_name";

                                ActivityOptions transitionOptions = ActivityOptions.makeSceneTransitionAnimation(
                                        ChatActivity.this,
                                        Pair.create(profileImgShared, imgTransition),
                                        Pair.create(profileNameShared, nameTransition));

                                profileIntent.putExtra("USER_ID", mChatUser);
                                startActivity(profileIntent, transitionOptions.toBundle());
                                //finish();
                            })
                            .setNegativeButton(android.R.string.no, (dialogInterface, i) -> finish())
                            .show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    private void loadSavedPreferences() {
        String editText = MainActivity.getSavedEditText().get("temp_chat_" + mChatUser);
        if (editText != null) {
            if (!editText.isEmpty()) {
                mChatMsgView.setText(editText);
                mChatMsgView.setSelection(mChatMsgView.length());
                mChatRef.setValue("typing");
            }
        }
    }

    private void savePreferences(String key, String value) {
        MainActivity.getSavedEditText().put(key, value);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        resetMsgSeenAndUnread();
        savePreferences("temp_chat_" + mChatUser, mChatMsgView.getText().toString());
        hideKeyboard(this);
        mTypingFeature.setValue("no");
        super.onBackPressed();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("SAVING", "DESTROYING: ");
    }

    private void resetMsgSeenAndUnread() {
        mRootRef.child("chat").child(mCurrentUserId).child(mChatUser).child("seen").setValue(true);
        mRootRef.child("chat").child(mCurrentUserId).child(mChatUser).child("unread").setValue(0);
    }

    @Override
    public void finishAfterTransition() {
        super.finish();
    }

    public void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void loadMessages() {

        mMessagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final List<Message> tempList = new ArrayList<>();
                for (DataSnapshot messages : dataSnapshot.getChildren()) {
                    tempList.add(messages.getValue(Message.class));
                }

                messageList.clear();
                messageList.addAll(tempList);
                mAdapter.notifyDataSetChanged();


                if (messageList.size() > 0) {
                    Message lastMsg = messageList.get(tempList.size() - 1);

                    int delay;
                    if (!firstInit) {
                        delay = 800;
                    } else {
                        if (lastMsg.getType().equals("text"))
                            delay = 100;
                        else
                            delay = 300;
                    }
                    mMessagesList.postDelayed(() -> mMessagesList.smoothScrollToPosition(tempList.size() - 1), delay);
                    Log.d("DELAY", "onDataChange: " + delay);
                }
                firstInit = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage() {
        String message = mChatMsgView.getText().toString();

        if (!message.isEmpty()) {

            String current_user_ref = "messages/" + mCurrentUserId + "/" + mChatUser;
            String chat_user_ref = "messages/" + mChatUser + "/" + mCurrentUserId;

            DatabaseReference user_message_push = mRootRef.child("messages")
                    .child(mCurrentUserId).child(mChatUser).push();

            String push_id = user_message_push.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", System.currentTimeMillis());
            messageMap.put("from", mCurrentUserId);

            Map messageUserMap = new HashMap();
            messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
            messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

            mChatMsgView.getText().clear();

            mRootRef.child("chat").child(mCurrentUserId).child(mChatUser).child("seen").setValue(true);
            mRootRef.child("chat").child(mCurrentUserId).child(mChatUser).child("timestamp").setValue(System.currentTimeMillis());

            mRootRef.child("chat").child(mChatUser).child(mCurrentUserId).child("seen").setValue(false);
            mRootRef.child("chat").child(mChatUser).child(mCurrentUserId).child("unread")
                    .runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                            Long currentValue = mutableData.getValue(Long.class);
                            if (currentValue == null) {
                                mutableData.setValue(1);
                            } else {
                                mutableData.setValue(currentValue + 1);
                            }

                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {

                        }
                    });
            mRootRef.child("chat").child(mChatUser).child(mCurrentUserId).child("timestamp").setValue(System.currentTimeMillis());

            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        Log.d("CHAT_LOG", "onComplete: ERROR:" + databaseError.getMessage());
                    }
                }
            });

        }
    }

    private void initClickListeners() {
        mChatAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPhoto();
            }
        });

        mChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        /*mChatMsgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (messageList.size() > 1)
                            mMessagesList.smoothScrollToPosition(messageList.size());
                    }
                }, 100);
            }
        });*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            initializeProgressDialog("Sending image...",
                    "Please wait while we upload and send your image");
            Uri imageUri;
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == Activity.RESULT_OK) {
                imageUri = result.getUri();

                Bitmap bmp = ImagePicker.getImageResized(this, imageUri);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] imageData = stream.toByteArray();

                uploadImageAndPushMsg(imageData);
            } else {
                mProgressDialog.dismiss();
            }
        }
    }

    private void uploadImageAndPushMsg(byte[] imageData) {
        final String current_user_ref = "messages/" + mCurrentUserId + "/" + mChatUser;
        final String chat_user_ref = "messages/" + mChatUser + "/" + mCurrentUserId;

        DatabaseReference user_message_push = mRootRef.child("messages")
                .child(mCurrentUserId).child(mChatUser).push();

        final String push_id = user_message_push.getKey();

        final StorageReference filepath = mImageStorage.child(push_id + ".jpg");
        filepath.putBytes(imageData).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    filepath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                String download_url = task.getResult().toString();

                                Map messageMap = new HashMap();
                                messageMap.put("message", download_url);
                                messageMap.put("seen", false);
                                messageMap.put("type", "image");
                                messageMap.put("time", System.currentTimeMillis());
                                messageMap.put("from", mCurrentUserId);

                                Map messageUserMap = new HashMap();
                                messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
                                messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);


                                mRootRef.child("chat").child(mCurrentUserId).child(mChatUser).child("seen").setValue(true);
                                mRootRef.child("chat").child(mCurrentUserId).child(mChatUser).child("timestamp").setValue(System.currentTimeMillis());

                                mRootRef.child("chat").child(mChatUser).child(mCurrentUserId).child("seen").setValue(false);
                                mRootRef.child("chat").child(mChatUser).child(mCurrentUserId).child("unread")
                                        .runTransaction(new Transaction.Handler() {
                                            @NonNull
                                            @Override
                                            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                                                Long currentValue = mutableData.getValue(Long.class);
                                                if (currentValue == null) {
                                                    mutableData.setValue(1);
                                                } else {
                                                    mutableData.setValue(currentValue + 1);
                                                }

                                                return Transaction.success(mutableData);
                                            }

                                            @Override
                                            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {

                                            }
                                        });
                                mRootRef.child("chat").child(mChatUser).child(mCurrentUserId).child("timestamp").setValue(System.currentTimeMillis());

                                mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            mProgressDialog.dismiss();
                                            Toast.makeText(getApplicationContext(), "::Failed to send message::\n" + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                                            Log.d("CHAT_LOG", "onComplete: ERROR:" + databaseError.getMessage());
                                        } else {
                                            mProgressDialog.dismiss();
                                        }
                                    }
                                });
                            } else {
                                mProgressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), "::Could not get image url::\n" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    mProgressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "::Image upload failed::\n" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void loadImage(final String profileImage, final CircleImageView imageView) {
        Picasso.get().load(profileImage).networkPolicy(NetworkPolicy.OFFLINE)
                .placeholder(R.drawable.default_profile)
                .into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError(Exception e) {
                        Picasso.get().load(profileImage).placeholder(R.drawable.default_profile)
                                .into(imageView);
                    }
                });
    }

    private void selectPhoto() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setInitialCropWindowPaddingRatio(0)
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .start(this);
    }

    private void initializeProgressDialog(String title, String msg) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(msg);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }
}
