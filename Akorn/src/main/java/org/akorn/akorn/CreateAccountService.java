package org.akorn.akorn;

import android.app.IntentService;
import android.app.NotificationManager;
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
 * Created by milo on 05/02/2014.
 */
public class CreateAccountService extends IntentService
{
  private Handler mHandler;
  private NotificationManager notificationManager;

  public static boolean isRunning = false;
  private static String REGISTER = "http://akorn.org/api/register";
  private static String TAG = "AkornCreateAccountService";

  public CreateAccountService()
  {
    super("CreateAccountService");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    // the purpose of this handler is to flip out and allow toasts to be
    // displayed by the uploader service when it starts and stops
    mHandler = new Handler();
    return super.onStartCommand(intent,flags,startId);
  }

  @Override
  public void onDestroy()
  {
    isRunning = false;
    super.onDestroy();
  }

  protected void onHandleIntent(Intent intent)
  {
    isRunning = true;

    // the notification service is essential for toasts
    notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    // this will be needed in order to set the hasAccount setting to true if
    // account creation succeeds
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    String email = intent.getStringExtra("email");
    String password = intent.getStringExtra("password");
    String password_confirmation = intent.getStringExtra("password_confirmation");

    DefaultHttpClient client = new DefaultHttpClient();
    HttpPost post = new HttpPost(REGISTER);

    List<NameValuePair> params = new ArrayList<NameValuePair>();
    params.add(new BasicNameValuePair("email",email));
    params.add(new BasicNameValuePair("password1",password));
    params.add(new BasicNameValuePair("password2",password_confirmation));

    UrlEncodedFormEntity formEntity = null;
    try
    {
      formEntity = new UrlEncodedFormEntity(params);
    }
    catch (UnsupportedEncodingException e)
    {
      Log.e(TAG, "Attempt to prepare URL failed.");
      e.printStackTrace();
      return;
    }
    post.setEntity(formEntity);

    // START
    try
    {
      HttpResponse response = client.execute(post);
      int statusCode = response.getStatusLine().getStatusCode();
      HttpEntity entity = response.getEntity();
      InputStream is = entity.getContent();
      if (statusCode == 200) // account created
      {
        // the body should be empty so there isn't really any need to do anything with it
        Log.i(TAG,"Status (success): " + statusCode);
        Log.i(TAG,"Got answer:" + is.toString());
        mHandler.post(new ToastRunnable(getString(R.string.create_success)));
      }
      else if (statusCode == 400) // error
      {
        //mHandler.post(new ToastRunnable(getString(R.string.loginFail)));
        //Log.i(TAG,"Bad username or password!");

        return;
      }
      else
      {
        // something
        Log.i(TAG,"Status (failure): " + statusCode);
        Log.i(TAG,"Response: " + response.toString());
        Log.i(TAG,"Post: " + post.toString());
        return;
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return;
    }
    // END

  }

  /*
  This code copied from CrowTrack.
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
