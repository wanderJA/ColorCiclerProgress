package com.wander.colorprogress;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ProgressBar;


/**
 * Created by wander on 2017/7/27.
 */

public class ColorfulProgress extends ProgressBar {

    private Paint mPaint;
    private Bitmap bitmap;
    private Paint innerPaint;
    /**
     * 外圆环
     */
    private RectF rectF;
    private int mRadius;
    private int mStrokeWidth;
    private Paint circlePaint;
    private float innerRadius;
    private String tip = "- 出借 -";
    private TextPaint mTextPaint;
    private float textStart;
    private float textBottom;
    private int innerColor1 = Color.parseColor("#F3FFEE");
    private int innerColor2 = Color.parseColor("#D5FEDE");
    private float offSet = -90;
    private Shader bitmapShader;
    private Paint innerShadowPaint;
    private int tipTextSize = 16;
    private int duration = 500;
    private static final int STOPPED = 0;

    private static final int RUNNING = 1;

    private int mPlayingState = STOPPED;


    public ColorfulProgress(Context context) {
        this(context, null);
    }

    public ColorfulProgress(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorfulProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.ColorfulProgress);
            mStrokeWidth = array.getDimensionPixelSize(R.styleable.ColorfulProgress_progress_stroke_width, mStrokeWidth);
            tip = array.getString(R.styleable.ColorfulProgress_progress_status);
            tipTextSize = (int) array.getDimension(R.styleable.ColorfulProgress_progress_text_size, tipTextSize);
            array.recycle();
        }

        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor("#E7E9E8"));
        circlePaint.setStyle(Paint.Style.STROKE);

        innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setStyle(Paint.Style.FILL);

        innerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerShadowPaint.setStyle(Paint.Style.STROKE);
        innerShadowPaint.setColor(Color.parseColor("#66000000"));
