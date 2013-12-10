package org.akorn.akorn.contentprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import org.akorn.akorn.Article;
import org.akorn.akorn.database.AkornDatabaseHelper;
import org.akorn.akorn.database.ArticleTable;
import org.akorn.akorn.database.SearchArticleTable;
import org.akorn.akorn.database.SearchTable;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by milo on 08/11/2013.
 */
public class AkornContentProvider extends ContentProvider
{

  private AkornDatabaseHelper database;

  public static final String AUTHORITY = "org.akorn.akorn.contentprovider";
  public static final String TAG = "AkornContentProvider";

  /*
    SEARCHES_ARTICLES_ID is expecting a search ID (string); if the reverse search is needed
    then an ARTICLES_SEARCHES_ID URI will be required to take the article ID
   */
  private static final int ARTICLES = 10;
  private static final int ARTICLES_ID = 15;
  private static final int SEARCHES = 25;
  private static final int SEARCHES_ID = 30;
  private static final int SEARCHES_ARTICLES = 35;
  private static final int SEARCHES_ARTICLES_ID = 40;


  private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
  static
  {
    sURIMatcher.addURI(AUTHORITY, "articles", ARTICLES);
    sURIMatcher.addURI(AUTHORITY, "articles/#", ARTICLES_ID);
    sURIMatcher.addURI(AUTHORITY, "searches", SEARCHES);
    sURIMatcher.addURI(AUTHORITY, "searches/#", SEARCHES_ID);
    sURIMatcher.addURI(AUTHORITY, "searches/articles", SEARCHES_ARTICLES);
    sURIMatcher.addURI(AUTHORITY, "searches/articles/*", SEARCHES_ARTICLES_ID);
  }

