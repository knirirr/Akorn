package org.akorn.akornapp.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by milo on 08/11/2013.
 */
public class SearchTable
{
  /*
  This table links authors to articles, just in case there's a need to search
  for articles by author name. Also, the authors are being supplied as separate
  entries in the XML from the server
*/
  public static final String TABLE_SEARCH = "searches";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_SEARCH_ID = "search_id"; // I'm assuming that there's a BSON ID on the server...
  public static final String COLUMN_FULL = "full";
  public static final String COLUMN_TEXT = "text";
  public static final String COLUMN_TYPE = "type";
  public static final String COLUMN_TERM_ID = "term_id";

  // SQL statement for creating the table
  private static final String DATABASE_CREATE = "create table "
      + TABLE_SEARCH
      + "("
      + COLUMN_ID + " integer primary key autoincrement, "
      + COLUMN_SEARCH_ID + " text not null, "
      + COLUMN_FULL + " text, "
      + COLUMN_TEXT + " text not null, "
      + COLUMN_TYPE + " text not null, "
      + COLUMN_TERM_ID + " text"
      + ");";

  public static void onCreate(SQLiteDatabase database)
  {
    database.execSQL("DROP TABLE IF EXISTS " + TABLE_SEARCH);
    database.execSQL(DATABASE_CREATE);
    database.execSQL("INSERT INTO " + TABLE_SEARCH +
        " VALUES(NULL,'all_articles','All articles','All articles','All articles saved on this device','NA')");
    database.execSQL("INSERT INTO " + TABLE_SEARCH +
        " VALUES(NULL,'saved_articles','Saved articles','Saved articles','All articles starred on this device','NA')");
  }

  /*
    For now the table can simply be dropped, as one can always re-sync with the server.
   */
  public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
  {
    Log.w(ArticleTable.class.getName(), "Upgrading database from version "
        + oldVersion + " to " + newVersion
        + " and dropping data.");
    database.execSQL("DROP TABLE IF EXISTS " + TABLE_SEARCH);
    onCreate(database);
  }
}
