package mobi.acpm.inspeckage

import android.app.Application
import android.content.Context
import android.util.Log
import de.robv.android.xposed.*
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import mobi.acpm.inspeckage.hooks.*
import mobi.acpm.inspeckage.hooks.entities.LocationHook
import mobi.acpm.inspeckage.preferences.InspeckagePreferences
import mobi.acpm.inspeckage.util.Config
import mobi.acpm.inspeckage.util.Config.SP_DATA_DIR
import mobi.acpm.inspeckage.util.FileType
import mobi.acpm.inspeckage.util.FileUtil
import java.io.File

/**
 * Created by acpm on 16/11/15.
 */
class Module : XC_MethodHook(), IXposedHookLoadPackage, IXposedHookZygoteInit {
    @Throws(Throwable::class)
    override fun initZygote(startupParam: StartupParam) {
    }

    @Throws(Throwable::class)
    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        

        //check if this module is enable
        if (loadPackageParam.packageName == "mobi.acpm.inspeckage") {
            XposedHelpers.findAndHookMethod("mobi.acpm.inspeckage.webserver.WebServer", loadPackageParam.classLoader, "isModuleEnabled", XC_MethodReplacement.returnConstant(true))

            //workaround to bypass MODE_PRIVATE of shared_prefs
//            findAndHookMethod("android.app.SharedPreferencesImpl.EditorImpl", loadPackageParam.classLoader, "notifyListeners",
//                    "android.app.SharedPreferencesImpl.MemoryCommitResult", new XC_MethodHook() {
//
//                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                            //workaround to bypass the concurrency (io)
//                            Handler handler = new Handler(Looper.getMainLooper());
//                            handler.postDelayed(new Runnable() {
//                                public void run() {
//                                    Context context = (Context) AndroidAppHelper.currentApplication();
//                                    FileUtil.fixSharedPreference(context);
//                                }
//                            }, 1000);
//                        }
//                    });
        }
        if (loadPackageParam.packageName == "mobi.acpm.inspeckage") return

        XposedHelpers.findAndHookMethod(Application::class.java, "attach", Context::class.java, object : XC_MethodHook() {
            // 方法执行前hook
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: MethodHookParam) {
            }

            // 方法执行后hook
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: MethodHookParam) {
                // 获取上下文
                val mContext = param.args[0] as Context
                // 获取配置
                val sPrefs = InspeckagePreferences(mContext)

                val dataDir = sPrefs.getString(SP_DATA_DIR, "") + Config.P_ROOT

                val savePackageName = sPrefs.getString("package")
                //absolutePath=/data/user/0/com.lanshifu.baselibraryktx/Inspeckage
                if (loadPackageParam.packageName != savePackageName) {
                    return
                }
                Log.i(TAG, "handleLoadPackage: packageName=" + loadPackageParam.packageName + ",savePackageName=" + savePackageName + ",dataDir" + dataDir)


                //inspeckage needs access to the files
                val folder = File(dataDir)
                folder.setExecutable(true, false)

                // todo 一条log会回调3次
                XposedHelpers.findAndHookMethod("android.util.Log", loadPackageParam.classLoader, "i",
                        String::class.java, String::class.java, object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val tag = param.args[0]
                        if (param.args[0] === "Xposed" || param.args[0] == "EdXposed-Bridge") {
                            val log = param.args[1] as String
                            var ft: FileType? = null
                            if (log.contains(SharedPrefsHook.TAG)) { //5
                                ft = FileType.PREFS
                            } else if (log.contains(CryptoHook.TAG)) { //2
                                ft = FileType.CRYPTO
                            } else if (log.contains(HashHook.TAG)) { //3
                                ft = FileType.HASH
                            } else if (log.contains(SQLiteHook.TAG)) { //6
                                ft = FileType.SQLITE
                            } else if (log.contains(ClipboardHook.TAG)) { //1
                                ft = FileType.CLIPB
                            } else if (log.contains(IPCHook.TAG)) { //4
                                ft = FileType.IPC
                            } else if (log.contains(WebViewHook.TAG)) { //8
                                ft = FileType.WEBVIEW
                            } else if (log.contains(FileSystemHook.TAG)) { //9
                                ft = FileType.FILESYSTEM
                            } else if (log.contains(MiscHook.TAG)) { //10
                                ft = FileType.MISC
                            } else if (log.contains(SerializationHook.TAG)) { //10
                                ft = FileType.SERIALIZATION
                            } else if (log.contains(HttpHook.TAG)) { //10
                                ft = FileType.HTTP
                            } else if (log.contains(UserHooks.TAG)) { //10
                                ft = FileType.USERHOOKS
                            }
                            if (ft != null) {
                                FileUtil.writeToFile(sPrefs, log, ft, "")
                            }
                        }
                    }
                })
                UIHook.initAllHooks(loadPackageParam)
                if (sPrefs.getBoolean(Config.SP_TAB_ENABLE_HTTP, true)) {
                    HttpHook.initAllHooks(loadPackageParam)
                }
                if (sPrefs.getBoolean(Config.SP_TAB_ENABLE_MISC, true)) {
                    MiscHook.initAllHooks(loadPackageParam)
                    ClipboardHook.initAllHooks(loadPackageParam)
                }
                if (sPrefs.getBoolean(Config.SP_TAB_ENABLE_WV, true)) {
                    WebViewHook.initAllHooks(loadPackageParam)
                }
                if (sPrefs.getBoolean(Config.SP_TAB_ENABLE_CRYPTO, true)) {
                    CryptoHook.initAllHooks(loadPackageParam)
                }
                //闪退
