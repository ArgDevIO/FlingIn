package argdev.io.flingin.kotlin

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import argdev.io.flingin.R
import argdev.io.flingin.java.utils.ImagePicker
import argdev.io.flingin.java.activities.MainActivity
import argdev.io.flingin.java.fragments.ProfileFragment
import com.facebook.*
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_login_register.*
import java.io.ByteArrayOutputStream
import java.util.*


class LoginRegisterActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    //region Gloobal Variables
    private var mCallbackManager: CallbackManager? = null
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener

    private var selectedPhotoUri: Uri? = null
    private var bmp: Bitmap? = null
    private var mProgressDialog: ProgressDialog? = null
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_register)

        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        mCallbackManager = CallbackManager.Factory.create()
        mAuth = FirebaseAuth.getInstance()
        mAuthListener = FirebaseAuth.AuthStateListener {
            val user = it.currentUser
            if (user != null) {
                // User is signed in
                Log.d(TAG, "onAuthStateChanged:signed_in: ${user.uid}")
            } else {
                // User is signed out
                Log.d(TAG, "onAuthStateChanged:signed_out:")
            }
        }

        initializeFacebookLogin()
        initializeGoogleLogin()

        setListeners()
        setCustomFonts()
    }

    override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(mAuthListener)
    }

    override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener)
        }
    }


    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed $p0: ")
        Toast.makeText(this, "Google Play Services error", Toast.LENGTH_LONG).show()
    }

    private fun updateMyUI(type: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference().child("users").child(uid)

        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
            if (it.isSuccessful) {
                ref.child("device_token").setValue(it.result!!.token).addOnSuccessListener {

                    ProfileFragment.fetchDataFromDB { done ->
                        if (done) {
                            Handler().postDelayed({
                                if (type == "Login") {
                                    login_editTxt_email.text.clear()
                                    login_editTxt_password.text.clear()
                                    mProgressDialog!!.dismiss()
                                }
                                val intent = Intent(this, MainActivity::class.java)
                                intent.putExtra("FromLogin", "FROM_LOGIN")
                                Log.d("MAIN-Activityy", "Calling intent from Login Screen")
                                startActivity(intent)
                                overridePendingTransition(
                                    R.anim.fade_in,
                                    R.anim.fade_out
                                )
                                finish()
                            }, 350)
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Couldn't get DEVICE_TOKEN_ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //region --------------- FACEBOOK LOGIN -----------------
    private fun initializeFacebookLogin() {
        login_button.setPermissions("email", "public_profile", "user_friends")
        login_button.registerCallback(mCallbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                firebaseAuthWithFacebook(result!!.accessToken)
            }

            override fun onCancel() {
            }

            override fun onError(error: FacebookException?) {
            }

        })
    }

    private fun firebaseAuthWithFacebook(token: AccessToken) {
        initializeProgressDialog(
            "Facebook Logging in...",
            "Please wait while we log you in with Facebook."
        )

        Log.d(TAG, "handleFacebookAccessToken: $token")
        val credential = FacebookAuthProvider.getCredential(token.token)

        mAuth.signInWithCredential(credential)
            .addOnCompleteListener {
                Log.d(TAG, "signInWithCredential:onComplete: ${it.isSuccessful}")
                if (!it.isSuccessful) {
                    mProgressDialog!!.dismiss()
                    Log.w(TAG, "signInWithCredential", it.exception)
                    Toast.makeText(
                        this,
                        "Authentication failed: ${it.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    if (it.result!!.additionalUserInfo!!.isNewUser) {
                        saveUserToFirebaseDatabase(it.result!!.user, "Facebook")
                    } else {
                        updateMyUI("Facebook")
                    }
                }
            }
    }

    //endregion

    //region --------------- GOOGLE LOGIN -----------------
    private fun initializeGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(
            signInIntent,
            GOOGLE_SIGN_IN
        )
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        initializeProgressDialog(
            "Google Logging in...",
            "Please wait while we log you in with Google."
        )

        Log.d(TAG, "firebaseAuthWithGoogle: ${acct.id}")
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener {
                if (!it.isSuccessful) {
                    mProgressDialog!!.dismiss()
                    Log.w(TAG, "signInWithCredential", it.exception)
                    Toast.makeText(
                        this,
                        "Authentication failed: ${it.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    if (it.result!!.additionalUserInfo!!.isNewUser) {
                        saveUserToFirebaseDatabase(it.result!!.user, "Google")
                    } else {
                        updateMyUI("Google")
                    }
                }
            }
    }
    //endregion

    //region --------------- SIMPLE LOGIN & REGISTER ---------------
    private fun performLogin() {
        val email = login_editTxt_email.text.toString()
        val password = login_editTxt_password.text.toString()

        if (email.isEmpty() && password.isEmpty()) {
            Toast.makeText(this, "Email & Password fields can't be empty!!", Toast.LENGTH_LONG)
                .show()
            return
        } else if (email.isEmpty()) {
            Toast.makeText(this, "Email field can't be empty!!", Toast.LENGTH_LONG).show()
            return
        } else if (password.isEmpty()) {
            Toast.makeText(this, "Password field can't be empty!!", Toast.LENGTH_LONG).show()
            return
        }

        Log.d(TAG, "Attempt login with email/pw: $email/***")

        initializeProgressDialog(
            "Logging in...",
            "Please wait while we log you in with Email."
        )

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener

                // else if successful
                updateMyUI("Login")
                Log.d(TAG, "Successfully logged in with uid: ${it.result?.user?.uid}")
            }
            .addOnFailureListener {
                mProgressDialog!!.dismiss()
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                Log.d(TAG, "Failed to login user: ${it.message}")
            }
    }

    private fun performRegister() {

        val email = register_editTxt_email.text.toString()
        val password = register_editTxt_password.text.toString()
        val passwordConfirm = register_editTxt_confirmPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email & password!", Toast.LENGTH_SHORT).show()
            return
        } else if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, "Passwords doesn't match!", Toast.LENGTH_SHORT).show()
            register_editTxt_password.text.clear()
            register_editTxt_confirmPassword.text.clear()
            register_editTxt_confirmPassword.setSelection(0)
            return
        }

        initializeProgressDialog(
            "Registering user...",
            "Please wait while registration finishes."
        )

        // Firebase Authentication
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener

                // else if successful
                Log.d(
                    TAG,
                    "Successfully created user with uid: ${it.result?.user?.uid}"
                )
                //Toast.makeText(this, "Successfully registered!", Toast.LENGTH_LONG).show()
                saveUserToFirebaseDatabase(it.result?.user, "Email")
            }
            .addOnFailureListener {
                mProgressDialog!!.dismiss()
                Log.d(TAG, "Failed to create user: ${it.message}")
                Toast.makeText(this, "Failed to create user: ${it.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }
    //endregion

    private fun setListeners() {
        register_btn_register.setOnClickListener {
            performRegister()
        }

        register_btn_selectPhoto.setOnClickListener {
            selectPhoto()
        }

        login_btn_showPassword.setOnClickListener {
            showHidePassword()
        }

        login_btn_login.setOnClickListener {
            performLogin()
        }

        login_btn_scrollDown2.setOnClickListener {
            scrollToBottom()
        }

        login_btn_scrollDown.setOnClickListener {
            scrollToBottom()
        }

        login_btn_twitter.setOnClickListener {
        }

        scrollView.setOnTouchListener { _, _ -> true }

        login_btn_facebook.setOnClickListener {
            login_button.performClick()
        }

        login_btn_googlePlus.setOnClickListener {
            signIn()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mCallbackManager?.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            var uriImage: Uri?

            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                uriImage = result.uri

                selectedPhotoUri = uriImage
                bmp = ImagePicker.getImageResized(this, uriImage)
                register_selectPhoto_imageView.setImageBitmap(bmp)
                register_btn_selectPhoto.alpha = 0f
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "Couldn't select image", Toast.LENGTH_LONG).show()
            }

        } else if (requestCode == GOOGLE_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                val account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            } else {
                Log.e(TAG, "Google Sign In failed.")
            }
        }
    }

    private fun saveUserToFirebaseDatabase(currentUser: FirebaseUser?, type: String) {
        Log.d(TAG, "FirebaseDB:: Saving to Database.......")
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference().child("users").child(uid).child("profile_info")
        //ref.keepSynced(true)

        val user: User? = if (type == "Email")
            User(
                register_editTxt_username.text.toString(),
                resources.getString(R.string.default_status)
            )
        else
            User(
                currentUser?.displayName.toString(),
                resources.getString(R.string.default_status)
            )

        ref.setValue(user)
            .addOnSuccessListener {
                Log.d(TAG, "FirebaseDB:: Finally we saved the user to Firebase Database")
            }
            .addOnFailureListener {
                Log.d(
                    TAG,
                    "FirebaseDB:: Failed to save the user to Firebase Database: ${it.message}"
                )
            }

        when (type) {
            "Facebook" -> {
                val fbID = Profile.getCurrentProfile().id
                val imageUri =
                    "https://graph.facebook.com/$fbID/picture?type=large&width=500&height=500"
                ref.child("image_url").setValue(imageUri).addOnCompleteListener {
                    if (it.isSuccessful) {
                        mProgressDialog!!.dismiss()
                        updateMyUI(type)
                    }
                }
                return
            }
            "Google" -> {
                val acct = GoogleSignIn.getLastSignedInAccount(applicationContext)
                if (acct != null) {
                    val imageUri = acct.photoUrl.toString().replace("s96-c", "s500-c")
                    ref.child("image_url").setValue(imageUri)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                mProgressDialog!!.dismiss()
                                updateMyUI(type)
                            }
                        }
                }
                return
            }
            else -> {
                if (selectedPhotoUri == null) {
                    val default_pic_url =
                        "https://firebasestorage.googleapis.com/v0/b/fling-in.appspot.com/o/users%2Fdefault_profile_picture?alt=media&token=token_here"
                    ref.child("image_url").setValue(default_pic_url)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                mProgressDialog!!.dismiss()
                                updateMyUI("Register")
                            }
                        }
                    return
                }

                val filename = UUID.randomUUID().toString()
                val imagesStorageRef =
                    FirebaseStorage.getInstance()
                        .getReference("/users/$uid/profileImage/$filename")

                val imageData: ByteArray
                val stream = ByteArrayOutputStream()
                bmp?.compress(Bitmap.CompressFormat.JPEG, 40, stream)
                imageData = stream.toByteArray()

                imagesStorageRef.putBytes(imageData)
                    .addOnSuccessListener {
                        imagesStorageRef.downloadUrl.addOnSuccessListener { it1 ->
                            it1.toString()
                            ref.child("image_url").setValue(it1.toString())
                                .addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        mProgressDialog!!.dismiss()
                                        updateMyUI("Register")
                                    }
                                }
                        }
                    }
            }
        }
    }

    //region ------------- Notifications --------------

    //endregion

    //region ------------- Utils -----------------
    private fun selectPhoto() {
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .setAspectRatio(1, 1)
            .setCropShape(CropImageView.CropShape.OVAL)
            .start(this)
    }

    private fun setCustomFonts() {
        val typeface = Typeface.createFromAsset(assets, "fonts/Tumbly.otf")

        txtView_signUp.typeface = typeface
        txtView_login.typeface = typeface
    }

    private fun scrollToBottom() {
        isBottom = if (isBottom) {
            login_btn_scrollDown2.setImageResource(R.drawable.ic_arrow_downward_black_24dp)
            login_btn_scrollDown.setImageResource(R.drawable.ic_arrow_downward_black_24dp)
            ObjectAnimator.ofInt(scrollView, "scrollY", scrollView.top).setDuration(620).start()
            false
        } else {
            login_btn_scrollDown2.setImageResource(R.drawable.ic_arrow_upward_black_24dp)
            login_btn_scrollDown.setImageResource(R.drawable.ic_arrow_upward_black_24dp)
            ObjectAnimator.ofInt(scrollView, "scrollY", scrollView.bottom).setDuration(620).start()
            true
        }
    }

    private fun showHidePassword() {
        if (isVisiblePwd) {
            login_editTxt_password.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            login_editTxt_password.setSelection(login_editTxt_password.text.length)
            login_btn_showPassword.setBackgroundResource(R.drawable.ic_eyered)
            isVisiblePwd = false
        } else {
            login_editTxt_password.transformationMethod = PasswordTransformationMethod.getInstance()
            login_editTxt_password.setSelection(login_editTxt_password.text.length)
            login_btn_showPassword.setBackgroundResource(R.drawable.ic_eyeblack)
            isVisiblePwd = true
        }
    }

    private fun initializeProgressDialog(title: String, msg: String) {
        mProgressDialog = ProgressDialog(this)
        mProgressDialog!!.setTitle(title)
        mProgressDialog!!.setMessage(msg)
        mProgressDialog!!.setCanceledOnTouchOutside(false)
        mProgressDialog!!.show()
    }
    //endregion

    companion object {
        private var isBottom: Boolean = false
        private var isVisiblePwd: Boolean = false
        private const val GOOGLE_SIGN_IN = 9001
        private const val TAG = "LoginRegister-Activity"
    }
}

//HELPER CLASSES
class User(
    val username: String,
    val status: String
)