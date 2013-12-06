package org.akorn.akorn;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.akorn.akorn.contentprovider.AkornContentProvider;
import org.akorn.akorn.database.ArticleTable;
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
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
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
  private static final String TAG = "AkornSyncService";
  private Handler mHandler;
  private CookieStore cookiestore;
  private Map<String,String> searchresults;

  public AkornSyncService()
  {
    super("AkornSyncService");
  }

  public static boolean isRunning = false;

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
    isRunning = false;
    super.onDestroy();
  }

  protected void onHandleIntent(Intent intent)
  {
    /*
      If the user has got this far then they must have supplied their username
      and password, for the app won't start this service unless both are defined.
     */
    isRunning = true;

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    String username = prefs.getString("pref_username", "");
    String password = prefs.getString("pref_password", "");
    String session_id = "";

    // store the values from each successful search result in a hash in order to
    // make getting the articles easier later
    searchresults = new HashMap<String, String>();

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
            mHandler.post(new ToastRunnable(getString(R.string.loginSuccess)));
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
      for (int h=0; h < namearray.length(); h++)
      {
        JSONArray jArray = jObject.getJSONArray(namearray.getString(h));
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
                searchresults.put(s_id, current + "%7Ck=" + j_text);
              }
              else
              {
                searchresults.put(s_id, current + "%7Cj=" + j_id);
              }
            }
            else
            {
              if (j_full == null)  // keyword
              {
                searchresults.put(s_id,"k=" + j_text);
              }
              else
              {
                searchresults.put(s_id,"j=" + j_id);
              }
            }

          }
          catch (JSONException e)
          {
            // Oops
            Log.e(TAG, "Couldn't parse JSON: " + e.toString());
          }
        }
      }
    }
    catch (JSONException e)
    {
      Log.e(TAG,"Can't parse JSON from server: " + e.toString());
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
      //String key = entry.getKey();
      String value = entry.getValue();
      String articleUrl = URL + "articles.xml?" + value.replace(" ","%20");
      Log.i(TAG,"URL: " + articleUrl);

      try
      {
        Document doc = Jsoup.connect(articleUrl).get();
        Elements articles = doc.getElementsByTag("article");
        ContentValues values;
        int count = 0;
        for (Element article : articles)
        {
          //Log.i(TAG, "ELEMENT: " + article.toString());
          values = new ContentValues();
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
          getContentResolver().insert(uri, values);
          //Log.i(TAG, String.valueOf(count) + ": VALUES: " + values.toString());

          /*
            The search_article table must also be populated here, otherwise one can't get articles from
            the search_id sent to the ArticleListFragment
           */
        }

      }
      catch (IOException e)
      {
        Log.i(TAG,"No articles for URL: " + articleUrl);
        //Log.e(TAG,"Failed to parse URL " + articleUrl + " error: " + e.toString());
      }
    }
    mHandler.post(new ToastRunnable(getString(R.string.syncFinish)));
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