//                if (sPrefs.getBoolean(Config.SP_TAB_ENABLE_FS, true)) {
//                    FileSystemHook.initAllHooks(loadPackageParam)
//                }
                FlagSecureHook.initAllHooks(loadPackageParam, sPrefs)
                if (sPrefs.getBoolean(Config.SP_TAB_ENABLE_HASH, true)) {
                    HashHook.initAllHooks(loadPackageParam)
                }
                if (sPrefs.getBoolean(Config.SP_TAB_ENABLE_IPC, true)) {
                    IPCHook.initAllHooks(loadPackageParam)
                }

                //自定义hook
                if (sPrefs.getBoolean(Config.SP_TAB_ENABLE_PHOOKS, true)) {
                    UserHooks.initAllHooks(loadPackageParam, sPrefs)
                }


                if (sPrefs.getBoolean(Config.SP_TAB_ENABLE_SHAREDP, true)) {
                    SharedPrefsHook.initAllHooks(loadPackageParam,sPrefs)
                }
                if (sPrefs.getBoolean(Config.SP_TAB_ENABLE_SQLITE, true)) {
                    SQLiteHook.initAllHooks(loadPackageParam)
                }
                SSLPinningHook.initAllHooks(loadPackageParam, sPrefs) // --

                //奔溃
                if (sPrefs.getBoolean(Config.SP_TAB_ENABLE_SERIALIZATION, true)) {
//                    SerializationHook.initAllHooks(loadPackageParam)
                }

                if (sPrefs.getBoolean(Config.SP_GEOLOCATION_SW, false)) {
                    LocationHook.initAllHooks(loadPackageParam, sPrefs)
                }

                //hook设备相关
                FingerprintHook.initAllHooks(loadPackageParam, sPrefs)

                //DexUtil.saveClassesWithMethodsJson(loadPackageParam, sPrefs);

                ProxyHook.initAllHooks(loadPackageParam,sPrefs) // --
                Log.i(TAG, "handleLoadPackage: end")
            }
        })


        
    }

    companion object {
        const val PREFS = "InspeckagePrefs"
        const val TAG = "Inspeckage_Module:"
        const val ERROR = "Inspeckage_Error"
        @JvmStatic
        fun logError(e: Error) {
            XposedBridge.log(ERROR + " " + e.message)
            Log.e(TAG, "logError: ${e.message}" )
        }
    }
}