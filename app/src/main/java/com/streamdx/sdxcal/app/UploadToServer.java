package com.streamdx.sdxcal.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vvo on 9/8/15.
 */

public class UploadToServer extends Activity
{
    private final String TAG = "UploadToServer";
    private final String upLoadServerUri = "http://db3.us/test/post-sdx-data.php";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Bundle extras = savedInstanceState;
        if (extras == null) {
            extras = getIntent().getExtras();
        }

        if (extras != null && extras.containsKey("DATA")) {
            String data = extras.getString("DATA");
            new SendPost().execute(upLoadServerUri, data);
        }

        finish();
    }

    private void sendData(String upLoadServerUri, String data)
    {
        if (data == null)
            return;

        HttpPost httppost = new HttpPost(upLoadServerUri);
        HttpClient httpclient = new DefaultHttpClient();

        try {
            // Add your data
            StringBuffer sb = new StringBuffer(data.length());
            sb.append("3|");
            sb.append("'");
            sb.append(data);
            sb.append("'");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            //nameValuePairs.add(new BasicNameValuePair("username", "scott"));
            //nameValuePairs.add(new BasicNameValuePair("password", "scott2015"));
            nameValuePairs.add(new BasicNameValuePair("new_data", sb.toString()));
            nameValuePairs.add(new BasicNameValuePair("postdata", "Post Data"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = httpclient.execute(httppost);

            Log.d(TAG, String.format("HttpResponse: (%d)\r\n%s",
                    response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity())));

        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "::sendData", e);
        }
    }

    class SendPost extends AsyncTask <String, Void, Integer>{

        private Exception exception;

        protected Integer doInBackground(String... s) {
            try {
                sendData(s[0], s[1]);
            } catch (Exception e) {
                this.exception = e;
                Log.e(TAG, "::SendPost::doInBackground", e);
            }

            return 0;
        }
    }

}
