package argdev.io.flingin.kotlin

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import argdev.io.flingin.R
import argdev.io.flingin.java.activities.MainActivity
import argdev.io.flingin.java.fragments.ProfileFragment
import com.google.firebase.auth.FirebaseAuth

class SplashScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser

        if (currentUser != null) {
            Log.d("SPLASHH", "SplashCreated: User is logged in")
            ProfileFragment.fetchDataFromDB { done ->
                if (done) {
                    Handler().postDelayed({
                        val intent = Intent(this@SplashScreen, MainActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        overridePendingTransition(
                            R.anim.fade_in,
                            R.anim.fade_out
                        )
                    }, 200)
                }
            }
        } else {
            Log.d("SPLASHH", "SplashCreated: User is NOT logged in")
            Handler().postDelayed({
                val intent = Intent(this@SplashScreen, LoginRegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                overridePendingTransition(
                    R.anim.fade_in,
                    R.anim.fade_out
                )
            }, 200)
        }
    }
}
