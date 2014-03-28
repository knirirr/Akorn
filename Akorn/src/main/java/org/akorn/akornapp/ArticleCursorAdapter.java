package org.akorn.akornapp;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.akorn.akornapp.database.ArticleTable;

/**
 * Created by milo on 12/12/2013.
 */
public class ArticleCursorAdapter extends SimpleCursorAdapter //implements PinnedSectionListView.PinnedSectionListAdapter
{
  private Cursor c;
  private Context context;
  private static final String TAG = "AkornArticleCursorAdapter";

  @Deprecated
  public ArticleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to)
  {
    super(context, layout, c, from, to);
    this.c = c;
    this.context = context;
  }

  @Override
  public void bindView(View row, Context cont, Cursor cursor)
  {
    super.bindView(row, cont, cursor);

    TextView title = (TextView) row.findViewById(R.id.article_title);
    String titleString = cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_TITLE));
    title.setText(titleString);
    title.invalidate();

    TextView journal = (TextView) row.findViewById(R.id.article_journal);
    journal.setText(cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_JOURNAL)));
    journal.invalidate();

    TextView authors = (TextView) row.findViewById(R.id.article_authors);
    authors.setText(cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_AUTHORS)));
    authors.invalidate();

    TextView date = (TextView) row.findViewById(R.id.article_date);
    String dateString = cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_DATE));
    date.setText(dateString);
    date.invalidate();

    // attempt to hide date if it has already been seen
    if (ArticleListFragment.headedArticles.contains(titleString))
    {
      date.setVisibility(TextView.VISIBLE);
    }
    else
    {
      date.setVisibility(TextView.GONE);
    }

    // hide date unless show authors has been selected
    if (ViewingActivity.show_authors == true)
    {
      authors.setVisibility(TextView.VISIBLE);
    }
    else
    {
      authors.setVisibility(TextView.GONE);
    }

    // set background colour for favourite articles
    try
    {
      int favourite = cursor.getInt(cursor.getColumnIndex(ArticleTable.COLUMN_FAVOURITE));
      if (favourite == 1)
      {
        row.setBackgroundColor(cont.getResources().getColor(R.color.pale_green));
        row.invalidate();
      }
      else
      {
        row.setBackgroundColor(cont.getResources().getColor(R.color.white));
        row.invalidate();
      }
    }
    catch (Exception e)
    {
      Log.e(TAG, "Row colour setting fail: " + e.toString());
    }
  }

  /*
  @Override
  public boolean isItemViewTypePinned(int viewType)
  {
    return viewType == 1;
  }
  */
}
