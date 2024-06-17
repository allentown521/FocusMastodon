package allen.town.focus.twitter.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.utils.IOUtils;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.utils.api_helper.GiphyHelper;
import allen.town.focus.twitter.adapters.GifSearchAdapter;
import com.lapism.arrow.ArrowDrawable;
import com.lapism.searchview.view.SearchView;

import java.util.List;

import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.focus_common.views.AccentProgressDialog;

public class GiphySearch extends WhiteToolbarActivity {

    private SearchView toolbar;
    private ImageView backArrow;
    private EditText searchText;

    private RecyclerView recycler;
    private View progressSpinner;

    private GifSearchAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.setUpTheme(this,null);
        super.onCreate(savedInstanceState);

        Utils.setTaskDescription(this);

        try {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        } catch (Exception e) {

        }

        setContentView(R.layout.giffy_search_activity);

        recycler = (RecyclerView) findViewById(R.id.recycler_view);
        progressSpinner = findViewById(R.id.list_progress);
        backArrow = (ImageView) findViewById(R.id.imageView_arrow_back);
        toolbar = (SearchView) findViewById(R.id.searchView);
        searchText = (EditText) findViewById(R.id.editText_input);

        final ArrowDrawable drawable = new ArrowDrawable(this);
        drawable.animate(ArrowDrawable.STATE_ARROW);
        backArrow.setImageDrawable(drawable);

        toolbar.setOnSearchMenuListener(new SearchView.SearchMenuListener() {
            @Override
            public void onMenuClick() {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });

        toolbar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                executeQuery(query);
                backArrow.performClick();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        recycler.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                adapter.releaseVideo();
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                drawable.animate(ArrowDrawable.STATE_ARROW);
                loadTrending();
            }
        }, 750);
    }

    private void loadTrending() {
        progressSpinner.setVisibility(View.VISIBLE);
        GiphyHelper.trends(new GiphyHelper.Callback() {
            @Override
            public void onResponse(List<GiphyHelper.Gif> gifs) {
                setAdapter(gifs);
            }
        });
    }

    private void executeQuery(String query) {
        progressSpinner.setVisibility(View.VISIBLE);

        GiphyHelper.search(query, new GiphyHelper.Callback() {
            @Override
            public void onResponse(List<GiphyHelper.Gif> gifs) {
                setAdapter(gifs);

                // inform the user that there is a 3mb limit and Focus_for_Mastodon is displaying those
                /*final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(GiphySearch.this);
                if (!sharedPrefs.getBoolean("seen_giffy_disclaimer", false)) {
                    new AlertDialog.Builder(GiphySearch.this)
                            .setTitle(R.string.three_mb_limit)
                            .setMessage(R.string.three_mb_message)
                            .setCancelable(false)
                            .setPositiveButton(R.string.dont_show_again, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sharedPrefs.edit().putBoolean("seen_giffy_disclaimer", true).commit();
                                }
                            })
                            .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).create().show();
                }*/
            }
        });
    }

    private void setAdapter(List<GiphyHelper.Gif> gifs) {
        progressSpinner.setVisibility(View.GONE);

        if (adapter != null) {
            adapter.releaseVideo();
        }

        adapter = new GifSearchAdapter(gifs, new GifSearchAdapter.Callback() {
            @Override
            public void onClick(final GiphyHelper.Gif item) {
                new DownloadVideo(GiphySearch.this, item.gifUrl).execute();
            }
        });

        recycler.setLayoutManager(new LinearLayoutManager(GiphySearch.this));
        recycler.setAdapter(adapter);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (adapter != null) {
            adapter.releaseVideo();
        }
    }

    private static class DownloadVideo extends AsyncTask<Void, Void, Uri> {

        Activity activity;
        String video;
        ProgressDialog dialog;

        public DownloadVideo(Activity activity, String videoLink) {
            this.activity = activity;
            this.video = videoLink;
        }

        @Override
        public void onPreExecute() {
            dialog = AccentProgressDialog.show(activity,activity.getString(R.string.downloading) + "...","",true);
        }

        @Override
        protected Uri doInBackground(Void... arg0) {
            try {
                return IOUtils.saveGiffy(activity, video);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Uri downloadedTo) {
            if (downloadedTo != null) {
                activity.setResult(Activity.RESULT_OK, new Intent().setData(downloadedTo));
                activity.finish();

                try {
                    dialog.dismiss();
                } catch (Exception e) { }
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    TopSnackbarUtil.showSnack(activity, "Error downloading GIF", Toast.LENGTH_SHORT);
                } else {
                    TopSnackbarUtil.showSnack(activity, "Error downloading GIF. Have you allowed the storage permission?", Toast.LENGTH_SHORT);
                }
            }
        }

    }
}
