package mobi.acpm.inspeckage.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import mobi.acpm.inspeckage.Module;
import mobi.acpm.inspeckage.preferences.InspeckagePreferences;
import mobi.acpm.inspeckage.util.Config;

/**
 * Created by acpm on 20/01/16.
 */
public class InspeckageWebReceiver extends BroadcastReceiver {

    private Context mContext;
    public InspeckageWebReceiver(){

    }
    public InspeckageWebReceiver(Context ctx){
        mContext = ctx;
    }
    @Override
    public void onReceive(Context context, Intent intent) {

        InspeckagePreferences mPrefs = new InspeckagePreferences(context);

        String action = intent.getExtras().getString("action");

        if(action.equals("fileTree")){

            String sub1 = intent.getExtras().getString("tree");

            String script = "<script>\n" +
                    "$(document).ready(function() {\n" +
                    "\n" +
                    "    CollapsibleLists.apply();\n" +
                    "\n" +
                    "});\n" +
                    "</script>";

            String tree = script+"<ul class=\"collapsibleList\">"+sub1+"</ul>";
            mPrefs.putString(Config.SP_DATA_DIR_TREE, tree);
        }else if(action.equals("checkApp")){

            boolean isRunning = intent.getExtras().getBoolean("isRunning");
            int pid = intent.getExtras().getInt("PID");
            mPrefs.putBoolean(Config.SP_APP_IS_RUNNING, isRunning);
            mPrefs.putInt(Config.SP_APP_PID, pid);
        }else if(action.equals("clipboard")){

            String value = intent.getExtras().getString("value");

            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText("simple text", value);
            clipboard.setPrimaryClip(clip);
        }
    }
}