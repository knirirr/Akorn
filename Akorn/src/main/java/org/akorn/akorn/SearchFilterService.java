package org.akorn.akorn;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.akorn.akorn.contentprovider.AkornContentProvider;
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
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by milo on 18/02/2014.
 */
public class SearchFilterService extends IntentService
{
  public static boolean isRunning = false;
  private static String TAG = "AkornSearchFilterService";
  private static int MAGIC_NUMBER = 2001;
  private String tempurl;
  private CookieStore cookiestore;
  public static final String URL = "http://akorn.org/api/";
  public static final String DEVURL = "http://akorn.org:8000/api/";
  private Handler mHandler;
  private NotificationManager notificationManager;

  public SearchFilterService()
  {
    super("SearchFilterService");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    // the purpose of this handler is to flip out and allow toasts to be
    // displayed by the uploader service when it starts and stops
    mHandler = new Handler();
    return super.onStartCommand(intent,flags,startId);
  }

  private void sendMessage()
  {
    Intent intent = new Intent("filters-changed");
    // add data
    intent.putExtra("message", "Filters have changed!");
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

  @Override
  protected void onHandleIntent(Intent intent)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    String server = prefs.getString("server_pref","prod");
    String username = prefs.getString("pref_username", "");
    String password = prefs.getString("pref_password", "");


    String session_id = "";
    if (server.equals("prod"))
    {
      tempurl = URL;
    }
    else
    {
      tempurl = DEVURL;
    }
    isRunning = true;
    Log.i(TAG, "Started!");

    // stuff passed in via the intent - needed as some information must go into the
    // notification which will be created next
    String search_id = intent.getStringExtra("search_id");
    String ftype = intent.getStringExtra("ftype");
    String jtext = intent.getStringExtra("jtext");
    String jid = intent.getStringExtra("jid");
    String notificationMessage = getString(R.string.background_sync); // default if correct message not set

    if (search_id.equals("new"))
    {
      if (ftype.equals("journal"))
      {
        notificationMessage = getString(R.string.creating_new_journal_filter);
      }
      else if (ftype.equals("keyword"))
      {
       notificationMessage = getString(R.string.creating_new_keyword_filter);
      }
    }
    else
    {
      notificationMessage = getString(R.string.deleting_filter);
    }


    // http://stackoverflow.com/questions/5061760/how-does-one-animate-the-android-sync-status-icon
    // a nice sync message, I hope
    notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    Notification twirler = new NotificationCompat.Builder(getApplicationContext())
        .setContentTitle(getString(R.string.app_name))
        .setContentText(notificationMessage)
        .setSmallIcon(android.R.drawable.ic_popup_sync)
        .setWhen(System.currentTimeMillis())
        .setOngoing(true)
        .build();
    notificationManager.notify(MAGIC_NUMBER, twirler);



    // log in as it seems the cookies must be obtained again here
    DefaultHttpClient client = new DefaultHttpClient();
    HttpPost post = new HttpPost(tempurl + "login");
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
      return;
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
          /*
          Log.i(TAG,"Name: " + bict.getName());
          Log.i(TAG,"Value: " + bict.getValue());
          */
          if (bict.getName().equals("sessionid"))
          {
            session_id = bict.getValue();
            mHandler.post(new ToastRunnable(getString(R.string.loginSuccess)));
          }
        }
        if (session_id.equals(""))
        {
          Log.i(TAG, "Failed to get a session id");
          mHandler.post(new ToastRunnable(getString(R.string.syncFail)));
          return;
        }
      }
      else if (statusCode == 400)
      {
        mHandler.post(new ToastRunnable(getString(R.string.loginFail)));
        Log.i(TAG,"Bad username or password!");
        return;
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
      mHandler.post(new ToastRunnable(getString(R.string.syncFail)));
      e.printStackTrace();
      return;
    }


    /*
      Create or delete searches
    */

    try
    {
      //String search_id = intent.getStringExtra("search_id");
      if (search_id.equals("new"))
      {
        // create a new search
        mHandler.post(new ToastRunnable("A search would be created here."));
        //String ftype = intent.getStringExtra("ftype");
        //String jtext = intent.getStringExtra("jtext");
        //String jid = intent.getStringExtra("jid");
        CreateSearch(ftype,jtext,jid);
        // created by a new dialog activity
      }
      else
      {
        // delete an existing search
        //mHandler.post(new ToastRunnable("A search would be deleted here: " + search_id));
        DeleteSearch(search_id);
      }
    }
    catch (Exception e)
    {
      Log.i(TAG, "Error: " + e.toString());
    }

    // finally, notify the activity that this service has finished
    sendMessage();
  }

  public void DeleteSearch(String search_id)
  {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpContext localContext = new BasicHttpContext();
    localContext.setAttribute(ClientContext.COOKIE_STORE, cookiestore);
    HttpGet httpGet = new HttpGet(tempurl + "remove_search?query_id=" + search_id);
    try
    {
      HttpResponse response = client.execute(httpGet,localContext);
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == 204)
      {
        Log.e(TAG, "Deleted search " + search_id + " from server.");
        // now delete the search from the local database, as well as the associated articles
        Uri cleanup = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/search/" + search_id);
        getContentResolver().delete(cleanup,null,null);
        // made it!
        mHandler.post(new ToastRunnable(getString(R.string.delete_win)));
      }
      else
      {
        Log.e(TAG, "FRC, status code: " + String.valueOf(statusCode));
        mHandler.post(new ToastRunnable(getString(R.string.delete_fail)));
      }
    }
    catch (Exception e)
    {
      Log.e(TAG, "Oh noes! " + e.toString());
      mHandler.post(new ToastRunnable(getString(R.string.delete_fail)));
    }

  }

  public void CreateSearch(String ftype, String jtext, String jid)
  {

    DefaultHttpClient client = new DefaultHttpClient();
    HttpContext localContext = new BasicHttpContext();
    localContext.setAttribute(ClientContext.COOKIE_STORE, cookiestore);
    Log.i(TAG, "FTYPE: " + ftype);
    Log.i(TAG, "JTEXT: " + jtext);
    Log.i(TAG, "JID: " + jid);
    /*
    Create Search endpoint (*)
    $ curl -vX POST -d "query=<json>" http://akorn.org/api/searches

    The JSON representation of the saved search should be of the same form as that returned by GET requests to the same endpoint.
    Success
    Status: 200
    Body: {"query_id": "<query_id>"} 
    {"bd20075c-e1ef-4b54-aec4-fadb9a6f5e4e": [{"text": "PLoS ONE", "full":
"PLoS ONE", "type": "journal", "id":
"8b05225aac657bd955fd49e2819d3609"}]}

     */
    //mHandler.post(new ToastRunnable("I haven't written this bit of the code yet."));

    /*
    String json = "";
    if (ftype.equals("keyword"))
    {
      json = "'%7B\"text\": \"" + jtext + "\", \"full\": \"" + jtext + "\", \"type\": \"" + ftype + "\"%7D'";
    }
    else
    {
      json = "'%7B\"text\": \"" + jtext + "\", \"full\": \"" + jtext + "\", \"type\": \"" + ftype + "\", \"id\": \"" + jid + "\"%7D'";
    }
    Log.i(TAG, "JSON: " + json);
    */
    JSONObject json = new JSONObject();
    try
    {
      json.put("text", jtext);
      json.put("full", jtext);
      json.put("type", ftype);
      if (!jid.isEmpty())
      {
        json.put("id",jid);
      }
      else
      {
        json.put("id",jtext);
      }
    }
    catch (Exception e)
    {
      Log.e(TAG, "Couldn't create JSON: " + e.toString());
    }

    JSONArray submission = new JSONArray();
    submission.put(json);
    JSONObject topJson = new JSONObject();
    try
    {
      topJson.put("query", submission);
    }
    catch (Exception e)
    {
      Log.e(TAG, "Couldn't create second JSON: " + e.toString());
    }

    //HttpPost httpPost = new HttpPost(tempurl + "searches?query=" + URLEncoder.encode(topJson.toString()));
    Log.i(TAG, "URLUnencoded: " + tempurl + "searches?query=" + submission.toString());
    Log.i(TAG, "URLEncoded: " + tempurl + "searches?query=" + java.net.URLEncoder.encode(submission.toString()));
    HttpPost httpPost = new HttpPost(tempurl + "searches?query=" + java.net.URLEncoder.encode(submission.toString()));
    //HttpPost httpPost = new HttpPost(tempurl + "searches?query=" + json.toString());
    //Log.i(TAG, "JSON: " + json.toString());
    //HttpPost httpPost = new HttpPost(tempurl + "searches?query=");

    try
    {
      /*
      //Log.i(TAG, "JSON: " + topJson.toString());http://codelikethis.tumblr.com/rss
      //StringEntity se = new StringEntity( "JSON: " + topJson.toString());
      Log.i(TAG, "JSON: " + json.toString());
      StringEntity se = new StringEntity( "JSON: " + json.toString());
      Log.i(TAG, "StringEntitiy: " + se.toString());
      se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
      httpPost.setEntity(se);
      */

      HttpResponse response = client.execute(httpPost,localContext);
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == 200)
      {
        mHandler.post(new ToastRunnable("Holy crap, it worked!"));
      }
      else
      {
        Log.e(TAG, "FRC, status code: " + String.valueOf(statusCode));
        mHandler.post(new ToastRunnable("FRC, it failed!"));
      }
    }
    catch (Exception e)
    {
      Log.e(TAG, "Oh noes! " + e.toString());
      mHandler.post(new ToastRunnable("Exception, FRC!"));
    }
  }

  @Override
  public void onDestroy()
  {
    isRunning = false;
    Log.i(TAG, "Finished!");
    notificationManager.cancel(MAGIC_NUMBER);
    super.onDestroy();
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
