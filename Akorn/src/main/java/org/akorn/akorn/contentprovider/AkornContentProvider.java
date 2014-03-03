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
import org.akorn.akorn.database.JournalsTable;
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
  private static final int SEARCH_ID = 30;
  private static final int SEARCHES_ARTICLES = 35;
  private static final int SEARCHES_ARTICLES_ID = 40;
  private static final int CLEANUP_ARTICLES = 45;
  private static final int SEARCHES_ARTICLES_SAVE = 50;
  private static final int SEARCHES_ARTICLES_DELETE = 55;
  private static final int PURGE_ARTICLES = 60;
  private static final int JOURNALS = 65;
  private static final int PURGE_JOURNALS = 70;
  private static final int SEARCHES_FILTER = 75;


  private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
  static
  {
    sURIMatcher.addURI(AUTHORITY, "articles", ARTICLES);
    sURIMatcher.addURI(AUTHORITY, "articles/#", ARTICLES_ID);
    sURIMatcher.addURI(AUTHORITY, "searches", SEARCHES);
    sURIMatcher.addURI(AUTHORITY, "search/*", SEARCH_ID);
    sURIMatcher.addURI(AUTHORITY, "searches/articles", SEARCHES_ARTICLES);
    sURIMatcher.addURI(AUTHORITY, "searches/articles/*", SEARCHES_ARTICLES_ID);
    sURIMatcher.addURI(AUTHORITY, "cleanup/articles", CLEANUP_ARTICLES);
    sURIMatcher.addURI(AUTHORITY, "searches_articles_save/*", SEARCHES_ARTICLES_SAVE);
    sURIMatcher.addURI(AUTHORITY, "searches_articles_delete/*", SEARCHES_ARTICLES_DELETE);
    sURIMatcher.addURI(AUTHORITY, "purge_articles", PURGE_ARTICLES);
    sURIMatcher.addURI(AUTHORITY, "journals", JOURNALS);
    sURIMatcher.addURI(AUTHORITY, "purge_journals", PURGE_JOURNALS);
    sURIMatcher.addURI(AUTHORITY, "searches_filter", SEARCHES_FILTER);
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
      case JOURNALS:
        queryBuilder.setTables(JournalsTable.TABLE_JOURNALS);
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
            " FROM " + SearchTable.TABLE_SEARCH + " GROUP BY " + SearchTable.COLUMN_SEARCH_ID +
            " ORDER BY " + SearchTable.COLUMN_ID
            , null);
        getContext().getContentResolver().notifyChange(uri, null);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
        /*
        This one is probably a dreadful hack, but having set up the SEARCHES filter so that I could do a group
        concat, I now find that inserting a simple WHERE clause isn't possible (well, I can't immediately see how).
        So, I've used the same rawQuery here, adding the WHERE.
         */
      case SEARCHES_FILTER:
         queryBuilder.setTables(SearchTable.TABLE_SEARCH);
        cursor = database.getReadableDatabase().rawQuery(
            "SELECT " + SearchTable.COLUMN_ID + ", " +
            SearchTable.COLUMN_SEARCH_ID + ", " +
            "group_concat(" + SearchTable.COLUMN_FULL + ", \" | \") AS " + SearchTable.COLUMN_FULL + ", " +
            "group_concat(" + SearchTable.COLUMN_TYPE + ", \" | \") AS " + SearchTable.COLUMN_TYPE + ", " +
            "group_concat(" + SearchTable.COLUMN_TEXT+ ", \" | \") AS " + SearchTable.COLUMN_TEXT +
            " FROM " + SearchTable.TABLE_SEARCH +
            " WHERE " + SearchTable.COLUMN_ID + " > 2 " +
            " GROUP BY " + SearchTable.COLUMN_SEARCH_ID +
            " ORDER BY " + SearchTable.COLUMN_ID
            , null);
        getContext().getContentResolver().notifyChange(uri, null);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
      case SEARCHES_ARTICLES_ID:
        cursor = database.getReadableDatabase().rawQuery("SELECT DISTINCT " +
            ArticleTable.TABLE_ARTICLES + "." + ArticleTable.COLUMN_ID + ", " +
            ArticleTable.TABLE_ARTICLES + "." + ArticleTable.COLUMN_TITLE + ", " +
            ArticleTable.TABLE_ARTICLES + "." + ArticleTable.COLUMN_JOURNAL + ", " +
            ArticleTable.TABLE_ARTICLES + "." + ArticleTable.COLUMN_FAVOURITE + ", " +
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
        ArticleTable.COLUMN_FAVOURITE,
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
        SearchTable.COLUMN_SEARCH_ID,
        JournalsTable.COLUMN_JOURNAL_ID,
        JournalsTable.COLUMN_FULL,
        JournalsTable.COLUMN_ID,
        JournalsTable.COLUMN_TEXT,
        JournalsTable.COLUMN_TYPE
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
          sqlDB.insertWithOnConflict(ArticleTable.TABLE_ARTICLES, null, values,sqlDB.CONFLICT_IGNORE);
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
      case SEARCHES_ARTICLES_SAVE:
        try
        {
          sqlDB.execSQL("INSERT INTO searches_articles VALUES('saved_articles','" + uri.getLastPathSegment() + "')");
        }
        catch (Exception e)
        {
          Log.e(TAG, "Insert failed: " + e.toString());
        }
        try
        {
          sqlDB.execSQL("UPDATE articles SET favourite = 1 where article_id ='" + uri.getLastPathSegment() + "'");
        }
        catch (Exception e)
        {
          Log.e(TAG, "Update failed: " + e.toString());
        }
        break;
      case JOURNALS:
        try
        {
          //Log.i(TAG,"Actually inserted something this time: " + values.toString());
          sqlDB.insertWithOnConflict(JournalsTable.TABLE_JOURNALS, null, values, sqlDB.CONFLICT_IGNORE);
        }
        catch (Exception e)
        {
          Log.e(TAG, "Insert failed: " + e.toString());
        }
        break;
      default:
        throw new IllegalArgumentException("FRC, Unknown URI: " + uri);
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
        SearchTable.onCreate(sqlDB);
        Log.i(TAG,"Purged!");
        break;
      case SEARCH_ID:
        try
        {
          sqlDB.execSQL("DELETE FROM " + SearchTable.TABLE_SEARCH +
            " WHERE " + SearchTable.COLUMN_SEARCH_ID + " = '"
            + uri.getLastPathSegment() + "'");
          sqlDB.execSQL("DELETE FROM " + SearchArticleTable.TABLE_SEARCHES_ARTICLES+
            " WHERE " + SearchArticleTable.COLUMN_SEARCH_ID + " = '"
            + uri.getLastPathSegment() + "'");
          sqlDB.execSQL("DELETE FROM " + ArticleTable.TABLE_ARTICLES + " WHERE " + ArticleTable.COLUMN_ARTICLE_ID
            + " NOT IN (SELECT ARTICLE_ID FROM " + SearchArticleTable.TABLE_SEARCHES_ARTICLES + ")");
        }
        catch (Exception e)
        {
          Log.e(TAG, "SEARCH_ID: " + e.toString());
        }
        break;
      case CLEANUP_ARTICLES:
        Log.i(TAG,"Cleaning up orphaned articles.");
        try
        {
          sqlDB.execSQL("DELETE FROM " + ArticleTable.TABLE_ARTICLES + " WHERE " + ArticleTable.COLUMN_ARTICLE_ID
              + " NOT IN (SELECT ARTICLE_ID FROM " + SearchArticleTable.TABLE_SEARCHES_ARTICLES + ")");
        }
        catch (Exception e)
        {
          Log.e(TAG,"Cleanup failed!");
        }
        break;
      case PURGE_ARTICLES:
        Log.i(TAG,"Cleaning up non-saved articles.");
        try
        {
          sqlDB.execSQL("DELETE FROM " + ArticleTable.TABLE_ARTICLES + " WHERE " + ArticleTable.COLUMN_ARTICLE_ID
              + " NOT IN (SELECT ARTICLE_ID FROM " + SearchArticleTable.TABLE_SEARCHES_ARTICLES + " WHERE "
              + SearchArticleTable.COLUMN_SEARCH_ID + " = 'saved_articles'"
              + ")");
        }
        catch (Exception e)
        {
          Log.e(TAG,"Non-saved article cleanup failed!");
        }
        try
        {
          sqlDB.execSQL("DELETE FROM " + SearchArticleTable.TABLE_SEARCHES_ARTICLES + " WHERE "
            + SearchArticleTable.COLUMN_SEARCH_ID + " != 'saved_articles'");
        }
        catch (Exception e)
        {
          Log.e(TAG,"Article/search table cleanup failed!");
        }
        break;
      case PURGE_JOURNALS:
        Log.i(TAG,"Clearing journals table.");
        try
        {
          JournalsTable.onCreate(sqlDB);
          Log.i(TAG, "Journals table purged!");
        }
        catch (Exception e)
        {
          Log.e(TAG, "Couldn't clear journals table!");
        }
        break;
      case SEARCHES_ARTICLES_DELETE:
        sqlDB.execSQL("DELETE FROM searches_articles where article_id = '" + uri.getLastPathSegment() + "'");
        sqlDB.execSQL("UPDATE articles SET favourite = 0 where article_id ='" + uri.getLastPathSegment() + "'");
        break;
      default:
        throw new IllegalArgumentException("Unknown URI: " + uri);
    }
    getContext().getContentResolver().notifyChange(uri, null);
    return 0;
  }
}
