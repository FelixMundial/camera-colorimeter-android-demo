package com.example.admin.cameracolorimeterdemo.views;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.view.View;

public class ScalableImageView extends ImageView implements View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {

    private static final float MAX_SCALE = 4.0f;
    private float defaultScale = 1.0f;

    ScaleGestureDetector scaleGestureDetector = null;
    Matrix scaleMatrix = new Matrix();
    /**
     * 处理矩阵的9个值
     */
//    float[] matrixValue = new float[9];

    public ScalableImageView(Context context) {
        this(context, null);
    }

    public ScalableImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScalableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setScaleType(ScaleType.MATRIX);
        scaleGestureDetector = new ScaleGestureDetector(context, this);
        this.setOnTouchListener(this);
    }

    /**
     * 获取当前缩放比例
     */
//    public float getScale() {
//        scaleMatrix.getValues(matrixValue);
//        return matrixValue[Matrix.MSCALE_X];
//    }

    //--------------------------implements OnTouchListener----------------------------//

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return scaleGestureDetector.onTouchEvent(event);
    }

    //----------------------implements OnScaleGestureListener------------------------//

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
//        float scale = getScale();
        float scaleFactor = detector.getScaleFactor();
        if (getDrawable() == null)
            return true;
//        if (scaleFactor * scale < initScale)
//            scaleFactor = initScale / scale;
//        if (scaleFactor * scale > SCALE_MAX)
//            scaleFactor = SCALE_MAX / scale;

        //设置缩放比例
        scaleMatrix.postScale(scaleFactor, scaleFactor, getWidth() / 2, getHeight() / 2);
        setImageMatrix(scaleMatrix);
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }
}
