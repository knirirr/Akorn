package org.akorn.akornapp;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import org.akorn.akornapp.contentprovider.AkornContentProvider;
import org.akorn.akornapp.database.JournalsTable;
import org.akorn.akornapp.database.SearchTable;

import java.util.ArrayList;

/**
 * Created by milo on 29/01/2014.
 */
public class FilterActivity extends Activity
{
  ActionBar actionBar;
  AutoCompleteTextView autoComplete;
  String ftype;
  String journal;
  private SimpleCursorAdapter mCursorAdapter;
  private SimpleCursorAdapter mDropDownAdapter;
  private String searchId;
  private static String TAG = "Akorn";
  String[] journalsString;
  String[] journalIdsString;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.filter_view);


    /*
    List adapter for drop-down textedit
    There are two arrays created here; one holds the journal names that the user sees and the other holds
    the Akorn server's journal IDs. These latter will be sent back to the server to create a new search.
     */
    Cursor ddCursor = getDropDown();
    ArrayList<String> journals = new ArrayList<String>();
    ArrayList<String> journalIds = new ArrayList<String>();
    for(ddCursor.moveToFirst(); ddCursor.moveToNext(); ddCursor.isAfterLast())
    {
      journalIds.add(ddCursor.getString(ddCursor.getColumnIndex(JournalsTable.COLUMN_JOURNAL_ID)));
      journals.add(ddCursor.getString(ddCursor.getColumnIndex(JournalsTable.COLUMN_FULL)));
    }
    // convert to String[]
    journalsString = (String[]) journals.toArray(new String[journals.size()]);
    journalIdsString = (String[]) journalIds.toArray(new String[journalIds.size()]);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, journalsString);
    autoComplete = (AutoCompleteTextView) findViewById(R.id.autocomplete_box);
    autoComplete.setAdapter(adapter);
    autoComplete.setDropDownWidth(ViewGroup.LayoutParams.MATCH_PARENT);
  }

  @Override
  public void onResume()
  {
    super.onResume();
    LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("filters-changed"));
  }

  // handler for received Intents for the "my-event" event
  private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
  {
    @Override
    public void onReceive(Context context, Intent intent)
    {
      // Extract data included in the Intent
      String message = intent.getStringExtra("message");
      Log.d(TAG, "FilterActivity got message: " + message);
    }
  };

  @Override
  protected void onPause() {
    // Unregister since the activity is not visible
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    super.onPause();
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

  /*
    This will add a journal widget
   */
  public void addJournal(View view)
  {
    ftype = "journal";
    journal = "";
    AutoCompleteTextView acView = (AutoCompleteTextView) findViewById(R.id.autocomplete_box);
    journal = acView.getText().toString();
    if (journal.isEmpty())
    {
      Toast.makeText(getApplicationContext(), getString(R.string.not_empty_journal_please), Toast.LENGTH_SHORT).show();
      return;
    }
    String jid = getJournalId(journal);
    // create a "widget" containing the details of this part of the filter
    // the second parameter needs to be an AttributeSet
    FilterWidget widget = new FilterWidget(this,null);
    widget.setTitle(journal);
    widget.setType(ftype);
    LinearLayout layout = (LinearLayout) findViewById(R.id.filter_widget_area);
    layout.addView(widget);

  }

   /*
    This will add a keyword widget
   */
  public void addKeyword(View view)
  {
    ftype = "keyword";
    String keyword = "";
    EditText acView = (EditText) findViewById(R.id.keyword_box);
    keyword = acView.getText().toString();
    if (keyword.isEmpty())
    {
      Toast.makeText(getApplicationContext(), getString(R.string.not_empty_keyword_please), Toast.LENGTH_SHORT).show();
      return;
    }
    // create a "widget" containing the details of this part of the filter
    // the second parameter needs to be an AttributeSet
    FilterWidget widget = new FilterWidget(this,null);
    widget.setTitle(keyword);
    widget.setType(ftype);
    LinearLayout layout = (LinearLayout) findViewById(R.id.filter_widget_area);
    layout.addView(widget);
  }

  /*
    And this should find all the keyword and journal filters and send them to the server.
    The searchFilterService code will need to be updated...
   */
  public void createFilter(View view)
  {
    Log.i(TAG,"createFilter");
  }

  /*
    Remove all the filter widgets
   */
  public void clearScreen(View view)
  {
    Log.i(TAG,"clearScreen");
    LinearLayout layout = (LinearLayout) findViewById(R.id.filter_widget_area);
    layout.removeAllViews();
  }

  /*
  Having got a journal name from the drop down box we need to get its ID to send to the Akorn server. As the
  box only holds names (it is only an array adapter, not a cursor adapter) another database call is required
  to get the ID associated with that name. Assuming, of course, that there are no duplicate journal names in
  the database.
   */
  public String getJournalId(String journal)
  {
    //Log.i(TAG, "Value passed to getJournalId: " + journal);
    Uri uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/journals");
    String finalResult = "";
    Cursor cursor = getContentResolver().query(uri,
        new String[]
            {
                JournalsTable.COLUMN_JOURNAL_ID
            },
        JournalsTable.COLUMN_FULL + " = ?",
        new String[] { journal },
        null);
    if (cursor == null)
    {
      Log.i(TAG, "FRC! Cursor is null in FilterActivityFragment!");
      Toast.makeText(this, getString(R.string.database_error), Toast.LENGTH_SHORT).show();
    }
    while (cursor.moveToNext())
    {
      finalResult = cursor.getString(cursor.getColumnIndex(JournalsTable.COLUMN_JOURNAL_ID));
      //Log.i(TAG, "FinalResult: " + finalResult);
    }
    return finalResult;
  }

  /*
  This query provides the list of journals necessary to fill up the array which is used for the
  drop down autocomplete box's array adapter.
   */
  private Cursor getDropDown()
  {
    Uri uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/journals");
    Cursor cursor = getContentResolver().query(uri,
        new String[]
            {
                JournalsTable.COLUMN_JOURNAL_ID,
                JournalsTable.COLUMN_FULL
            },
        null,
        null,
        null);
    if (cursor == null)
    {
      Log.i(TAG, "FRC! Cursor is null in FilterActivityFragment!");
      Toast.makeText(this, getString(R.string.database_error), Toast.LENGTH_SHORT).show();
    }
    return cursor;
  }

}
