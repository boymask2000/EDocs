package com.boymask.edocs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class EDocs extends AppCompatActivity {

    private TextView serverAddr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edocs);
        serverAddr=(TextView)findViewById(R.id.serverAddr);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        // using toolbar as ActionBar
        setSupportActionBar(toolbar);


        View settings = findViewById(R.id.settings);


        // "on click" operations to be performed
        settings.setOnClickListener(new View.OnClickListener() {
            @Override

            // incrementing the value of textView
            public void onClick( View view ) {
                Intent intent = new Intent(EDocs.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        Button tesseraSanitaria = (Button)findViewById(R.id.tessera_sanitaria);
        tesseraSanitaria.setOnClickListener(new View.OnClickListener() {
            @Override

            // incrementing the value of textView
            public void onClick( View view ) {
                Intent intent = new Intent(EDocs.this, MainActivity.class);
                startActivity(intent);
            }
        });


        serverAddr.setText(getServerAddress(this));
    }
    public static String getServerAddress(Context ctx){
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(ctx /* Activity context */);
        String name = sharedPreferences.getString("server", "");
        return name;
    }
}