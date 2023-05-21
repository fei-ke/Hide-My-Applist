package icu.nullptr.hidemyapplist.ui.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.NameNotFoundException
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import com.tsng.hidemyapplist.R
import icu.nullptr.hidemyapplist.ui.activity.MainActivity
import icu.nullptr.hidemyapplist.ui.fragment.AppSettingsFragmentArgs
import icu.nullptr.hidemyapplist.util.PackageHelper

class AppChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AppChangeReceiver"

        private val actions = setOf(
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED
        )

        fun register(context: Context) {
            val filter = IntentFilter().apply {
                actions.forEach(::addAction)
                addDataScheme("package")
            }
            context.registerReceiver(AppChangeReceiver(), filter)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in actions) {
            Log.i(TAG, "Received intent: $intent")
            PackageHelper.invalidateCache()
        }
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val packageName = intent.data?.schemeSpecificPart ?: return
            showNotificationForInstalledApp(context, packageName)
        }
    }

    private fun showNotificationForInstalledApp(context: Context, packageName: String) {
        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.main_nav_graph)
            .setDestination(R.id.nav_app_settings)
            .setArguments(AppSettingsFragmentArgs(packageName).toBundle())
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        NotificationChannelCompat.Builder("package_added", NotificationManager.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.app_name))
            .build()
            .let(NotificationManagerCompat.from(context)::createNotificationChannel)

        val appName = try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(packageName, 0)
            )
        } catch (e: NameNotFoundException) {
            packageName
        }

        val notification = NotificationCompat.Builder(context, "package_added")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$appName Installed")
            .setContentText("Tap to setting this app")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(packageName.hashCode(), notification)
    }
}
