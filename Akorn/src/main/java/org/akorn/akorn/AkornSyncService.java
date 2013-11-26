package org.akorn.akorn;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
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
  private CookieStore cookiestore;

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
        cookiestore = client.getCookieStore();
        List<Cookie> cookiejar = cookiestore.getCookies();
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
    /*
      The next step is to get the user's saved searches, before using these to obtain the user's articles.
     */
    HttpContext localContext = new BasicHttpContext();
    localContext.setAttribute(ClientContext.COOKIE_STORE, cookiestore); // use cookie store grabbed above
    HttpGet httpGet = new HttpGet(URL + "searches");
    InputStream inputStream = null;
    String jsonResult;
    try
    {
      HttpResponse response = client.execute(httpGet, localContext);
      HttpEntity entity = response.getEntity();
      inputStream = entity.getContent();

      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
      StringBuilder sb = new StringBuilder();

      String line = null;
      while ((line = reader.readLine()) != null)
      {
        sb.append(line + "\n");
      }
      jsonResult = sb.toString();
      Log.i(TAG, "Some JSON: " + jsonResult);
    }
    catch (IOException e)
    {
      Log.i(TAG, "Error getting search info: " + e.toString());
      return;
    }
    finally
    {
      try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
    }

    /*
      Now we should have the json in jsonResult and can try parsing it
     */
    try
    {
      JSONObject jObject = new JSONObject(jsonResult);
      // There must be a better way to get the json array out than using what appears to be
      // an arbitrary key here. I'll have to look into it.
      JSONArray jArray = jObject.getJSONArray("bd20075c-e1ef-4b54-aec4-fadb9a6f5e4e");
      for (int i=0; i < jArray.length(); i++)
      {
        try
        {
          JSONObject oneObject = jArray.getJSONObject(i);
          // Pulling items from the array
          String j_text = oneObject.getString("text");
          String j_full = oneObject.getString("full");
          String j_type = oneObject.getString("type");
          String j_id = oneObject.getString("id");
          Log.i(TAG, "JSON parsed: " + j_text + "," + j_full + "," + j_type + "," + j_id);
        }
        catch (JSONException e)
        {
          // Oops
        }
      }

    }
    catch (JSONException e)
    {
      Log.e(TAG,"Can't parse JSON from server: " + e.toString());
      // a toast here, perhaps?
      return;
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
