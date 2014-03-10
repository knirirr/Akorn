package org.akorn.akorn;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.akorn.akorn.contentprovider.AkornContentProvider;
import org.akorn.akorn.database.ArticleTable;
import org.akorn.akorn.database.SearchArticleTable;
import org.akorn.akorn.database.JournalsTable;
import org.akorn.akorn.database.SearchTable;
import org.apache.commons.lang3.StringUtils;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * Created by milo on 18/11/2013.
 */
public class AkornSyncService extends IntentService
{
  public static final String URL = "http://akorn.org/api/";
  public static final String DEVURL = "http://akorn.org:8000/api/";
  private static final String TAG = "AkornSyncService";
  private Handler mHandler;
  private CookieStore cookiestore;
  private Map<String,String> searchresults;
  private NotificationManager notificationManager;
  private Bitmap icon;
  private Notification noti;
  private int notificationId;

  public AkornSyncService()
  {
    super("AkornSyncService");
  }

  public static boolean isRunning = false;
  public static boolean hasFailed = false;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    // the purpose of this handler is to flip out and allow toasts to be
    // displayed by the uploader service when it starts and stops
    mHandler = new Handler();
    //mHandler.post(new ToastRunnable(getString(R.string.startString)));
    return super.onStartCommand(intent,flags,startId);
  }

  private void sendMessage()
  {
    Intent intent = new Intent("filters-changed");
    // add data
    intent.putExtra("message", "Filters have changed!");
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

  @Deprecated // must do something about this eventually
  @Override
  public void onDestroy()
  {
    isRunning = false;
    String notificationText = getString(R.string.syncFinish);
    if (hasFailed == true)
    {
      notificationText = getString(R.string.syncFail);
      hasFailed = false;
    }
    Intent activityIntent = new Intent(this,ViewingActivity.class);
    PendingIntent launchIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

    noti = new Notification.Builder(this)
        .setContentTitle("Akorn")
        .setContentIntent(launchIntent)
        .setContentText(notificationText)
        .setSmallIcon(R.drawable.ic_stat)
        .setLargeIcon(icon)
        .getNotification();

    // Hide the notification after its selected
    noti.flags |= Notification.FLAG_AUTO_CANCEL;
    notificationManager.notify(1000, noti);
    notificationManager.cancel(2000);
    super.onDestroy();
  }

  protected void onHandleIntent(Intent intent)
  {
    /*
      If the user has got this far then they must have supplied their username
      and password, for the app won't start this service unless both are defined.
     */
    isRunning = true;



    // bitmap for notification icon
    icon = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_launcher);

    // the notification service is essential
    notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    // http://stackoverflow.com/questions/5061760/how-does-one-animate-the-android-sync-status-icon
    // a nice sync message, I hope
    Notification twirler = new NotificationCompat.Builder(getApplicationContext())
        .setContentTitle(getString(R.string.app_name))
        .setContentText(getString(R.string.background_sync))
        .setSmallIcon(android.R.drawable.ic_popup_sync)
        .setWhen(System.currentTimeMillis())
        .setOngoing(true)
        .build();
    notificationManager.notify(2000, twirler);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    String username = prefs.getString("pref_username", "");
    String password = prefs.getString("pref_password", "");
    String server = prefs.getString("server_pref","prod");
    String session_id = "";

    Log.i(TAG, "Server settings: " + server);

    // store the values from each successful search result in a hash in order to
    // make getting the articles easier later
    searchresults = new HashMap<String, String>();

    /*
      First, get the session id for use later
     */
    DefaultHttpClient client = new DefaultHttpClient();
    String tempurl = "";
    if (server.equals("prod"))
    {
      mHandler.post(new ToastRunnable("Using production url (port 80)."));
      tempurl = URL;
    }
    else
    {
      mHandler.post(new ToastRunnable("Using development url (port 8000)."));
      tempurl = DEVURL;
    }
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
      hasFailed = true;
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
          Log.i(TAG,"Name: " + bict.getName());
          Log.i(TAG,"Value: " + bict.getValue());
          if (bict.getName().equals("sessionid"))
          {
            session_id = bict.getValue();
            mHandler.post(new ToastRunnable(getString(R.string.loginSuccess)));
          }
        }
        if (session_id.equals(""))
        {
          Log.i(TAG,"Failed to get a session id");
          mHandler.post(new ToastRunnable(getString(R.string.syncFail)));
          hasFailed = true;
          return;
        }
      }
      else if (statusCode == 400)
      {
        mHandler.post(new ToastRunnable(getString(R.string.loginFail)));
        hasFailed = true;
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
      hasFailed = true;
      mHandler.post(new ToastRunnable(getString(R.string.syncFail)));
      e.printStackTrace();
      return;
    }
    /*
      The next step is to get the user's saved searches, before using these to obtain the user's articles.
     */
    HttpContext localContext = new BasicHttpContext();
    localContext.setAttribute(ClientContext.COOKIE_STORE, cookiestore); // use cookie store grabbed above
    HttpGet httpGet = new HttpGet(tempurl + "searches");
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
      /*
        If all those data have come back successfully then the logical next step given the database schema
        is to purge the searches table ready to insert the new stuff below...
      */
      Uri uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches");
      getContentResolver().delete(uri, null, null);
    }
    catch (IOException e)
    {
      Log.i(TAG, "Error getting search info: " + e.toString());
      hasFailed = true;
      return;
    }
    finally
    {
      try{ if(inputStream != null)inputStream.close(); }
      catch(Exception squish){
      // make sure it's closed
      }
    }

    /*
      Now we should have the json in jsonResult and can try parsing it.
      If anyone can think of a more cunning way to parse what the Akorn server sends then by
      all means let me know.
     */
    try
    {
      JSONObject jObject = new JSONObject(jsonResult);
      // all the keys of the array must be collected and iterated over in order to pull out the arrays
      // of search terms. Then, these will have to be crammed into the database somehow
      JSONArray namearray=jObject.names();

      // namearray may in fact be empty at this point...
      if (namearray == null)
      {
        mHandler.post(new ToastRunnable(getString(R.string.nosearches)));
        // clean out all articles not marked as starred
        Log.i(TAG,"No searches, cleaning up...");
        Uri cleanup = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/purge_articles");
        getContentResolver().delete(cleanup,null,null);
        Log.i(TAG,"Done!");
        return;
      }

      for (int h=0; h < namearray.length(); h++)
      {
        JSONArray jArray = jObject.getJSONArray(namearray.getString(h));
        ArrayList<String> keywordseen = new ArrayList<String>();
        ArrayList<String> journalseen = new ArrayList<String>();
        //Map<String,Boolean> keywordseen = new HashMap<String, Boolean>();
        //Map<String,Boolean> journalseen = new HashMap<String, Boolean>();
        for (int i=0; i < jArray.length(); i++)
        {
          try
          {
            JSONObject oneObject = jArray.getJSONObject(i);
            // Pulling items from the array 
            String pe = "Couldn't get term from JSON object: ";
            String j_type = null;
            String j_full = null;
            String j_text = null;
            String j_id = null;
            try { j_text = oneObject.getString("text"); } catch (JSONException e) {Log.e(TAG, pe + e.toString());}
            try { j_full = oneObject.getString("full"); } catch (JSONException e) {Log.e(TAG, pe + e.toString());}
            try { j_type = oneObject.getString("type"); } catch (JSONException e) {Log.e(TAG, pe + e.toString());}
            try { j_id = oneObject.getString("id"); } catch (JSONException e) { Log.e(TAG, pe + e.toString()); }
            Log.i(TAG, "JSON parsed: " + namearray.getString(h) + ": " + j_text + "," + j_full + "," + j_type + "," + j_id);

            // now construct some ContentValues to insert
            Uri uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches");
            String s_id = namearray.getString(h);
            ContentValues values = new ContentValues();
            values.put(SearchTable.COLUMN_TEXT,j_text);
            values.put(SearchTable.COLUMN_FULL,j_full);
            values.put(SearchTable.COLUMN_TYPE,j_type);
            values.put(SearchTable.COLUMN_SEARCH_ID,s_id);
            values.put(SearchTable.COLUMN_TERM_ID,j_id);
            getContentResolver().insert(uri, values);

            // also update the searchresults hashmap so this can be used to grab the results
            if (searchresults.containsKey(s_id))
            {
              String current = searchresults.get(s_id);
              if (j_full == null)
              {
                if (keywordseen.contains(s_id))
                {
                  searchresults.put(s_id, current + "%7Ck=" + j_text);
                  keywordseen.add(s_id);
                }
                else
                {
                  searchresults.put(s_id, current + "%7C" + j_text);
                }
              }
              else
              {
                if (journalseen.contains(s_id))
                {
                  searchresults.put(s_id, current + "%7Cj=" + j_id);
                  journalseen.add(s_id);
                }
                else
                {
                  searchresults.put(s_id, current + "%7C" + j_id);
                }
              }
            }
            else
            {
              if (j_full == null)  // keyword
              {
                searchresults.put(s_id,"k=" + j_text);
                journalseen.add(s_id);
              }
              else
              {
                searchresults.put(s_id,"j=" + j_id);
                keywordseen.add(s_id);
              }
            }

          }
          catch (JSONException e)
          {
            // Oops
            hasFailed = true;
            Log.e(TAG, "Couldn't parse JSON: " + e.toString());
            return;
          }
        }
      }
    }
    catch (JSONException e)
    {
      Log.e(TAG,"Can't parse JSON from server: " + e.toString());
      hasFailed = true;
      // a toast here, perhaps?
      return;
    }

    /*
      Now the searches have been obtained, it's finally time to get the articles. According to the website devs:
      http://akorn.org/api/articles?skip=0&limit=20&k=hello%7Cmilo
        &j=f45f136fbd14caa156e5b4b846113877%7Cf45f136fbd14caa156e5b4b8461113e9%7Cf45f136fbd14caa156e5b4b8460b3344
      It looks like the "keyword" type items are being put in the k argument and the journal ids from the "journal"
      type items are being put in the j argument, joined together with whatever %7C is
      un-urlencoded, possibly a "+" symbol.
      ...oh, it's a pipe!
     */

    Log.i(TAG, "START");
    for (Map.Entry<String,String> entry : searchresults.entrySet())
    {
      String key = entry.getKey();
      String value = entry.getValue();
      //String articleUrl = URL + "articles.xml?" + value.replace(" ","%20");
      String articleUrl = tempurl + "articles.xml?" + value.replace(" ","%20");
      Log.i(TAG,"URL: " + articleUrl);

      try
      {
        StringBuilder content = new StringBuilder();
        java.net.URL xmlPage = new java.net.URL(articleUrl);
        URLConnection connection = xmlPage.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
        {
          content.append(inputLine + "\n");
        }
        Document doc = Jsoup.parse(content.toString(), "", Parser.xmlParser());
        Elements articles = doc.getElementsByTag("article");
        ContentValues values;
        ContentValues joinValues;
        for (Element article : articles)
        {
          values = new ContentValues();
          joinValues = new ContentValues();
          joinValues.put(SearchArticleTable.COLUMN_SEARCH_ID,key);
          Elements authors = article.getElementsByTag("authors");
          for (Element a : authors)
          {
            Elements names = a.getElementsByTag("author");
            StringBuilder builder = new StringBuilder();
            int first = 0;
            for (Element n : names)
            {
              if (first == 0)
              {
                builder.append(org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4(n.html()));
              }
              else
              {
                builder.append(", " + org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4(n.html()));
              }
              first++;
            }
            values.put(ArticleTable.COLUMN_AUTHORS, builder.toString());
          }
          Elements id = article.getElementsByTag("id");
          for (Element i : id)
          {
            values.put(ArticleTable.COLUMN_ARTICLE_ID,org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4(i.html()));
            joinValues.put(SearchArticleTable.COLUMN_ARTICLE_ID,org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4(i.html()));
          }
          Elements journal = article.getElementsByTag("journal");
          for (Element j : journal)
          {
            values.put(ArticleTable.COLUMN_JOURNAL,org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4(j.html()));
          }
          Elements title = article.getElementsByTag("title");
          for (Element t : title)
          {
            values.put(ArticleTable.COLUMN_TITLE,org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4(t.html()));
          }
          Elements waffle = article.getElementsByTag("abstract");
          for (Element w : waffle)
          {
            values.put(ArticleTable.COLUMN_ABSTRACT,org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4(w.html()));
          }
          Elements link = article.getElementsByTag("link");
          for (Element l : link)
          {
            values.put(ArticleTable.COLUMN_LINK,org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4(l.html()));
          }
          Elements date = article.getElementsByTag("date_published");
          for (Element d : date)
          {
            values.put(ArticleTable.COLUMN_DATE,org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4(d.html()));
          }
          Uri uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/articles");
          values.put(ArticleTable.COLUMN_READ,0);
          values.put(ArticleTable.COLUMN_FAVOURITE,0);
          getContentResolver().insert(uri, values);
          Uri joinUri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches/articles");
          getContentResolver().insert(joinUri, joinValues);
        }

      }
      catch (IOException e)
      {
        Log.i(TAG,"No articles for URL: " + articleUrl);
        //Log.e(TAG,"Failed to parse URL " + articleUrl + " error: " + e.toString());
      }
    }

    // finally, clean up the search_articles table by removing articles which are not linked
    // to a search...
    Uri cleanup = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/cleanup/articles");
    getContentResolver().delete(cleanup,null,null);

    /*
    As if that weren't enough, we now require the information on all the journals available
    on the server, in order that filters can be set up.
     */
    httpGet = new HttpGet(tempurl + "journals");
    inputStream = null;
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
      //Log.i(TAG, jsonResult);

      // make some JSON out of that string
      try
      {
        JSONArray jArray =  new JSONArray(jsonResult);
        Log.i(TAG, "Journals found: " + jArray.length());
        // if jArray.length() > 0, purge journals
        // like searches, purge the journals, as a rather poor means of making sure that journals which
        // are deleted from the server don't end up hanging around in the database on devices
        if (jArray.length() > 0)
        {
          //
          cleanup = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/purge_journals");
          getContentResolver().delete(cleanup,null,null);
          // get individual journal info.
          Uri jUri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/journals");
          for(int i = 0; i < jArray.length(); i++)
          {
            JSONObject jData = jArray.getJSONObject(i);
            String journal_id = jData.getString("id");
            String text = jData.getString("text");
            String full = jData.getString("full");
            String type = jData.getString("type");
            // use "full" for the display name when listing journals
            //Log.i(TAG,"Journal: " + journal_id + ", " + text + ", " + full + ", " + type);
            ContentValues values = new ContentValues();
            values.put(JournalsTable.COLUMN_JOURNAL_ID,journal_id);
            values.put(JournalsTable.COLUMN_TEXT,text);
            values.put(JournalsTable.COLUMN_FULL,full);
            values.put(JournalsTable.COLUMN_TYPE,type);
            getContentResolver().insert(jUri, values);
          }
        }
      }
      catch (Exception e)
      {
        Log.e(TAG, "Couldn't parse list of journals: " + e.toString());
      }
    }
    catch (IOException e)
    {
      Log.i(TAG, "Error getting search info: " + e.toString());
      hasFailed = true;
      return;
    }
    finally
    {
      try{ if(inputStream != null)inputStream.close(); }
      catch(Exception squish){
      // make sure it's closed
      }
    }
    // finally, if we have at last finished, the user can be notified
    mHandler.post(new ToastRunnable(getString(R.string.syncFinish)));
    sendMessage();
    Log.i(TAG, "FINISH");
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
