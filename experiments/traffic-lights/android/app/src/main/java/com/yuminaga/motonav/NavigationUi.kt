package com.yuminaga.motonav

import android.app.Activity
import android.content.Context
import android.content.Intent

/** Page navigation only: never prepares, stops or restarts a guidance session. */
object NavigationUi {
    const val EXTRA_SETTINGS_PAGE="navigation_settings_page"
    fun active()=(WebMotorGuide.active && !WebMotorGuide.arrived) || NaviBridge.hasActiveNavigation()
    fun openSettings(activity:Activity,page:Int=1) {
        // Do not CLEAR_TOP to MainActivity: that would finish the live SDK page.
        activity.startActivity(Intent(activity,MainActivity::class.java)
            .putExtra(EXTRA_SETTINGS_PAGE,page.coerceIn(0,4)))
        android.util.Log.i("MotoNavLifecycle","settings-open epoch=${NaviBridge.navigationEpoch()} page=$page")
    }
    fun returnToNavigation(context:Context):Boolean {
        val target=when {
            WebMotorGuide.active && !WebMotorGuide.arrived -> Intent(context,MotorWebNavActivity::class.java)
            NaviBridge.hasActiveNavigation() -> Intent(context,NavActivity::class.java)
                .putExtra("emu",NaviBridge.navigationIsSimulation())
            else -> return false
        }
        // Clear only the settings pages above guidance, retaining its map and controls.
        target.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if(context !is Activity)target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(target)
        android.util.Log.i("MotoNavLifecycle","settings-return epoch=${NaviBridge.navigationEpoch()}")
        return true
    }
}
