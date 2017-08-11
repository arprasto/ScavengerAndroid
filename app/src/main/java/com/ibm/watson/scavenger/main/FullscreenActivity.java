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
import android.widget.EditText;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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

    //n - where n negative and n positive images.
    private int train_no_of_image = 10;

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
    private boolean training_flag = false,positive_flag=false,negative_flag=false;
    String classifier_name = "";
    File positive_zip = null,negative_zip = null;

    final Context context = this;

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

        /*ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }*/

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

                tts_svc.playText("you can use camera capture button to capture the images. " +
                        "you need to give ten positive and ten negative images to train the model. " +
                        "Once you capture them custom classifier will be automatically created." +
                        "please capture ten positive images first.",Voice.EN_MICHAEL);

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
                                        classifier_name = userInput.getText().toString().trim();
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
                capture_btn.setVisibility(View.VISIBLE);
                train_btn.setVisibility(View.VISIBLE);
                exit_button.setVisibility(View.VISIBLE);

                trainCaptureImgActionButton.setVisibility(View.INVISIBLE);
                exitTraining_btn.setVisibility(View.INVISIBLE);
                training_flag = false;
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
                    FullscreenActivity.iot_event_for_img_base64);

            try {
                this.iot_svc.getIot_client().connect(true);
            } catch (MqttException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(),"can not connect to IoT, App will exit now "+e.getMessage(),Toast.LENGTH_LONG);
                System.exit(0);
            }

            /*stt_svc = new SpeechToText();
            stt_svc.setUsernameAndPassword(stt_uname,stt_pass);*/

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
                selectedImage = resizeBitmapForWatson(selectedImage, 904);
                // Reformat Bitmap into a .jpg and save as file to input to Watson.
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                selectedImage.compress(Bitmap.CompressFormat.JPEG, 0, bytes);

                File tempPhoto = null;
                try {
                    tempPhoto = File.createTempFile("capture", ".jpg", getCacheDir());
                    FileOutputStream out = new FileOutputStream(tempPhoto);
                    out.write(bytes.toByteArray());
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("created file = "+tempPhoto.exists()+":"+tempPhoto.getAbsolutePath());
                if(!training_flag)
                this.queue.add(tempPhoto.toURI());
                if(training_flag)
                    {
                    trainingFiles.add(tempPhoto);
                        if(trainingFiles.size()>=train_no_of_image){
                            if(positive_flag) {
                                positive_flag=false;
                                tts_svc.playText("you have captured ten positive images. Please wait.", Voice.EN_MICHAEL);
                                positive_zip = new ZipFiles().createAndAddZipFiles("positive_example.zip",trainingFiles);
                                trainingFiles.removeAll(trainingFiles);
                                tts_svc.playText("you need to capture 10 negative images now.", Voice.EN_MICHAEL);
                            }
                            if(!positive_flag) {
                                trainingFiles.removeAll(trainingFiles);
                                negative_zip = new ZipFiles().createAndAddZipFiles("negative_example.zip",trainingFiles);
                                tts_svc.playText("classifier is being trained now. Please wait.", Voice.EN_MICHAEL);
                                exitTraining_btn.performClick();
                            }
                        }
                        if(negative_zip != null && positive_zip != null)
                        new CreateClassifier(negative_zip,positive_zip,classifier_name).execute();
                    }
            }
            if(requestCode == REQUEST_GALLERY){
                Uri uri = data.getData();

                // Fetch the Bitmap from the Uri.
                Bitmap selectedImage = fetchBitmapFromUri(uri);

                // Resize the Bitmap to constrain within Watson Image Recognition's Size Limit.
                selectedImage = resizeBitmapForWatson(selectedImage, 904);

                // Reformat Bitmap into a .jpg and save as file to input to Watson.
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                selectedImage.compress(Bitmap.CompressFormat.JPEG, 0, bytes);

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

        if(!this.getIntent().hasExtra("calling_act"))
        tts_svc.playText("welcome watson bluemix platform. To end the game anytime click on exit button.", Voice.EN_MICHAEL);
        //new ListenVoice().execute();

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

 /*   //Private Methods - Speech to Text
    private RecognizeOptions getRecognizeOptions() {
        return new RecognizeOptions.Builder()
                .continuous(true)
                .contentType(ContentType.OPUS.toString())
                .model("en-US_BroadbandModel")
                .interimResults(true)
                .inactivityTimeout(-1)
                .build();
    }

    private class MicrophoneRecognizeDelegate implements RecognizeCallback {
        @Override
        public void onTranscription(SpeechResults speechResults) {
            System.out.println(speechResults);
            if(speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
                String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
                System.out.println("---------------------------"+text+"---------------------------");
                if(text.toLowerCase().contains("game")){
                    Intent scavenger = new Intent(FullscreenActivity.this,Scavenger.class);
                    FullscreenActivity.this.startActivityForResult(scavenger,1);
                }

                if(text.toLowerCase().contains("i'm done") ||
                        text.toLowerCase().contains("i am done") ||
                        text.toLowerCase().contains("exit")){
                    applicationExit();
                }
            }
        }

        @Override public void onConnected() {

        }

        @Override public void onError(Exception e) {
        }

        @Override public void onDisconnected() {
        }

        @Override
        public void onInactivityTimeout(RuntimeException runtimeException) {

        }

        @Override
        public void onListening() {

        }

        @Override
        public void onTranscriptionComplete() {

        }
    }*/

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
        String classifier_name = null;
        public CreateClassifier(File negative_zip,File positive_zip,String classifier_name){
            this.negative_zip = negative_zip;
            this.positive_zip = positive_zip;
            this.classifier_name=classifier_name;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ClassifierOptions classifierOptions = new ClassifierOptions.Builder().classifierName(vr_classifier_name)
                    .negativeExamples(negative_zip)
                    .addClass(classifier_name, positive_zip)
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
                        if(claz.getName().equalsIgnoreCase(classifier_name)){
                            Log.d(TAG," found preexisting "+classifier_id+"."+claz.getName());
                            tts_svc.playText("looks like given class name already exists. Please try giving different class name and rerun this application",Voice.EN_MICHAEL);
                            System.exit(0);
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
                while(true){
                    String res = null;
                    try {
                        Thread.sleep(6000);
                        try{
                            res = vr_svc.getVRInstance().getClassifier(vc.getId()).execute().getStatus().toString();
                        }catch(Exception e){
                            e.printStackTrace();
                            tts_svc.playText("classifier is being trained. please try using it after some time",Voice.EN_MICHAEL);
                            System.exit(0);
                        }
                        if(res.toLowerCase().contains("training")){
                            tts_svc.playText("please wait classifier is being trained.",Voice.EN_MICHAEL);
                        }
                        else if(res.toLowerCase().contains("ready")){
                            tts_svc.playText("classifier has been trained now. To create another classifier you need to rerun this application.",Voice.EN_MICHAEL);
                            System.exit(0);
                        }
                        else if(res.toLowerCase().contains("fail")){
                            tts_svc.playText("there was some error while creating classifier. Please try again later.",Voice.EN_MICHAEL);
                            vr_svc.getVRInstance().deleteClassifier(vc.getId());
                            System.exit(0);
                        }
                        else{
                            tts_svc.playText("System will exit now. Please try again later.",Voice.EN_MICHAEL);
                            System.exit(0);
                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            if(update_classifier_flage && classifier_id!=null){
                Log.d(TAG,"updating classifier "+classifierOptions.classifierName());
                tts_svc.playText("Let me update classifier for you.",Voice.EN_MICHAEL);
                VisualClassifier vc = vr_svc.getVRInstance().updateClassifier(classifier_id, classifierOptions).execute();
                while(true){
                    String res = null;
                    try {
                        Thread.sleep(6000);
                        try{
                            res = vr_svc.getVRInstance().getClassifier(vc.getId()).execute().getStatus().toString();
                        }catch(Exception e){
                            e.printStackTrace();
                            tts_svc.playText("classifier is being trained. please try using it after some time",Voice.EN_MICHAEL);
                            System.exit(0);
                        }
                        if(res.toLowerCase().contains("retraining")){
                            tts_svc.playText("please wait classifier is being retrained.",Voice.EN_MICHAEL);
                        }
                        else if(res.toLowerCase().contains("ready")){
                            tts_svc.playText("classifier has been trained now. To create another classifier you need to rerun this application.",Voice.EN_MICHAEL);
                            System.exit(0);
                        }
                        else if(res.toLowerCase().contains("fail")){
                            tts_svc.playText("there was some error while creating classifier. Please try again later.",Voice.EN_MICHAEL);
                            vr_svc.getVRInstance().deleteClassifier(vc.getId());
                            System.exit(0);
                        }
                        else{
                            tts_svc.playText("System will exit now. Please try again later.",Voice.EN_MICHAEL);
                            System.exit(0);
                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }


            return null;
        }
    }
}

