/*
    The MIT License (MIT)
    Copyright (c) 2013 Vlad Stirbu
    
    Permission is hereby granted, free of charge, to any person obtaining
    a copy of this software and associated documentation files (the
    "Software"), to deal in the Software without restriction, including
    without limitation the rights to use, copy, modify, merge, publish,
    distribute, sublicense, and/or sell copies of the Software, and to
    permit persons to whom the Software is furnished to do so, subject to
    the following conditions:
    
    The above copyright notice and this permission notice shall be
    included in all copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
    EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
    NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
    LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
    OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
    WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.vladstirbu.cordova;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import androidx.core.content.FileProvider;

@TargetApi(Build.VERSION_CODES.FROYO)
public class CDVInstagramPlugin extends CordovaPlugin {

    private static final FilenameFilter OLD_IMAGE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith("instagram");
        }
    };

    CallbackContext cbContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        this.cbContext = callbackContext;

        Log.v("Instagram", "execute thing?? " + action);

        if (action.equals("share")) {
            String imageString = args.getString(0);
            String captionString = args.getString(1);
            String postURL = args.getString(2);

            PluginResult result = new PluginResult(Status.NO_RESULT);
            result.setKeepCallback(true);

            this.share(imageString, captionString, postURL);
            return true;
        } else if (action.equals("isInstalled")) {
            this.isInstalled();
        } else {
            callbackContext.error("Invalid Action");
        }
        return false;
    }

    private void isInstalled() {
        try {
            this.webView.getContext().getPackageManager().getApplicationInfo("com.instagram.android", 0);
            this.cbContext.success(this.webView.getContext().getPackageManager().getPackageInfo("com.instagram.android", 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            this.cbContext.error("Application not installed");
        }
    }

    private void share(String imageString, String captionString, String postURL) {
        if (imageString != null && imageString.length() > 0) {
            byte[] imageData = Base64.decode(imageString, 0);

            File file = null;
            FileOutputStream os = null;

            File parentDir = this.webView.getContext().getExternalFilesDir(null);
            File[] oldImages = parentDir.listFiles(OLD_IMAGE_FILTER);
            for (File oldImage : oldImages) {
                oldImage.delete();
            }

            try {
                file = File.createTempFile("instagram", ".png", parentDir);
                os = new FileOutputStream(file, true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.v("Instagram", "file created thing??");

            try {
                os.write(imageData);
                os.flush();
                os.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Intent shareIntent = new Intent("com.instagram.share.ADD_TO_STORY");
            shareIntent.setType("image/jpeg");


            Log.v("Instagram", "intent added to story thing??");
            Activity activity = this.cordova.getActivity();


                FileProvider FileProvider = new FileProvider();

                Uri photoURI = FileProvider.getUriForFile(
                        this.cordova.getActivity().getApplicationContext(),
                        this.cordova.getActivity().getPackageName() + ".provider",
                        file);



                shareIntent.putExtra("interactive_asset_uri", photoURI);
                shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Log.v("Instagram", "URI: " + photoURI);

                activity.grantUriPermission(
                        "com.instagram.android", photoURI, Intent.FLAG_GRANT_READ_URI_PERMISSION);


            Log.v("Instagram", "share activity starting");

            shareIntent.putExtra("top_background_color", "#3A3A3A");
            shareIntent.putExtra("bottom_background_color", "#3A3A3A");

            if (postURL != null) {
                shareIntent.putExtra("content_url", postURL);
            } else {
                shareIntent.putExtra("content_url", "https://trackster.us/download");
            }


            if (activity.getPackageManager().resolveActivity(shareIntent, 0) != null) {

                Log.v("Instagram", "share actually activity starting");
                activity.startActivityForResult(shareIntent, 0);
            }

//            this.cordova.startActivityForResult((CordovaPlugin) this, Intent.createChooser(shareIntent, "Share to"), 12345);
        } else {
            this.cbContext.error("Expected one non-empty string argument.");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Log.v("Instagram", "shared ok");
            if(this.cbContext != null) {
                this.cbContext.success();
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.v("Instagram", "share cancelled");
            if(this.cbContext != null) {
                this.cbContext.error("Share Cancelled");
            }
        }
    }
}