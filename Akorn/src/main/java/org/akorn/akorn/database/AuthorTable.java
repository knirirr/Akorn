package org.akorn.akorn.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by milo on 08/11/2013.
 */
public class AuthorTable
{
/*
 Note that authors will be in the authors table, with an author_article
 table to link the two together, thus allowing multiple authors per
 article at the cost of querying hassle
*/
  public static final String TABLE_AUTHORS = "authors";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_AUTHOR_NAME = "author_name";
  // do we need a column to define who is the first author?

  // SQL statement for creating the table
  private static final String DATABASE_CREATE = "create table "
      + TABLE_AUTHORS
      + "("
      + COLUMN_ID + " integer primary key autoincrement, "
      + COLUMN_AUTHOR_NAME + " text not null"
      + ");";

  public static void onCreate(SQLiteDatabase database)
  {
    database.execSQL(DATABASE_CREATE);
  }

  /*
    For now the table can simply be dropped, as one can always re-sync with the server.
   */
  public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
  {
    Log.w(ArticleTable.class.getName(), "Upgrading database from version "
        + oldVersion + " to " + newVersion
        + " and dropping data.");
    database.execSQL("DROP TABLE IF EXISTS " + TABLE_AUTHORS);
    onCreate(database);
  }
}
