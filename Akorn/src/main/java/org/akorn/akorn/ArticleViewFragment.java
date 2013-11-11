package org.akorn.akorn;

import android.app.Fragment;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.akorn.akorn.database.ArticleTable;

/**
 * Created by milo on 04/11/2013.
 */
public class ArticleViewFragment extends Fragment
{
  final static String ARG_POSITION = "position";
  final static String ARG_ID = "id";
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
    }

    // Inflate the layout for this fragment
    TextView article = (TextView) inflater.inflate(R.layout.article_view, container, false);
    return article;
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
      //getActivity().invalidateOptionsMenu();
    }
  }


  public void updateArticleView(int position, int sql_article_id)
  {
    TextView article;
    if (getActivity().findViewById(R.id.fragment_container) != null)
    {
      article = (TextView) getActivity().findViewById(R.id.individual_article);
    }
    else
    {
      article = (TextView) getActivity().findViewById(R.id.view_fragment);
    }
    mCurrentPosition = position;
    mSqlId = sql_article_id;
    article.setMovementMethod(new ScrollingMovementMethod()); // make textview scrollable

    //article.setText(Article.Articles[position]);
    // rather than the above, load the correct article text
    Uri uri = Uri.parse("content://org.akorn.akorn.contentprovider/articles" + "/" + sql_article_id);
    Cursor cursor = getActivity().getContentResolver().query(uri,
        new String[]
        {
            ArticleTable.COLUMN_ID,
            ArticleTable.COLUMN_ABSTRACT,
            ArticleTable.COLUMN_TITLE
        }, // need title in order to share
        null, null, null);
    Log.i("AKORN", "Cursor: " + cursor.getColumnNames().toString());
    if (cursor.moveToFirst())
    {
      do
      {
        String abs = cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_ABSTRACT));
        article.setText(abs.replaceAll("[\n\r]", " "));
      }
      while(cursor.moveToNext());
    }
    cursor.close();
  }

  @Override
  public void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);

    // Save the current article selection in case we need to recreate the fragment
    outState.putInt(ARG_POSITION, mCurrentPosition);
  }


}
