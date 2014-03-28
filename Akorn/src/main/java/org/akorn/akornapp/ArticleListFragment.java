package org.akorn.akornapp;

import android.app.ListFragment;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.akorn.akornapp.contentprovider.AkornContentProvider;
import org.akorn.akornapp.database.ArticleTable;

import java.util.ArrayList;


/**
 * Created by milo on 04/11/2013.
 */
public class ArticleListFragment extends ListFragment
{
  OnHeadlineSelectedListener mCallback;
  private static final String TAG = "AkornArticleListFragment";
  private String searchId = "";
  private SimpleCursorAdapter mCursorAdapter;
  private Cursor cursor = null;
  private Cursor hCursor = null;
  final static String ARG_FRAG = "fragName";
  public static ArrayList<String> headedArticles;
  public static ArrayList<String> seenDates;
  private final static String[] selectArgs = new String[]{ArticleTable.COLUMN_ID,
                                                          ArticleTable.COLUMN_TITLE,
                                                          ArticleTable.COLUMN_JOURNAL,
                                                          ArticleTable.COLUMN_ARTICLE_ID,
                                                          ArticleTable.COLUMN_DATE,
                                                          ArticleTable.COLUMN_AUTHORS,
                                                          ArticleTable.COLUMN_FAVOURITE};

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
      //Log.e(TAG,"Got search_id: " + searchId);
    }
    catch (Exception e)
    {
      Log.e(TAG,"Couldn't get search_id: " + e.toString());
    }
    headedArticles = new ArrayList<String>();
    seenDates = new ArrayList<String>();
  }

  /*
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View list = inflater.inflate(R.layout.pinned_section, container);
    return list;
  }
  */

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState)
  {
    //Log.i(TAG, "Called onViewCreated");
    //Log.i(TAG,"SEARCH_ID: " + searchId);
    super.onViewCreated(view, savedInstanceState);
    refreshUi(searchId);
  }

  @Override
  public void onStart()
  {
    super.onStart();

    // set up an empty list message when no articles are found
    TextView empty = new TextView(getActivity());
    empty.setText("No articles.");
    empty.setTextColor(getResources().getColor(R.color.black));
    empty.setTextSize(24);
    empty.setPadding(5,5,5,5);
    empty.setVisibility(View.GONE);
    empty.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
    getListView().setEmptyView(empty);
    ((ViewGroup) getListView().getParent()).addView(empty);

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

    // Notify the parent activity of selected item
    mCallback.onArticleSelected(position,sql_article_id);

    // Set the item as checked to be highlighted when in two-pane layout
    getListView().setItemChecked(position, true);
  }

  public void refreshUi(String search_id)
  {
    //Log.i(TAG,"ArticleListFragment: " + search_id);
    searchId = search_id;
    setHeaders(search_id);
    mCursorAdapter = getList(search_id);
    mCursorAdapter.notifyDataSetChanged();
    setListAdapter(mCursorAdapter);
  }

  // create the database cursor
  private ArticleCursorAdapter getList(String search_id)
  {
    int layout = R.layout.article_title;

    Uri uri = null;

    if (search_id == null || search_id.isEmpty() || search_id.equals("all_articles"))
    {
      uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/articles");
    }
    else
    {
      uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches/articles/" + search_id);
    }
    String orderBy =  String.valueOf(ArticleTable.COLUMN_DATE) + " DESC";
    cursor = getActivity().getContentResolver().query(uri,
                   selectArgs,
                   null,
                   null,
                   orderBy);

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
      ArticleTable.COLUMN_JOURNAL,
      ArticleTable.COLUMN_AUTHORS,
      ArticleTable.COLUMN_DATE
    };

    // Defines a list of View IDs that will receive the Cursor columns for each row
    int[] mWordListItems = { R.id.article_title, R.id.article_journal, R.id.article_authors, R.id.article_date};
    //int[] mWordListItems = { R.id.article_title, R.id.article_journal, R.id.article_date};

    // Creates a new SimpleCursorAdapter
    //SimpleCursorAdapter mCursorAdapter = new SimpleCursorAdapter(
    ArticleCursorAdapter mCursorAdapter = new ArticleCursorAdapter(
      getActivity(),               // The application's Context object
      layout,
      cursor,                               // The result from the query
      mWordListColumns,                      // A string array of column names in the cursor
      mWordListItems); //,                        // An integer array of view IDs in the row layout
      //0);                                    // Flags (usually none are needed)

    return mCursorAdapter;
  }

  /*
  Something about this function causes:
  W/CursorWrapperInner( 1886): Cursor finalized without prior close()
   */
  private void setHeaders(String search_id)
  {
    Uri uri = null;

    if (search_id == null || search_id.isEmpty() || search_id.equals("all_articles"))
    {
      uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/articles");
    }
    else
    {
      uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches/articles/" + search_id);
    }
    String orderBy =  String.valueOf(ArticleTable.COLUMN_DATE) + " DESC";
    hCursor = getActivity().getContentResolver().query(uri,
                   selectArgs,
                   null,
                   null,
                   orderBy);

    if (hCursor == null)
    {
      Log.i(TAG, "FRC! Cursor is null when trying to set headers!");
      Toast.makeText(getActivity(), getString(R.string.database_error), Toast.LENGTH_SHORT).show();
    }

    // before setting up the adapter we need a couple of arrays to determine which articles should have
    // section headings by date.
    seenDates.clear();
    headedArticles.clear();
    while (hCursor.moveToNext())
    {
      String titleString = hCursor.getString(hCursor.getColumnIndex(ArticleTable.COLUMN_TITLE));
      String dateString = hCursor.getString(hCursor.getColumnIndex(ArticleTable.COLUMN_DATE));
      if (seenDates.contains(dateString))
      {
        //Log.i(TAG, "Alreadyseen: " + dateString);
      }
      else
      {
        headedArticles.add(titleString);
        seenDates.add(dateString);
        //Log.i(TAG, "Added article: " + titleString);
      }
    }
    hCursor.close();
  }

  @Override
  public void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);

    // Save the current article selection in case we need to recreate the fragment
    outState.putString(ARG_FRAG,"list_frag");
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();
  }



}
