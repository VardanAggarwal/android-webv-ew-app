package com.ssc.seedsavers

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class VectorFirebaseMessagingService: FirebaseMessagingService() {
    private val TAG = VectorFirebaseMessagingService::class.qualifiedName
    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG,"notification received ${message.data.toString()}")
            showNotification(message)
    }
    override fun onNewToken(token: String) {
        Log.i(TAG,"onNewToken: FCM Token has been updated [$token]")
    }
    private fun showNotification(message: RemoteMessage){
        val intent: Intent
        val pendingIntent: PendingIntent?
        intent = Intent(this,MainActivity::class.java)
        if(message.data.containsKey("url")) {
            intent.setData(Uri.parse(message.data["url"]))
        }
        pendingIntent = TaskStackBuilder.create(this).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(intent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val bitmap: Bitmap?
        var style: NotificationCompat.Style
        if (message.notification?.getImageUrl() != null) {
            bitmap = Glide.with(this).asBitmap().load(message.notification?.imageUrl).submit().get()
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