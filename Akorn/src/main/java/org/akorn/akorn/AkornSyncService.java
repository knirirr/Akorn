package org.akorn.akorn;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

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
  private Handler mHandler;

  public AkornSyncService()
  {
    super("AkornSyncService");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    // the purpose of this handler is to flip out and allow toasts to be
    // displayed by the uploader service when it starts and stops
    mHandler = new Handler();
    //mHandler.post(new ToastRunnable(getString(R.string.startString)));
    return super.onStartCommand(intent,flags,startId);
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();
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
      else if (statusCode == 204) // no content but successful login
      {
        // get the cookie here
        List<Cookie> cookiejar = client.getCookieStore().getCookies();
        for (Cookie bict : cookiejar)
        {
          Log.i(TAG,"Name: " + bict.getName());
          Log.i(TAG,"Value: " + bict.getValue());
          if (bict.getName().equals("sessionid"))
          {
            session_id = bict.getValue();
            mHandler.post(new ToastRunnable("Got the session ID!"));
          }
        }
        if (session_id.equals(""))
        {
          Log.i(TAG,"Failed to get a session id");
          mHandler.post(new ToastRunnable("FRC no session ID!"));
          return;
        }
      }
      else if (statusCode == 400)
      {
        mHandler.post(new ToastRunnable(getString(R.string.loginFail)));
        Log.i(TAG,"Bad username or password!");
      }
      else
      {
        // something
        Log.i(TAG,"Status (failure): " + statusCode);
        Log.i(TAG,"Response: " + response.toString());
        Log.i(TAG,"Post: " + post.toString());
        mHandler.post(new ToastRunnable(getString(R.string.syncFail)));
        return;
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /*
    This code copied from CrowTrack
   */
  private class ToastRunnable implements Runnable
  {
    String mText;

    public ToastRunnable(String text)
    {
      mText = text;
    }

    @Override
    public void run()
    {
      Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT).show();
    }
  }

}
