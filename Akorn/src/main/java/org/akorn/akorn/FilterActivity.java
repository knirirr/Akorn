package org.akorn.akorn;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by milo on 29/01/2014.
 */
public class FilterActivity extends Activity
{
  ActionBar actionBar;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.filter_view);


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
      case R.id.action_share:
        //Toast.makeText(this, "Sharing action!", Toast.LENGTH_SHORT).show();
        TextView content = (TextView) findViewById(R.id.article_content);
        TextView title = (TextView) findViewById(R.id.article_title);
        String text_to_send = content.getText().toString();
        text_to_send = text_to_send + "\n\n" + getString(R.string.sharing_text); // make this optional
        // perhaps the URL should be added in here
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text_to_send);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, title.getText().toString());// add the article title if an email
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
        return true;
      case R.id.action_favourite:
        // The view fragment must be located by a different tag depending whether it's already on screen
        // (two-column layout), or not.
        ArticleViewFragment vf = (ArticleViewFragment) getFragmentManager().findFragmentById(R.id.view_fragment);
        if (vf == null)
        {
          vf = (ArticleViewFragment) getFragmentManager().findFragmentByTag("view_frag");
          // set the favourite value
        }
        vf.toggleFavourite();
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
        Intent i= new Intent(this, AkornSyncService.class);
        // potentially add data to the intent
        //i.putExtra("KEY1", "Value to be used by the sync service");
        this.startService(i);
        return true;
      case R.id.action_filters:
        actionIntent = new Intent(this, FilterActivity.class);
        startActivity(actionIntent);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
