package mobi.acpm.inspeckage.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import java.io.File;

import mobi.acpm.inspeckage.Module;
import mobi.acpm.inspeckage.R;
import mobi.acpm.inspeckage.preferences.InspeckagePreferences;
import mobi.acpm.inspeckage.util.Config;
import mobi.acpm.inspeckage.util.FileUtil;
import mobi.acpm.inspeckage.webserver.InspeckageService;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private InspeckagePreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = new InspeckagePreferences(this);
        
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        //main fragment
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        MainFragment mainFragment = new MainFragment(this);
        fragmentTransaction.replace(R.id.container, mainFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            boolean granted = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
            boolean grantedPhone = checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED;
            if (granted || grantedPhone) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE}, 0);
            }

            AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    AdvertisingIdClient.Info idInfo = null;
                    try {
                        idInfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
                    } catch (GooglePlayServicesNotAvailableException e) {
                        e.printStackTrace();
                    } catch (GooglePlayServicesRepairableException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String advertId = null;
                    try{
                        advertId = idInfo.getId();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    return advertId;
                }
                @Override
                protected void onPostExecute(String advertId) {
                    mPrefs.putString(Config.SP_ADS_ID,advertId);
                    //Toast.makeText(getApplicationContext(), advertId, Toast.LENGTH_SHORT).show();
                }
            };
            task.execute();

        }else{
            File inspeckage = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + Config.P_ROOT);
            if (!inspeckage.exists()) {
                inspeckage.mkdirs();
            }
            hideItem();
        }

        setWorldReadable();
    }

    @SuppressWarnings({"deprecation", "ResultOfMethodCallIgnored"})
    @SuppressLint({"SetWorldReadable", "WorldReadableFiles"})
    private void setWorldReadable() {
        String dir = getApplicationInfo().dataDir;
        File dataDir = new File(dir);
        File prefsDir = new File(dataDir, "shared_prefs");
        File prefsFile = new File(prefsDir, Module.PREFS+".xml");
        Log.d("Q_M", "if 设置 文件可读 之前" + prefsFile);
        if (prefsFile.exists()) {
            Log.d("Q_M", "for 循环设置 文件可读 之前");
            for (File file : new File[]{dataDir, prefsDir, prefsFile}) {
                file.setReadable(true, false);
                file.setExecutable(true, false);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    File inspeckage = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + Config.P_ROOT);
                    if (!inspeckage.exists()) {
                        inspeckage.mkdirs();
                    }
                } else {
                    // permission denied
                    //Util.showNotification(getApplicationContext(),"");
                }
                return;
            }
            case 1:{
                return;
            }
        }
    }

    private void hideItem()
    {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        Menu nav_Menu = navigationView.getMenu();
        nav_Menu.findItem(R.id.nav_auth).setVisible(false);
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        int count = getFragmentManager().getBackStackEntryCount();

        if (count == 1) {
            stopService();
            super.onBackPressed();
            //additional code
        } else {
            getFragmentManager().popBackStack();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        int id = item.getItemId();

        if (id == R.id.nav_clear) {

            clearAll();
            TextView txtAppSelected = (TextView) findViewById(R.id.txtAppSelected);
            if(txtAppSelected!=null) {
                txtAppSelected.setText("... ");
            }

        } else if (id == R.id.nav_close) {

            clearAll();
            stopService();
            super.finish();
            android.os.Process.killProcess(android.os.Process.myPid());

        } else if (id == R.id.nav_config) {

            ConfigFragment configFragment = new ConfigFragment(this);
            fragmentTransaction.replace(R.id.container, configFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();

        } else if (id == R.id.nav_auth) {

            AuthFragment authFragment = new AuthFragment(this);
            fragmentTransaction.replace(R.id.container, authFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();

        } else if (id == R.id.nav_share) {

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "https://github.com/ac-pm/Inspeckage");
            sendIntent.setType("text/plain");
            startActivity(sendIntent);

        }else{

            MainFragment mainFragment = new MainFragment();
            fragmentTransaction.replace(R.id.container, mainFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void stopService() {
        stopService(new Intent(this, InspeckageService.class));
    }

    private void clearAll(){

        Log.i("TAG", "clearAll: ");
//        if (!mPrefs.getBoolean(Config.SP_HAS_W_PERMISSION, false)) {
//            appPath = mPrefs.getString(Config.SP_DATA_DIR, "");
//        }

        mPrefs.putString(Config.SP_PROXY_HOST, "");
        mPrefs.putString(Config.SP_PROXY_PORT, "");
        mPrefs.putBoolean(Config.SP_SWITCH_PROXY, false);
        mPrefs.putBoolean(Config.SP_FLAG_SECURE, false);
        mPrefs.putBoolean(Config.SP_UNPINNING, false);
        mPrefs.putBoolean(Config.SP_EXPORTED, false);
        mPrefs.putBoolean(Config.SP_HAS_W_PERMISSION, true);
//        mPrefs.putString(Config.SP_SERVER_HOST, "");
//        mPrefs.putString(Config.SP_SERVER_PORT, "");
//        mPrefs.putString(Config.SP_SERVER_IP, "");
//        mPrefs.putString(Config.SP_SERVER_INTERFACES, "");

        mPrefs.putString(Config.SP_PACKAGE, "");
        mPrefs.putString(Config.SP_APP_NAME, "");
        mPrefs.putString(Config.SP_APP_VERSION, "");
        mPrefs.putString(Config.SP_DEBUGGABLE, "");
        mPrefs.putString(Config.SP_APK_DIR, "");
        mPrefs.putString(Config.SP_UID, "");
        mPrefs.putString(Config.SP_GIDS, "");
        mPrefs.putString(Config.SP_DATA_DIR, "");
        //white img
        mPrefs.putString(Config.SP_APP_ICON_BASE64, "iVBORw0KGgoAAAANSUhEUgAAABoAAAAbCAIAAADtdAg8AAAAA3NCSVQICAjb4U/gAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAAJUlEQVRIiWP8//8/A/UAExXNGjVu1LhR40aNGzVu1LhR44aScQDKygMz8IbG2QAAAABJRU5ErkJggg==");

        mPrefs.putString(Config.SP_EXP_ACTIVITIES, "");
        mPrefs.putString(Config.SP_N_EXP_ACTIVITIES, "");
        mPrefs.putString(Config.SP_REQ_PERMISSIONS, "");
        mPrefs.putString(Config.SP_APP_PERMISSIONS, "");
        mPrefs.putString(Config.SP_N_EXP_PROVIDER, "");
        mPrefs.putString(Config.SP_N_EXP_SERVICES, "");
        mPrefs.putString(Config.SP_N_EXP_BROADCAST, "");

        mPrefs.putString(Config.SP_EXP_SERVICES, "");
        mPrefs.putString(Config.SP_EXP_BROADCAST, "");
        mPrefs.putString(Config.SP_EXP_PROVIDER, "");
        mPrefs.putString(Config.SP_SHARED_LIB, "");

        mPrefs.putBoolean(Config.SP_APP_IS_RUNNING, false);
        mPrefs.putString(Config.SP_DATA_DIR_TREE, "");

        mPrefs.putString(Config.SP_USER_HOOKS, "");



        String appPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File root = new File(appPath + Config.P_ROOT);
        FileUtil.deleteRecursive(root);
        Log.i("TAG", "clearAll: end,root=" + root.getAbsolutePath());

    }
}
