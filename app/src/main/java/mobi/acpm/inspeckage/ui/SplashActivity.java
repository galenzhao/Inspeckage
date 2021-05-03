package mobi.acpm.inspeckage.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import mobi.acpm.inspeckage.Module;
import mobi.acpm.inspeckage.R;
import mobi.acpm.inspeckage.util.FileUtil;

public class SplashActivity extends AppCompatActivity {

    private static int TIME_OUT = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        FileUtil.fixSharedPreference(this);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, TIME_OUT);
    }


}
