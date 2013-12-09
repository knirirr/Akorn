package org.akorn.akorn.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by milo on 08/11/2013.
 */
public class ArticleTable
{
  /*
   Note that authors will be in the authors table, with an author_article
   table to link the two together, thus allowing multiple authors per
   article at the cost of querying hassle
  */
  public static final String TABLE_ARTICLES = "articles";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_TITLE = "title";
  public static final String COLUMN_ARTICLE_ID = "article_id";
  public static final String COLUMN_JOURNAL = "journal";
  public static final String COLUMN_LINK = "link";
  public static final String COLUMN_ABSTRACT = "abstract";
  public static final String COLUMN_AUTHORS = "authors";
  public static final String COLUMN_DATE = "date_published";
  public static final String COLUMN_READ = "read";
  // date published needed here

  // SQL statement for creating the table
  private static final String DATABASE_CREATE = "create table "
      + TABLE_ARTICLES
      + "("
      + COLUMN_ID + " integer primary key autoincrement, "
      + COLUMN_TITLE + " text not null, "
      + COLUMN_ARTICLE_ID + " text not null, "
      + COLUMN_JOURNAL + " text not null, "
      + COLUMN_LINK + " text not null, "
      + COLUMN_ABSTRACT + " text not null, " // must be null if I web scrape rather than get XML
      + COLUMN_AUTHORS + " text not null, "
      + COLUMN_DATE + " text not null, "
      + COLUMN_READ + " integer not null"
      + ");";

  public static void onCreate(SQLiteDatabase database)
  {
    database.execSQL(DATABASE_CREATE);
    database.execSQL("CREATE UNIQUE INDEX article_id_index ON " + TABLE_ARTICLES + "  (" + COLUMN_ARTICLE_ID + ")");
  }

  /*
    For now the table can simply be dropped, as one can always re-sync with the server.
   */
  public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
  {
    Log.w(ArticleTable.class.getName(), "Upgrading database from version "
        + oldVersion + " to " + newVersion
        + " and dropping data.");
    database.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTICLES);
    onCreate(database);
  }
}
