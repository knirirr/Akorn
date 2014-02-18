package org.akorn.akorn.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by milo on 17/02/2014.
 */
public class JournalsTable
{
  public static final String TABLE_JOURNALS = "journals";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_JOURNAL_ID = "journal_id";
  public static final String COLUMN_TEXT = "text";
  public static final String COLUMN_FULL = "full";
  public static final String COLUMN_TYPE = "type";

  // SQL statement for creating the table
  private static final String DATABASE_CREATE = "create table "
      + TABLE_JOURNALS
      + "("
      + COLUMN_ID + " integer primary key autoincrement, "
      + COLUMN_JOURNAL_ID + " text not null, "
      + COLUMN_TEXT + " text not null, "
      + COLUMN_FULL + " text not null, "
      + COLUMN_TYPE + " text not null"
      + ");";

  public static void onCreate(SQLiteDatabase database)
  {
    database.execSQL("DROP TABLE IF EXISTS " + TABLE_JOURNALS);
    database.execSQL(DATABASE_CREATE);
    database.execSQL("CREATE UNIQUE INDEX journal_id_index ON " + TABLE_JOURNALS + "  (" + COLUMN_JOURNAL_ID + ")");
  }

 /*
  For now the table can simply be dropped, as one can always re-sync with the server.
 */
  public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
  {
    Log.w(ArticleTable.class.getName(), "Upgrading database from version "
        + oldVersion + " to " + newVersion
        + " and dropping data.");
    database.execSQL("DROP TABLE IF EXISTS " + TABLE_JOURNALS);
    onCreate(database);
  }
}


/*
{"text": "J. Am. Chem. Soc.", "full": "Journal of the American Chemical Society", "type": "journal", "id": "f45f136fbd14caa156e5b4b8467e7da1"}

 */
