package org.akorn.akorn;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by milo on 18/11/2013.
 */
public class AkornSyncService extends IntentService
{
  public static final String URL = "http://akorn.org/api/";
  private static final String TAG = "AkornSyncService";

  public AkornSyncService()
  {
    super("AkornSyncService");
  }

  protected void onHandleIntent(Intent intent)
  {
    /*
      If the user has got this far then they must have supplied their username
      and password, for the app won't start this service unless both are defined.
     */
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    String username = prefs.getString("pref_username", "");
    String password = prefs.getString("pref_password", "");
    String session_id = "";

    /*
      First, get the session id for use later
     */
    DefaultHttpClient client = new DefaultHttpClient();
    HttpPost post = new HttpPost(URL + "login");

    List<NameValuePair> params = new ArrayList<NameValuePair>();
    params.add(new BasicNameValuePair("username",username));
    params.add(new BasicNameValuePair("password",password));

    UrlEncodedFormEntity formEntity = null;
    try
    {
      formEntity = new UrlEncodedFormEntity(params);
    }
    catch (UnsupportedEncodingException e)
    {
      Log.e(TAG, "Attempt to prepare URL failed.");
      e.printStackTrace();
    }
    post.setEntity(formEntity);

    try
    {
      HttpResponse response = client.execute(post);
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == HttpStatus.SC_OK)
      {
        HttpEntity entity = response.getEntity();
        InputStream is = entity.getContent();
        Log.i(TAG,"Status (success): " + statusCode);
        Log.i(TAG,"Got answer:" + is.toString());
      }
      else
      {
        // something
        Log.i(TAG,"Status (failure): " + statusCode);
        Log.i(TAG,"Response: " + response.toString());
        Log.i(TAG,"Post: " + post.toString());
        // return here, with a toast mentioning failure to sync
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

}
