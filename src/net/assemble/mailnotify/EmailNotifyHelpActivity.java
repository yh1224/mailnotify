package net.assemble.mailnotify;

import java.io.IOException;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import net.assemble.android.AssetsReader;

public class EmailNotifyHelpActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);

        AssetsReader ar = new AssetsReader(this);
        try {
            String str = ar.getText("description.txt");
            TextView text = (TextView) findViewById(R.id.description);
            text.setText(Html.fromHtml(str, new Html.ImageGetter() {
                @Override
                public Drawable getDrawable(String source) {
                    Drawable d = getResources().getDrawable(R.drawable.icon);
                    d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                    return d;
                }
            }, null));
        } catch (IOException e) {}
    }

}
