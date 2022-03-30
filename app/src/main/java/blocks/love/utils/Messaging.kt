package blocks.love.utils

//https://github.com/firebase/quickstart-android/blob/8d5a8bc2578ff74c6b3f53bc5c8aa945c7e03fa6/messaging/app/src/main/java/com/google/firebase/quickstart/fcm/kotlin/MyFirebaseMessagingService.kt#L65-L77

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import blocks.love.MainActivity
import blocks.love.R
import blocks.love.RestApiManager
import blocks.love.TokenData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class Messaging : FirebaseMessagingService() {

    private val channelId = "fcm_default_channel"
    private val channelName = "blocks.love"

    companion object {
        private const val TAG = "FCM"
    }

//    https://www.geeksforgeeks.org/how-to-push-notification-in-android-using-firebase-cloud-messaging/
    private fun generateNotification(title: String, message : String){

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        var builder: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_stat_ic_notification)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000,1000,1000,1000,1000))
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)

        builder = builder.setContent(getRemoteView(title, message))
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(0, builder.build())
    }

    private fun getRemoteView(title: String, message: String): RemoteViews{
        val remoteView = RemoteViews("blocks.love", R.layout.notification)
        remoteView.setTextViewText(R.id.title, title)
        remoteView.setTextViewText(R.id.message, message)
        remoteView.setImageViewResource(R.id.app_logo, R.drawable.ic_stat_ic_notification)
        return remoteView
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            Log.d(TAG, "Notification: $it")
            generateNotification(it.title!!, it.body!!)
        }

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
//            handleNow(remoteMessage.data)
            scheduleJob(remoteMessage.data)
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }


    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

//    fun onTokenRefresh() {
//        // Get updated InstanceID token.
//        val refreshedToken: String = FirebaseInstanceId.getInstance().getToken()
//        Log.d(TAG, "Refreshed token: $refreshedToken")
//
//        // If you want to send messages to this application instance or
//        // manage this apps subscriptions on the server side, send the
//        // Instance ID token to your app server.
//        sendRegistrationToServer(refreshedToken)
//    }

    /**
     * Schedule async work using WorkManager.
     */
    private fun scheduleJob(messageData: Map<String, String>) {
        if (messageData["url"] != null && messageData["name"] != null){
            val worker = OneTimeWorkRequest.Builder(MyWorker::class.java)
            val data = Data.Builder()
            data.putString("url", messageData["url"])
            data.putString("name", messageData["name"])
            Log.d("FCM", "Url: ${messageData["url"]} Name: ${messageData["name"]}")
            worker.setInputData(data.build())
            WorkManager.getInstance(this).enqueue(worker.build())
        }
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private fun handleNow() {
        Log.d(TAG, "Short lived task is done.")
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM registration token with any server-side account
     * maintained by your application.
     *
     * @param fcmToken The new token.
     */
    private fun sendRegistrationToServer(fcmToken: String) {
        val user = Firebase.auth.currentUser
        user?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val idToken = task.result.token
                val tokenData = TokenData(
                    id_token = idToken!!,
                    fcm_token = fcmToken,
                )
                RestApiManager().sendToken(tokenData) { responseData ->
                    if (responseData?.errors?.error != null) {
                        Log.d("TOKEN", "response: " + responseData.errors.error)
                    }
                }
            } else {
                // todo Handle error -> task.getException();
                Log.w("TOKEN", "Fetching ID token failed", task.exception)
            }
        }
        Log.d("TOKEN", "sendRegistrationTokenToServer($fcmToken)")
    }

//    /**
//     * Create and show a simple notification containing the received FCM message.
//     *
//     * @param messageBody FCM message body received.
//     */
//    private fun sendNotification(messageBody: String) {
//        val intent = Intent(this, MainActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
//            PendingIntent.FLAG_ONE_SHOT)
//
//        val channelId = getString(R.string.default_notification_channel_id)
//        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//        val notificationBuilder = NotificationCompat.Builder(this, channelId)
//            .setSmallIcon(R.drawable.ic_stat_ic_notification)
//            .setContentTitle(getString(R.string.fcm_message))
//            .setContentText(messageBody)
//            .setAutoCancel(true)
//            .setSound(defaultSoundUri)
//            .setContentIntent(pendingIntent)
//
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        // Since android Oreo notification channel is needed.
//        val channel = NotificationChannel(channelId, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT)
//        notificationManager.createNotificationChannel(channel)
//
//        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
//    }
}