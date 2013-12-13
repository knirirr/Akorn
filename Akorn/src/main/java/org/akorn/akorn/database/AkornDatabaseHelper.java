package org.akorn.akorn.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by milo on 11/11/2013.
 */
public class AkornDatabaseHelper extends SQLiteOpenHelper
{
  private static final String DATABASE_NAME = "akorn.db";
  private static final int DATABASE_VERSION = 3;

  public AkornDatabaseHelper(Context context)
  {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase database)
  {
    Log.i("AKORN", "DatabaseHelper onCreate()");
    ArticleTable.onCreate(database);
    SearchArticleTable.onCreate(database);
    SearchTable.onCreate(database);
  }

  @Override
  public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
  {
    Log.i("AKORN", "DatabaseHelper onUpgrade()");
    ArticleTable.onUpgrade(database, oldVersion, newVersion);
    SearchArticleTable.onUpgrade(database, oldVersion, newVersion);
    SearchTable.onUpgrade(database, oldVersion, newVersion);
  }

}

