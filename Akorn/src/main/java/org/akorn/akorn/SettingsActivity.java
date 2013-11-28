package org.akorn.akorn;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Created by milo on 14/11/2013.
 */
public class SettingsActivity extends Activity
{
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    // Display the fragment as the main content.
    getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();

    // show an action bar to allow an easy exit to the viewingactivity
    ActionBar actionBar = getActionBar();
    actionBar.setHomeButtonEnabled(true);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.settings, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    switch (item.getItemId()) {
      case android.R.id.home:
        Intent intent = new Intent(this, ViewingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        break;
      case R.id.action_website:
        Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://akorn.org"));
        startActivity(viewIntent);
        break;
      default:
        break;
    }
    return true;
  }
}
