package org.akorn.akorn;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.akorn.akorn.contentprovider.AkornContentProvider;
import org.akorn.akorn.database.ArticleTable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by milo on 04/11/2013.
 */
public class ArticleViewFragment extends Fragment
{
  final static String ARG_POSITION = "position";
  final static String ARG_ID = "id";
  final static String ARG_FRAG = "fragName";
  final static String TAG = "AkornArticleViewFragment";
  private String article_id;
  private int favourite;
  int mCurrentPosition = -1;
  int mSqlId = 0;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    // an option to share the text must be added here
    setHasOptionsMenu(true);

    // If activity recreated (such as from screen rotate), restore
    // the previous article selection set by onSaveInstanceState().
    // This is primarily necessary when in the two-pane layout.
    if (savedInstanceState != null)
    {
      mCurrentPosition = savedInstanceState.getInt(ARG_POSITION);
      mSqlId = savedInstanceState.getInt(ARG_ID);
      //Log.i(TAG, "onCreateView: " + String.valueOf(mCurrentPosition) + ", " + String.valueOf(mSqlId));
    }

    // Inflate the layout for this fragment
    LinearLayout article = (LinearLayout) inflater.inflate(R.layout.article_view, container, false);
    return article;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState)
  {
    super.onActivityCreated(savedInstanceState);
    updateArticleView(mCurrentPosition,mSqlId);
  }

  @Override
  public void onStart()
  {
    super.onStart();

    // During startup, check if there are arguments passed to the fragment.
    // onStart is a good place to do this because the layout has already been
    // applied to the fragment at this point so we can safely call the method
    // below that sets the article text.
    Bundle args = getArguments();
    if (args != null)
    {
      // Set article based on argument passed in
      updateArticleView(args.getInt(ARG_POSITION),args.getInt(ARG_ID));
    }
    else if (mCurrentPosition != -1)
    {
      // Set article based on saved instance state defined during onCreateView
      updateArticleView(mCurrentPosition,mSqlId);
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
  {
    //super.onCreateOptionsMenu(menu, inflater);
    if (menu.findItem(R.id.action_share) == null)
    {
      inflater.inflate(R.menu.viewing, menu);
    }
    if (menu.findItem(R.id.action_sync) != null)
    {
      ArticleListFragment articleListFrag = (ArticleListFragment) getFragmentManager().findFragmentById(R.id.list_fragment);
      if (articleListFrag == null) // we have a single-pane layout
      {
        MenuItem item = menu.findItem(R.id.action_sync);
        item.setVisible(false);
      }
    }
    else
    {
      //Toast.makeText(getActivity(), "Failed to clear the menu.", Toast.LENGTH_SHORT).show();
    }

  }

  @Override
  public void onPrepareOptionsMenu(Menu menu)
  {
    if (favourite == 1)
    {
      MenuItem item = menu.findItem(R.id.action_favourite);
      item.setIcon(android.R.drawable.star_big_on);
    }
  }


  public void updateArticleView(int position, int sql_article_id)
  {
    mCurrentPosition = position;
    mSqlId = sql_article_id;

    // content, title &c. all textviews
    TextView article_content = (TextView) getActivity().findViewById(R.id.article_content);
    TextView article_authors = (TextView) getActivity().findViewById(R.id.article_authors);
    TextView article_url = (TextView) getActivity().findViewById(R.id.article_url);
    TextView title_of_article = (TextView) getActivity().findViewById(R.id.title_of_article);
    TextView journal_of_article = (TextView) getActivity().findViewById(R.id.journal_of_article);

    //article.setText(Article.Articles[position]);
    // rather than the above, load the correct article text
    Uri uri = Uri.parse("content://org.akorn.akorn.contentprovider/articles" + "/" + sql_article_id);
    Cursor cursor = getActivity().getContentResolver().query(uri,
        new String[]
        {
            ArticleTable.COLUMN_ID,
            ArticleTable.COLUMN_ABSTRACT,
            ArticleTable.COLUMN_TITLE,
            ArticleTable.COLUMN_LINK,
            ArticleTable.COLUMN_JOURNAL,
            ArticleTable.COLUMN_AUTHORS,
            ArticleTable.COLUMN_DATE,
            ArticleTable.COLUMN_LINK,
            ArticleTable.COLUMN_FAVOURITE,
            ArticleTable.COLUMN_ARTICLE_ID
        }, // need title in order to share
        null, null, null);
    if (cursor.moveToFirst())
    {
      do
      {
        //article.setText(abs.replaceAll("[\n\r]", " "));
        String myFormatString = "yyyy-MM-d'T'h:m:s";
        Date showdate = null;
        try
        {
          showdate = new SimpleDateFormat(myFormatString, Locale.ENGLISH).parse(
            cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_DATE))
          );
        }
        catch (ParseException e)
        {
          Log.e(TAG,"Can't parse date: " + cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_DATE)));
        }
        if (showdate != null)
        {
          String journal = cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_JOURNAL));
          journal_of_article.setText(journal + ", " + showdate.toString() );
        }
        else
        {
         journal_of_article.setText(cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_JOURNAL)));
        }
        title_of_article.setText(cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_TITLE)));
        String authors = cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_AUTHORS));
        article_authors.setText(authors);
        String link = cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_LINK));
        article_url.setText(link);
        String abs = cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_ABSTRACT));
        article_content.setText(abs);
        favourite = cursor.getInt(cursor.getColumnIndex(ArticleTable.COLUMN_FAVOURITE));
        article_id = cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_ARTICLE_ID));
        this.getActivity().invalidateOptionsMenu();

      }
      while(cursor.moveToNext());
    }
    cursor.close();

    // cause textviews to refresh in two-column view (I hope)
    //ArticleViewFragment articleViewFrag = (ArticleViewFragment) getFragmentManager().findFragmentById(R.id.view_fragment);
    //if (articleViewFrag != null)
    /*
    ViewGroup artView = (ViewGroup) getActivity().findViewById(R.id.view_fragment);
    if (artView != null)
    {
      Log.i(TAG, "Trying to invalidate textviews!");
      //LinearLayout artView = (LinearLayout) getActivity().findViewById(R.id.view_fragment);
      artView.invalidate();
    }
    */
  }

  @Override
  public void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);
    // Save the current article selection in case we need to recreate the fragment
    outState.putInt(ARG_POSITION, mCurrentPosition);
    outState.putInt(ARG_ID, mSqlId);
    outState.putString(ARG_FRAG,"view_frag");
  }

  @Override
  public void onAttach(Activity activity)
  {
    super.onAttach(activity);
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();
  }

  public void toggleFavourite()
  {
    Log.i(TAG, "Toggling favourite: " + String.valueOf(favourite));
    if (favourite == 0)
    {
      Uri uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches_articles_save/" + article_id);
      ContentValues values = new ContentValues();
      values.put("article_id",article_id);
      values.put("search_id","searches_articles");
      getActivity().getContentResolver().insert(uri, values);
      favourite = 1;
    }
    else
    {
      Uri uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches_articles_delete/" + article_id);
      getActivity().getContentResolver().delete(uri, null, null);
      favourite = 0;
    }
    this.getActivity().invalidateOptionsMenu();
    Log.i(TAG, "Toggled favourite: " + String.valueOf(favourite));
  }

}
