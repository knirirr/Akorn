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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
    LinearLayout article = (LinearLayout) inflater.inflate(R.layout.article_view, container, false);
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
    if (menu.findItem(R.id.action_sync) != null)
    {
      MenuItem item = menu.findItem(R.id.action_sync);
      item.setVisible(false);
    }
    else
    {
      //Toast.makeText(getActivity(), "Failed to clear the menu.", Toast.LENGTH_SHORT).show();
    }
  }


  public void updateArticleView(int position, int sql_article_id)
  {
    mCurrentPosition = position;
    mSqlId = sql_article_id;

    // content, title &c. all textviews
    TextView article_content = (TextView) getActivity().findViewById(R.id.article_content);
    article_content.setMovementMethod(new ScrollingMovementMethod()); // make textview scrollable
    TextView article_title = (TextView) getActivity().findViewById(R.id.article_title);
    TextView article_journal = (TextView) getActivity().findViewById(R.id.article_journal);
    TextView article_authors = (TextView) getActivity().findViewById(R.id.article_authors);

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
            ArticleTable.COLUMN_JOURNAL
        }, // need title in order to share
        null, null, null);
    //Log.i("AKORN", "Cursor: " + cursor.getColumnNames().toString());
    if (cursor.moveToFirst())
    {
      do
      {
        String abs = cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_ABSTRACT));
        //article.setText(abs.replaceAll("[\n\r]", " "));
        article_content.setText(abs);
        article_title.setText(cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_TITLE)));
        article_journal.setText(cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_JOURNAL)));
      }
      while(cursor.moveToNext());
    }
    cursor.close();

    // another query to get the authors? I think so, as there will be several authors per article
    article_authors.setText("One Author, Another Author, Yet Another and Final Author");
  }

  @Override
  public void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);

    // Save the current article selection in case we need to recreate the fragment
    outState.putInt(ARG_POSITION, mCurrentPosition);
  }


}
