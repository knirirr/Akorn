package org.akorn.akorn;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.akorn.akorn.contentprovider.AkornContentProvider;
import org.akorn.akorn.database.SearchTable;

/**
 * Created by milo on 29/01/2014.
 */
public class FilterActivity extends Activity
{
  ActionBar actionBar;
  private SimpleCursorAdapter mCursorAdapter;
  private String searchId;
  private static String TAG = "Akorn";

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.filter_view);

    ListView listView = (ListView) findViewById(R.id.filter_list);
    mCursorAdapter = getList();
    listView.setAdapter(mCursorAdapter);

    listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
    {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id)
      {
        String search_id = "";
        try
        {
          Cursor c = (Cursor) mCursorAdapter.getItem(position);
          search_id = c.getString(c.getColumnIndex(SearchTable.COLUMN_SEARCH_ID));
          ItemClicked(search_id);
        }
        catch (Exception e)
        {
          Log.e(TAG,"CURSOR FAIL: " + e.toString());
        }
      }
    });

  }

  /*
  An item in the list has been selected...
   */
  public void ItemClicked(String search_id)
  {
    searchId = search_id; // global variable to make sure I can get use it in the dialogs
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setIcon(android.R.drawable.ic_dialog_alert);
    builder.setMessage(R.string.confirmDelete).setCancelable(false).setPositiveButton(R.string.yesButton, new DialogInterface.OnClickListener()
    {
      public void onClick(DialogInterface dialog, int id)
      {
        // delete the search with this name, meaning that one must call the SearchFilterService so that
        // the change is sent to the server...
        Intent i = new Intent(FilterActivity.this, SearchFilterService.class);
        i.putExtra("search_id", searchId);
        if (SearchFilterService.isRunning == false)
        {
          FilterActivity.this.startService(i);
        }
        return;
      }
    }).setNegativeButton(R.string.noButton, new DialogInterface.OnClickListener()
    {
      public void onClick(DialogInterface dialog, int id)
      {
        dialog.cancel();
      }
    });
    AlertDialog alert = builder.create();
    alert.show();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    getMenuInflater().inflate(R.menu.filters, menu);
    actionBar = getActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setHomeButtonEnabled(true);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    switch (item.getItemId())
    {
      case android.R.id.home:
        Intent intent = new Intent(this, ViewingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        break;
      case R.id.action_add:
        Intent i = new Intent(FilterActivity.this, SearchFilterService.class);
        i.putExtra("search_id", "new");
        if (SearchFilterService.isRunning == false)
        {
          FilterActivity.this.startService(i);
        }
        return true;
      case R.id.action_website:
        Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://akorn.org"));
        startActivity(viewIntent);
        return true;
      case R.id.action_settings:
        //Toast.makeText(this, "Settings selected (main).", Toast.LENGTH_SHORT).show();
        Intent actionIntent = new Intent(this, SettingsActivity.class);
        startActivity(actionIntent);
        return true;
      case R.id.action_sync:
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("pref_username", "");
        String password = prefs.getString("pref_password", "");
        if (username.isEmpty() || password.isEmpty())
        {
          Toast.makeText(this, getString(R.string.please_configure), Toast.LENGTH_SHORT).show();
          return true;
        }
        if (AkornSyncService.isRunning == true)
        {
          Toast.makeText(this, getString(R.string.in_progress), Toast.LENGTH_SHORT).show();
          return true;
        }
        Intent sync = new Intent(this, AkornSyncService.class);
        // potentially add data to the intent
        //i.putExtra("KEY1", "Value to be used by the sync service");
        this.startService(sync);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }



  private SimpleCursorAdapter getList()
  {
    Uri uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches_filter");
    // this particular syntax is effectively un-needed, as a raw query is being used
    // in the content provider if a search of searches is used, in order that a
    // group_concat may be used
    // http://www.sqlite.org/lang_aggfunc.html
    String[] orderBy = { String.valueOf(SearchTable.COLUMN_ID) };
    Cursor cursor = getContentResolver().query(uri,
        new String[]
            {
                SearchTable.COLUMN_ID,
                SearchTable.COLUMN_SEARCH_ID,
                SearchTable.COLUMN_FULL,
                SearchTable.COLUMN_TYPE,
                SearchTable.COLUMN_TEXT
            },
        null, orderBy , null);

    if (cursor == null)
    {
      Log.i(TAG, "FRC! Cursor is null in NavigationDrawerFragment!");
      Toast.makeText(this, getString(R.string.database_error), Toast.LENGTH_SHORT).show();
    }
    // Defines a list of columns to retrieve from the Cursor and load into an output row
    String[] mWordListColumns =
        {
            SearchTable.COLUMN_TEXT,
            SearchTable.COLUMN_TYPE
        };

    // Defines a list of View IDs that will receive the Cursor columns for each row
    int[] mWordListItems = { R.id.search_full, R.id.search_type};

    // layout for each of the articles in the sidebar
    int layout = R.layout.search_title;

    // Creates a new SimpleCursorAdapter to bind to the navigation drawer
    mCursorAdapter = new SimpleCursorAdapter(
        this,
        layout,
        cursor,
        mWordListColumns,
        mWordListItems,
        0);
    return mCursorAdapter;
  }
}
