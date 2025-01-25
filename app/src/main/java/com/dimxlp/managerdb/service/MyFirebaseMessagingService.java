package com.dimxlp.managerdb.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.dimxlp.managerdb.MainActivity;
import com.dimxlp.managerdb.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String LOG_TAG = "RAFI|FCMService";

    // This method is triggered whenever a new message is received
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Log the message details
        Log.d(LOG_TAG, "From: " + remoteMessage.getFrom());

        // Check if the message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            Log.d(LOG_TAG, "Notification - Title: " + title + ", Body: " + body);
            // Call a method to display the notification (we'll define it in the next step)
            showNotification(title, body);
        }

        // Check if the message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");

            Log.d(LOG_TAG, "Data Payload - Title: " + title + ", Message: " + message);

            // Save the message to SharedPreferences
            saveMessageToPreferences(title, message);

            // Process custom data payload
//            handleDataPayload(remoteMessage.getData());
        }
    }

    // This method is triggered when the FCM token is updated
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(LOG_TAG, "New FCM Token: " + token);

        // Send the token to your server
//        sendTokenToServer(token);
    }

    private void saveMessageToPreferences(String title, String message) {
        SharedPreferences preferences = getSharedPreferences("AppMessages", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("title", title);
        editor.putString("message", message);
        editor.apply();
    }

//    private void sendTokenToServer(String token) {
//        // TODO: Implement logic to send the token to your backend
//    }
//
//    private void handleDataPayload(Map<String, String> data) {
//        // TODO: Add logic to process data payload (e.g., update UI, refresh content)
//    }

    private void showNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "default_channel";

        // Create a notification channel (required for Android 8.0+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Create an intent to open the app when the user taps the notification
        Intent intent = new Intent(this, MainActivity.class); // Replace MainActivity with your main activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.notification_filled_64) // Replace with your app's notification icon
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Show the notification
        notificationManager.notify(0, builder.build());    }
}
