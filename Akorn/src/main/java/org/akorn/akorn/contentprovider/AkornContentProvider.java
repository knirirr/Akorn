package org.akorn.akorn.contentprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.net.Uri;

/**
 * Created by milo on 08/11/2013.
 */
public class AkornContentProvider extends ContentProvider
{
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
}
