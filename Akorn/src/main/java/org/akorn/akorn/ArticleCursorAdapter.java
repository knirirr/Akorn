package org.akorn.akorn;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.akorn.akorn.contentprovider.AkornContentProvider;
import org.akorn.akorn.database.ArticleTable;

import java.util.ArrayList;

/**
 * Created by milo on 12/12/2013.
 */
public class ArticleCursorAdapter extends SimpleCursorAdapter
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
    title.setText(cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_TITLE)));
    title.invalidate();
    TextView journal = (TextView) row.findViewById(R.id.article_journal);
    journal.setText(cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_JOURNAL)));
    journal.invalidate();
    try
    {
      int favourite = cursor.getInt(cursor.getColumnIndex(ArticleTable.COLUMN_FAVOURITE));
      if (favourite == 1)
      {
        row.setBackgroundColor(cont.getResources().getColor(R.color.pale_green));
        row.invalidate();
      }
    }
    catch (Exception e)
    {
      Log.e(TAG, "Row colour setting fail: " + e.toString());
    }
  }
}
