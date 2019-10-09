package com.example.admin.cameracolorimeterdemo;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final int PIC_TAKEN = 1;
    public static final int PIC_RETRIEVED = 2;

    public static final int SAMPLE_NUMBER_ROOT = 4; // 16 sample points

    public static Resources resources;

    private ImageView sampleImageView;
    private Uri imageUri;
    private TextView HSVHTextView, HSVSTextView, HSVVTextView;
    private Bitmap decodedBitmap, workingBitmap;
    private Canvas canvas;
    private Paint paint;
    private float bmpWidth;
    private float bmpHeight;
    private File outputFile;
    private TextView alertTextView;
    private LinearLayout outerLinearLayout;
    private SwipeLayout swipeLayout;
    private LinearLayout swipeSurfaceLinearLayout;
    private LinearLayout spinnerLinearLayout;
    private TextView swipeSurfaceTextView;
    private Spinner spinner;
    private TextView concentrationTextView;

    private float[] average;

    private static StringBuilder stringBuilder = new StringBuilder();

    private static SparseArray<String> analyteCandidates = new SparseArray<>();

    private static int selectedAnalyte = 0; // 0 for glucose, 1 for uric acid
    private static boolean isInitialized = false;

    private static CustomSwiperListener swipeListener = new CustomSwiperListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        FadingActionBarHelper helper = new FadingActionBarHelper()
