package com.ibm.watson.scavenger.visualrecognition;

import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;

/**
 * Created by arpitrastogi on 10/08/17.
 */

public class VRMain {
    private String version=null;
    private String apiKey=null;
    private VisualRecognition vr_svc = null;

    public VRMain(String version, String apiKey)
    {
        this.version = version;
        this.apiKey = apiKey;
        vr_svc = new VisualRecognition(version);
        vr_svc.setApiKey(apiKey);
    }

    public VisualRecognition getVRInstance()
    {
        return vr_svc;
    }
}
