package com.sharmadhiraj.installed_apps

import MyPackageReceiver
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.annotation.NonNull
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import com.sharmadhiraj.installed_apps.Util.Companion.convertAppToMap
import com.sharmadhiraj.installed_apps.Util.Companion.getPackageManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.util.Locale.ENGLISH
import android.content.BroadcastReceiver
import io.flutter.plugin.common.EventChannel
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.util.Log
import java.util.Calendar


class InstalledAppsPlugin : MethodCallHandler, FlutterPlugin, ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var context: Context? = null
     private lateinit var activity: Activity

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "installed_apps")
        channel.setMethodCallHandler(this)
        context = binding.applicationContext

        // LISTEN APP INSTALL AND UNINSTALL EVENTS
        val EVENT_CHANNEL = "com.sharmadhiraj.installed_apps/app_monitor"

        eventChannel = EventChannel(binding.binaryMessenger, EVENT_CHANNEL)
        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                val receiver = MyPackageReceiver(events)
                val intentFilter = IntentFilter()
                intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
                intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
                intentFilter.addDataScheme("package")
                binding.applicationContext.registerReceiver(receiver, intentFilter)
            }

            override fun onCancel(arguments: Any?) {}
        })
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }

    override fun onAttachedToActivity(activityPluginBinding: ActivityPluginBinding) {
        // context = activityPluginBinding.activity
        activity = activityPluginBinding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onReattachedToActivityForConfigChanges(activityPluginBinding: ActivityPluginBinding) {
        // context = activityPluginBinding.activity
        activity = activityPluginBinding.activity
    }

    override fun onDetachedFromActivity() {}

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (context == null) {
            result.error("ERROR", "Context is null", null)
            return
        }
        when (call.method) {
            "getInstalledApps" -> {
                val excludeSystemApps = call.argument<Boolean>("exclude_system_apps") ?: true
                val withLaunchIntentOnly = call.argument("with_launch_intent_only") ?: true
                val withIcon = call.argument<Boolean>("with_icon") ?: false
                val packageNamePrefix = call.argument<String>("package_name_prefix") ?: ""
                val platformTypeName = call.argument<String>("platform_type") ?: ""

                Thread {
                    val apps: List<Map<String, Any?>> =
                        getInstalledApps(excludeSystemApps, withLaunchIntentOnly, withIcon, packageNamePrefix, PlatformType.fromString(platformTypeName))
                    result.success(apps)
                }.start()
            }

            "startApp" -> {
                val packageName = call.argument<String>("package_name")
                result.success(startApp(packageName))
            }

            "openSettings" -> {
                val packageName = call.argument<String>("package_name")
                openSettings(packageName)
            }

            "toast" -> {
                val message = call.argument<String>("message") ?: ""
                val short = call.argument<Boolean>("short_length") ?: true
                toast(message, short)
            }

            "getAppInfo" -> {
                val packageName = call.argument<String>("package_name") ?: ""
                val platformTypeName = call.argument<String>("platform_type") ?: ""
                val platformType = PlatformType.fromString(platformTypeName)
                result.success(getAppInfo(getPackageManager(context!!), packageName, platformType))
            }

            "isSystemApp" -> {
                val packageName = call.argument<String>("package_name") ?: ""
                result.success(isSystemApp(getPackageManager(context!!), packageName))
            }

            "uninstallApp" -> {
                val packageName = call.argument<String>("package_name") ?: ""
                result.success(uninstallApp(packageName))
            }

            "isAppInstalled" -> {
                val packageName = call.argument<String>("package_name") ?: ""
                result.success(isAppInstalled(packageName))
            }

            "getUsage" -> {
                getTodaysUsage(result)
            }

            else -> result.notImplemented()
        }
    }

    private fun getInstalledApps(
        excludeSystemApps: Boolean,
        withLaunchIntentOnly: Boolean,
        withIcon: Boolean,
        packageNamePrefix: String,
        platformType: PlatformType?
    ): List<Map<String, Any?>> {
        val packageManager = getPackageManager(context!!)
        var installedApps = packageManager.getInstalledApplications(0)
        if (excludeSystemApps)
            installedApps =
                installedApps.filter { app -> !isSystemApp(packageManager, app.packageName) }
        if (withLaunchIntentOnly)
            installedApps = installedApps.filter { app -> isLaunchable(packageManager, app.packageName) }
        if (packageNamePrefix.isNotEmpty())
            installedApps = installedApps.filter { app ->
                app.packageName.startsWith(
                    packageNamePrefix.lowercase(ENGLISH)
                )
            }
        return installedApps.map { app -> convertAppToMap(packageManager, app, withIcon, platformType) }
    }

    private fun startApp(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return try {
            val launchIntent = getPackageManager(context!!).getLaunchIntentForPackage(packageName)
            context!!.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            print(e)
            false
        }
    }

    private fun toast(text: String, short: Boolean) {
        Toast.makeText(context!!, text, if (short) LENGTH_SHORT else LENGTH_LONG)
            .show()
    }

    private fun isSystemApp(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isLaunchable(packageManager: PackageManager, packageName: String): Boolean {
        return packageManager.getLaunchIntentForPackage(packageName) != null
    }

    private fun openSettings(packageName: String?) {
        if (!isAppInstalled(packageName)) {
            print("App $packageName is not installed on this device.")
            return;
        }
        val intent = Intent().apply {
            flags = FLAG_ACTIVITY_NEW_TASK
            action = ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        }
        context!!.startActivity(intent)
    }

    private fun getAppInfo(
        packageManager: PackageManager,
        packageName: String,
        platformType: PlatformType?
    ): Map<String, Any?>? {
        var installedApps = packageManager.getInstalledApplications(0)
        installedApps = installedApps.filter { app -> app.packageName == packageName }
        return if (installedApps.isEmpty()) null
        else convertAppToMap(packageManager, installedApps[0], true, platformType)
    }

    private fun uninstallApp(packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            context!!.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isAppInstalled(packageName: String?): Boolean {
        val packageManager: PackageManager = context!!.packageManager
        return try {
            packageManager.getPackageInfo(packageName ?: "", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    // fun getUsage(@NonNull call: MethodCall, @NonNull result: Result) {
    //     // Firstly, permission must be given by the user must be set correctly by the user
    //     handlePermissions()

    //     // Parse parameters, i.e. start- and end-date
    //     val start: Long? = call.argument("start")
    //     val end: Long? = call.argument("end")

    //     /// Query the Usage API
    //     val usage = Stats.getUsageMap(context, start!!, end!!)

    //     /// Return the result

    //     result.success(usage)
    // }

    fun getTodaysUsage(@NonNull result: Result) {
        // Firstly, permission must be given by the user must be set correctly by the user
        handlePermissions()

        //Get Today's Usage
        val totalScreenTime = getTotalScreenTime(result)

        /// Return the result
        result.success(totalScreenTime)
    }

    //Original Source Code for the function below: https://stackoverflow.com/a/79386906
    //Adjusted according needs
    @SuppressWarnings("ResourceType")
    private fun getTotalScreenTime(@NonNull result: Result): MutableMap<String, Long> {
        val usageStatsManager = context!!.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar: Calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        val calendarEnd = Calendar.getInstance()
        calendarEnd.set(Calendar.HOUR_OF_DAY, 23)
        calendarEnd.set(Calendar.MINUTE, 59)
        calendarEnd.set(Calendar.SECOND, 59)
        calendarEnd.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendarEnd.timeInMillis

        val eventList = usageStatsManager.queryEvents(startOfDay, endOfDay)
        val stateMap = mutableMapOf<String, AppStateModel>()

        while (eventList.hasNextEvent()) {
            val event = UsageEvents.Event()
            eventList.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val packageCheck = stateMap[event.packageName]
                if (packageCheck != null) {
                    if (stateMap[event.packageName]!!.classMap[event.className] != null) {
                        stateMap[event.packageName]!!.className = event.className
                        stateMap[event.packageName]!!.startTime = event.timeStamp
                        stateMap[event.packageName]!!.classMap[event.className]!!.startTime = event.timeStamp
                        stateMap[event.packageName]!!.classMap[event.className]!!.isResume = true
                    } else {
                        stateMap[event.packageName]!!.className = event.className
                        stateMap[event.packageName]!!.startTime = event.timeStamp
                        stateMap[event.packageName]!!.classMap[event.className] = BoolObj(event.timeStamp, true)
                    }
                } else {
                    val appStates = AppStateModel(packageName = event.packageName, className = event.className, startTime = event.timeStamp)
                    appStates.classMap[event.className] = BoolObj(event.timeStamp, true)
                    stateMap[event.packageName] = appStates
                }
            } else if (event.eventType == UsageEvents.Event.ACTIVITY_STOPPED) {
                val packageCheck = stateMap[event.packageName]
                if (packageCheck != null) {
                    if (stateMap[event.packageName]!!.classMap[event.className] != null) {
                        stateMap[event.packageName]!!.totalTime += event.timeStamp - stateMap[event.packageName]!!.classMap[event.className]!!.startTime
                        stateMap[event.packageName]!!.classMap[event.className]!!.isResume = false
                    }
                }
            }
        }

        // Filter out packages with "launcher" in the name
        val filteredStateList = stateMap.values.filter { !it.packageName.contains("launcher", ignoreCase = true) }

        // Sort the stateMap by totalTime in descending order
        val sortedStateList = filteredStateList.sortedByDescending { it.totalTime }

        val returnList = mutableMapOf<String, Long>()

        // Log the foreground time per package
        sortedStateList.forEach { appState ->
            Log.d("AppUsagePlugin", "Package: ${appState.packageName}, Total Time in Foreground: ${appState.totalTime / 60000} min")
            returnList[appState.packageName] = appState.totalTime / 1000
        }

        // Calculate the total time
        val totalTime = sortedStateList.sumOf { it.totalTime }

        Log.d("AppUsagePlugin", "Total screen time for today: ${totalTime / 1000 / 60} minutes")
        // result.success(totalTime / 1000) // Return total time in seconds
        return returnList
    }

    fun handlePermissions() {
        /// If stats are not available, show the permission screen to give access to them
        if (!Stats.checkIfStatsAreAvailable(context)) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            this.activity.startActivity(intent)
        }
    }
}

data class BoolObj(
    var startTime: Long = 0L,
    var isResume: Boolean = false
)

data class AppStateModel(
    var packageName: String,
    var className: String,
    var startTime: Long = 0L,
    var totalTime: Long = 0L,
    val classMap: MutableMap<String, BoolObj> = mutableMapOf()
)