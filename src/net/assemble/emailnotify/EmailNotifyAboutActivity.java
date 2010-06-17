package net.assemble.emailnotify;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ToggleButton;

public class EmailNotifyAboutActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.about);

        try {
            PackageInfo pi = getPackageManager().getPackageInfo("net.assemble.emailnotify", 0);
            setTitle(getResources().getString(R.string.app_name) + " ver." + pi.versionName);
        } catch (NameNotFoundException e) {}
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                R.drawable.icon);

        // OK
        Button btn_ok = (Button) findViewById(R.id.ok);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
            
        });
        
        // Send report 
        Button btn_report = (Button) findViewById(R.id.report);
        btn_report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:yh1224@gmail.com"));
                intent.putExtra(Intent.EXTRA_SUBJECT, "About EmailNotify");
                startActivity(intent);
            }
        });
    }
}