//                .actionBarBackground(R.drawable.ab_background)
//                .headerLayout(R.layout.header)
//                .contentLayout(R.layout.activity_main);
//        setContentView(helper.createView(this));
//        helper.initActionBar(this);

        Button takePicBtn = (Button) findViewById(R.id.take_picture);
        Button retrievePicBtn = (Button) findViewById(R.id.retrieve_picture);
        sampleImageView = (ImageView) findViewById(R.id.picture);
        HSVHTextView = (TextView) findViewById(R.id.hsvh);
        HSVSTextView = (TextView) findViewById(R.id.hsvs);
        HSVVTextView = (TextView) findViewById(R.id.hsvv);
        alertTextView = (TextView) findViewById(R.id.alert);

        outerLinearLayout = (LinearLayout) findViewById(R.id.linear_layout_outer);
        swipeLayout = (SwipeLayout) findViewById(R.id.swipe_layout_hsv);
        swipeSurfaceLinearLayout = (LinearLayout) findViewById(R.id.linear_layout_hsv_surface);
        swipeSurfaceTextView = (TextView) findViewById(R.id.text_swipe_layout_surface);
        spinnerLinearLayout = (LinearLayout) findViewById(R.id.linear_layout_spinner);
        TextView spinnerTextView = (TextView) findViewById(R.id.spinner_title);
        spinner = (Spinner) findViewById(R.id.spinner);
        concentrationTextView = (TextView) findViewById(R.id.conc);

        alertTextView.setVisibility(View.GONE);
        swipeSurfaceTextView.setVisibility(View.GONE);
        spinnerLinearLayout.setVisibility(View.GONE);

        resources = getResources();
        String[] analyteArray = resources.getStringArray(R.array.analyte);
        for (int i = 0; i < analyteArray.length; i++) {
            analyteCandidates.put(i, analyteArray[i]);
        }

        takePicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName = UUID.randomUUID().toString().replaceAll("-", "") + ".jpg";
                outputFile = new File(getExternalCacheDir(), fileName);
                try {
                    if (outputFile.exists()) {
                        outputFile.delete();
                    }
                    outputFile.createNewFile();
                } catch (IOException e) {
                    Log.e("Exception", e.getMessage(), e);
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
                isInitialized = true;
            }
        });

        sampleImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (isInitialized) {
                    clearCanvas();
                    double width = v.getWidth(); // 640
                    double height = v.getHeight(); // 480
                    double rawX = event.getX();
                    double rawY = event.getY();
                    int x = (int) Math.round(rawX * (bmpWidth / width));
                    int y = (int) Math.round(rawY * (bmpHeight / height));
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        getColorInfo(x, y);
                    }
                }
                return true;
            }
        });

        retrievePicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
                startActivityForResult(intent, PIC_RETRIEVED);
                isInitialized = true;
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (isInitialized) {
                    String selectedItem = (String) spinner.getSelectedItem();
                    Toast.makeText(MainActivity.this, "现在对" + selectedItem + "进行线性分析～", Toast.LENGTH_SHORT).show();

                    for (int i = 0; i < analyteCandidates.size(); i++) {
                        if (analyteCandidates.get(i).equals(selectedItem)) {
                            selectedAnalyte = i;
                            break;
                        }
                    }

                    displayConcentrationInfo();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(MainActivity.this, "尚未选择合适的分析物！", Toast.LENGTH_SHORT).show();
            }
        });
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_menu, menu);
//        return true;
//    }

    private void onBitmapInit(Uri imageUri) throws FileNotFoundException {
//        toggleFlashLight(false);
        decodedBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
        int bitmapWidth = decodedBitmap.getWidth();
        int bitmapHeight = decodedBitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(0);

        decodedBitmap = Bitmap.createBitmap(decodedBitmap, 0, 0, bitmapWidth, bitmapHeight, matrix, false);
        clearCanvas();

        bmpWidth = workingBitmap.getWidth(); // 2976
        bmpHeight = workingBitmap.getHeight(); // 3968
        int centerX = Math.round(bmpWidth / 2);
        int centerY = Math.round(bmpHeight / 2);

//            By default, displaying color info on the center of the image
        getColorInfo(centerX, centerY);
    }

    private void clearCanvas() {
//        decodedBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        workingBitmap = decodedBitmap.copy(Bitmap.Config.ARGB_8888, true);
        sampleImageView.setImageBitmap(workingBitmap);
        canvas = new Canvas(workingBitmap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PIC_TAKEN:
                if (resultCode == RESULT_OK) {
                    try {
                    onBitmapInit(imageUri);
                    } catch (FileNotFoundException e) {
                        Log.e("Exception", e.getMessage(), e);
                    }
                }
                break;
            case PIC_RETRIEVED:
                if (resultCode == RESULT_OK && data != null) {
                    Uri selectedImage = data.getData();
                    if (selectedImage != null) {
                        try {
                            onBitmapInit(selectedImage);
                        } catch (FileNotFoundException e) {
                            Log.e("Exception", e.getMessage(), e);
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        decodedBitmap.recycle();
        workingBitmap.recycle();
//        outputFile.delete();
        super.onDestroy();
    }

    private void getColorInfo(int centerX, int centerY) {
        stringBuilder.setLength(0);

        ArrayList<Point> samplePoints = new ArrayList<>();
        ArrayList<Integer> colorList = new ArrayList<>();
        int printedTextColor = 0;
        int sampleCount = 0;

        average = new float[3];
        float[][] channels = new float[SAMPLE_NUMBER_ROOT * SAMPLE_NUMBER_ROOT][3];

//            Drawing 16 sampling points
        int row = SAMPLE_NUMBER_ROOT;
        int col = row;
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                samplePoints.add(new Point(centerX - 30 + 60 * j / col, centerY - 30 + 60 * i / row));
            }
        }

        for (Point p : samplePoints) {
            try {
                colorList.add(workingBitmap.getPixel(p.x, p.y));
            } catch (IllegalArgumentException e) {}
        }

        sampleCount = colorList.size();

//        遍历取样点求取各通道均值
        if (sampleCount > 0) {
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
//        colorDataTextView.setText(output);
//        colorDataTextView.setTextColor(Color.argb(average[3], average[0], average[1], average[2]));

                channels[sampleIndex] = getHSVChannels(colorList.get(sampleIndex));
//                output += (sampleIndex + 1) + "- H: " + channels[0] + ", S: " + channels[1] + ", V: " + channels[2] + "\n";
                for (int channelIndex = 0; channelIndex < average.length; channelIndex++) {
                    average[channelIndex] += channels[sampleIndex][channelIndex];
                }
            }
        }

        for (int channelIndex = 0; channelIndex < average.length; channelIndex++) {
            average[channelIndex] /= sampleCount;
        }

//        检查H通道均值合理性并输出
        for (float[] individualColorInfo : channels) {
            if (Math.abs(average[0] - individualColorInfo[0]) > Math.sqrt(average[0])) {
                stringBuilder.append("取值误差较大，请重新取点！");
                alertTextView.setText(stringBuilder.toString());
                alertTextView.setVisibility(View.VISIBLE);
                alertTextView.setBackgroundColor(Color.BLACK);
                alertTextView.setTextColor(Color.WHITE);
                break;
            }
            alertTextView.setVisibility(View.GONE);
        }

//        output += "均值为\nH:" + average[0] + "\nS: " + average[1] + "\nV: " + average[2];

        swipeLayout.setShowMode(SwipeLayout.ShowMode.LayDown);
        swipeLayout.addSwipeListener(swipeListener);

        Typeface spaceMonoRegTypeFace = Typeface.createFromAsset(getAssets(), "SpaceMono-Regular.ttf");
        HSVHTextView.setTypeface(spaceMonoRegTypeFace);
        HSVSTextView.setTypeface(spaceMonoRegTypeFace);
        HSVVTextView.setTypeface(spaceMonoRegTypeFace);

        HSVHTextView.setText(String.format("%.3f", average[0]));
        HSVSTextView.setText(String.format("%.3f", average[1]));
        HSVVTextView.setText(String.format("%.3f", average[2]));

        int color = Color.HSVToColor(average);
//        outerLinearLayout.setBackgroundColor(color);

        swipeSurfaceLinearLayout.setBackgroundColor(color);

        if (Math.abs(color - Color.WHITE) < Math.abs(color - Color.BLACK)) {
            printedTextColor = Color.BLACK;
        } else {
            printedTextColor = Color.WHITE;
        }

        swipeSurfaceTextView.setVisibility(View.VISIBLE);
//        swipeSurfaceTextView.setTypeface(spaceMonoRegTypeFace);
        swipeSurfaceTextView.setTextColor(printedTextColor);

        HSVHTextView.setBackgroundColor(color);
        HSVHTextView.setTextColor(printedTextColor);
        HSVSTextView.setBackgroundColor(Color.HSVToColor(128, average));
        HSVSTextView.setTextColor(printedTextColor);
        HSVVTextView.setBackgroundColor(Color.HSVToColor(64, average));
        HSVVTextView.setTextColor(Color.BLACK);

        try {
            paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStrokeWidth(5.0f);
            paint.setTextSize(15.0f);
            paint.setDither(true);
            int pointNum = 1;

            for (Point p : samplePoints) {
                canvas.drawPoint(p.x, p.y, paint);
                canvas.drawText(String.valueOf(pointNum++), p.x + 2, p.y - 2, paint);
            }
        } catch (Exception e) {
            Log.e("Exception", e.getMessage(), e);
        }

        spinnerLinearLayout.setVisibility(View.VISIBLE);
        displayConcentrationInfo();
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

    private void displayConcentrationInfo() {
        stringBuilder.setLength(0);
        stringBuilder.append("所选择回归曲线的序号为：").append(selectedAnalyte).append("\n\n");
        stringBuilder.append("样品点所对应").append(analyteCandidates.get(selectedAnalyte)).append("的浓度为：");
        stringBuilder.append(getConcentrationFromColorInfo(average[1], selectedAnalyte));
        concentrationTextView.setText(stringBuilder.toString());
    }

    private String getConcentrationFromColorInfo(double colorValue, int selectedAnalyte) {
        double resultConcentration;
        if (resources != null) {
            String[] formulae = resources.getStringArray(R.array.formula_coefficients);
            String[] formulaCoefficents = formulae[selectedAnalyte].trim().split(" ");
            resultConcentration = colorValue * Double.parseDouble(formulaCoefficents[0]) + Double.parseDouble(formulaCoefficents[1]);

            return String.format("%.3f", resultConcentration) + " (μM)";
//            return String.format("%.3d", resultConcentration) + " k: " +  Double.parseDouble(formulaCoefficents[0]) + " b: " +  Double.parseDouble(formulaCoefficents[1]);
        }
        return "NULL";
    }

//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void toggleFlashLight(boolean openOrClose) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                //获取CameraManager
                CameraManager mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
                //获取当前手机所有摄像头设备ID
                if (mCameraManager != null) {
                    String[] ids = mCameraManager.getCameraIdList();
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
                }
            } catch (CameraAccessException e) {
                Log.e("Exception", e.getMessage(), e);
            }
        }
    }
}

class CustomSwiperListener implements SwipeLayout.SwipeListener {

    @Override
    public void onStartOpen(SwipeLayout layout) {}

    @Override
    public void onOpen(SwipeLayout layout) {}

    @Override
    public void onStartClose(SwipeLayout layout) {}

    @Override
    public void onClose(SwipeLayout layout) {}

    @Override
    public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {}

    @Override
    public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {}
}
