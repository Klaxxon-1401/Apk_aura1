package com.auraclone.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.auraclone.R
import com.auraclone.data.IrRepository
import com.auraclone.ir.IrManager

class HomeActivity : AppCompatActivity() {
    private val irRepository by lazy { IrRepository(this) }
    private val irManager by lazy { IrManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Setup bottom navigation
        setupBottomNavigation()

        // Load remote fragment by default
        if (savedInstanceState == null) {
            loadRemoteFragment()
        }
    }

    private fun setupBottomNavigation() {
        // Remote button - already on remote screen
        findViewById<View>(R.id.iv_remote)?.setOnClickListener {
            loadRemoteFragment()
        }
        findViewById<View>(R.id.tv_remote)?.setOnClickListener {
            loadRemoteFragment()
        }

        // Home button - show help section (if visible)
        findViewById<View>(R.id.iv_home)?.setOnClickListener {
            // Could navigate to smart home features
        }
        findViewById<View>(R.id.tv_home)?.setOnClickListener {
            // Could navigate to smart home features
        }

        // Me button - could show settings/profile
        findViewById<View>(R.id.iv_me)?.setOnClickListener {
            // Could navigate to settings
        }
        findViewById<View>(R.id.tv_me)?.setOnClickListener {
            // Could navigate to settings
        }
    }

    private fun loadRemoteFragment() {
        val fragment = RemoteFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fm_content, fragment)
            .commit()
    }

    fun navigateToBrandList() {
        val intent = Intent(this, BrandListActivity::class.java)
        startActivity(intent)
    }
}

