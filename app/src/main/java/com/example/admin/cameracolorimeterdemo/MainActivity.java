package com.example.admin.cameracolorimeterdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
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
        text.setMovementMethod(ScrollingMovementMethod.getInstance());

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
                            "com.example.admin.cameracolorimeterdemo.fileprovider",
                            outputFile);
                }
                else {
                    imageUri = Uri.fromFile(outputFile);
                }

//                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
//                toggleFlashLight(true);
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
//                        toggleFlashLight(false);
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
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        bitmap0.recycle();
        bitmap.recycle();
        super.onDestroy();
    }

    private void displayColorInfo(int centerX, int centerY) {
        if (Build.VERSION.SDK_INT >= 24) {
            isHigherSDK = true;
        }

        ArrayList<Point> points = new ArrayList<>();

        int row = 4;
        int col = 4;
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                points.add(new Point(centerX - 30 + 60 * j / col, centerY - 30 + 60 * i / row));
            }
        }

        ArrayList<Integer> lists = new ArrayList<>();
        for (Point p : points) {
            lists.add(bitmap.getPixel(p.x, p.y));
        }
        int sampleCount = lists.size();
        String output = "";
//        int[] average;
//        if (isHigherSDK) {
//            average = new int[5];
//        } else {
//            average = new int[4];
//        }

        float[] average;
        average = new float[3];

        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
//            Integer[] channels = getRGBChannels(lists.get(sampleIndex));
//            if (isHigherSDK) {
//                output += (sampleIndex + 1) + "- R: " + channels[0] + ", G: " + channels[1] + ", B: " + channels[2] + ", a: " + channels[3] + " (L: " + ((double) channels[4] / 1000) + ")" + "\n";
//                for (int channelIndex = 0; channelIndex < average.length; channelIndex++) {
//                    average[channelIndex] += channels[channelIndex];
//                }
//            } else {
//                output += (sampleIndex + 1) + ": " + channels[0] + ", " + channels[1] + ", " + channels[2] + ", " + channels[3] + "\n";
//                for (int channelIndex = 0; channelIndex < average.length; channelIndex++) {
//                    average[channelIndex] += channels[channelIndex];
//                }
//            }
//        }
//        for (int channelIndex = 0; channelIndex < average.length; channelIndex++) {
//            average[channelIndex] /= sampleCount;
//        }
//        if (isHigherSDK) {
//            output += "Average- R: " + average[0] + ", G: " + average[1]  + ", B: " + average[2] + ", a: " + average[3]  + " (L: " + ((double) average[4] / 1000) + ")";
//        } else {
//            output += "Average: " + average[0] + ", " + average[1] + ", " + average[2] + ", " + average[3] ;
//        }
//        text.setText(output);
//        text.setTextColor(Color.argb(average[3], average[0], average[1], average[2]));

            float[] channels = getHSVChannels(lists.get(sampleIndex));
            output += (sampleIndex + 1) + "- H: " + channels[0] + ", S: " + channels[1] + ", V: " + channels[2] + "\n";
            for (int channelIndex = 0; channelIndex < average.length; channelIndex++) {
                average[channelIndex] += channels[channelIndex];
            }
        }
        for (int channelIndex = 0; channelIndex < average.length; channelIndex++) {
            average[channelIndex] /= sampleCount;
        }
        output += "Average- H: " + average[0] + ", S: " + average[1] + ", V: " + average[2];
        text.setText(output);
        text.setTextColor(Color.HSVToColor(average));

        try {
            paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStrokeWidth(5.0f);
            paint.setTextSize(15.0f);
            paint.setDither(true);
            int pointNum = 1;
            for (Point p : points) {
                canvas.drawPoint(p.x, p.y, paint);
                canvas.drawText(String.valueOf(pointNum++), p.x + 2, p.y - 2, paint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Integer[] getRGBChannels(int color) {
        ArrayList<Integer> channelsList = new ArrayList<>();
        int r = Color.red(color);
        channelsList.add(r);
        int g = Color.green(color);
        channelsList.add(g);
        int b = Color.blue(color);
        channelsList.add(b);
        int a = Color.alpha(color);
        channelsList.add(a);
        if (Build.VERSION.SDK_INT >= 24) {
            float l = Color.luminance(color) * 1000;
            channelsList.add((int) l);
        }
        Integer[] channelsArray = new Integer[channelsList.size()];

        return channelsList.toArray(channelsArray);
    }

    private float[] getHSVChannels(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv;
    }

    private void clearCanvas() {
        bitmap = bitmap0.copy(Bitmap.Config.ARGB_8888, true);
        pic.setImageBitmap(bitmap);
        canvas = new Canvas(bitmap);
    }

//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void toggleFlashLight(boolean openOrClose) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                //获取CameraManager
                CameraManager mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
                //获取当前手机所有摄像头设备ID
                String[] ids  = mCameraManager.getCameraIdList();
                for (String id : ids) {
                    CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
                    //查询该摄像头组件是否包含闪光灯
                    Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                    if (flashAvailable != null && flashAvailable
                            && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        //打开或关闭手电筒
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            mCameraManager.setTorchMode(id, openOrClose);
                        }
                    }
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
