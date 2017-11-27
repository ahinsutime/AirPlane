package com.mx.krakensoft.opencv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import com.mx.krakensoft.opencv.imageProcessing.ColorBlobDetector;

import android.app.Activity;
import android.graphics.SumPathEffect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.Size;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.os.SystemClock;


import static org.opencv.core.Core.flip;
import static org.opencv.core.Core.sqrt;

public class MainActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {

    static {
        System.loadLibrary("opencv_java3");
    }

    /*simplified keyboard buttons */
    private Button keyButton1;
    private Button keyButton2;
    private Button keyButton3;
    private Button keyButton4;
    private Button keyButton5;
    private Button keyButton6;
    private Button keyButton7;
    private Button keyButton8;
    private Button keyButton9;
    private Map<String, ButtonCoordinates> buttons;
    private double centerX = 0;//Added by ahinsutime
    private double centerY = 0;//Added by ahinsutime
    private double PrevCenterX = 0;//Added by ahinsutime
    private double PrevCenterY = 0;//Added by ahinsutime
    private double mappedX = 0;
    private double mappedY = 0;

    int DefaultCursorRadius = 50;
    int ncols;
    int nrows;

    private List<Point> listPos = new LinkedList<Point>();//Added by ahinsutime
    private List<MatOfPoint> contoursDraw;
    public RelativeLayout RL;//Added by ahinsutime
    public DrawingView DV;//Added by ahinsutime
    public DrawingHandView DHV;//Added by ahinsutime
    public RelativeLayout BL;//Added by ahinsutime

    // public DrawingLoadingView DLV;

    private static final String TAG = "HandPose::MainActivity";
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;

    private Mat mRgba;
    private Mat mGray;
    private Mat mIntermediateMat;
    private boolean flag = false;

    private int mDetectorType = JAVA_DETECTOR;

    private CustomSufaceView mOpenCvCameraView;
    private List<Size> mResolutionList;

    private double XOffset = 0;
    private double YOffset = 0;

    private SeekBar minTresholdSeekbar = null;
    private SeekBar maxTresholdSeekbar = null;
    private TextView minTresholdSeekbarText = null;
    private TextView numberOfFingersText = null;

    double iThreshold = 0;

    private Scalar mBlobColorHsv;
    private Scalar mBlobColorRgba;
    private ColorBlobDetector mDetector;
    private Mat mSpectrum;
    private boolean mIsColorSelected = false;

    private Size SPECTRUM_SIZE;
    private Scalar CONTOUR_COLOR;
    private Scalar CONTOUR_COLOR_BLUE;//Added by ahinsutime
    private Scalar CONTOUR_COLOR_GREEN;//Added by ahinsutime
    private Scalar CONTOUR_COLOR_WHITE;
    private Scalar CONTOUR_COLOR_BLACK;//Added by ahinsutime

    final Handler mHandler = new Handler();

    int numberOfFingers = 0;

    boolean pressed = false;
    boolean flicker = false;
    boolean tracking = false;
    double flickerDist = 0;
    double optimalArea = 4000;
    double currentArea = 4000;
    Point InitialP1 = new Point(0,0);
    Point InitialP2 = new Point(1,1);
    long mPauseTime=0;
    long mTimeNow=0;
    boolean Detecting=true;

    Rect PrevBoundRect = new Rect(InitialP1, InitialP2);

    MotionEvent motionEvent;




