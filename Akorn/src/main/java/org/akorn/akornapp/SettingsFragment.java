package org.akorn.akornapp;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by milo on 14/11/2013.
 */
public class SettingsFragment extends PreferenceFragment
{
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences);
  }
}
