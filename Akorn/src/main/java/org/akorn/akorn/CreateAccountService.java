package org.akorn.akorn;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by milo on 05/02/2014.
 */
public class CreateAccountService extends IntentService
{
  public CreateAccountService()
  {
    super("CreateAccountService");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    return 0;
  }

  @Override
  public void onDestroy()
  {

  }

  protected void onHandleIntent(Intent intent)
  {

  }

}
