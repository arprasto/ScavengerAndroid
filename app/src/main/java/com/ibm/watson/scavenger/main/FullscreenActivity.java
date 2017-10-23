package com.ibm.watson.scavenger.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arpitrastogi.stest.R;
import com.ibm.watson.developer_cloud.android.library.camera.CameraHelper;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifierOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;
import com.ibm.watson.scavenger.iot.IoTUtilService;
import com.ibm.watson.scavenger.tts.ScavengerTextToSpeech;
import com.ibm.watson.scavenger.util.ZipFiles;
import com.ibm.watson.scavenger.visualrecognition.VRMain;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.internal.huc.OkHttpsURLConnection;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable =new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    FloatingActionButton exit_button = null,upload_btn=null,capture_btn=null,train_btn=null,trainCaptureImgActionButton = null,exitTraining_btn=null;
    SpeechToText stt_svc = null;
    boolean permissionToRecordAccepted = false,permissionOfCamera = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String TAG = "MainActivity";
    private static final int RECORD_REQUEST_CODE = 101,REQUEST_GALLERY =2;
    private CameraHelper cameraHelper;
    private List<File> trainingFiles = new ArrayList<File>();
    VRMain vr_svc = null;
    private Context mContext;
    private Chronometer counter = null;
    private int train_no_of_image = 10;
    private TextView trainCountRemaining = null;

    /*
    *
    * all services credentials
    *
     */

    private String
            tts_uname = null,
            tts_pass = null;

    /*
    * IoT device configs
     */
     public static String
            Organization_ID=null,
            type=null,
            id=null,
            Authentication_Token=null,
            Authentication_Method=null,
            iot_event_for_img_base64 = null,

    /*
    * Visual Recognition credentials
     */
            vr_api_key = null,
            vr_classifier_name = null,
            vr_version = VisualRecognition.VERSION_DATE_2016_05_20;


    public static ScavengerTextToSpeech tts_svc = null;
    private PowerManager.WakeLock wakeLock = null;
    public static IoTUtilService iot_svc = null;
    public static BlockingQueue<URI> queue = new LinkedBlockingQueue<URI>();
    private boolean training_flag = false,positive_flag=false,remove_capture_btn_flag=false;
    String vr_class_name = "",default_negative_zip=null;
    File positive_zip = null,negative_zip = null;
    final Context context = this;
    long camera_visible_time_frame=0;
    public static TextView imgsQueSize=null,imgsProcessing=null;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case RECORD_REQUEST_CODE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permission has been denied by user");
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                }
                return;
            }
            case CameraHelper.REQUEST_PERMISSION:
                permissionOfCamera = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case CameraHelper.REQUEST_IMAGE_CAPTURE:
                permissionOfCamera = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted || !permissionOfCamera) finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        //set all watson services credentials
        mContext = getApplicationContext();

        try {
        negative_zip = File.createTempFile("negative_examples","zip");
            default_negative_zip = mContext.getString(R.string.default_negative_zip);
            FileOutputStream os = new FileOutputStream(negative_zip);
        InputStream is = mContext.getAssets().open(default_negative_zip);
            byte[] buffer = new byte[1024];
            while(is.read(buffer) != -1){
                os.write(buffer);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera_visible_time_frame = Long.valueOf(mContext.getString(R.string.time_frame).trim());
        train_no_of_image = Integer.valueOf(mContext.getString(R.string.train_no_of_image).trim());

        System.out.println(negative_zip.getPath()+":: arpit ::"+negative_zip.exists());
        tts_uname = mContext.getString(R.string.tts_uname);
        tts_pass= mContext.getString(R.string.tts_pass);
        Organization_ID= mContext.getString(R.string.Organization_ID);
        type= mContext.getString(R.string.type);
        id= mContext.getString(R.string.id);
        Authentication_Token= mContext.getString(R.string.Authentication_Token);
        Authentication_Method= mContext.getString(R.string.Authentication_Method);
        iot_event_for_img_base64= mContext.getString(R.string.iot_event_for_img_base64);
        vr_api_key= mContext.getString(R.string.vr_api_key);
        vr_classifier_name= mContext.getString(R.string.vr_classifier_name);

        //final Iterator<VisualClassifier> classifiers_it = null;

        imgsQueSize=(TextView)findViewById(R.id.imgsQueSize);
        imgsProcessing=(TextView)findViewById(R.id.imgsProcessing);

        counter = (Chronometer) findViewById(R.id.chronCounter);
        //counter.setFormat("H:MM:SS");
        counter.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {

                String[] data = chronometer.getText().toString().split(":");
                long time_millis = 0;
                if(data.length == 3) {

                    int hours = Integer.parseInt(data[0]);
                    int minutes = Integer.parseInt(data[1]);
                    int seconds = Integer.parseInt(data[2]);

                    int time_secs = seconds + 60 * minutes + 3600 * hours;
                    time_millis = TimeUnit.MILLISECONDS.convert(time_secs, TimeUnit.SECONDS);
                }

                else{
                    int minutes = Integer.parseInt(data[0]);
                    int seconds = Integer.parseInt(data[1]);

                    int time_secs = seconds + 60 * minutes ;
                    time_millis = TimeUnit.MILLISECONDS.convert(time_secs, TimeUnit.SECONDS);
                }

                if(time_millis >= camera_visible_time_frame-10000
                        && time_millis <= camera_visible_time_frame)
                {
                    counter.setTextColor(Color.rgb(255,0,0));
                }
                if(time_millis >= camera_visible_time_frame)
                {
                    counter.stop();
                    capture_btn.setVisibility(View.INVISIBLE);
                    remove_capture_btn_flag = true;
                }
            }
        });


        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.ExitActionButton).setOnTouchListener(mDelayHideTouchListener);

        trainCountRemaining = (TextView) findViewById(R.id.trainCountRemaining);
        trainCountRemaining.setVisibility(View.INVISIBLE);

        /*ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }*/

        if(checkInternetConnection()){
            /*
            initiate all the services instances
             */
            tts_svc = new ScavengerTextToSpeech(tts_uname,tts_pass);
            vr_svc = new VRMain(vr_version,vr_api_key);


            this.iot_svc = new IoTUtilService(FullscreenActivity.Organization_ID,
                    FullscreenActivity.type,
                    FullscreenActivity.id,
                    FullscreenActivity.Authentication_Token,
                    FullscreenActivity.Authentication_Method,
                    FullscreenActivity.iot_event_for_img_base64,
                    FullscreenActivity.this);

            try {
                this.iot_svc.getIot_client().connect(true);
            } catch (MqttException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(),"can not connect to IoT, App will exit now "+e.getMessage(),Toast.LENGTH_LONG);
                System.exit(0);
            }

            /*stt_svc = new SpeechToText();
            stt_svc.setUsernameAndPassword(stt_uname,stt_pass);*/



            train_btn = (FloatingActionButton) findViewById(R.id.trainClassifier);
            train_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    training_flag=true;
                    upload_btn.setVisibility(View.INVISIBLE);
                    capture_btn.setVisibility(View.INVISIBLE);
                    train_btn.setVisibility(View.INVISIBLE);
                    exit_button.setVisibility(View.INVISIBLE);

                    trainCaptureImgActionButton.setVisibility(View.VISIBLE);
                    exitTraining_btn.setVisibility(View.VISIBLE);
                    trainCountRemaining.setVisibility(View.VISIBLE);
                    trainCountRemaining.setText("Trainer Imgs remain : "+train_no_of_image);
                    trainCountRemaining.setTextColor(Color.rgb(0,0,255));

                    tts_svc.playText("you can use camera capture button to capture the images. " +
                            "you need to give. "+train_no_of_image+". positive images to train the model. " +
                            "Once you capture them custom classifier will be automatically created.",Voice.EN_MICHAEL);

                    LayoutInflater li = LayoutInflater.from(context);
                    View promptsView = li.inflate(R.layout.prompts, null);

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                            context);

                    // set prompts.xml to alertdialog builder
                    alertDialogBuilder.setView(promptsView);

                    final EditText userInput = (EditText) promptsView
                            .findViewById(R.id.editTextDialogUserInput);

                    // set dialog message
                    alertDialogBuilder
                            .setCancelable(false)
                            .setPositiveButton("OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,int id) {
                                            // get user input and set it to result
                                            // edit text
                                            vr_class_name = userInput.getText().toString().trim();
                                            tts_svc.playText("please capture. "+train_no_of_image+". positive images now.",Voice.EN_MICHAEL);
                                        }
                                    })
                            .setNegativeButton("Cancel",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,int id) {
                                            dialog.cancel();
                                            exitTraining_btn.performClick();
                                        }
                                    });

                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();

                    // show it
                    alertDialog.show();
                    positive_flag = true;
                }
            });

            trainCaptureImgActionButton = (FloatingActionButton) findViewById(R.id.trainCaptureImgActionButton);
            trainCaptureImgActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cameraHelper.dispatchTakePictureIntent();
                }
            });

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MyWakelockTag");
            wakeLock.acquire();

            exit_button = (FloatingActionButton) findViewById(R.id.ExitActionButton);
            exit_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applicationExit();
                }
            });

            exitTraining_btn=(FloatingActionButton) findViewById(R.id.ExitTrainingActionButton);
            exitTraining_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    upload_btn.setVisibility(View.VISIBLE);
                    trainCountRemaining.setVisibility(View.INVISIBLE);
                    if(!remove_capture_btn_flag)
                    capture_btn.setVisibility(View.VISIBLE);
                    train_btn.setVisibility(View.VISIBLE);
                    exit_button.setVisibility(View.VISIBLE);

                    trainCaptureImgActionButton.setVisibility(View.INVISIBLE);
                    exitTraining_btn.setVisibility(View.INVISIBLE);
                    training_flag = false;

                    if(trainingFiles.size() < train_no_of_image){
                        Toast.makeText(getApplicationContext(), "cleaning up training images", Toast.LENGTH_LONG).show();
                        for(File f:trainingFiles){
                            f.delete();
                        }
                        trainingFiles.removeAll(trainingFiles);
                    }

                }
            });

            upload_btn = (FloatingActionButton) findViewById(R.id.uploadActionButton);
            upload_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, REQUEST_GALLERY);
                }
            });

            cameraHelper = new CameraHelper(this);

            capture_btn = (FloatingActionButton) findViewById(R.id.CaptureImgActionButton);
            capture_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cameraHelper.dispatchTakePictureIntent();
                }
            });

        }
        else{
            Toast.makeText(this, " No Internet Connection available ", Toast.LENGTH_LONG).show();
        }
        trainCaptureImgActionButton.setVisibility(View.INVISIBLE);
        exitTraining_btn.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if(requestCode == CameraHelper.REQUEST_IMAGE_CAPTURE){

                Bitmap selectedImage = cameraHelper.getBitmap(resultCode);
                //System.out.println(" bitmap captured : "+selectedImage.getHeight());
                selectedImage = resizeBitmapForWatson(selectedImage, 902);
                // Reformat Bitmap into a .jpg and save as file to input to Watson.
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                selectedImage.compress(Bitmap.CompressFormat.JPEG, 80, bytes);

                File tempPhoto = null;
                try {
                    tempPhoto = File.createTempFile("capture", ".jpg", getCacheDir());
                    FileOutputStream out = new FileOutputStream(tempPhoto);
                    out.write(bytes.toByteArray());
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //System.out.println("created file = "+tempPhoto.exists()+":"+tempPhoto.getAbsolutePath());
                if(!training_flag) {
                    this.queue.add(tempPhoto.toURI());
                }
                if(training_flag)
                    {
                    trainingFiles.add(tempPhoto);
                        trainCountRemaining.setText("Trainer Imgs remain : "+(train_no_of_image-trainingFiles.size()));

                        if(trainingFiles.size()>=train_no_of_image){
                            if(positive_flag) {
                                positive_flag=false;
                                tts_svc.playText("you have captured positive images. Please wait.", Voice.EN_MICHAEL);
                                positive_zip = new ZipFiles().createAndAddZipFiles(vr_class_name +"_positive_example.zip",trainingFiles);
                                trainingFiles=null;
                                trainingFiles=new ArrayList<File>();
                            }
                            /*if(!positive_flag) {
                                tts_svc.playText("you have captured ten negative images now. Please wait.", Voice.EN_MICHAEL);
                                negative_zip = new ZipFiles().createAndAddZipFiles("negative_example.zip",trainingFiles);
                                trainingFiles=null;
                                trainingFiles=new ArrayList<File>();
                            }*/
                        }
                        if(negative_zip != null && positive_zip != null) {
                            new CreateClassifier(negative_zip, positive_zip, vr_class_name).execute();
                            training_flag = false;
                            exitTraining_btn.performClick();
                        }
                    }
            }
            if(requestCode == REQUEST_GALLERY){
                Uri uri = data.getData();

                // Fetch the Bitmap from the Uri.
                Bitmap selectedImage = fetchBitmapFromUri(uri);

                // Resize the Bitmap to constrain within Watson Image Recognition's Size Limit.
                selectedImage = resizeBitmapForWatson(selectedImage, 902);

                // Reformat Bitmap into a .jpg and save as file to input to Watson.
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                selectedImage.compress(Bitmap.CompressFormat.JPEG, 80, bytes);

                File tempPhoto = null;
                try {
                    tempPhoto = File.createTempFile("gallery", ".jpg", getCacheDir());

                    FileOutputStream out = new FileOutputStream(tempPhoto);
                    out.write(bytes.toByteArray());
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("created file = "+tempPhoto.exists()+":"+tempPhoto.getAbsolutePath());
                this.queue.add(tempPhoto.toURI());
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
        String random_img_obj_str=getRandomImgObjString(mContext.getString(R.string.allowable_obj_set).split(","),
                Integer.valueOf(mContext.getString(R.string.possible_number_of_obj).trim()));

        if(!this.getIntent().hasExtra("calling_act"))
        tts_svc.playText("welcome watson bluemix platform. To end the game anytime click on exit " +
                "button. To train and create the custom class. click on train button." +
                " you have "+camera_visible_time_frame/1000+". seconds to capture the images. "+
                " you need to capture images that should contain objects like "+random_img_obj_str, Voice.EN_MICHAEL);

        counter.start();
        //new ListenVoice().execute();

    }

    private String getRandomImgObjString(String[] allowable_obj_set, int possible_number_of_obj) {
        String obj_str = "";
        int nxt,i=0;
        Set<Integer> random_num = new HashSet<Integer>();
        if(possible_number_of_obj>allowable_obj_set.length){
            possible_number_of_obj = allowable_obj_set.length;
        }

        do{
            nxt = new Random().nextInt(allowable_obj_set.length);
            if(!random_num.contains(new Integer(nxt)))
            {
                obj_str = allowable_obj_set[nxt]+". "+obj_str;
                random_num.add(nxt);
                i++;
            }

        }while(i<possible_number_of_obj);

        return obj_str;
    }

    /**
     * Scales the given image to an image that fits within the size constraints placed by Visual Recognition.
     * @param originalImage Full-sized Bitmap to be scaled.
     * @param maxSize The maximum allowed dimension of the image.
     * @return The original image rescaled so that it's largest dimension is equal to maxSize
     */
    private Bitmap resizeBitmapForWatson(Bitmap originalImage, float maxSize) {

        int originalHeight = originalImage.getHeight();
        int originalWidth = originalImage.getWidth();

        int boundingDimension = (originalHeight > originalWidth) ? originalHeight : originalWidth;

        float scale = maxSize / boundingDimension;

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        originalImage = Bitmap.createBitmap(originalImage, 0, 0, originalWidth, originalHeight, matrix, true);

        return originalImage;
    }

    /**
     * Fetches a bitmap image from the device given the image's uri.
     * @param imageUri Uri of the image on the device (either in the gallery or from the camera).
     * @return A Bitmap representation of the image on the device, correctly orientated.
     */
    private Bitmap fetchBitmapFromUri(Uri imageUri) {
        try {
            // Fetch the Bitmap from the Uri.
            Bitmap selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            // Fetch the orientation of the Bitmap in storage.
            String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
            Cursor cursor = getContentResolver().query(imageUri, orientationColumn, null, null, null);
            int orientation = 0;
            if (cursor != null && cursor.moveToFirst()) {
                orientation = cursor.getInt(cursor.getColumnIndex(orientationColumn[0]));
            }
            cursor.close();

            // Rotate the bitmap with the found orientation.
            Matrix matrix = new Matrix();
            matrix.setRotate(orientation);
            selectedImage = Bitmap.createBitmap(selectedImage, 0, 0, selectedImage.getWidth(), selectedImage.getHeight(), matrix, true);

            return selectedImage;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * application exit task
     */
     void applicationExit(){
        tts_svc.playText("Thanks for using Watson Bluemix platform. Hoping to see you soon on " +
                "Bluemix",Voice.EN_MICHAEL);
        try {
            Thread.sleep(Long.parseLong("10000"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        wakeLock.release();
         this.iot_svc.getIot_client().disconnect();
        finish();
        System.exit(0);
    }

    /**
     * Check Internet Connection
     * @return
     */
    private boolean checkInternetConnection() {
        // get Connectivity Manager object to check connection
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        // Check for network connections
        if (isConnected){
            return true;
        }
        else {
            Toast.makeText(this, " No Internet Connection available ", Toast.LENGTH_LONG).show();
            return false;
        }

    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    /*private class ListenVoice extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {
            MicrophoneInputStream voice_stream = new MicrophoneInputStream(true);
            stt_svc.recognizeUsingWebSocket(voice_stream, getRecognizeOptions(), new MicrophoneRecognizeDelegate());
            return null;
        }
    }*/

    public class CreateClassifier extends AsyncTask<Void,Void,Void>{
        public OkHttpsURLConnection con = null;

        File negative_zip=null,positive_zip = null;
        String class_name = null;
        public CreateClassifier(File negative_zip,File positive_zip,String class_name){
            this.negative_zip = negative_zip;
            this.positive_zip = positive_zip;
            this.class_name =class_name;
        }

        @Override
        protected Void doInBackground(Void... params) {
            tts_svc.playText("classifier is being trained now. Please wait.", Voice.EN_MICHAEL);
            ClassifierOptions classifierOptions = new ClassifierOptions.Builder().classifierName(vr_classifier_name)
                    .negativeExamples(negative_zip)
                    .addClass(class_name, positive_zip)
                    .build();

            boolean create_classifier_flag = true, update_classifier_flage=false,exit_loop=false;
            String classifier_id=null;

            Iterator<VisualClassifier> classifiers_it = vr_svc.getVRInstance().getClassifiers().execute().iterator();
            while(classifiers_it.hasNext()){
                VisualClassifier classifier = classifiers_it.next();
                Log.d(TAG,"classifier varification");
                Log.d(TAG,"checking for "+classifier.getName());
                if(classifier.getName().equalsIgnoreCase(vr_classifier_name))
                {
                    create_classifier_flag = false;
                    classifier_id = classifier.getId();
                    Log.d(TAG,"found pre-existing "+classifier.getName()+":"+classifier_id);
                    int cnt=0;
                    for(VisualClassifier.VisualClass claz:classifier.getClasses())
                    {
                        cnt++;
                        Log.d(TAG," checking for "+classifier_id+"."+claz.getName());
                        if(claz.getName().equalsIgnoreCase(class_name)){
                            Log.d(TAG," found preexisting "+classifier_id+"."+claz.getName());
                            tts_svc.playText("looks like given class name already exists. Please try giving different class name. and rerun training.",Voice.EN_MICHAEL);
                            try {
                                Thread.sleep(6000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //System.exit(0);
                        }
                        else if(cnt==classifier.getClasses().size()){
                            update_classifier_flage = true;
                            exit_loop =true;
                        }
                        if(exit_loop) break;
                    }
                    if(exit_loop)
                        break;
                }
                if(exit_loop) break;
            }


            if(create_classifier_flag){
                Log.d(TAG,"creating new classifier "+classifierOptions.classifierName());
                tts_svc.playText("looks like you are training very first time. Let me train the model for you.",Voice.EN_MICHAEL);
                VisualClassifier vc = vr_svc.getVRInstance().createClassifier(classifierOptions).execute();
                tts_svc.playText("your classifier is being trained. please try using it after some time.",Voice.EN_MICHAEL);

            }

            if(update_classifier_flage && classifier_id!=null){
                Log.d(TAG,"updating classifier "+classifierOptions.classifierName());
                tts_svc.playText("Let me update classifier for you.",Voice.EN_MICHAEL);
                VisualClassifier vc = vr_svc.getVRInstance().updateClassifier(classifier_id, classifierOptions).execute();
                tts_svc.playText("your classifier is being updated. please try using it after some time.",Voice.EN_MICHAEL);
            }
            return null;
        }
    }
}

