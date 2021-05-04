package mobi.acpm.inspeckage.webserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import mobi.acpm.inspeckage.R
import java.io.IOException

/**
 * Created by acpm on 17/11/15.
 */
class InspeckageService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        //如果API大于18，需要弹出一个可见通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            //数字是随便写的“40”，
            nm.createNotificationChannel(
                    NotificationChannel(
                            "41",
                            "InspeckageService",
                            NotificationManager.IMPORTANCE_DEFAULT
                    )
            )
            val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, "41")

            //其中的2，是也随便写的，正式项目也是随便写
            startForeground(3, builder.build())
        }
    }

    private var ws: WebServer? = null
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Let it continue running until it is stopped.
        val context = applicationContext
        var host: String? = null
        var port = 8008
        if (intent != null && intent.extras != null) {
            host = intent.getStringExtra("host")
            port = intent.getIntExtra("port", 8008)
        }
        try {
            ws = WebServer(host, port, context)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Toast.makeText(this, "Service started on port $port", Toast.LENGTH_LONG).show()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ws != null) ws!!.stop()
        Toast.makeText(this, "Service stopped", Toast.LENGTH_LONG).show()
    }

    companion object {
        const val NOTICE_ID = 100
    }
}