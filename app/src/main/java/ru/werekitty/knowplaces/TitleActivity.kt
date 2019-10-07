package ru.werekitty.knowplaces

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_title.*

class TitleActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_title)

        mAuth = FirebaseAuth.getInstance()
        mAuth.signInAnonymously()

        primitiveAnimation(2500, 15)
    }

    fun primitiveAnimation(delay: Long, step: Long) {
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.95F }, delay)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.9F }, delay + step * 1)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.85F }, delay + step * 2)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.8F }, delay + step * 3)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.75F }, delay + step * 4)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.7F }, delay + step * 5)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.65F }, delay + step * 6)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.6F }, delay + step * 7)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.55F }, delay + step * 8)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.5F }, delay + step * 9)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.45F }, delay + step * 10)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.4F }, delay + step * 11)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.35F }, delay + step * 12)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.3F }, delay + step * 13)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.25F }, delay + step * 14)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.2F }, delay + step * 15)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.15F }, delay + step * 16)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.1F }, delay + step * 17)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.05F }, delay + step * 18)
        android.os.Handler().postDelayed({ TitleLayout.alpha = 0.0F }, delay + step * 19)
        android.os.Handler().postDelayed({ toMain() }, delay + step * 20)
    }

    fun toMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
