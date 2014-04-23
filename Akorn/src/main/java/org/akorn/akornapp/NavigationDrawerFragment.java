package org.akorn.akornapp;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import org.akorn.akornapp.contentprovider.AkornContentProvider;
import org.akorn.akornapp.database.SearchTable;


/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment
{

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;

    private static String TAG = "Akorn";
    private String search_id;

    public NavigationDrawerFragment() { }

    private AkornObserver observer;
    private SimpleCursorAdapter mCursorAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);

      // Read in the flag indicating whether or not the user has demonstrated awareness of the
      // drawer. See PREF_USER_LEARNED_DRAWER for details.
      SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
      mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

      if (savedInstanceState != null)
      {
        mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
        mFromSavedInstanceState = true;
      }

      // Select either the default item (0) or the last selected item.
      //selectItem(mCurrentSelectedPosition,"");

      // Indicate that this fragment would like to influence the set of actions in the action bar.
      setHasOptionsMenu(true);

      // register for changes in the content adapter
      Uri searchUri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches");
      observer = new AkornObserver(new Handler());
      getActivity().getContentResolver().registerContentObserver(searchUri, true, observer);
    }

    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
    {
      @Override
      public void onReceive(Context context, Intent intent)
      {
        // Extract data included in the Intent
        String message = intent.getStringExtra("message");
        Log.d(TAG, "Nav drawer got message: " + message);
        //mDrawerListView.invalidateViews();
        refreshViews();
      }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
      /*
       The list of the user's searches is required for display in the navigation bar
       */
      mDrawerListView = (ListView) inflater.inflate(R.layout.fragment_navigation_drawer, container, false);

      mCursorAdapter = getList();
      mDrawerListView.setAdapter(mCursorAdapter);
      mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);

      final Context mContext = this.getActivity();

      LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver, new IntentFilter("filters-changed"));

      // single press to load articles
      mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
      {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
          try
          {
            Cursor c = (Cursor) mCursorAdapter.getItem(position);
            search_id = c.getString(c.getColumnIndex(SearchTable.COLUMN_SEARCH_ID));
          }
          catch (Exception e)
          {
            Log.e(TAG,"CURSOR FAIL: " + e.toString());
          }
          selectItem(position,search_id);
        }
      });

      // long press to delete
      mDrawerListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
      {
        public boolean onItemLongClick(AdapterView<?> arg0, View v, int index, long arg3)
        {
          // TODO Auto-generated method stub
          Cursor c = (Cursor) mCursorAdapter.getItem(index);
          search_id = c.getString(c.getColumnIndex(SearchTable.COLUMN_SEARCH_ID));
          if (search_id.equals("all_articles") || search_id.equals("saved_articles"))
          {
            return true;
          }
          String name = c.getString(c.getColumnIndex(SearchTable.COLUMN_TEXT));
          AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
          builder.setIcon(android.R.drawable.ic_dialog_alert);
          builder.setMessage(getString(R.string.confirmDelete) + "\n" + name).setCancelable(false).setPositiveButton(R.string.yesButton, new DialogInterface.OnClickListener()
          {
            public void onClick(DialogInterface dialog, int id)
            {
              // delete the search with this name, meaning that one must call the SearchFilterService so that
              // the change is sent to the server...
              Intent i = new Intent(mContext, SearchFilterService.class);
              i.putExtra("search_id", search_id);
              if (SearchFilterService.isRunning == false)
              {
                getActivity().startService(i);
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
          Log.i(TAG, "Got listview item: " + search_id);
          return true;
        }
      });


      return mDrawerListView;
    }

    public boolean isDrawerOpen()
    {
      return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout)
    {
      mFragmentContainerView = getActivity().findViewById(fragmentId);
      mDrawerLayout = drawerLayout;

      // set a custom shadow that overlays the main content when the drawer opens
      mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
      // set up the drawer's list view with items and click listener

      ActionBar actionBar = getActionBar();
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setHomeButtonEnabled(true);

      // ActionBarDrawerToggle ties together the the proper interactions
      // between the navigation drawer and the action bar app icon.
      mDrawerToggle = new ActionBarDrawerToggle(
              getActivity(),                    /* host Activity */
              mDrawerLayout,                    /* DrawerLayout object */
              R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
              R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
              R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
          @Override
          public void onDrawerClosed(View drawerView)
          {
              super.onDrawerClosed(drawerView);
              if (!isAdded())
              {
                return;
              }
              getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
          }

            @Override
            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }
                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void selectItem(int position, String search_id)
    {
      mCurrentSelectedPosition = position;
      if (mDrawerListView != null)
      {
        mDrawerListView.setItemChecked(position, true);
      }
      if (mDrawerLayout != null)
      {
        mDrawerLayout.closeDrawer(mFragmentContainerView);
      }
      if (mCallbacks != null)
      {
        mCallbacks.onNavigationDrawerItemSelected(position);
        mCallbacks.updateSearchId(search_id);
      }
    }

    @Override
    public void onAttach(Activity activity)
    {
      super.onAttach(activity);
      try
      {
          mCallbacks = (NavigationDrawerCallbacks) activity;
      }
      catch (ClassCastException e)
      {
          throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
      }
    }

    @Override
    public void onDetach()
    {
      super.onDetach();
      mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
      super.onSaveInstanceState(outState);
      outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
      super.onConfigurationChanged(newConfig);
      // Forward the new configuration the drawer toggle component.
      mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
      // If the drawer is open, show the global app actions in the action bar. See also
      // showGlobalContextActionBar, which controls the top-left area of the action bar.
      if (mDrawerLayout != null && isDrawerOpen())
      {
        inflater.inflate(R.menu.global, menu);
        super.onCreateOptionsMenu(menu, inflater);
        showGlobalContextActionBar();
      }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
      // hide extra items
      if (mDrawerLayout != null && isDrawerOpen())
      {
        // try to force a refresh of the UI here.
        // this sucks, but if Android won't do it for me then I must do it
        // by brute force...
        mCursorAdapter = getList();
        mCursorAdapter.notifyDataSetChanged();
        ListView listView = (ListView) getActivity().findViewById(R.id.navigation_drawer);
        listView.setAdapter(mCursorAdapter);
        listView.invalidateViews();
        MenuItem item;

        if (menu.findItem(R.id.action_share) != null)
        {
          item = menu.findItem(R.id.action_share);
          item.setVisible(false);
        }
        else
        {
          //Toast.makeText(getActivity(), "Failed to clear the menu.", Toast.LENGTH_SHORT).show();
        }
        if (menu.findItem(R.id.action_sync) != null)
        {
          item = menu.findItem(R.id.action_sync);
          item.setVisible(false);
        }
        else
        {
          //Toast.makeText(getActivity(), "Failed to clear the menu.", Toast.LENGTH_SHORT).show();
        }
        if (menu.findItem(R.id.action_favourite) != null)
        {
          item = menu.findItem(R.id.action_favourite);
          item.setVisible(false);
        }
        else
        {
          //Toast.makeText(getActivity(), "Failed to clear the menu.", Toast.LENGTH_SHORT).show();
        }
        if (menu.findItem(R.id.action_filters) != null)
        {
          item = menu.findItem(R.id.action_filters);
          item.setVisible(false);
        }
      }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (mDrawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar()
    {
      ActionBar actionBar = getActionBar();
      actionBar.setDisplayShowTitleEnabled(true);
      actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
      actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar()
    {
      return getActivity().getActionBar();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface NavigationDrawerCallbacks
    {
      /**
       * Called when an item in the navigation drawer is selected.
       */
      void onNavigationDrawerItemSelected(int position);
      void updateSearchId(String search_id);
    }

  class AkornObserver extends ContentObserver
  {
    public AkornObserver(Handler handler)
    {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange)
    {
      this.onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri)
    {
      // do s.th.
      // depending on the handler you might be on the UI
      // thread, so be cautious!
      mCursorAdapter.notifyDataSetChanged();
      mDrawerListView.invalidate();
    }
  }

  private SimpleCursorAdapter getList()
  {
    Uri uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches");
    // this particular syntax is effectively un-needed, as a raw query is being used
    // in the content provider if a search of searches is used, in order that a
    // group_concat may be used
    // http://www.sqlite.org/lang_aggfunc.html
    String[] orderBy = { String.valueOf(SearchTable.COLUMN_ID) };
    Cursor cursor = getActivity().getContentResolver().query(uri,
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
        Toast.makeText(getActivity(), getString(R.string.database_error), Toast.LENGTH_SHORT).show();
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
          getActivity(),
          layout,
          cursor,
          mWordListColumns,
          mWordListItems,
          0);
    return mCursorAdapter;
  }

  public void refreshViews()
  {
    Log.i(TAG, "Refreshing!");
    //mCursorAdapter.notifyDataSetChanged();
    //mDrawerListView.invalidate();
    /*
    This is probably not the best way to do this, but nothing
    else seems to work.
     */
    mCursorAdapter.getCursor().close();
    mCursorAdapter = getList();
    mDrawerListView.setAdapter(mCursorAdapter);
  }

}
