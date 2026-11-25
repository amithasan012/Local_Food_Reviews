package com.localfoodreviews.app.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.TextView
import com.localfoodreviews.app.R
import com.localfoodreviews.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val signedInDestinations = setOf(
        R.id.feedFragment,
        R.id.createReviewFragment,
        R.id.myReviewsFragment,
        R.id.changePasswordFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController
        appBarConfiguration = AppBarConfiguration(signedInDestinations, binding.drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navigationView.setupWithNavController(navController)
        binding.navigationView.setNavigationItemSelectedListener { item -> onDrawerItemSelected(item) }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val canUseDrawer = destination.id in signedInDestinations && FirebaseAuth.getInstance().currentUser != null
            binding.drawerLayout.setDrawerLockMode(
                if (canUseDrawer) androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED
                else androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            )
            binding.navigationView.menu.setGroupVisible(R.id.drawer_group, canUseDrawer)
            if (canUseDrawer) updateDrawerHeader()
        }

        // Firebase Auth remembers an authenticated user between app launches.
        if (savedInstanceState == null && FirebaseAuth.getInstance().currentUser != null) {
            // The navigation graph starts on Login. Replace it with Feed only for a remembered session.
            navController.navigate(R.id.feedFragment, null, NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, true)
                .build())
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun updateDrawerHeader() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val header = binding.navigationView.getHeaderView(0)
        val username = header.findViewById<TextView>(R.id.drawer_username)
        val email = header.findViewById<TextView>(R.id.drawer_email)
        username.text = "Food reviewer"
        email.text = user.email.orEmpty()
        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                username.text = document.getString("username") ?: "Food reviewer"
            }
    }

    private fun onDrawerItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.logout) {
            FirebaseAuth.getInstance().signOut()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            navController.navigate(R.id.loginFragment, null, NavOptions.Builder()
                .setPopUpTo(navController.graph.startDestinationId, true)
                .build())
            return true
        }
        val handled = androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
        if (handled) binding.drawerLayout.closeDrawer(GravityCompat.START)
        return handled
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
}
