package me.lucky.wasted.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

// owns the fine→background permission settle flow: BACKGROUND must be
// requested on its own (Q+), combined requests silently ignore it
class LocationPermission(private val frag: Fragment) {
    private var onSettled: ((Boolean) -> Unit)? = null

    private val fineRequester =
        frag.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.any { it }) maybeBackground()
            else onSettled?.invoke(false)
        }

    private val backgroundRequester =
        frag.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // a denied background permission still allows while-in-use fixes
            // from a foreground service that was started while in use
            onSettled?.invoke(true)
        }

    fun ensure(onSettled: (Boolean) -> Unit) {
        this.onSettled = onSettled
        val fineGranted = ContextCompat.checkSelfPermission(
            frag.requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted) maybeBackground()
        else fineRequester.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        )
    }

    // BACKGROUND must be requested alone (Q+), also when FINE is already held
    private fun maybeBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                frag.requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) backgroundRequester.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else onSettled?.invoke(true)
    }
}