    final Runnable mUpdateFingerCountResults = new Runnable() {
        public void run() {

            DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
            int width = dm.widthPixels;
            int height = dm.heightPixels;
            if(tracking==false){

                DHV.setVisibility(DHV.VISIBLE);//Added by ahinsutime
                DV.setVisibility(DV.GONE);
                BL.setVisibility(BL.GONE);
                DV.invalidate();//Added by ahinsutime
                DHV.invalidate();//Added by ahinsutime
                BL.invalidate();//Added by ahinsutime
                keyButton1.setVisibility(keyButton1.GONE);
                keyButton2.setVisibility(keyButton2.GONE);
                keyButton3.setVisibility(keyButton3.GONE);
                keyButton4.setVisibility(keyButton4.GONE);
                keyButton5.setVisibility(keyButton5.GONE);
                keyButton6.setVisibility(keyButton6.GONE);
                keyButton7.setVisibility(keyButton7.GONE);
                keyButton8.setVisibility(keyButton8.GONE);
                keyButton9.setVisibility(keyButton9.GONE);
                keyButton1.invalidate();
                keyButton2.invalidate();
                keyButton3.invalidate();
                keyButton4.invalidate();
                keyButton5.invalidate();
                keyButton6.invalidate();
                keyButton7.invalidate();
                keyButton8.invalidate();
                keyButton9.invalidate();

                TimerTask adTast = new TimerTask() {
                    public void run() {
                        long downTime = SystemClock.uptimeMillis();
                        long eventTime = SystemClock.uptimeMillis() + 100;

                        int metaState = 0;

                        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
                        int width = dm.widthPixels;
                        int height = dm.heightPixels;

                        float tx = width / 2;
                        float ty = height / 2;
                        motionEvent = MotionEvent.obtain(
                                downTime,
                                eventTime,
                                MotionEvent.ACTION_UP,
                                tx,
                                ty,
                                metaState
                        );
                        mOpenCvCameraView.dispatchTouchEvent(motionEvent);


                        if (currentArea < ((width / 2) * (height / 2)) && currentArea > (width / 20 * height / 20)) {
                            tracking = true;
                            optimalArea = currentArea;
                        } else {
                            tracking = false;
                            optimalArea = 4000;
                            currentArea = 4000;
                        }

                    }

                };

                Timer timer = new Timer();
                timer.schedule(adTast, 2000);
            }
            else {

                DV.setVisibility(DV.VISIBLE);
                BL.setVisibility(BL.VISIBLE);
                DHV.setVisibility(BL.GONE);;//Added by ahinsutime
                keyButton1.setVisibility(keyButton1.VISIBLE);
                keyButton2.setVisibility(keyButton2.VISIBLE);
                keyButton3.setVisibility(keyButton3.VISIBLE);
                keyButton4.setVisibility(keyButton4.VISIBLE);
                keyButton5.setVisibility(keyButton5.VISIBLE);
                keyButton6.setVisibility(keyButton6.VISIBLE);
                keyButton7.setVisibility(keyButton7.VISIBLE);
                keyButton8.setVisibility(keyButton8.VISIBLE);
                keyButton9.setVisibility(keyButton9.VISIBLE);
                keyButton1.invalidate();
                keyButton2.invalidate();
                keyButton3.invalidate();
                keyButton4.invalidate();
                keyButton5.invalidate();
                keyButton6.invalidate();
                keyButton7.invalidate();
                keyButton8.invalidate();
                keyButton9.invalidate();
                updateNumberOfFingers();
                DV.invalidate();//Added by ahinsutime
                DHV.invalidate();//Added by ahinsutime
                BL.invalidate();//Added by ahinsutime


                flickerDist = Math.sqrt((PrevCenterX - centerX) * (PrevCenterX - centerX) + (PrevCenterY - centerY) * (PrevCenterY - centerY));

                if (((((int) PrevCenterX)) == 0) && ((((int) PrevCenterY)) == 0)) {//Initial setting
                } else if (flickerDist < 50 && flickerDist >= 0) {//For micro control
                    centerX = PrevCenterX + (centerX - PrevCenterX) / 5;
                    centerY = PrevCenterY + (centerY - PrevCenterY) / 5;
                    PrevCenterX = centerX;
                    PrevCenterY = centerY;
                } else if (flickerDist < 100 && flickerDist >= 50) {//For micro control
                    centerX = PrevCenterX + (centerX - PrevCenterX) / 4;
                    centerY = PrevCenterY + (centerY - PrevCenterY) / 4;
                    PrevCenterX = centerX;
                    PrevCenterY = centerY;
                } else if (flickerDist < 200 && flickerDist >= 100) {//For micro control
                    centerX = PrevCenterX + (centerX - PrevCenterX) / 3;
                    centerY = PrevCenterY + (centerY - PrevCenterY) / 3;
                    PrevCenterX = centerX;
                    PrevCenterY = centerY;
                } else if (flickerDist < 500 && flickerDist >= 200) {//For micro control
                    centerX = PrevCenterX + (centerX - PrevCenterX) / 2;
                    centerY = PrevCenterY + (centerY - PrevCenterY) / 2;
                } else {//Large flickering
                    centerX = PrevCenterX + (centerX - PrevCenterX) / 4;
                    centerY = PrevCenterY + (centerY - PrevCenterY) / 4;
                }
                PrevCenterX = centerX;
                PrevCenterY = centerY;

                //keyButton1.dispatchTouchEvent(motionEvent);
                if (pressed) {
                    DV.invalidate();
                    BL.dispatchTouchEvent(motionEvent);
                    Log.d(TAG, "Simulated Touch Pressed=" + pressed);
                } else {
                    DV.invalidate();
                    Log.d(TAG, "Simulated Touch Released=" + pressed);
                    long downTime = SystemClock.uptimeMillis();
                    long eventTime = SystemClock.uptimeMillis() + 100;
                    float tx = (float) centerX + (float) XOffset;
                    float ty = (float) centerY + (float) YOffset;
                    int metaState = 0;

                    motionEvent = MotionEvent.obtain(
                            downTime,
                            eventTime,
                            MotionEvent.ACTION_UP,
                            tx,
                            ty,
                            metaState
                    );
                    BL.dispatchTouchEvent(motionEvent);
                }
            }
        }
    };

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    //Log.i(TAG, "OpenCV loaded successfully");

                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                    // 640x480
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        //Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main_surface_view);
        if (!OpenCVLoader.initDebug()) {
            Log.e("Test", "man");
        } else {
        }

