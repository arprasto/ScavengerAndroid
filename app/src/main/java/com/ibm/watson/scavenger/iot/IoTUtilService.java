package com.ibm.watson.scavenger.iot;

import android.util.Log;

import com.ibm.watson.scavenger.main.FullscreenActivity;
import com.google.gson.JsonObject;
import com.ibm.iotf.client.device.Command;
import com.ibm.iotf.client.device.CommandCallback;
import com.ibm.iotf.client.device.DeviceClient;
import com.ibm.watson.scavenger.util.IMGUtils;

import java.io.File;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by arpitrastogi on 02/08/17.
 */

public class IoTUtilService{

    public String getIot_event_for_img_base64() {
        return iot_event_for_img_base64;
    }

    private String iot_event_for_img_base64=null;
    private DeviceClient iot_client = null;

    public IoTUtilService(String Organization_ID,
                          String type,
                          String id,
                          String Authentication_Token,
                          String Authentication_Method,
                          String iot_event_for_img_base64) {
        Properties options = new Properties();
        options.setProperty("Organization-ID", Organization_ID);
        options.setProperty("type", type);
        options.setProperty("id", id);
        options.setProperty("Authentication-Token", Authentication_Token);
        options.setProperty("Authentication-Method", Authentication_Method);
        this.iot_event_for_img_base64 = iot_event_for_img_base64;
        try {
            iot_client = new DeviceClient(options);
            //start the call back thread to receive device-commands from IoT.
            MyNewCommandCallback callback = new MyNewCommandCallback();
            Thread t = new Thread(callback);
            t.start();
            iot_client.setCommandCallback(callback);
            iot_client.setKeepAliveInterval(-1);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("IoTUtilService", " Not able to connect to watson IoT " + e.getMessage(),e);
        }
    }

    public DeviceClient getIot_client()
    {
        return iot_client;
    }

}

/*
 * Utility class to receive device commands for registered device from IoT.
 */

class MyNewCommandCallback implements CommandCallback, Runnable {

    // A queue to hold & process the commands for smooth handling of MQTT messages
    private BlockingQueue<Command> queue = new LinkedBlockingQueue<Command>();

    /**
     * This method is invoked by the library whenever there is command matching the subscription criteria
     */
    public void processCommand(Command cmd) {
        try {
            queue.put(cmd);
        } catch (InterruptedException e) {
        }
    }

    public void run() {
        System.out.println("Backgroud thread started");
        boolean publish_flag = true,first_run = true;
        while(true) {
            Command cmd = null;
            try {
                cmd = queue.take();
                System.out.println("COMMAND RECEIVED = '" + cmd.getCommand() + "'\t with data="+cmd.getData().toString());

                if(cmd.getCommand().equals("refreshUI"))
                {
                    publish_flag = true;
                }

                if(cmd.getCommand().equals("checkForPublishIoT") || first_run){
                    first_run = false;
                    if(FullscreenActivity.queue.size() > 0 && publish_flag)
                    {
                        try {
                            publish_flag = false;
                            URI uri = FullscreenActivity.queue.take();
                            System.out.println("posting to IoT:"+uri.toString());
                            JsonObject event_payload = new JsonObject();
                            event_payload.addProperty("img_base64",new IMGUtils().encodeIMGFileToBase64Binary(new File(uri)));
                            event_payload.addProperty("img_id", new File(uri).getName());
                            ///new FullscreenActivity.PushToIoT(event_payload).execute();
                            FullscreenActivity.iot_svc.getIot_client().publishEvent(FullscreenActivity.iot_svc.getIot_event_for_img_base64(),event_payload);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (InterruptedException e) {}
        }
    }
}
