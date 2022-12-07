package com.example.wheelboardapplication;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    TextView welcomeText;
    TextView carText;
    ImageButton junoButton;
    ImageButton idraButton;
    String message = "No usb attached";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        welcomeText = findViewById(R.id.welcomeText);
        carText = findViewById(R.id.carSelected);
        welcomeText.setText(message);

        junoButton = findViewById(R.id.junoButton);
        idraButton = findViewById(R.id.idraButton);

        junoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                carText.setText("Juno select");
            }
        });

        idraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                carText.setText("Idra select");
                Intent intent = new Intent(MainActivity.this, IdraActivity.class);
                startActivity(intent);
            }

        });

    }
}