  @Override
  public boolean onCreate()
  {
    database = new AkornDatabaseHelper(getContext());
    //Log.i(TAG, "Database onCreate()");
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
  {

    //Log.i(TAG, "URI: " + uri.getPath());
    //Log.i(TAG, "LAST: " + uri.getLastPathSegment());
    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
    Cursor cursor = null;

    // check if the caller has requested a column which does not exists
    checkColumns(projection);
    int uriType = sURIMatcher.match(uri);
    //Log.i(TAG, "URITYPE: " + String.valueOf(uriType));
    switch (uriType)
    {
      case ARTICLES:
        queryBuilder.setTables(ArticleTable.TABLE_ARTICLES);
        cursor = queryBuilder.query(database.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
        getContext().getContentResolver().notifyChange(uri, null);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
      case ARTICLES_ID:
        // adding the ID to the original query
        queryBuilder.setTables(ArticleTable.TABLE_ARTICLES);
        queryBuilder.appendWhere(ArticleTable.COLUMN_ID + "=" + uri.getLastPathSegment());
        cursor = queryBuilder.query(database.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
        getContext().getContentResolver().notifyChange(uri, null);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
      case SEARCHES:
        queryBuilder.setTables(SearchTable.TABLE_SEARCH);
        cursor = database.getReadableDatabase().rawQuery(
            "SELECT " + SearchTable.COLUMN_ID + ", " +
            SearchTable.COLUMN_SEARCH_ID + ", " +
            "group_concat(" + SearchTable.COLUMN_FULL + ", \" | \") AS " + SearchTable.COLUMN_FULL + ", " +
            "group_concat(" + SearchTable.COLUMN_TYPE + ", \" | \") AS " + SearchTable.COLUMN_TYPE + ", " +
            "group_concat(" + SearchTable.COLUMN_TEXT+ ", \" | \") AS " + SearchTable.COLUMN_TEXT +
             " FROM " + SearchTable.TABLE_SEARCH + " GROUP BY " + SearchTable.COLUMN_SEARCH_ID
            , null);
        getContext().getContentResolver().notifyChange(uri, null);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
      case SEARCHES_ARTICLES_ID:
        cursor = database.getReadableDatabase().rawQuery("SELECT DISTINCT " +
            ArticleTable.TABLE_ARTICLES + "." + ArticleTable.COLUMN_ID + ", " +
            ArticleTable.TABLE_ARTICLES + "." + ArticleTable.COLUMN_TITLE + ", " +
            ArticleTable.TABLE_ARTICLES + "." + ArticleTable.COLUMN_JOURNAL + ", " +
            ArticleTable.TABLE_ARTICLES + "." + ArticleTable.COLUMN_ARTICLE_ID +
            " FROM " + ArticleTable.TABLE_ARTICLES + " INNER JOIN " +
            SearchArticleTable.TABLE_SEARCHES_ARTICLES +
            " ON " +
            SearchArticleTable.TABLE_SEARCHES_ARTICLES + "." + SearchArticleTable.COLUMN_ARTICLE_ID + "=" +
            ArticleTable.TABLE_ARTICLES + "." + ArticleTable.COLUMN_ARTICLE_ID +
            " WHERE " +
            SearchArticleTable.COLUMN_SEARCH_ID + "=" + " '" + uri.getLastPathSegment() + "'"
        , null);
        return cursor;
      default:
        throw new IllegalArgumentException("Unknown URI: " + uri);
    }
  }


  private void checkColumns(String[] projection)
  {
    String[] available = {
        ArticleTable.COLUMN_ARTICLE_ID,
        ArticleTable.COLUMN_READ ,
        ArticleTable.COLUMN_JOURNAL,
        ArticleTable.COLUMN_ID,
        ArticleTable.COLUMN_LINK,
        ArticleTable.COLUMN_ABSTRACT,
        ArticleTable.COLUMN_TITLE,
        ArticleTable.COLUMN_AUTHORS,
        ArticleTable.COLUMN_DATE,
        SearchArticleTable.COLUMN_ID,
        SearchArticleTable.COLUMN_ARTICLE_ID,
        SearchArticleTable.COLUMN_SEARCH_ID,
        SearchTable.COLUMN_ID,
        SearchTable.COLUMN_TERM_ID,
        SearchTable.COLUMN_TEXT,
        SearchTable.COLUMN_FULL,
        SearchTable.COLUMN_TYPE,
        SearchTable.COLUMN_SEARCH_ID
    };

    if (projection != null)
    {
      HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
      HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
      // check if all columns which are requested are available
      if (!availableColumns.containsAll(requestedColumns))
      {
        throw new IllegalArgumentException("Unknown columns in projection");
      }
    }
  }

  @Override
  public String getType(Uri uri)
  {
    return null;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
  {
    int rowsUpdated = 0;
    int uriType = sURIMatcher.match(uri);
    SQLiteDatabase sqlDB = database.getWritableDatabase();
    switch (uriType)
    {
      case SEARCHES:
        rowsUpdated = sqlDB.update(SearchTable.TABLE_SEARCH,
            values,
            selection,
            selectionArgs);
        break;
      default:
        throw new IllegalArgumentException("Unknown URI: " + uri);
    }
    getContext().getContentResolver().notifyChange(uri, null);
    return rowsUpdated;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values)
  {
    int uriType = sURIMatcher.match(uri);
    SQLiteDatabase sqlDB = database.getWritableDatabase();
    switch (uriType)
    {
      case ARTICLES:
        try
        {
          sqlDB.insertWithOnConflict(ArticleTable.TABLE_ARTICLES, null, values,sqlDB.CONFLICT_REPLACE);
        }
        catch (Exception e)
        {
          Log.e(TAG,"Failed to insert data: " + e.toString());
        }
        break;
      case SEARCHES:
        try
        {
          sqlDB.insert(SearchTable.TABLE_SEARCH, null, values);
        }
        catch (Exception e)
        {
          Log.e(TAG,"Failed to insert data: " + e.toString());
        }
        break;
      case SEARCHES_ARTICLES:
        try
        {
          sqlDB.insertWithOnConflict(SearchArticleTable.TABLE_SEARCHES_ARTICLES, null, values,sqlDB.CONFLICT_REPLACE);
        }
        catch (Exception e)
        {
          Log.e(TAG,"Failed to insert data: " + e.toString());
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown URI: " + uri);
    }
    getContext().getContentResolver().notifyChange(uri, null);
    return uri;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs)
  {
    int uriType = sURIMatcher.match(uri);
    SQLiteDatabase sqlDB = database.getWritableDatabase();
    switch (uriType)
    {
      case SEARCHES:
        Log.i(TAG,"Purging search table!");
        sqlDB.delete(SearchTable.TABLE_SEARCH, null, null);
        // reset IDs. Probably not necessary, but might as well be done
        sqlDB.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + SearchTable.TABLE_SEARCH + "'");
        Log.i(TAG,"Purged!");
        break;
      default:
        throw new IllegalArgumentException("Unknown URI: " + uri);
    }
    getContext().getContentResolver().notifyChange(uri, null);
    return 0;
  }
}
