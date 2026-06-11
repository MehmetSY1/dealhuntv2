package com.dealhunt.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dealhunt.app.R
import com.dealhunt.app.databinding.ActivityMainBinding
import com.dealhunt.app.ui.fragments.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) showFragment(HomeFragment())

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home     -> showFragment(HomeFragment())
                R.id.nav_explore  -> showFragment(ExploreFragment())
                R.id.nav_wishlist -> showFragment(WishlistFragment())
                R.id.nav_ai       -> showFragment(AiFragment())
                R.id.nav_profile  -> showFragment(ProfileFragment())
            }
            true
        }
    }

    private fun showFragment(f: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, f)
            .commit()
    }
}
