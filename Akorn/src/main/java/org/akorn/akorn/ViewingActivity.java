package org.akorn.akorn;

import android.app.Activity;
;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;

import android.widget.TextView;
import android.widget.Toast;


public class ViewingActivity extends Activity
        implements  NavigationDrawerFragment.NavigationDrawerCallbacks, ArticleListFragment.OnHeadlineSelectedListener
{
  /*
     Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */
  private NavigationDrawerFragment mNavigationDrawerFragment;
  private final String TAG = "AkornViewingActivity";

  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private CharSequence mTitle;
  private int currentFragmentIndex = 0;
  private int pos;
  private int sqlid;
  public Boolean favourite = false;
  SharedPreferences prefs;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.list_view);

    mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
    mTitle = getTitle();
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    Boolean hasAccount = prefs.getBoolean("has_account", true); // should be set to false
    /*
      SharedPreferences.Editor editor = settings.edit();
      //Some test characters for the blacklist
      mBlackListChars = "a,g,t,h,f";
      editor.putString("character_blacklist", mBlackListChars);
     */

    // Set up the drawer.
    mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));

    // here's the phone part - the tablet's views should be set up automatically from the layout
    if (findViewById(R.id.fragment_container) != null)
    {
      // If we're being restored from a previous state,
      // then we don't need to do anything and should return or else
      // we could end up with overlapping fragments.
      if (savedInstanceState != null)
      {
        currentFragmentIndex = savedInstanceState.getInt("currentFragment",0);
        if (currentFragmentIndex == 0)
        {
          currentFragmentIndex = 0;
          ArticleListFragment lf = (ArticleListFragment) getFragmentManager().findFragmentByTag("list_frag");
          FragmentTransaction transaction = getFragmentManager().beginTransaction();
          transaction.replace(R.id.fragment_container, lf,"list_frag");
          transaction.addToBackStack(null);
          transaction.commit();
        }
        else
        {
          currentFragmentIndex = 1;
          ArticleViewFragment vf = (ArticleViewFragment) getFragmentManager().findFragmentByTag("view_frag");
          FragmentTransaction transaction = getFragmentManager().beginTransaction();
          transaction.replace(R.id.fragment_container, vf, "view_frag");
          transaction.addToBackStack(null);
          transaction.commit();
        }
        return;
      }

      // create a fragment for viewing the list of articles
      ArticleListFragment firstFragment = new ArticleListFragment();

      // In case this activity was started with special instructions from an Intent,
      // pass the Intent's extras to the fragment as arguments
      firstFragment.setArguments(getIntent().getExtras());

      // Add the fragment in the 'fragment_container' FrameLayout
      //getFragmentManager().beginTransaction().add(R.id.fragment_container, firstFragment).commit();
      FragmentTransaction transaction = getFragmentManager().beginTransaction();

      // Replace whatever is in the fragment_container view with this fragment,
      // and add the transaction to the back stack so the user can navigate back
      transaction.replace(R.id.fragment_container, firstFragment, "list_frag");
      transaction.addToBackStack(null);

      // Commit the transaction
      transaction.commit();

    }
  }

  public void onResume(Bundle savedInstanceState)
  {
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
  }

    @Override
    public void onNavigationDrawerItemSelected(int position)
    {
      //Log.i(TAG,"ViewingActivity: " + String.valueOf(position));
    }

    @Override
    public void updateSearchId(String search_id)
    {
      // nothing
      ArticleListFragment articleListFrag = (ArticleListFragment)
          getFragmentManager().findFragmentById(R.id.list_fragment);
      if (articleListFrag != null)
      {
        // If article frag is available, we're in two-pane layout...

        // Call a method in the ArticleFragment to update its content
        articleListFrag.refreshUi(search_id);
      }
      else
      {
        // If the frag is not available, we're in the one-pane layout and must swap frags...
        // Create fragment and give it an argument for the selected article
        Log.i(TAG, "Selecting search: " + search_id);
        ArticleListFragment newFragment = new ArticleListFragment();
        Bundle args = new Bundle();
        args.putString("search_id",search_id);
        newFragment.setArguments(args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
      }
    }

    public void restoreActionBar()
    {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (!mNavigationDrawerFragment.isDrawerOpen())
        {

          // Only show items in the action bar relevant to this screen
          // if the drawer is not showing. Otherwise, let the drawer
          // decide what to show in the action bar.
          getMenuInflater().inflate(R.menu.global, menu);
          restoreActionBar();
          return true;
        }
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
            //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String username = prefs.getString("pref_username", "");
            String password = prefs.getString("pref_password", "");
            String password_confirm = prefs.getString("pref_password_confirm", "");
            Boolean hasAccount = prefs.getBoolean("has_account",true); // should be false
            if (!hasAccount)
            {
              //Toast.makeText(this, "You'd be directed to account creation at this point", Toast.LENGTH_SHORT).show();
              Intent createActivity = new Intent(this, CreateAccountActivity.class);
              startActivity(createActivity);
              return true;
            }
            // username, password or confirmation missing
            if (username.isEmpty() || password.isEmpty())
            {
              Toast.makeText(this, getString(R.string.please_configure), Toast.LENGTH_SHORT).show();
              Intent settingsActivity = new Intent(this, SettingsActivity.class);
              startActivity(settingsActivity);
              return true;
            }
            // sync already running
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

    public void onArticleSelected(int position,int sql_article_id)
    {
      // The user selected the headline of an article from the HeadlinesFragment

      // Capture the article fragment from the activity layout
      ArticleViewFragment articleViewFrag = (ArticleViewFragment)
          getFragmentManager().findFragmentById(R.id.view_fragment);

      if (articleViewFrag != null)
      {
        // If article frag is available, we're in two-pane layout...

        // Call a method in the ArticleFragment to update its content
        articleViewFrag.updateArticleView(position,sql_article_id);

      }
      else
      {
        // If the frag is not available, we're in the one-pane layout and must swap frags...
        // Create fragment and give it an argument for the selected article
        pos = position;
        sqlid = sql_article_id;
        currentFragmentIndex = 1;
        ArticleViewFragment newFragment = new ArticleViewFragment();
        Bundle args = new Bundle();
        args.putInt(ArticleViewFragment.ARG_POSITION, position);
        args.putInt(ArticleViewFragment.ARG_ID, sql_article_id);
        newFragment.setArguments(args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, newFragment,"view_frag");
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
      }
    }

  protected void onSaveInstanceState(Bundle bundle)
  {
    super.onSaveInstanceState(bundle);
    bundle.putInt("currentFragment", currentFragmentIndex);
    bundle.putInt("position", pos);
    bundle.putInt("sql_article_id", sqlid);
  }

  /*
    This re-sets the article fragment index to 0 if currently 1, the idea being that if it's one and back
    has just been pressed then it means that someone was looking at an article and has just pressed back
    to return to the list...
   */
  @Override
  public void onBackPressed()
  {
    if (currentFragmentIndex == 1)
    {
      currentFragmentIndex = 0;
    }
    super.onBackPressed();
  }
}
