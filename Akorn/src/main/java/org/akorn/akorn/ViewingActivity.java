package org.akorn.akorn;

import android.app.Activity;
;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ViewingActivity extends Activity
        implements  NavigationDrawerFragment.NavigationDrawerCallbacks, ArticleListFragment.OnHeadlineSelectedListener
{
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
        getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

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
            return;
          }

          // create a fragment for viewing the list of articles
          ArticleListFragment firstFragment = new ArticleListFragment();

          // In case this activity was started with special instructions from an Intent,
          // pass the Intent's extras to the fragment as arguments
          firstFragment.setArguments(getIntent().getExtras());

          // Add the fragment in the 'fragment_container' FrameLayout
          getFragmentManager().beginTransaction().add(R.id.fragment_container, firstFragment).commit();

        }

    }

    @Override
    public void onNavigationDrawerItemSelected(int position)
    {
        // load the list fragment if not already loaded, and refresh it with the appropriate
        // list of articles, or for now show a "doesn't work" message
        /*
        FragmentManager fragmentManager = getFragmentManager();
        ArticleViewFragment secondFragment = new ArticleViewFragment(); // position + 1
        Bundle args = new Bundle();
        args.putInt("position", position + 1);
        secondFragment.setArguments(args);
        fragmentManager.beginTransaction().replace(R.id.fragment_container, secondFragment).commit();
        */
        Toast.makeText(this, getString(R.string.not_working), Toast.LENGTH_SHORT).show();
    }

    public void onSectionAttached(int number)
    {
        switch (number)
        {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
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
            case R.id.action_settings:
              Toast.makeText(this, "Settings selected (main).", Toast.LENGTH_SHORT).show();
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
        ArticleViewFragment newFragment = new ArticleViewFragment();
        Bundle args = new Bundle();
        args.putInt(ArticleViewFragment.ARG_POSITION, position);
        args.putInt(ArticleViewFragment.ARG_ID, sql_article_id);
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
}
