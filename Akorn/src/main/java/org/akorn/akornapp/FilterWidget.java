/**
 * Created by milo on 24/04/2014.
 */
package org.akorn.akornapp;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FilterWidget extends RelativeLayout
{
  public static String TAG = "AkornFilterWidget";
  Button deleteButton;

  public FilterWidget(Context context, AttributeSet attrs)
  {
    super(context, attrs);

    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.filter_widget, this, true);

  }

  public void setTitle(String text)
  {
    // filter_title
    TextView label = (TextView) findViewById(R.id.filter_title);
    label.setText(text);

  }

  public void setType(String text)
  {
    // filter_type
    TextView label = (TextView) findViewById(R.id.filter_type);
    label.setText(text);
  }

}
