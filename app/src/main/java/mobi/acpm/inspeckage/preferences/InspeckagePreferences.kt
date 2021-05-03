package mobi.acpm.inspeckage.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.crossbowffs.remotepreferences.RemotePreferences
import mobi.acpm.inspeckage.provider.SharedPreferencesProvider.Companion.PREFERENCE_PROVIDER_AUTHORITY
import mobi.acpm.inspeckage.provider.SharedPreferencesProvider.Companion.PREFS_WIFI_INFO

/**
 * @Title: WifiInfoPrefs类
 * @Package: com.knoyo.wifisimulator.preferences
 * @Description: WIFI信息配置
 * @author XueLong xuelongqy@foxmail.com
 * @date 2018/7/5 12:20
 * @update_author
 * @update_time
 * @version V1.0
 * @exception
 */
class InspeckagePreferences(val context: Context) {
    companion object {
        const val IS_SIMULATION = "is_simulation"
        const val WIFI_NAME = "wifi_name"
        const val WIFI_BSSID = "wifi_bssid"
        const val WIFI_IP = "wifi_ip"
        const val APPS = "apps"
        const val TAG = "InspeckagePreferences"
    }

    // 获取WIFI信息配置
    val prefs: SharedPreferences = RemotePreferences(context,
            PREFERENCE_PROVIDER_AUTHORITY,
            PREFS_WIFI_INFO, true)


    fun putString(key: String, value: String) {
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.putString(key, value)
        editor.apply()
        Log.i(TAG, "putString: key=$key,value=$value")
    }

    fun putBoolean(key: String, value: Boolean) {
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.putBoolean(key, value)
        editor.apply()
        Log.i(TAG, "putBoolean: key=$key,value=$value")
    }

    fun putInt(key: String, default: Int):Int {
        val result = prefs.getInt(key, default)
        Log.i(TAG, "getInt: key=$key,result=$result")
        return result
    }

    fun getString(key: String,default:String = ""): String {
        val result = prefs.getString(key, default) ?: ""
        Log.i(TAG, "getString: key=$key,result=$result")
        return result
    }

    fun getBoolean(key: String, default:Boolean = false): Boolean {
        val result = prefs.getBoolean(key, default)
        Log.i(TAG, "getBoolean: key=$key,result=$result")
        return result
    }

    fun getInt(key: String,default: Int = -1): Int {
        val result = prefs.getInt(key, default)
        Log.i(TAG, "getInt: key=$key,result=$result")
        return result
    }

}