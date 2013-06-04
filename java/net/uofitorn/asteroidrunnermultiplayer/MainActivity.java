package net.uofitorn.asteroidrunnermultiplayer;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import net.uofitorn.asteroidrunnermultiplayer.LunarView.LunarThread;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private LunarThread lunarThread;
    private LunarView lunarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        LunarView lunarView = new LunarView(this);
        setContentView(lunarView);

        //setContentView(R.layout.lunar_layout);
        //lunarView = (LunarView) findViewById(R.id.lunar);
        lunarThread = lunarView.getThread();
        //setContentView(new LunarView(this));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
