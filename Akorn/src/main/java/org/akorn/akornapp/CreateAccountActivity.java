package org.akorn.akornapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by milo on 05/02/2014.
 */
public class CreateAccountActivity extends Activity
{
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.create_account);
  }

  public void submitRequest(View view)
  {
    //Toast.makeText(this, "Account request would be sent now.", Toast.LENGTH_SHORT).show();

    // sync already running
    if (CreateAccountService.isRunning == true)
    {
      Toast.makeText(this, getString(R.string.account_in_progress), Toast.LENGTH_SHORT).show();
      return;
    }

    TextView email = (TextView) findViewById(R.id.email);
    TextView password = (TextView) findViewById(R.id.password);
    TextView password_confirmation = (TextView) findViewById(R.id.password_confirmation);

    Intent i = new Intent(this, CreateAccountService.class);
    i.putExtra("email", email.getText().toString());
    i.putExtra("password", password.getText().toString());
    i.putExtra("password_confirmation", password_confirmation.getText().toString());

    if (CreateAccountService.isRunning == false)
    {
      this.startService(i);
    }
    return;
  }

  public void gotoSettings(View view)
  {
    Intent i = new Intent(this, SettingsActivity.class);
    this.startActivity(i);
  }
}
