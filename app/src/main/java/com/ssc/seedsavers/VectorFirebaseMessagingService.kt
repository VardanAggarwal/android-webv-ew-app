package com.ssc.seedsavers

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class VectorFirebaseMessagingService: FirebaseMessagingService() {
    private val TAG = VectorFirebaseMessagingService::class.qualifiedName
    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG,"notification received ${message.data.toString()}")
            showNotification(message)
    }
    override fun onNewToken(token: String) {
        super.onNewToken(token);
        getSharedPreferences("_", MODE_PRIVATE).edit().putString("fcmtoken", token).apply();
        Log.i(TAG,"onNewToken: FCM Token has been updated [$token]")

    }
    private fun showNotification(message: RemoteMessage){
        val intent = Intent(this,MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        if(message.data.containsKey("url")) {
            intent.setData(Uri.parse(message.data["url"]))
        }
        val flags =
            if (SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent,  flags)
        val bitmap: Bitmap?
        var style: NotificationCompat.Style
        if (message.notification?.getImageUrl() != null) {
            bitmap = GlideApp.with(this).asBitmap().load(message.notification?.imageUrl).submit().get()
            style = NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .bigLargeIcon(null)
                .setSummaryText(message.notification?.body)
        } else {
            bitmap = null
            style = NotificationCompat.BigTextStyle()
                .bigText(message.notification?.body)
        }
        if (message.data.containsKey("style")) {
            if (message.data["style"] == "text") {
                style = NotificationCompat.BigTextStyle()
                    .bigText(message.notification?.body)
            } else if (message.data["style"] == "picture") {
                style = NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null)
                    .setSummaryText(message.notification?.body)
            }
        }
        val builder = NotificationCompat.Builder(this, "SSC")
            .setSmallIcon(R.drawable.ic_ssc_notification)
            .setLargeIcon(bitmap)
            .setColor(ContextCompat.getColor(this, R.color.green))
            .setContentTitle(message.notification?.title)
            .setContentText(message.notification?.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(style)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(0, builder.build())
        }
    }
}