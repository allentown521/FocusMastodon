package allen.town.focus.twitter.utils;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.receivers.IntentConstant;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.activities.drawer_activities.discover.trends.SearchedTrendsActivity;
import allen.town.focus.twitter.activities.search.SearchPager;
import com.lapism.searchview.adapter.SearchAdapter;
import com.lapism.searchview.adapter.SearchItem;
import com.lapism.searchview.view.SearchCodes;

import java.util.ArrayList;
import java.util.List;

public class SearchUtils {

    private Activity activity;

    private com.lapism.searchview.view.SearchView mSearchView;
    private List<SearchItem> mSuggestionsList = new ArrayList();
    private String defaultQuery;

    public SearchUtils(Activity activity) {
        this.activity = activity;
    }

    public void setUpSearch() {
        setUpSearch(true);
    }

    public void setUpSearch(boolean translate) {
        mSearchView = (com.lapism.searchview.view.SearchView) activity.findViewById(R.id.searchView);

        if (mSearchView == null) {
            return;
        }

        if (translate) {
            mSearchView.setTranslationY(Utils.getStatusBarHeight(activity));
        }

        mSearchView.setTheme(AppSettings.getInstance(activity).darkTheme ? SearchCodes.THEME_DARK : SearchCodes.THEME_LIGHT);

        mSearchView.setOnQueryTextListener(new com.lapism.searchview.view.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchView.hide(false);
                startSearchIntent(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        List<SearchItem> mResultsList = new ArrayList();
        SearchAdapter mSearchAdapter = new SearchAdapter(activity, mResultsList, mSuggestionsList,
                AppSettings.getInstance(activity).darkTheme ? SearchCodes.THEME_DARK : SearchCodes.THEME_LIGHT);
        mSearchAdapter.setOnItemClickListener(new SearchAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                mSearchView.hide(false);
                TextView textView = (TextView) view.findViewById(R.id.textView_item_text);
                CharSequence text = textView.getText();
                startSearchIntent(text + "");
            }
        });

        mSearchView.setAdapter(mSearchAdapter);
    }

    private void startSearchIntent(String query) {
        if (activity instanceof SearchPager) {
            Intent search = new Intent();
            search.setAction(Intent.ACTION_SEARCH);
            search.putExtra(SearchManager.QUERY, query);
            ((SearchPager) activity).handleIntent(search);
            ((SearchPager) activity).actionBar.setTitle(query.replace("-RT", ""));
            setText(query.replace("-RT", ""));

            Intent broadcast = new Intent(IntentConstant.NEW_SEARCH_ACTION);
            broadcast.putExtra("query", query);
            activity.sendBroadcast(broadcast);
        } else if (activity instanceof SearchedTrendsActivity) {
            Intent search = new Intent();
            search.setAction(Intent.ACTION_SEARCH);
            search.putExtra(SearchManager.QUERY, query);
            ((SearchedTrendsActivity) activity).handleIntent(search);
        } else {
            Intent search = new Intent(activity, SearchPager.class);
            search.setAction(Intent.ACTION_SEARCH);
            search.putExtra(SearchManager.QUERY, query);
            activity.startActivity(search);
        }
    }

    // returns true if it is one of the activities that always needs to translate the search view down.
    private boolean correctActivity() {
        return activity instanceof SearchPager;
    }

    public boolean isShowing() {
        return mSearchView.isShown();
    }

    public void hideSearch(boolean animated) {
        mSearchView.hide(animated);
    }

    public void showSearchView() {
        MySuggestionsProvider provider = new MySuggestionsProvider();

        mSuggestionsList.clear();
        mSuggestionsList.addAll(provider.getAllSuggestions(activity));
        mSearchView.show(true);
        setText(defaultQuery);
    }

    public void setText(String text) {
        defaultQuery = text;
        EditText et = (EditText) mSearchView.findViewById(com.lapism.searchview.R.id.editText_input);
        et.setText(text);

        if (text != null) {
            et.setSelection(text.length());
        }
    }
}