        mOpenCvCameraView = (CustomSufaceView) findViewById(R.id.main_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(1);

        minTresholdSeekbarText = (TextView) findViewById(R.id.textView3);
        numberOfFingersText = (TextView) findViewById(R.id.numberOfFingers);
        minTresholdSeekbar = (SeekBar) findViewById(R.id.seekBar1);
        minTresholdSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChanged = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                minTresholdSeekbarText.setText(String.valueOf(progressChanged));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                minTresholdSeekbarText.setText(String.valueOf(progressChanged));
            }
        });
        minTresholdSeekbar.setProgress(8700);


        RL = (RelativeLayout) findViewById(R.id.main_relative_view);//Added by ahinsutime
        DV = new DrawingView(this);//Added by ahinsutime
        DV.setId(17);//Added by ahinsutime
        RL.addView(DV);//Added by ahinsutime
        DV.bringToFront();//Added by ahinsutime
        DHV = new DrawingHandView(this);//Added by ahinsutime
        DHV.setId(18);//Added by ahinsutime
        RL.addView(DHV);//Added by ahinsutime
        DHV.bringToFront();//Added by ahinsutime
        BL = (RelativeLayout) findViewById(R.id.buttons);
        //mHandler.post(mUpdateFingerCountResults);

        //mOpenCvCameraView.bringToFront();
        mOpenCvCameraView.setAlpha((float) 50);//Added by ahinsutime

        keyButton1 = (Button) findViewById(R.id.button1);
        keyButton2 = (Button) findViewById(R.id.button2);
        keyButton3 = (Button) findViewById(R.id.button3);
        keyButton4 = (Button) findViewById(R.id.button4);
        keyButton5 = (Button) findViewById(R.id.button5);
        keyButton6 = (Button) findViewById(R.id.button6);
        keyButton7 = (Button) findViewById(R.id.button7);
        keyButton8 = (Button) findViewById(R.id.button8);
        keyButton9 = (Button) findViewById(R.id.button9);
        keyButton1.setVisibility(keyButton1.GONE);
        keyButton2.setVisibility(keyButton2.GONE);
        keyButton3.setVisibility(keyButton3.GONE);
        keyButton4.setVisibility(keyButton4.GONE);
        keyButton5.setVisibility(keyButton5.GONE);
        keyButton6.setVisibility(keyButton6.GONE);
        keyButton7.setVisibility(keyButton7.GONE);
        keyButton8.setVisibility(keyButton8.GONE);
        keyButton9.setVisibility(keyButton9.GONE);
        buttons = new HashMap<>();
        if (!flag) {
            keyButton1.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    addButtonCoordinates(keyButton1);
                }
            });
            keyButton2.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    addButtonCoordinates(keyButton2);
                }
            });
            keyButton3.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    addButtonCoordinates(keyButton3);
                }
            });
            keyButton4.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    addButtonCoordinates(keyButton4);
                }
            });
            keyButton5.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    addButtonCoordinates(keyButton5);
                }
            });
            keyButton6.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    addButtonCoordinates(keyButton6);
                }
            });
            keyButton7.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    addButtonCoordinates(keyButton7);
                }
            });
            keyButton8.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    addButtonCoordinates(keyButton8);
                }
            });
            keyButton9.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    addButtonCoordinates(keyButton9);
                }
            });
            flag = true;
        }
        keyButton1.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {//Added by ahinsutime
                    case MotionEvent.ACTION_DOWN :
                        keyButton1.setTextColor(Color.BLUE);
                        break;
                    case MotionEvent.ACTION_MOVE :
                        keyButton1.setTextColor(Color.BLUE);
                        break;
                    //case MotionEvent.ACTION_UP   :
                    default:
                        keyButton1.setTextColor(Color.BLACK);
                        break;
                }
                return false;
            }
        });
        keyButton2.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {//Added by ahinsutime
                    case MotionEvent.ACTION_DOWN :
                        keyButton2.setTextColor(Color.BLUE);
                        break;
                    case MotionEvent.ACTION_MOVE :
                        keyButton2.setTextColor(Color.BLUE);
                        break;
                    //case MotionEvent.ACTION_UP   :
                    default:
                        keyButton2.setTextColor(Color.BLACK);
                        break;
                }
                return false;
            }
        });
        keyButton3.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {//Added by ahinsutime
                    case MotionEvent.ACTION_DOWN :
                        keyButton3.setTextColor(Color.BLUE);
                        break;
                    case MotionEvent.ACTION_MOVE :
                        keyButton3.setTextColor(Color.BLUE);
                        break;
                    //case MotionEvent.ACTION_UP   :
                    default:
                        keyButton3.setTextColor(Color.BLACK);
                        break;
                }
                return false;
            }
        });
        keyButton4.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {//Added by ahinsutime
                    case MotionEvent.ACTION_DOWN :
                        keyButton4.setTextColor(Color.BLUE);
                        break;
                    case MotionEvent.ACTION_MOVE :
                        keyButton4.setTextColor(Color.BLUE);
                        break;
                    //case MotionEvent.ACTION_UP   :
                    default:
                        keyButton4.setTextColor(Color.BLACK);
                        break;
                }
                return false;
            }
        });

        keyButton5.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {//Added by ahinsutime
                    case MotionEvent.ACTION_DOWN :
                        keyButton5.setTextColor(Color.BLUE);
                        break;
                    case MotionEvent.ACTION_MOVE :
                        keyButton5.setTextColor(Color.BLUE);
                        break;
                    //case MotionEvent.ACTION_UP   :
                    default:
                        keyButton5.setTextColor(Color.BLACK);
                        break;
                }
                return false;
            }
        });
        keyButton6.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {//Added by ahinsutime
                    case MotionEvent.ACTION_DOWN :
                        keyButton6.setTextColor(Color.BLUE);
                        break;
                    case MotionEvent.ACTION_MOVE :
                        keyButton6.setTextColor(Color.BLUE);
                        break;
                    //case MotionEvent.ACTION_UP   :
                    default:
                        keyButton6.setTextColor(Color.BLACK);
                        break;
                }
                return false;
            }
        });
        keyButton7.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {//Added by ahinsutime
                    case MotionEvent.ACTION_DOWN :
                        keyButton7.setTextColor(Color.BLUE);
                        break;
                    case MotionEvent.ACTION_MOVE :
                        keyButton7.setTextColor(Color.BLUE);
                        break;
                    //case MotionEvent.ACTION_UP   :
                    default:
                        keyButton7.setTextColor(Color.BLACK);
                        break;
                }
                return false;
            }
        });
        keyButton8.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {//Added by ahinsutime
                    case MotionEvent.ACTION_DOWN :
                        keyButton8.setTextColor(Color.BLUE);
                        break;
                    case MotionEvent.ACTION_MOVE :
                        keyButton8.setTextColor(Color.BLUE);
                        break;
                    //case MotionEvent.ACTION_UP   :
                    default:
                        keyButton8.setTextColor(Color.BLACK);
                        break;
                }
                return false;
            }
        });
        keyButton9.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {//Added by ahinsutime
                    case MotionEvent.ACTION_DOWN :
                        keyButton9.setTextColor(Color.BLUE);
                        break;
                    case MotionEvent.ACTION_MOVE :
                        keyButton9.setTextColor(Color.BLUE);
                        break;
                    //case MotionEvent.ACTION_UP   :
                    default:
                        keyButton9.setTextColor(Color.BLACK);
                        break;
                }
                return false;
            }
        });
    }
    public void addButtonCoordinates (Button b) {
        Point point1 = getPointOfView(b);
        double xMax = point1.x + b.getWidth();
        double yMax = point1.y + b.getHeight();
        ButtonCoordinates coo = new ButtonCoordinates();
        coo.setxMin(point1.x);
        coo.setyMin(point1.y);
        coo.setxMax(xMax);
        coo.setyMax(yMax);
        buttons.put(String.valueOf(b.getText()), coo);
        }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        mIntermediateMat = new Mat();


        Camera.Size resolution = mOpenCvCameraView.getResolution();
        String caption = "Resolution " + Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
        Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();

        Camera.Parameters cParams = mOpenCvCameraView.getParameters();
        cParams.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
        mOpenCvCameraView.setParameters(cParams);
        Toast.makeText(this, "Focus mode : " + cParams.getFocusMode(), Toast.LENGTH_SHORT).show();

        //mOpenCvCameraView.setCameraIndex(1);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255, 0, 0, 255);
        CONTOUR_COLOR_WHITE = new Scalar(255, 255, 255, 255);
        CONTOUR_COLOR_BLUE = new Scalar(0, 0, 255, 255);
        CONTOUR_COLOR_GREEN = new Scalar(0, 255, 0, 255);
        CONTOUR_COLOR_BLACK = new Scalar(0, 0, 0, 255);

    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        ncols = cols;
        nrows = rows;

        //Needed to be changed xOffset and yOffset are location dependent dynamic values

        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int touchedX = (int) Math.floor(event.getX() * cols/width);
        int touchedY = (int) Math.floor(event.getY() * rows/height);



        XOffset =  cols/ (double) width;
        YOffset = rows / (double) height;

        Log.i(TAG, "XOffset="+XOffset+" YOffset="+YOffset);

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        x = touchedX;
        y = touchedY;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");
        Log.i(TAG, "Touch image coordinates RAW: (" + event.getX() + ", " + event.getY() + ")");
        //Needed to be changed


       /* given (x, y) that was touched, click on according button */
        //touchEventDispatch(event.getX(), event.getY());

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x > 5) ? x - 5 : 0;
        touchedRect.y = (y > 5) ? y - 5 : 0;

        touchedRect.width = (x + 5 < cols) ? x + 5 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y + 5 < rows) ? y + 5 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    public class DrawingView extends View {//Added by ahinsutime
        int x = 0, y = 0;
        int tempX = 0, tempY = 0;

        public DrawingView(Context context) {
            super(context);
        }

        @Override
        public void onDraw(Canvas canvas) {
            Log.d(TAG, "Inside DrawingView.onDraw");
            Paint paint = new Paint();
            Paint far = new Paint();
            Paint near = new Paint();
            Paint click = new Paint();
            Paint innerMapping = new Paint();
            Paint outerMapping = new Paint();
            Paint ref = new Paint();
            innerMapping.setStyle(Paint.Style.STROKE);
            outerMapping.setStyle(Paint.Style.STROKE);

            paint.setColor(Color.BLUE);
            far.setColor(Color.YELLOW);
            near.setColor(Color.RED);
            click.setColor(Color.WHITE);
            innerMapping.setColor(Color.BLACK);
            outerMapping.setColor(Color.RED);
            ref.setColor(Color.BLACK);

            paint.setAlpha(50);
            far.setAlpha(50);
            near.setAlpha(50);
            innerMapping.setStrokeWidth(10);
            outerMapping.setStrokeWidth(10);

            double optimalDist = optimalArea/10000;
            double currentDist = currentArea/10000;

            int width = canvas.getWidth();
            int height = canvas.getHeight();
            float mapX =width * 2/4;
            float mapY = height * 2/4;

            canvas.drawRect(width/2-mapX/2,height/2-mapY/2,width/2+mapX/2,height/2+mapY/2, innerMapping);
            canvas.drawRect(10,10,width-10,height-10, outerMapping);

            x = (int) (centerX / XOffset);
            y = (int) (centerY / YOffset);

            if (y>height/2) {
                y = height/2 + (int) (Math.abs(y - height / 2) * height / mapY);
            }
            else if (y<=height/2){
                y = height/2 - (int) (Math.abs(y - height / 2) * height / mapY);
            }

            if (x>width/2){
                x = width/2 + (int) (Math.abs(x - width / 2) * width / mapX);
            }
            else if(x<=width/2){
                x = width/2 - (int) (Math.abs(x - width / 2) * width / mapX);
            }
            mappedX = x;
            mappedY = y;


            if(optimalDist<=currentDist){//When hand is near

               double tempRadius = (1-(currentDist - optimalDist)/(optimalDist)) * DefaultCursorRadius;

                //Log.d(TAG, "(near)tempRadius=" + tempRadius);
                Log.d(TAG, "optimalDist=" + optimalDist+" currentDist="+currentDist);
                canvas.drawCircle(x, y, DefaultCursorRadius, near);
                canvas.drawCircle(x, y, (int) Math.abs(tempRadius), paint);
                if(pressed){
                    canvas.drawCircle(x, y, (int) Math.abs(tempRadius), click);
                }
                canvas.drawCircle((int) (centerX / XOffset), (int) (centerY / YOffset),DefaultCursorRadius,ref);
            }
            else{//When hand is far

                double tempRadius = ((optimalDist - currentDist)/(optimalDist)) * DefaultCursorRadius + DefaultCursorRadius;
                //double tempRadius = DefaultCursorRadius;

                canvas.drawCircle(x, y, (int) Math.abs(tempRadius), far);
                canvas.drawCircle(x, y, DefaultCursorRadius, paint);
                if(pressed){
                    canvas.drawCircle(x, y, DefaultCursorRadius, click);
                }
                canvas.drawCircle((int) (centerX / XOffset), (int) (centerY / YOffset),DefaultCursorRadius,ref);
            }
        }
    }

    public class DrawingHandView extends View {//Added by ahinsutime
        int x = 0, y = 0;

        public DrawingHandView(Context context) {
            super(context);
        }

        @Override
        public void onDraw(Canvas canvas) {
            Log.d(TAG, "Inside DrawingHandView.onDraw");
            Paint paint = new Paint();
            Paint textPaint = new Paint();

            paint.setColor(Color.GREEN);
            textPaint.setColor(Color.GREEN);
            textPaint.setTextSize(60);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10);
            DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
            //int width = dm.widthPixels;
            //int height = dm.heightPixels;
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            int rectSize = height/4;
            android.graphics.Rect InitialRect = new android.graphics.Rect(width/2-rectSize,height/2-rectSize, width/2+rectSize,height/2+rectSize);
            canvas.drawRect(InitialRect, paint);
            canvas.drawText("Show your hand in this square for 2 seconds.", width/5, height/5, textPaint);

        }
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        flip(mRgba, mRgba, 1);//Added by ahinsutime
        flip(mGray, mGray, 1);//Added by ahinsutime

        iThreshold = minTresholdSeekbar.getProgress();

        //Imgproc.blur(mRgba, mRgba, new Size(5,5));
        Imgproc.GaussianBlur(mRgba, mRgba, new org.opencv.core.Size(3, 3), 1, 1);
        //Imgproc.medianBlur(mRgba, mRgba, 3);

        if (!mIsColorSelected) {
            tracking=false;
            mHandler.post(mUpdateFingerCountResults);
            return mRgba;
        }

        List<MatOfPoint> contours = mDetector.getContours();
        mDetector.process(mRgba);

        Log.d(TAG, "Contours count: " + contours.size());

        if (contours.size() <= 0) {
            tracking=false;
            mHandler.post(mUpdateFingerCountResults);
            return mRgba;
        }

        RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(0).toArray()));

        double boundWidth = rect.size.width;
        double boundHeight = rect.size.height;
        int boundPos = 0;

        for (int i = 1; i < contours.size(); i++) {
            rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i).toArray()));
            if (rect.size.width * rect.size.height > boundWidth * boundHeight) {
                boundWidth = rect.size.width;
                boundHeight = rect.size.height;
                boundPos = i;
            }
        }

        Rect boundRect = Imgproc.boundingRect(new MatOfPoint(contours.get(boundPos).toArray()));

        //Imgproc.rectangle(mRgba, boundRect.tl(), boundRect.br(), CONTOUR_COLOR_WHITE, 2, 8, 0);

        Log.d(TAG,
                " (Before)Row start [" +
                        (int) boundRect.tl().y + "] row end [" +
                        (int) boundRect.br().y + "] Col start [" +
                        (int) boundRect.tl().x + "] Col end [" +
                        (int) boundRect.br().x + "]");

        Log.d(TAG,
                " (After)Row start [" +
                        (int) boundRect.tl().y + "] row end [" +
                        (int) boundRect.br().y + "] Col start [" +
                        (int) boundRect.tl().x + "] Col end [" +
                        (int) boundRect.br().x + "]");


        int rectHeightThresh = 0;
        double a = boundRect.br().y - boundRect.tl().y;
        a = a * 0.7;
        a = boundRect.tl().y + a;

        Log.d(TAG,
                " A [" + a + "] br y - tl y = [" + (boundRect.br().y - boundRect.tl().y) + "]");


        //Imgproc.rectangle(mRgba, boundRect.tl(), new Point(boundRect.br().x, a), CONTOUR_COLOR_BLUE, 2, 8, 0);

        MatOfPoint2f pointMat = new MatOfPoint2f();
        Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(boundPos).toArray()), pointMat, 3, true);
        contours.set(boundPos, new MatOfPoint(pointMat.toArray()));

        MatOfInt hull = new MatOfInt();
        MatOfInt4 convexDefect = new MatOfInt4();
        Imgproc.convexHull(new MatOfPoint(contours.get(boundPos).toArray()), hull);

        if (hull.toArray().length < 3) return mRgba;

        Imgproc.convexityDefects(new MatOfPoint(contours.get(boundPos).toArray()), hull, convexDefect);

        List<MatOfPoint> hullPoints = new LinkedList<MatOfPoint>();
        List<Point> listPo = new LinkedList<Point>();
        for (int j = 0; j < hull.toList().size(); j++) {
            listPo.add(contours.get(boundPos).toList().get(hull.toList().get(j)));
        }

        Iterator<Point> iterator = listPo.iterator();
        while (iterator.hasNext()) {
            Point data = iterator.next();

            //data.getClass().
            if (data.y > (mRgba.size().height - 200)) {
                iterator.remove();
            }
        }

        //DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        //int width = dm.widthPixels;
        //int height = dm.heightPixels;

        MatOfPoint e = new MatOfPoint();
        e.fromList(listPo);
        hullPoints.add(e);

        List<MatOfPoint> defectPoints = new LinkedList<MatOfPoint>();
        List<Point> listPoDefect = new LinkedList<Point>();
        for (int j = 0; j < convexDefect.toList().size(); j = j + 4) {
            Point farPoint = contours.get(boundPos).toList().get(convexDefect.toList().get(j + 2));
            Integer depth = convexDefect.toList().get(j + 3);
            if (depth > iThreshold && farPoint.y < a) {
                listPoDefect.add(contours.get(boundPos).toList().get(convexDefect.toList().get(j + 2)));
            }
            Log.d(TAG, "defects [" + j + "] " + convexDefect.toList().get(j + 3));
        }

        MatOfPoint e2 = new MatOfPoint();
        e2.fromList(listPo);
        defectPoints.add(e2);

        Log.d(TAG, "hull: " + hull.toList());
        Log.d(TAG, "defects: " + convexDefect.toList());
        if(tracking==true) {
            Imgproc.drawContours(mRgba, hullPoints, -1, CONTOUR_COLOR_GREEN, 3);
        }

        listPos = listPo;//Added by ahinsutime

        centerX = 0;
        centerY = 0;
        for (int j = 0; j < listPos.size(); j++) {//Added by ahinsutime

            centerX = centerX + listPos.get(j).x;
            centerY = centerY + listPos.get(j).y;
        }
        centerX = centerX / listPos.size();//Added by ahinsutime
        centerY = centerY / listPos.size();//Added by ahinsutime

        int defectsTotal = (int) convexDefect.total();
        Log.d(TAG, "Defect total " + defectsTotal);

        this.numberOfFingers = listPoDefect.size();
        if (this.numberOfFingers > 5) this.numberOfFingers = 5;

        mHandler.post(mUpdateFingerCountResults);
        //mHandler.postDelayed(mUpdateFingerCountResults,500);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////Use this!!

        for (Point p : listPoDefect) {
            Imgproc.circle(mRgba, p, 6, new Scalar(0, 0, 255));
        }

        int maxY = 0;
        int AX =0;

        for (int i=0; i<listPo.size(); i++){
            if(maxY<=listPo.get(i).y){
                maxY = (int) listPo.get(i).y;
            }
        }

        double tlDelX, tlDelY, brDelX, brDelY, tlDel, brDel;
        double temptlX=0, temptlY=0, tempbrX=0, tempbrY=0;
        tlDelX = boundRect.tl().x-PrevBoundRect.tl().x;
        tlDelY = boundRect.tl().y-PrevBoundRect.tl().y;
        tlDel = Math.sqrt(tlDelX*tlDelX+tlDelY*tlDelY);

        brDelX = boundRect.br().x-PrevBoundRect.br().x;
        brDelY = maxY-PrevBoundRect.br().y;
        brDel = Math.sqrt(brDelX*brDelX+brDelY*brDelY);


        if (PrevBoundRect.tl()==InitialP1 || PrevBoundRect.br()==InitialP2){

        }
        else if(Math.abs(tlDel)>200){
            temptlX=PrevBoundRect.tl().x+tlDelX/5;
            temptlY=PrevBoundRect.tl().y+tlDelY/5;
        }
        else if(Math.abs(tlDel)<=200 && Math.abs(tlDel)>100){
            temptlX=PrevBoundRect.tl().x+tlDelX/4;
            temptlY=PrevBoundRect.tl().y+tlDelY/4;
        }
        else if(Math.abs(tlDel)<=100 && Math.abs(tlDel)>50){
            temptlX=PrevBoundRect.tl().x+tlDelX/3;
            temptlY=PrevBoundRect.tl().y+tlDelY/3;
        }
        else{
            temptlX=PrevBoundRect.tl().x+tlDelX/2;
            temptlY=PrevBoundRect.tl().y+tlDelY/2;
        }


        if (PrevBoundRect.tl()==InitialP1 || PrevBoundRect.br()==InitialP2){
        }
        else if(Math.abs(brDel)>200){
            tempbrX=PrevBoundRect.br().x+brDelX/5;
            tempbrY=PrevBoundRect.br().y+brDelY/5;
        }
        else if(Math.abs(brDel)<=200 && Math.abs(brDel)>100){
            tempbrX=PrevBoundRect.br().x+brDelX/4;
            tempbrY=PrevBoundRect.br().y+brDelY/4;
        }
        else if(Math.abs(brDel)<=100 && Math.abs(brDel)>50){
            tempbrX=PrevBoundRect.br().x+brDelX/3;
            tempbrY=PrevBoundRect.br().y+brDelY/3;
        }
        else{

            tempbrX=PrevBoundRect.br().x+brDelX/2;
            tempbrY=PrevBoundRect.br().y+brDelY/2;
        }


        Rect tempRect = new Rect(new Point(temptlX, temptlY), new Point(tempbrX, tempbrY));
        boundRect = tempRect;
        if(tracking==true) {
            Imgproc.rectangle(mRgba, boundRect.tl(), boundRect.br(), CONTOUR_COLOR_BLACK, 2, 8, 0);
        }

        //Imgproc.rectangle(mRgba, boundRect.tl(), boundRect.br(), CONTOUR_COLOR_BLACK, 2, 8, 0);
        PrevBoundRect = boundRect;


        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        float tx = (float) mappedX;
        float ty = (float) mappedY;
        int metaState = 0;

        double horizontal = boundRect.br().x-boundRect.tl().x;
        double vertical = boundRect.br().y-boundRect.tl().y;

        currentArea = horizontal*vertical;

        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        if(currentArea>((width/2)*(height/2)) ||  currentArea<(width/20*height/20) || horizontal>=width/2 || horizontal<=20 || vertical <= 20){
            tracking = false;
            optimalArea = 4000;
            currentArea = 4000;
        }

        if(numberOfFingers>=5 && horizontal*vertical<mRgba.rows()/2*mRgba.cols()/2) {
            //optimalArea = horizontal*vertical;
        }

        if(numberOfFingers<5 && vertical*1.8<horizontal && pressed==false) {//For when start touching

            motionEvent = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    MotionEvent.ACTION_DOWN,
                    tx,
                    ty,
                    metaState
            );
            pressed = true;
        }
        else if(numberOfFingers<5 && vertical*1.8<horizontal && pressed==true){//For dragging
            motionEvent = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    MotionEvent.ACTION_MOVE,
                    tx,
                    ty,
                    metaState
            );
            pressed = true;
        }
        else{
            pressed = false;
        }

        return mRgba;
    }

    public void updateNumberOfFingers() {
        numberOfFingersText.setText(String.valueOf(this.numberOfFingers));
    }


    private Point getPointOfView(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        return new Point(location[0], location[1]);
    }
}
