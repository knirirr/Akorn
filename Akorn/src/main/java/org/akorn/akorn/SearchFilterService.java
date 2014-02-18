package org.akorn.akorn;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.akorn.akorn.contentprovider.AkornContentProvider;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;


/**
 * Created by milo on 18/02/2014.
 */
public class SearchFilterService extends IntentService
{
  public static boolean isRunning = false;
  private static String TAG = "AkornSearchFilterService";
  private String tempurl;
  public static final String URL = "http://akorn.org/api/";
  public static final String DEVURL = "http://akorn.org:8000/api/";
  private Handler mHandler;

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

  @Override
  protected void onHandleIntent(Intent intent)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    String server = prefs.getString("server_pref","prod");
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
    try
    {
      String search_id = intent.getStringExtra("search_id");
      if (search_id.equals("new"))
      {
        // create a new search
        mHandler.post(new ToastRunnable("A search would be created here."));
        CreateSearch();
        // created by a new dialog activity
      }
      else
      {
        // delete an existing search
        mHandler.post(new ToastRunnable("A search would be deleted here: " + search_id));
        DeleteSearch(search_id);
      }
    }
    catch (Exception e)
    {
      Log.i(TAG, "Error: " + e.toString());
    }
  }

  public void DeleteSearch(String search_id)
  {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpGet httpGet = new HttpGet(tempurl + "remove_search?query_id=" + search_id);
    try
    {
      HttpResponse response = client.execute(httpGet);
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == 200)
      {
        Log.e(TAG, "Deleted search " + search_id + " from server.");
        // now delete the search from the local database, as well as the associated articles
        Uri cleanup = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches/" + search_id);
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

  public void CreateSearch()
  {

  }

  @Override
  public void onDestroy()
  {
    isRunning = false;
    Log.i(TAG, "Finished!");
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