//        float[] direction = new float[]{50, 0, 0};
//        //设置环境光亮度
//        float light = 0.1f;
//        // 选择要应用的反射等级
//        float specular = 1;
//        // 向mask应用一定级别的模糊
//        float blur = 23.5f;
//        EmbossMaskFilter emboss = new EmbossMaskFilter(direction, light, specular, blur);
//
        BlurMaskFilter blurMaskFilter = new BlurMaskFilter(8, BlurMaskFilter.Blur.OUTER);
        innerShadowPaint.setMaskFilter(blurMaskFilter);

        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(sp2px(tipTextSize));
        mTextPaint.setColor(Color.parseColor("#31B27F"));
        setTip("- 出借 -");

        initBitmapShader();
        refreshStroke();

    }

    void refreshStroke() {
        mPaint.setStrokeWidth(mStrokeWidth);
        circlePaint.setStrokeWidth(mStrokeWidth);
        innerShadowPaint.setStrokeWidth(mStrokeWidth >> 1);
        refreshCircle();
    }

    void refreshCircle() {
        rectF = new RectF(mStrokeWidth, mStrokeWidth, mRadius * 2 - mStrokeWidth, mRadius * 2 - mStrokeWidth);
        innerRadius = rectF.width() / 2 - mStrokeWidth * 0.5f;
        offSet = (float) (mStrokeWidth * 0.5 / (2 * (innerRadius + mStrokeWidth) * Math.PI) * 360 - 90);
        LinearGradient linearGradient = new LinearGradient(0, 0, 0, innerRadius * 2, innerColor1, innerColor2, Shader.TileMode.CLAMP);
        innerPaint.setShader(linearGradient);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!TextUtils.isEmpty(tip)) {
            float measureText = mTextPaint.measureText(tip);
            float v = (getWidth() - measureText) / 2;
            textStart = v < 0 ? 0 : v;
            textBottom = (getHeight() - (mTextPaint.descent() + mTextPaint.ascent())) / 2;
        }
        initBitmapShader();
    }

    private void initBitmapShader() {
        BitmapFactory.Options opts = setBitmapOption(getResources(), R.mipmap.progress_round, getWidth(), getHeight());
        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.progress_round, opts);
        bitmap = zoomImage(bitmap, getWidth(), getHeight());
        mRadius = bitmap.getWidth() / 2;
        bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mPaint.setShader(bitmapShader);
        if (mStrokeWidth == 0) {
            mStrokeWidth = (int) (getWidth() * 0.08f);
        }
        refreshStroke();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float sweep = getProgress() * 1.0f / getMax() * 360+0.01f;
        float v = offSet + sweep;
        //剩余进度
        canvas.drawArc(rectF, v, 360.0f - sweep, false, circlePaint);
        //当前进度
        canvas.drawArc(rectF, offSet, sweep, false, mPaint);
        //内环的阴影
        canvas.drawCircle(rectF.centerX(), rectF.centerY() + 1, innerRadius - (mStrokeWidth >> 2), innerShadowPaint);
        //内环
        canvas.drawCircle(rectF.centerX(), rectF.centerY(), innerRadius, innerPaint);
        canvas.drawText(tip, textStart, textBottom, mTextPaint);

    }

    public void setColorProgress(int progress) {
        if (getProgress() != progress /*&& !isRunning()*/) {
            runInt(progress);
            mPlayingState = RUNNING;
        }
    }

    public boolean isRunning() {
        return mPlayingState == RUNNING;
    }

    /**
     * 跑整数动画
     *
     * @param progress
     */
    private void runInt(int progress) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(getProgress(), progress);
        valueAnimator.setDuration(duration);

        valueAnimator
                .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        //设置瞬时的数据值到界面上
                        setProgress((Integer) valueAnimator.getAnimatedValue());
                        if (valueAnimator.getAnimatedFraction() >= 1) {
                            //设置状态为停止
                            mPlayingState = STOPPED;
                        }
                    }
                });
        valueAnimator.start();
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            int widthSize = (int) rectF.width() + mStrokeWidth * 2;
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            int heightSize = (int) rectF.height() + mStrokeWidth * 2;
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setTip(String tip) {
        this.tip = tip;
    }

    public void setStrokeWidth(int mStrokeWidth) {
        this.mStrokeWidth = dp2px(mStrokeWidth);
        refreshStroke();
        invalidate();
    }

    public void setTipTextSize(int tipTextSize) {
        this.tipTextSize = tipTextSize;
        mTextPaint.setTextSize(sp2px(tipTextSize));
    }

    protected int dp2px(int dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, getResources().getDisplayMetrics());
    }

    protected int sp2px(int dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                dpVal, getResources().getDisplayMetrics());
    }

    private BitmapFactory.Options setBitmapOption(Resources resources, int resId,
                                                  int width, int height) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        // 设置只是解码图片的边距，此操作目的是度量图片的实际宽度和高度
        BitmapFactory.decodeResource(resources, resId, opt);

        int outWidth = opt.outWidth; // 获得图片的实际高和宽
        int outHeight = opt.outHeight;
        opt.inDither = false;
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        // 设置加载图片的颜色数为16bit，默认是RGB_8888，表示24bit颜色和透明通道，但一般用不上
        opt.inSampleSize = 1;
        // 设置缩放比,1表示原比例，2表示原来的四分之一....
        // 计算缩放比
        if (outWidth != 0 && outHeight != 0 && width != 0 && height != 0) {
            opt.inSampleSize = (outWidth / width + outHeight / height) / 2;
        }

        opt.inJustDecodeBounds = false;// 最后把标志复原
        return opt;
    }

    /***
     * 图片的缩放方法
     *
     * @param oldBitmap
     *            ：源图片资源
     * @param newWidth
     *            ：缩放后宽度
     * @param newHeight
     *            ：缩放后高度
     */
    private static Bitmap zoomImage(Bitmap oldBitmap, double newWidth,
                                    double newHeight) {
        if (newHeight <= 0 || newWidth <= 0) {
            return oldBitmap;
        }
        // 获取这个图片的宽和高
        float width = oldBitmap.getWidth();
        float height = oldBitmap.getHeight();
        // 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();
        // 计算宽高缩放率
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 缩放图片动作
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bitmap = Bitmap.createBitmap(oldBitmap, 0, 0, (int) width,
                (int) height, matrix, true);
        return bitmap;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
    }
}
