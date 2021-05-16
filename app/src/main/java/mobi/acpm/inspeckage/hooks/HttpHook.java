package mobi.acpm.inspeckage.hooks;

import android.os.Build;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import mobi.acpm.inspeckage.Module;
import mobi.acpm.inspeckage.interceptor.HttpLoggingInterceptor;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

/**
 * Created by acpm on 14/02/16.
 */
public class HttpHook extends XC_MethodHook {

    public static final String TAG = "Inspeckage_Http:";

    public static void initAllHooks(final XC_LoadPackage.LoadPackageParam loadPackageParam) {

        try {
            final Class<?> httpUrlConnection = findClass("java.net.HttpURLConnection", loadPackageParam.classLoader);
            hookAllConstructors(httpUrlConnection, new XC_MethodHook() {

                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                    if (param.args.length != 1 || param.args[0].getClass() != URL.class) {
                        return;
                    }

                    XposedBridge.log(TAG + "HttpURLConnection: " + param.args[0] + "");
                }
            });
        } catch (Error e) {
            Module.logError(e);
        }

        XC_MethodHook RequestHook = new XC_MethodHook() {

            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                HttpURLConnection urlConn = (HttpURLConnection) param.thisObject;

                if (urlConn != null) {
                    StringBuilder sb = new StringBuilder();
                    boolean connected = (boolean)getObjectField(param.thisObject, "connected");

                    if(!connected){


                        Map<String, List<String>> properties = urlConn.getRequestProperties();
                        if (properties != null && properties.size() > 0) {


                            for (Map.Entry<String, List<String>> entry : properties.entrySet()){
                                sb.append(entry.getKey()+": "+entry.getValue()+", ");
                            }

/**
 Collection<List<String>> coll = properties.values();
 if (coll != null && coll.size() > 0) {
 for (List<String> ls : coll) {
 for (String s : ls) {
 sb.append(s+", ");
 }
 }
 }
 **/
                        }

                        DataOutputStream dos = (DataOutputStream) param.getResult();


                        XposedBridge.log(TAG + "REQUEST: method=" + urlConn.getRequestMethod() + " " +
                                "URL=" + urlConn.getURL().toString() + " " +
                                "Params=" + sb.toString());
                    }
                }

            }
        };


        XC_MethodHook ResponseHook = new XC_MethodHook() {

            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                HttpURLConnection urlConn = (HttpURLConnection) param.thisObject;

                if (urlConn != null) {
                    StringBuilder sb = new StringBuilder();
                    int code = urlConn.getResponseCode();
                    if(code==200){

                        Map<String, List<String>> properties = urlConn.getHeaderFields();
                        if (properties != null && properties.size() > 0) {

                            for (Map.Entry<String, List<String>> entry : properties.entrySet()) {
                                sb.append(entry.getKey() + ": " + entry.getValue() + ", ");
                            }
                        }
                    }

                    XposedBridge.log(TAG + "RESPONSE: method=" + urlConn.getRequestMethod() + " " +
                            "URL=" + urlConn.getURL().toString() + " " +
                            "Params=" + sb.toString());
                }

            }
        };

        try {
            final Class<?> okHttpClient = findClass("okhttp3.OkHttpClient", loadPackageParam.classLoader);
            if(okHttpClient != null) {

                //
                final Class<?> Request = findClass("okhttp3.Request", loadPackageParam.classLoader);

                Log.i(TAG, "initAllHooks: findAndHookMethod OkHttpClient");
                findAndHookMethod(okHttpClient, "newCall", Request, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log(TAG + "OkHttpClient: " + param.args[0].toString() + "");

                        Log.i(TAG, "findAndHookMethod,newCall:request=" + param.args[0].toString() );
                    }
                });
            }
        } catch (Error e) {
            Log.e(TAG, "initAllHooks: e="+e.getMessage() );
            Module.logError(e);
        }
        try {
            final Class<?> okHttpClientBuilder = findClass("okhttp3.OkHttpClient$Builder", loadPackageParam.classLoader);

            if (okHttpClientBuilder == null) {
                Log.w(TAG, "initAllHooks: okHttpClientBuilder == null");
                return;
            }
            Log.i(TAG, "initAllHooks: findAndHookMethod okHttpClientBuilder->build");


            XC_MethodHook xc_methodHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object thisObject = param.thisObject;

                    if (thisObject.getClass().getName().equals("okhttp3.OkHttpClient$Builder")) {
                        Log.i(TAG, "beforeHookedMethod okHttpClientBuilder addInterceptor = " + thisObject);
                        OkHttpClient.Builder builder = (OkHttpClient.Builder) thisObject;
                        builder.addInterceptor(new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                            @Override
                            public void log(String message) {

                                Log.i(TAG, "HttpLoggingInterceptor=" + message);
                                XposedBridge.log(TAG + "OkHttpClient: " + message);
                            }
                        }));
                    }


                }
            };
            XposedBridge.hookAllConstructors(okHttpClientBuilder, xc_methodHook);
            findAndHookMethod(okHttpClientBuilder, "build", xc_methodHook);
        } catch (Error e) {
            Log.e(TAG, "initAllHooks: e="+e.getMessage() );
            Module.logError(e);
        }


        try {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                findAndHookMethod("libcore.net.http.HttpURLConnectionImpl", loadPackageParam.classLoader, "getOutputStream", RequestHook);
            } else {
                //com.squareup.okhttp.internal.http.HttpURLConnectionImpl
                final Class<?> httpURLConnectionImpl = findClass("com.android.okhttp.internal.http.HttpURLConnectionImpl", loadPackageParam.classLoader);
                if(httpURLConnectionImpl != null) {
                    findAndHookMethod("com.android.okhttp.internal.http.HttpURLConnectionImpl", loadPackageParam.classLoader, "getOutputStream", RequestHook);
                    findAndHookMethod("com.android.okhttp.internal.http.HttpURLConnectionImpl", loadPackageParam.classLoader, "getInputStream", ResponseHook);
                }
            }
        } catch (Error e) {
            Module.logError(e);
        }

        findAndHookMethod(SSLContext.class, "init",
                KeyManager[].class, TrustManager[].class, SecureRandom.class, new XC_MethodHook() {

                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        KeyManager[] km = (KeyManager[]) param.args[0];
                        TrustManager[] tm_ = (TrustManager[]) param.args[1];

                        if (tm_ != null && tm_[0] != null) {
                            X509TrustManager tm = (X509TrustManager) tm_[0];
                            X509Certificate[] chain = new X509Certificate[]{};

                            XposedBridge.log(TAG + "Possible pinning.");
                            boolean check = false;
                            /**
                             try {
                             tm.checkClientTrusted(chain, "");
                             tm.checkServerTrusted(chain, "");

                             }catch (CertificateException ex){
                             check = true;
                             }

                             if(check){
                             XposedBridge.log(TAG + " Custom TrustManager - Possible pinning.");
                             }else {
                             XposedBridge.log(TAG + " App not verify SSL.");
                             }**/
                        }
                    }
                });
    }

}
