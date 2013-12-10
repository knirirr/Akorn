package org.akorn.akorn;

import android.app.ListFragment;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import org.akorn.akorn.contentprovider.AkornContentProvider;
import org.akorn.akorn.database.ArticleTable;

import java.net.URI;

/**
 * Created by milo on 04/11/2013.
 */
public class ArticleListFragment extends ListFragment
{
  OnHeadlineSelectedListener mCallback;
  private static final String TAG = "AkornArticleListFragment";
  private String searchId = "";
  private SimpleCursorAdapter mCursorAdapter;

  // The container Activity must implement this interface so the frag can deliver messages
  public interface OnHeadlineSelectedListener
  {
    /** Called by HeadlinesFragment when a list item is selected */
    public void onArticleSelected(int position, int sql_article_id);
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    try
    {
      searchId = getArguments().getString("search_id","");
      Log.e(TAG,"Got search_id: " + searchId);
    }
    catch (Exception e)
    {
      Log.e(TAG,"Couldn't get search_id: " + e.toString());
    }
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState)
  {
    super.onActivityCreated(savedInstanceState);
    mCursorAdapter = getList(searchId);
    setListAdapter(mCursorAdapter);
  }

  @Override
  public void onStart()
  {
    super.onStart();

    // When in two-pane layout, set the listview to highlight the selected list item
    // (We do this during onStart because at the point the listview is available.)
    if (getFragmentManager().findFragmentById(R.id.list_fragment) != null)
    {
      getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }
  }

  @Override
  public void onAttach(Activity activity)
  {
    super.onAttach(activity);

    // This makes sure that the container activity has implemented
    // the callback interface. If not, it throws an exception.
    try
    {
      mCallback = (OnHeadlineSelectedListener) activity;
    }
    catch (ClassCastException e)
    {
      throw new ClassCastException(activity.toString()
          + " must implement OnHeadlineSelectedListener");
    }
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id)
  {
    // the article id at this location in the database is needed (either the
    // local sqlite id or the remote one)
    Cursor data = (Cursor) getListView().getItemAtPosition(position);
    int sql_article_id = data.getInt(data.getColumnIndex(ArticleTable.COLUMN_ID));
    Log.i("AKORN", "The ID selected was: " + String.valueOf(sql_article_id));

    // Notify the parent activity of selected item
    //mCallback.onArticleSelected(position);
    mCallback.onArticleSelected(position,sql_article_id);

    // Set the item as checked to be highlighted when in two-pane layout
    getListView().setItemChecked(position, true);
  }

  public void updateSearchId(String search_id)
  {
    Log.i(TAG,"ArticleListFragment: " + search_id);
    searchId = search_id;
    mCursorAdapter = getList(search_id);
    mCursorAdapter.notifyDataSetChanged();
    setListAdapter(mCursorAdapter);
  }

  // add a getList() method
  private SimpleCursorAdapter getList(String search_id)
  {
    //int layout = android.R.layout.simple_list_item_activated_1;
    int layout = R.layout.article_title;

    Uri uri = null;
    Cursor cursor = null;
    String[] selectArgs = null;
    if (search_id == null || search_id.isEmpty())
    {
      //Log.i(TAG, "SEARCH_ID is NULL!");
      uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/articles");
      selectArgs = new String[]{ArticleTable.COLUMN_ID,
                                ArticleTable.COLUMN_TITLE,
                                ArticleTable.COLUMN_JOURNAL,
                                ArticleTable.COLUMN_ARTICLE_ID};
    }
    else
    {
      //Log.i(TAG, "SEARCH_ID is: " + search_id);
      uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches/articles/" + search_id);
    }
    cursor = getActivity().getContentResolver().query(uri,
                   selectArgs,
                   null,
                   null,
                   null
      );

    if (cursor == null)
    {
      Log.i(TAG, "FRC! Cursor is null in ArticleListFragment!");
      Toast.makeText(getActivity(), getString(R.string.database_error), Toast.LENGTH_SHORT).show();
      return null;
    }

    // Defines a list of columns to retrieve from the Cursor and load into an output row
    String[] mWordListColumns =
    {
      ArticleTable.COLUMN_TITLE,
      ArticleTable.COLUMN_JOURNAL
    };

  // Defines a list of View IDs that will receive the Cursor columns for each row
  int[] mWordListItems = { R.id.article_title, R.id.article_journal};

  // Creates a new SimpleCursorAdapter
  SimpleCursorAdapter mCursorAdapter = new SimpleCursorAdapter(
    getActivity(),               // The application's Context object
    layout,
    cursor,                               // The result from the query
    mWordListColumns,                      // A string array of column names in the cursor
    mWordListItems,                        // An integer array of view IDs in the row layout
    0);                                    // Flags (usually none are needed)

    return mCursorAdapter;
  }

}
