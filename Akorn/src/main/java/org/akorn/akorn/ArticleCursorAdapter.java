package org.akorn.akorn;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
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
  //private ArrayList<String> list = new ArrayList<String>();
  //private ArrayList<Boolean> itemChecked = new ArrayList<Boolean>();
  private static final String TAG = "AkornArticleCursorAdapter";
  //private int pos;

  // itemChecked will store the position of the checked items.

  @Deprecated
  public ArticleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to)
  {
    super(context, layout, c, from, to);
    this.c = c;
    this.context = context;

    /*
    for (int i = 0; i < this.getCount(); i++)
    {
      itemChecked.add(i, false); // initializes all items value with false
      list.add(i,"");
    }

    if (c.moveToFirst())
    {
      do
      {
        list.add(c.getInt(c.getColumnIndex(ArticleTable.COLUMN_ID)), c.getString(c.getColumnIndex(ArticleTable.COLUMN_ARTICLE_ID)));
      }
      while (c.moveToNext());
    }
    */
  }

  @Override
  public void bindView(View row, Context cont, Cursor cursor)
  {

    //Log.i(TAG, "CURSOR: " + DatabaseUtils.dumpCursorToString(cursor));
    context = cont;
    super.bindView(row, context, cursor);
    TextView title = (TextView) row.findViewById(R.id.article_title);
    title.setText(cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_TITLE)));
    title.invalidate();
    TextView journal = (TextView) row.findViewById(R.id.article_journal);
    journal.setText(cursor.getString(cursor.getColumnIndex(ArticleTable.COLUMN_JOURNAL)));
    journal.invalidate();

    /*
    int starred = cursor.getInt(cursor.getColumnIndex(ArticleTable.COLUMN_FAVOURITE));
    final CheckBox cBox = (CheckBox) row.findViewById(R.id.star);
    if (starred == 0)
    {
      // do nothing
      Log.i(TAG,"NOT STARRED: " + cursor.getString(c.getColumnIndex(ArticleTable.COLUMN_TITLE)));
    }
    else
    {
      Log.i(TAG,"STARRED: " + cursor.getString(c.getColumnIndex(ArticleTable.COLUMN_TITLE)));
      cBox.setChecked(itemChecked.get(pos));
      cBox.invalidate();
    }
    pos = cursor.getInt(cursor.getColumnIndex(ArticleTable.COLUMN_ID));
    Log.i(TAG,"POS: " + String.valueOf(pos));
    cBox.setOnClickListener(new View.OnClickListener()
    {
      public void onClick(View v)
      {
        CheckBox cb = (CheckBox) v.findViewById(R.id.star);
        String article_id = c.getString(c.getColumnIndex(ArticleTable.COLUMN_ARTICLE_ID));
        if (cb.isChecked())
        {
          itemChecked.set(pos, true);
          Log.i(TAG, "Checked ON");
          // set checked = 1 and link to saved searches
          Uri uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches_articles_save/" + list.get(pos));
          ContentValues values = new ContentValues();
          values.put("article_id",article_id);
          context.getContentResolver().insert(uri, values);
        }
        else if (!cb.isChecked())
        {
          itemChecked.set(pos, false);
          Log.i(TAG, "Checked OFF");
          // set checked = 0 and unlink from saved searches
          Uri uri = Uri.parse("content://" + AkornContentProvider.AUTHORITY + "/searches_articles_delete/" + list.get(pos));
          context.getContentResolver().delete(uri, null, null);
        }
      }
    });
    cBox.setChecked(itemChecked.get(pos));
  */
  }

}
