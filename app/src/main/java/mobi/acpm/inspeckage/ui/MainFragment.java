package mobi.acpm.inspeckage.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import mobi.acpm.inspeckage.Module;
import mobi.acpm.inspeckage.R;
import mobi.acpm.inspeckage.preferences.InspeckagePreferences;
import mobi.acpm.inspeckage.util.Config;
import mobi.acpm.inspeckage.util.FileUtil;
import mobi.acpm.inspeckage.util.PackageDetail;
import mobi.acpm.inspeckage.util.Util;
import mobi.acpm.inspeckage.webserver.InspeckageService;
import mobi.acpm.inspeckage.webserver.WebServer;


public class MainFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private Context context;
    private Activity mainActivity;
    private InspeckagePreferences mPrefs;
    private PackageDetail pd;

    @SuppressLint("ValidFragment")
    public MainFragment(Activity act) {
        mainActivity = act;
        context = mainActivity.getApplicationContext();
    }

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mPrefs = new InspeckagePreferences(context);

            String host = null;
            if(!mPrefs.getString(Config.SP_SERVER_HOST, "All interfaces").equals("All interfaces")){
                host = mPrefs.getString(Config.SP_SERVER_HOST, "All interfaces");
            }
            startService(host,mPrefs.getInt(Config.SP_SERVER_PORT, 8008));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // / Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        ExpandableListView mExpandableList = (ExpandableListView) view.findViewById(R.id.appsListView);

        loadListView(view);

        TextView txtModule = (TextView) view.findViewById(R.id.txtModule);
        if (WebServer.isModuleEnabled()) {
            txtModule.setText(R.string.module_enabled);
            txtModule.setBackgroundColor(Color.TRANSPARENT);
        }

        TextView txtServer = (TextView) view.findViewById(R.id.txtServer);
        if (Util.isMyServiceRunning(context, InspeckageService.class)) {
            txtServer.setText(R.string.server_started);
            txtServer.setBackgroundColor(Color.TRANSPARENT);
        }

        mExpandableList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                TextView txtPackage = (TextView) v.findViewById(R.id.txtListPkg);
                TextView txtAppName = (TextView) v.findViewById(R.id.txtListItem);


                loadSelectedApp(txtPackage.getText().toString());

                TextView txtAppSelected = (TextView) view.findViewById(R.id.txtAppSelected);
                txtAppSelected.setText(">>> " + txtPackage.getText().toString());

                Toast.makeText(context, "" + txtAppName.getText().toString(), Toast.LENGTH_SHORT).show();
                loadListView(view);

                return true;
            }
        });

        Switch mSwitch = (Switch) view.findViewById(R.id.only_user_app_switch);
        Boolean sw = mPrefs.getBoolean(Config.SP_SWITCH_OUA, true);
        mSwitch.setChecked(sw);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {


                //Only User App
                if (isChecked) {
                    mPrefs.putBoolean(Config.SP_SWITCH_OUA, true);
                } else {
                    mPrefs.putBoolean(Config.SP_SWITCH_OUA, false);
                }
                loadListView(view);
            }
        });

        final Button button = (Button) view.findViewById(R.id.btnLaunchApp);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(pd==null){
                    pd = new PackageDetail(context, mPrefs.getString(Config.SP_PACKAGE,""));
                }
                Intent i = pd.getLaunchIntent();
                if(i!=null) {
                    startActivity(i);
                }else{
                    Toast.makeText(context, "Launch Intent not found.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        loadInterfaces();

        String scheme = "http://";
        if(mPrefs.getBoolean(Config.SP_SWITCH_AUTH, false)) {
            scheme = "https://";
        }

        String port = String.valueOf(mPrefs.getInt(Config.SP_SERVER_PORT, 8008));
        String host = "";
        if(mPrefs.getString(Config.SP_SERVER_HOST, "All interfaces").equals("All interfaces")){
            String[] adds = mPrefs.getString(Config.SP_SERVER_INTERFACES, "--").split(",");
            for(int i=0; i<adds.length; i++){
                if(!adds[i].equals("All interfaces"))
                    host = host + scheme + adds[i] + ":" + port+"\n";
            }
        }else{
            String ip = mPrefs.getString(Config.SP_SERVER_HOST, "127.0.0.1");
            host = scheme + ip + ":" + port;

            mPrefs.putString(Config.SP_SERVER_IP, ip);
        }

        TextView txtHost = (TextView) view.findViewById(R.id.txtHost);
        txtHost.setText(host);

        TextView txtAdb = (TextView) view.findViewById(R.id.txtAdb);
        txtAdb.setText("adb forward tcp:"+port+" tcp:"+port);

        TextView txtAppSelected = (TextView) view.findViewById(R.id.txtAppSelected);
        txtAppSelected.setText(">>> " + mPrefs.getString(Config.SP_PACKAGE, "..."));


        return view;
    }

    public void loadInterfaces(){

        StringBuilder sb = new StringBuilder();
        sb.append("All interfaces,");
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface netInterface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = netInterface.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    String address = inetAddress.getHostAddress();
                    boolean isIPv4 = address.indexOf(':') < 0;
                    if (isIPv4) {
                        sb.append(address+",");
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("Inspeckage_Error", ex.toString());
        }
        mPrefs.putString(Config.SP_SERVER_INTERFACES, sb.toString().substring(0,sb.length()-1));
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    //----------------------------------------------METHODS--------------------------------------

    public void startService(String host, int port) {
        Intent i = new Intent(context, InspeckageService.class);
        i.putExtra("port", port);
        i.putExtra("host", host);

        context.startService(i);
    }

    public void stopService() {
        context.stopService(new Intent(context, InspeckageService.class));
    }

    private ArrayList<ExpandableListItem> getInstalledApps() {
        ArrayList<ExpandableListItem> appsList = new ArrayList<>();
        List<PackageInfo> packs = context.getPackageManager().getInstalledPackages(0);

        for (int i = 0; i < packs.size(); i++) {

            android.content.pm.PackageInfo p = packs.get(i);
            // Installed by user
            if (mPrefs.getBoolean(Config.SP_SWITCH_OUA, true) ? (p.applicationInfo.flags & 129) == 0 : true) {
                ExpandableListItem pInfo = new ExpandableListItem();
                pInfo.setAppName(p.applicationInfo.loadLabel(context.getPackageManager()).toString());
                pInfo.setPckName(p.packageName);
                pInfo.setIcon(p.applicationInfo.loadIcon(context.getPackageManager()));

                String pack = mPrefs.getString(Config.SP_PACKAGE, "");

                if (p.packageName.trim().equals(pack.trim())) {
                    pInfo.setSelected(true);
                }

                appsList.add(pInfo);
            }
        }
        return appsList;
    }

    private void loadListView(View view) {
        List<String> mListDataHeader = new ArrayList<String>();
        mListDataHeader.add(context.getString(R.string.fragment_config_choose));

        HashMap<String, List<ExpandableListItem>> mListDataChild = new HashMap<String, List<ExpandableListItem>>();

        ArrayList<ExpandableListItem> mApps = getInstalledApps();
        Collections.sort(mApps, new Comparator<ExpandableListItem>() {

            public int compare(ExpandableListItem o1, ExpandableListItem o2) {
                return o1.getAppName().compareTo(o2.getAppName());
            }
        });

        ExpandableListView appList = (ExpandableListView) view.findViewById(R.id.appsListView);


        mListDataChild.put(mListDataHeader.get(0), mApps);
        appList.setAdapter(new ExpandableListAdapter(getActivity(), mListDataHeader, mListDataChild));

    }

    private void loadSelectedApp(String pkg) {

        //this put has to come before the PackageDetail
        mPrefs.putString(Config.SP_PACKAGE, pkg);
        

        pd = new PackageDetail(context, pkg);

        mPrefs.putBoolean(Config.SP_HAS_W_PERMISSION, false);
        if (pd.getRequestedPermissions().contains("android.permission.WRITE_EXTERNAL_STORAGE") &&
                Build.VERSION.SDK_INT < 23) {
            mPrefs.putBoolean(Config.SP_HAS_W_PERMISSION, true);
        }

        mPrefs.putString(Config.SP_APP_NAME, pd.getAppName());
        mPrefs.putString(Config.SP_APP_ICON_BASE64, pd.getIconBase64());
        mPrefs.putString(Config.SP_PROCESS_NAME, pd.getProcessName());
        mPrefs.putString(Config.SP_APP_VERSION, pd.getVersion());
        mPrefs.putString(Config.SP_DEBUGGABLE, pd.isDebuggable());
        mPrefs.putString(Config.SP_ALLOW_BACKUP, pd.allowBackup());
        mPrefs.putString(Config.SP_APK_DIR, pd.getApkDir());
        mPrefs.putString(Config.SP_UID, pd.getUID());
        mPrefs.putString(Config.SP_GIDS, pd.getGIDs());

        //todo 这个目录要多进程可以访问才行
        String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();

        //todo 换app，清除历史
//        File file = new File(absolutePath);
        clear();

        mPrefs.putString(Config.SP_DATA_DIR, absolutePath);

        mPrefs.putString(Config.SP_REQ_PERMISSIONS, pd.getRequestedPermissions());
        mPrefs.putString(Config.SP_APP_PERMISSIONS, pd.getAppPermissions());

        mPrefs.putString(Config.SP_EXP_ACTIVITIES, pd.getExportedActivities());
        mPrefs.putString(Config.SP_N_EXP_ACTIVITIES, pd.getNonExportedActivities());

        mPrefs.putString(Config.SP_EXP_SERVICES, pd.getExportedServices());
        mPrefs.putString(Config.SP_N_EXP_SERVICES, pd.getNonExportedServices());

        mPrefs.putString(Config.SP_EXP_BROADCAST, pd.getExportedBroadcastReceivers());
        mPrefs.putString(Config.SP_N_EXP_BROADCAST, pd.getNonExportedBroadcastReceivers());

        mPrefs.putString(Config.SP_EXP_PROVIDER, pd.getExportedContentProvider());
        mPrefs.putString(Config.SP_N_EXP_PROVIDER, pd.getNonExportedContentProvider());

        mPrefs.putString(Config.SP_SHARED_LIB, pd.getSharedLibraries());

        mPrefs.putBoolean(Config.SP_APP_IS_RUNNING, false);
        mPrefs.putString(Config.SP_DATA_DIR_TREE, "");

        //test
        //mPrefs.putString(Config.SP_REPLACE_SP, "limitEventUsage,true");


        //resolve this problem
        if (pd.getRequestedPermissions().contains("android.permission.WRITE_EXTERNAL_STORAGE")) {
            pd.extractInfoToFile();
        }

    }

    private void clear(){
        String appPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File root = new File(appPath + Config.P_ROOT);
        FileUtil.deleteRecursive(root);
        Log.i("TAG", "clearAll: end,root=" + root.getAbsolutePath());
    }


}
