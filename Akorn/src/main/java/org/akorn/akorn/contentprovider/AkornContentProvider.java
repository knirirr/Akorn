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
import org.akorn.akorn.database.AuthorArticleTable;
import org.akorn.akorn.database.AuthorTable;
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

  private static final int ARTICLES = 10;
  private static final int ARTICLES_ID = 15;
  private static final int AUTHORS = 20;
  private static final int AUTHORS_ID = 25;
  private static final int SEARCHES = 30;
  private static final int SEARCHES_ID = 35;
  private static final int AUTHORS_ARTICLES = 40;
  private static final int AUTHORS_ARTICLES_ID = 45;
  private static final int SEARCHES_ARTICLES = 50;
  private static final int SEARCHES_ARTICLES_ID = 55;

  private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
  static
  {
    sURIMatcher.addURI(AUTHORITY, "articles", ARTICLES);
    sURIMatcher.addURI(AUTHORITY, "articles/#", ARTICLES_ID);
    sURIMatcher.addURI(AUTHORITY, "authors", AUTHORS);
    sURIMatcher.addURI(AUTHORITY, "authors/#", AUTHORS_ID);
    sURIMatcher.addURI(AUTHORITY, "searches", SEARCHES);
    sURIMatcher.addURI(AUTHORITY, "searches/#", SEARCHES_ID);
    sURIMatcher.addURI(AUTHORITY, "authors_articles", AUTHORS_ARTICLES);
    sURIMatcher.addURI(AUTHORITY, "authors_articles/#", AUTHORS_ARTICLES_ID);
    sURIMatcher.addURI(AUTHORITY, "searches_articles", SEARCHES_ARTICLES);
    sURIMatcher.addURI(AUTHORITY, "searches_articles/#", SEARCHES_ARTICLES_ID);
  }

  @Override
  public boolean onCreate()
  {
    database = new AkornDatabaseHelper(getContext());
    Log.i("AKORN", "Database onCreate()");
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
  {

    Log.i("AKORN","URI is: " + uri.toString());
    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

    // check if the caller has requested a column which does not exists
    checkColumns(projection);

    // Set the table
    queryBuilder.setTables(ArticleTable.TABLE_ARTICLES);

    int uriType = sURIMatcher.match(uri);
    switch (uriType) {
      case ARTICLES:
        break;
      case ARTICLES_ID:
        // adding the ID to the original query
        queryBuilder.appendWhere(ArticleTable.COLUMN_ID + "=" + uri.getLastPathSegment());
        break;
      default:
        throw new IllegalArgumentException("Unknown URI: " + uri);
    }

    SQLiteDatabase db = database.getWritableDatabase();
    Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
    // make sure that potential listeners are getting notified
    cursor.setNotificationUri(getContext().getContentResolver(), uri);

    return cursor;
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
        AuthorArticleTable.COLUMN_ID,
        AuthorArticleTable.COLUMN_ARTICLE_ID,
        AuthorArticleTable.COLUMN_AUTHOR_ID,
        AuthorTable.COLUMN_ID,
        AuthorTable.COLUMN_AUTHOR_NAME,
        SearchArticleTable.COLUMN_ID,
        SearchArticleTable.COLUMN_ARTICLE_ID,
        SearchArticleTable.COLUMN_SEARCH_ID,
        SearchTable.COLUMN_ID,
        SearchTable.COLUMN_DESCRIPTION,
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
    return 0;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values)
  {
    return uri;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs)
  {
    return 0;
  }
}
