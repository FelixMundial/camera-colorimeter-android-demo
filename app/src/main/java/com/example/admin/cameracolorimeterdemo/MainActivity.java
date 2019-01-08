package com.example.admin.cameracolorimeterdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
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
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final int PIC_TAKEN = 1;

    private ImageView pic;
    private Uri imageUri;
    private TextView text;
    Bitmap bitmap0, bitmap;
    Canvas canvas;
    Paint paint;
    float bmpWidth;
    float bmpHeight;

    boolean isHigherSDK = false;

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
                    throw new RuntimeException(e);
                }

                if (Build.VERSION.SDK_INT >= 24) {
                    imageUri = FileProvider.getUriForFile(MainActivity.this,
                            "com.example.admin.cameracolorimeterdemo.fileprovider",
                            outputFile);
                }
                else {
                    imageUri = Uri.fromFile(outputFile);
                }

//                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, PIC_TAKEN);
            }
        });

        pic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                clearCanvas();
                double width = v.getWidth(); // 640
                double height = v.getHeight(); // 480
                double rawX = event.getX();
                double rawY = event.getY();
                int x = (int) (rawX * (bmpWidth / width));
                int y = (int) (rawY * (bmpHeight / height));
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    displayColorInfo(x, y);

//                    int color = bitmap.getPixel(x, y);
//                    int r = Color.red(color);
//                    int g = Color.green(color);
//                    int b = Color.blue(color);
//                    int a = Color.alpha(color);
//
//                    if (android.os.Build.VERSION.SDK_INT >= 24) {
//                        float l = Color.luminance(color);
//                        text.setText("Coordinate: " + x + ", " + y + "\n" + r + ", " + g + ", " + b + ", " + a + " (" + l + ")");
//                    } else {
//                        text.setText("Coordinate: " + x + ", " + y + "\n" + r + ", " + g + ", " + b + ", " + a);
//                    }
//                    text.setTextColor(Color.argb(a, r, g, b));
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
                        bitmap0 = BitmapFactory.decodeStream(getContentResolver().
                                openInputStream(imageUri));
                        bitmap = bitmap0.copy(Bitmap.Config.ARGB_8888, true);
                        pic.setImageBitmap(bitmap);
                        canvas = new Canvas(bitmap);

                        bmpWidth = bitmap.getWidth(); // 2976
                        bmpHeight = bitmap.getHeight(); // 3968
                        int centerX = (int) (bmpWidth / 2);
                        int centerY = (int) (bmpHeight / 2);
                        displayColorInfo(centerX, centerY);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy()
    {
        bitmap.recycle();
        super.onDestroy();
    }

    private void displayColorInfo(int centerX, int centerY) {
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            isHigherSDK = true;
        }

        ArrayList<Point> points = new ArrayList<>();
        points.add(new Point(centerX - 30, centerY - 30));
        points.add(new Point(centerX + 30, centerY - 30));
        points.add(new Point(centerX - 30, centerY + 30));
        points.add(new Point(centerX + 30, centerY + 30));

        ArrayList<Integer> lists = new ArrayList<>();
        for (Point p : points) {
            lists.add(bitmap.getPixel(p.x, p.y));
        }
        int sampleCount = lists.size();
        String output = "";
        int[] average;
        if (isHigherSDK) {
            average = new int[5];
        } else {
            average = new int[4];
        }

        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            Integer[] channels = getColorChannels(lists.get(sampleIndex));
            if (isHigherSDK) {
                output += (sampleIndex + 1) + ": " + channels[0] + ", " + channels[1] + ", " + channels[2] + ", " + channels[3] + " (" + ((double) channels[4] / 1000) + ")" + "\n";
                for (int channelIndex = 0; channelIndex < average.length; channelIndex++) {
                    average[channelIndex] += channels[channelIndex];
                }
            } else {
                output += (sampleIndex + 1) + ": " + channels[0] + ", " + channels[1] + ", " + channels[2] + ", " + channels[3] + "\n";
                for (int channelIndex = 0; channelIndex < average.length; channelIndex++) {
                    average[channelIndex] += channels[channelIndex];
                }
            }
        }
        for (int channelIndex = 0; channelIndex < average.length; channelIndex++) {
            average[channelIndex] /= sampleCount;
        }
        if (isHigherSDK) {
            output += "Average: " + average[0] + ", " + average[1]  + ", " + average[2] + ", " + average[3]  + " (" + ((double) average[4] / 1000) + ")";
        } else {
            output += "Average: " + average[0] + ", " + average[1] + ", " + average[2] + ", " + average[3] ;
        }
        text.setText(output);
        text.setTextColor(Color.argb(average[3], average[0], average[1], average[2]));

        try {
            paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStrokeWidth(30.0f);
            paint.setTextSize(100.0f);
            paint.setDither(true);
            int pointNum = 1;
            for (Point p : points) {
                canvas.drawPoint(p.x, p.y, paint);
                canvas.drawText(String.valueOf(pointNum++), p.x + 10, p.y - 10, paint);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Integer[] getColorChannels(int color) {
        ArrayList<Integer> channelsList = new ArrayList<>();
        int r = Color.red(color);
        channelsList.add(r);
        int g = Color.green(color);
        channelsList.add(g);
        int b = Color.blue(color);
        channelsList.add(b);
        int a = Color.alpha(color);
        channelsList.add(a);
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            float l = Color.luminance(color) * 1000;
            channelsList.add((int) l);
        }
        Integer[] channelsArray = new Integer[channelsList.size()];
        return channelsList.toArray(channelsArray);
    }

    private void clearCanvas() {
//        pic.invalidate();
        bitmap = bitmap0.copy(Bitmap.Config.ARGB_8888, true);
        pic.setImageBitmap(bitmap);
        canvas = new Canvas(bitmap);
    }
}
