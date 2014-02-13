package org.akorn.akorn;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.akorn.akorn.contentprovider.AkornContentProvider;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
  private static String DEVREGISTER = "http://akorn.org:8000/api/register";
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
    Log.i(TAG, "Create account service finishing");
    isRunning = false;
    super.onDestroy();
  }

  protected void onHandleIntent(Intent intent)
  {
    isRunning = true;

    // the notification service is essential for toasts
    notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    Log.i(TAG, "Started create account service");

    // this will be needed in order to set the hasAccount setting to true if
    // account creation succeeds
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    String server = prefs.getString("server_pref","prod");

    String email = intent.getStringExtra("email");
    String password = intent.getStringExtra("password");
    String password_confirmation = intent.getStringExtra("password_confirmation");

    DefaultHttpClient client = new DefaultHttpClient();
    String tempurl = "";
    if (server.equals("prod"))
    {
      tempurl = REGISTER;
    }
    else
    {
      tempurl = DEVREGISTER;
    }
    HttpPost post = new HttpPost(tempurl);

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

        // having created the account the proper values should be inserted into the app's preferences
        // for use the next time a sync is attempted
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("pref_username", email);
        editor.putString("pref_password", password);
        editor.commit();

        // success
        mHandler.post(new ToastRunnable(getString(R.string.create_success)));

        // having succeeded, the next step should be to re-direct to the main activity
        Intent dialogIntent = new Intent(getBaseContext(), ViewingActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplication().startActivity(dialogIntent);

      }
      else if (statusCode == 400) // error
      {
        //mHandler.post(new ToastRunnable(getString(R.string.loginFail)));
        //Log.i(TAG,"Bad username or password!");
        String jsonResult;
        try
        {
          BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
          StringBuilder sb = new StringBuilder();

          String line = null;
          while ((line = reader.readLine()) != null)
          {
            sb.append(line + "\n");
          }
          jsonResult = sb.toString();
          Log.i(TAG, "Some JSON: " + jsonResult);
          /*
          At this point it should be possible to tell the user why their attempt to create an account failed...
          I've not tried catching all the things individually here, as there's an IOException catcher at the end.
          What the server returns should look like this:
          ome JSON: {"errors": {"password2": ["Passwords don't match"], "email": ["Akorn user with this Email address already exists."]}}
          */
          JSONObject jObject = new JSONObject(jsonResult);
          String errorString = "";
          try { errorString = jObject.getString("errors"); } catch (JSONException e) {Log.e(TAG, e.toString());}
          JSONObject errorObject = new JSONObject(errorString);

          JSONArray emails = errorObject.getJSONArray("email");
          JSONArray passes2 = errorObject.getJSONArray("password2");
          JSONArray passes1 = errorObject.getJSONArray("password1");

          Log.i(TAG, "emails: " + emails.get(0).toString());
          Log.i(TAG, "passes2: " + passes2.get(0).toString());
          Log.i(TAG, "passes1: " + passes1.get(0).toString());

          String emailError = emails.get(0).toString();
          String passError2 = passes2.get(0).toString();
          String passError1 = passes1.get(0).toString();

          StringBuilder errorMsg = new StringBuilder();
          errorMsg.append(getString(R.string.failed_because) + "\n");
          if (!emailError.toString().isEmpty())
          {
            Log.i(TAG,"Email: " + emailError);
            errorMsg.append("Email: " + emailError + "\n");
          }
          if (!passError1.toString().isEmpty())
          {
            Log.i(TAG,"Password: " + passError1);
            errorMsg.append("Password: " + passError1 + "\n");
          }
          if (!passError2.toString().isEmpty())
          {
            Log.i(TAG,"Password confirmation: " + passError2);
            errorMsg.append("Password confirmation: " + passError2);
          }
          mHandler.post(new ToastRunnable(errorMsg.toString()));
        }
        catch (IOException e)
        {
          // mark error here
          Log.e(TAG, "JSON error: " + e.toString());
          return;
        }
        finally
        {
          try{ if(is != null) is.close(); }
          catch(Exception squish){
            // make sure it's closed
          }
        }
        return;
      }
      else
      {
        // something
        Log.i(TAG,"Status (failure): " + statusCode);
        Log.i(TAG,"Response: " + response.toString());
        Log.i(TAG,"Post: " + post.toString());
        mHandler.post(new ToastRunnable(getString(R.string.create_fail)));
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
      Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_LONG).show();
    }
  }

}
