package com.example.admin.cameracolorimeterdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static final int PIC_TAKEN = 1;

    private ImageView pic;
    private Uri imageUri;
    private TextView text;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button takePic = (Button) findViewById(R.id.take_picture);
        pic = (ImageView) findViewById(R.id.picture);
        text = (TextView) findViewById(R.id.rgb);

        takePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File outputFile = new File(getExternalCacheDir(), "output_img.jpg");
                try {
                    if (outputFile.exists()) {
                        outputFile.delete();
                    }
                    outputFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (Build.VERSION.SDK_INT >= 24) {
                    imageUri = FileProvider.getUriForFile(MainActivity.this,
                            "com.example.felixyin.camera_album_test.fileprovider",
                            outputFile);
                }
                else {
                    imageUri = Uri.fromFile(outputFile);
                }

                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, PIC_TAKEN);
            }
        });

        pic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int color = bitmap.getPixel(x, y);
                    int r = Color.red(color);
                    int g = Color.green(color);
                    int b = Color.blue(color);
                    int a = Color.alpha(color);

                    text.setText(r + ", " + g + ", " + b + ", " + a);
                    if (android.os.Build.VERSION.SDK_INT >= 24) {
                        float l = Color.luminance(color);
                        text.setText(r + ", " + g + ", " + b + ", " + a + " (" + l + ")");
                    }
                    text.setTextColor(Color.argb(a, r, g, b));
                }
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PIC_TAKEN:
                if (resultCode == RESULT_OK) {
                    try {
//                        Log.d("MainActivity", "in switch");
                        bitmap = BitmapFactory.decodeStream(getContentResolver().
                                openInputStream(imageUri));
                        pic.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }
}
