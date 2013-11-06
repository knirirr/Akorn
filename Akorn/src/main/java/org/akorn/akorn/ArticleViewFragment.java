package org.akorn.akorn;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by milo on 04/11/2013.
 */
public class ArticleViewFragment extends Fragment
{
  final static String ARG_POSITION = "position";
  int mCurrentPosition = -1;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    // an option to share the text must be added here
    setHasOptionsMenu(true);

    // If activity recreated (such as from screen rotate), restore
    // the previous article selection set by onSaveInstanceState().
    // This is primarily necessary when in the two-pane layout.
    if (savedInstanceState != null)
    {
      mCurrentPosition = savedInstanceState.getInt(ARG_POSITION);
    }

    // Inflate the layout for this fragment
    TextView article = (TextView) inflater.inflate(R.layout.article_view, container, false);
    return article;
  }

  @Override
  public void onStart()
  {
    super.onStart();

    // During startup, check if there are arguments passed to the fragment.
    // onStart is a good place to do this because the layout has already been
    // applied to the fragment at this point so we can safely call the method
    // below that sets the article text.
    Bundle args = getArguments();
    if (args != null)
    {
      // Set article based on argument passed in
      updateArticleView(args.getInt(ARG_POSITION));
    }
    else if (mCurrentPosition != -1)
    {
      // Set article based on saved instance state defined during onCreateView
      updateArticleView(mCurrentPosition);
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
  {
    inflater.inflate(R.menu.viewing, menu);
  }


  public void updateArticleView(int position)
  {
    TextView article;
    if (getActivity().findViewById(R.id.fragment_container) != null)
    {
      article = (TextView) getActivity().findViewById(R.id.individual_article);
    }
    else
    {
      article = (TextView) getActivity().findViewById(R.id.view_fragment);
    }
    article.setText(Article.Articles[position]);
    mCurrentPosition = position;
  }

  @Override
  public void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);

    // Save the current article selection in case we need to recreate the fragment
    outState.putInt(ARG_POSITION, mCurrentPosition);
  }


}
