package net.assemble.emailnotify;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import net.assemble.emailnotify.R;

public class EmailNotifyActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button btn_mail = (Button) findViewById(R.id.btn_report);
        btn_mail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:yh1224@gmail.com"));
                i.putExtra(Intent.EXTRA_SUBJECT, "About EmailNotify");
                startActivity(i);
            }
        });

        Button btn_ok = (Button) findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        new EmailObserver(this);
    }

}
