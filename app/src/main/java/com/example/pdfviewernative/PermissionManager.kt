package com.example.pdfviewernative

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import java.lang.ref.WeakReference

class PermissionManager(private val componentActivity: ComponentActivity) {

    private var activityWeakReference: WeakReference<Activity>? = null
    private var runnable: Runnable? = null

    private val permissionNotGrantedList = mutableListOf<String>()

    private var title = ""
    private var message = ""
    private var positiveButtonTitle = ""

    private var activityPermisionResult: ActivityResultLauncher<Intent>
    private var activityPermission: ActivityResultLauncher<Array<String>>

    init {
        activityPermisionResult = registerActivityResult(componentActivity)
        activityPermission = registerPermission(componentActivity)
    }

    fun registerPermission(componentActivity: ComponentActivity): ActivityResultLauncher<Array<String>> {
        return componentActivity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

            activityWeakReference?.get()?.let { activity ->

                val deniedPermissionList = mutableListOf<String>()

                it.entries.forEach { permissionResulutMap ->

                    val permission = permissionResulutMap.key
                    val grantResult = permissionResulutMap.value

                    if (!grantResult) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                permission
                            )
                        ) {
                            deniedPermissionList.add(permission)
                        } else {
                            AlertDialog.Builder(activity).apply {

                                this.setTitle(title)
                                this.setMessage(message)
                                this.setPositiveButton(positiveButtonTitle) { _: DialogInterface?, _: Int ->

                                    val intent =
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    val uri = Uri.fromParts("package", activity.packageName, null)
                                    intent.data = uri
                                    activityPermisionResult.launch(intent)
                                }
                                return@let
                            }
                        }
                    }
                }
            }
        }
    }

    private fun registerActivityResult(componentActivity: ComponentActivity): ActivityResultLauncher<Intent> {
        return componentActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->

            activityWeakReference?.get()?.let {

                val deniedPermissionList = mutableListOf<String>()

                if (result.resultCode == Activity.RESULT_CANCELED) {

                    for (i in permissionNotGrantedList.indices) {
                        val grantResult =
                            ActivityCompat.checkSelfPermission(it, permissionNotGrantedList[i])
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            deniedPermissionList.add(permissionNotGrantedList[i])
                        }
                    }
                }
                if (deniedPermissionList.isEmpty()) {
                    runnable?.run()
                    permissionNotGrantedList.clear()
                }
                activityWeakReference?.clear()
            }
        }
    }

    fun requestPermission(
        permissionDialogTitle: String,
        permissionDialogMessage: String,
        permissionDialogPositiveButtonTitle: String,
        permissions: Array<String>,
        runnableAfterPermissionGranted: Runnable? = null
    ) {
        permissionNotGrantedList.clear()

        for (i in permissions.indices) {
            if (ActivityCompat.checkSelfPermission(
                    componentActivity,
                    permissions[i]
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionNotGrantedList.add(permissions[i])
            }
        }
        if (permissionNotGrantedList.isNotEmpty()) {

            title = permissionDialogTitle
            message = permissionDialogMessage
            positiveButtonTitle = permissionDialogPositiveButtonTitle
            runnable = runnableAfterPermissionGranted
            activityPermission.launch(permissions)

        } else {
            runnableAfterPermissionGranted?.run()
        }
    }

}