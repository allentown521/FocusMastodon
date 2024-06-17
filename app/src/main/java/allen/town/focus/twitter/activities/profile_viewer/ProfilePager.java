package allen.town.focus.twitter.activities.profile_viewer;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.reflect.TypeToken;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.WhiteToolbarActivity;
import allen.town.focus.twitter.activities.compose.ComposeActivity;
import allen.town.focus.twitter.activities.media_viewer.image.ImageViewerActivity;
import allen.town.focus.twitter.activities.media_viewer.image.TimeoutThread;
import allen.town.focus.twitter.adapters.TimeLineCursorAdapter;
import allen.town.focus.twitter.api.requests.accounts.GetAccountByID;
import allen.town.focus.twitter.api.requests.accounts.GetAccountInLists;
import allen.town.focus.twitter.api.requests.accounts.GetAccountRelationships;
import allen.town.focus.twitter.api.requests.accounts.GetAccountStatuses;
import allen.town.focus.twitter.api.requests.accounts.SetAccountBlocked;
import allen.town.focus.twitter.api.requests.accounts.SetAccountFollowed;
import allen.town.focus.twitter.api.requests.accounts.SetAccountMuted;
import allen.town.focus.twitter.api.requests.accounts.UpdateAccountCredentials;
import allen.town.focus.twitter.api.requests.list.AddAccountToList;
import allen.town.focus.twitter.api.requests.list.DeleteListAccount;
import allen.town.focus.twitter.api.requests.notifications.GetNotifications;
import allen.town.focus.twitter.api.requests.statuses.GetFavoritedStatuses;
import allen.town.focus.twitter.data.sq_lite.FavoriteUsersDataSource;
import allen.town.focus.twitter.data.sq_lite.FollowersDataSource;
import allen.town.focus.twitter.databinding.ChangeProfileInfoDialogBinding;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.HeaderPaginationList;
import allen.town.focus.twitter.model.MastoList;
import allen.town.focus.twitter.model.Notification;
import allen.town.focus.twitter.model.Relationship;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.HtmlParser;
import allen.town.focus.twitter.utils.IOUtils;
import allen.town.focus.twitter.utils.MySuggestionsProvider;
import allen.town.focus.twitter.utils.StatusFilterPredicate;
import allen.town.focus.twitter.utils.UiUtils;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.utils.text.TextUtils;
import allen.town.focus.twitter.views.TweetView;
import allen.town.focus.twitter.views.popups.profile.PicturesPopup;
import allen.town.focus.twitter.views.popups.profile.ProfileFollowersPopup;
import allen.town.focus.twitter.views.popups.profile.ProfileFriendsPopup;
import allen.town.focus.twitter.views.popups.profile.ProfileTimelinePopupLayout;
import allen.town.focus.twitter.views.popups.profile.ProfileTweetsPopup;
import allen.town.focus.twitter.views.popups.profile.ProfileUsersListsPopup;
import allen.town.focus.twitter.views.widgets.text.FontPrefTextView;
import allen.town.focus_common.extensions.ColorExtensionsUtils;
import allen.town.focus_common.util.BasePreferenceUtil;
import allen.town.focus_common.util.JsonHelper;
import allen.town.focus_common.util.ThemeUtils;
import allen.town.focus_common.util.Timber;
import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.focus_common.views.AccentMaterialDialog;
import allen.town.focus_common.views.AccentProgressDialog;
import code.name.monkey.appthemehelper.ThemeStore;
import fisk.chipcloud.ChipCloud;
import fisk.chipcloud.ChipCloudConfig;
import fisk.chipcloud.ChipListener;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import twitter4j.Status;
import twitter4j.StatusJSONImplMastodon;
import twitter4j.User;
import twitter4j.UserJSONImplMastodon;
import xyz.klinker.android.drag_dismiss.DragDismissIntentBuilder;
import xyz.klinker.android.drag_dismiss.delegate_hack.DragDismissDelegateHackHack;

public class ProfilePager extends WhiteToolbarActivity implements DragDismissDelegateHackHack.Callback {

    private static final int NUM_TWEETS_ON_TIMELINE = 15;
    private static final int LOAD_CAPACITY_PER_LIST = 20;

    public static void start(Context context, User user) {
        start(context, user.getName(), user.getId() + "", user.getOriginalProfileImageURL());
    }

    public static void start(Context context, String accountId) {
        start(context, null, accountId, null);
    }

    public static void start(Context context, String name, String accountId, String profilePic) {
        Intent intent = new Intent(context, ProfilePager.class);

        DragDismissIntentBuilder.Theme theme = DragDismissIntentBuilder.Theme.LIGHT;
        AppSettings settings = AppSettings.getInstance(context);
        if (settings.blackTheme) {
            theme = DragDismissIntentBuilder.Theme.BLACK;
        } else if (settings.darkTheme) {
            theme = DragDismissIntentBuilder.Theme.DARK;
        }

        new DragDismissIntentBuilder(context)
//                .setPrimaryColorValue(settings.themeColors.primaryColor)
                .setDragElasticity(DragDismissIntentBuilder.DragElasticity.XLARGE)
                .setShouldScrollToolbar(true)
                .setToolbarTitle(name)
                .setShowToolbar(true)
                .setTheme(theme)
                .setDrawUnderStatusBar(true)
                .build(intent);

        intent.putExtra("name", name);
        intent.putExtra("accountId", accountId);
        intent.putExtra("proPic", profilePic);

        context.startActivity(intent);
    }

    private Context context;
    private AppSettings settings;
    private SharedPreferences sharedPrefs;

    public ImageView followButton;
    public ImageView profilePic;
    public TextView followerCount;
    public TextView followingCount;
    public FontPrefTextView description;
    public FontPrefTextView location;
    public FontPrefTextView website;
    public View profileButtons;
    public LinearLayout chipLayout;
    public LinearLayout timelineContent;

    private PicturesPopup picsPopup;
    private ProfileFollowersPopup fol;
    private ProfileFriendsPopup fri;
    private ProfileUsersListsPopup usersListsPopup;
    public ProfileTweetsPopup tweetsPopup;
    public ProfileTimelinePopupLayout timelinePopup;

    // start with tweets, replies, retweets as checked. Likes and mentions as not checked.
    public boolean[] chipSelectedState = new boolean[]{true, true, true, false, false};
    public ChipCloud chipCloud;

    private String accountId;
    private String proPic;

    private boolean isMyProfile = false;
    private boolean isBlocking;
    private boolean isFollowing;
    private boolean followingYou;
    private boolean isFavorite;
    private boolean isMuted;
    private boolean isMuffled;
    private boolean isFollowingSet = false;

    public List<Status> tweets = new ArrayList<>();
    public String tweetsPaging = "";

    public List<Status> favorites = new ArrayList<>();
    public String favoritesPaging = "";

    public List<Status> mentions = new ArrayList<>();
    public String mentionsQuery = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DragDismissDelegateHackHack delegate = new DragDismissDelegateHackHack(this, this);
        delegate.onCreate(savedInstanceState);

        if (!AppSettings.getInstance(this).dragDismiss) {
            findViewById(R.id.dragdismiss_drag_dismiss_layout).setEnabled(false);
        }

        overridePendingTransition(R.anim.activity_slide_up, 0);
    }

    @Override
    public View onCreateContent(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        Utils.setTaskDescription(this);
        Utils.setSharedContentTransition(this);

        context = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        settings = AppSettings.getInstance(this);

        Utils.setUpProfileTheme(context, settings);
        getFromIntent();
        View root = inflater.inflate(R.layout.user_profile, parent, false);

        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.windowBackground});
        int resource = a.getResourceId(0, 0);
        a.recycle();

        findViewById(R.id.dragdismiss_content).setBackgroundResource(resource);

        setUpContent(root);
        getUser();

        return root;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recreate();
    }

    public void setUpContent(View root) {
        profilePic = (ImageView) root.findViewById(R.id.profile_pic);

        followerCount = (TextView) root.findViewById(R.id.followers_number);
        followingCount = (TextView) root.findViewById(R.id.following_number);
        description = (FontPrefTextView) root.findViewById(R.id.user_description);
        location = (FontPrefTextView) root.findViewById(R.id.user_location);
        website = (FontPrefTextView) root.findViewById(R.id.user_webpage);

        profileButtons = root.findViewById(R.id.profile_buttons);
        chipLayout = (LinearLayout) root.findViewById(R.id.chip_layout);

        ChipCloudConfig config = new ChipCloudConfig()
                .selectMode(ChipCloud.SelectMode.multi)
                .checkedChipColor(BasePreferenceUtil.getMaterialYou() ? ThemeUtils.getColorFromAttr(context, R.attr.colorPrimaryContainer) : ThemeStore.accentColor(context))
                .checkedTextColor(ThemeUtils.getColorFromAttr(context, android.R.attr.textColorPrimary))
                .uncheckedChipColor(ThemeUtils.getColorFromAttr(context, R.attr.windowBackground))
                .uncheckedTextColor(ThemeUtils.getColorFromAttr(context, android.R.attr.textColorPrimary))
                .useInsetPadding(true);


        chipCloud = new ChipCloud(this, chipLayout, config);
        chipCloud.setListener(new ChipListener() {
            @Override
            public void chipCheckedChange(int index, boolean checked, boolean userClicked) {
                if (userClicked) {
                    chipSelectedState[index] = checked;
                    addTweetsToLayout(filterTweets());
                }
            }
        }, true);

        loadProfilePicture();
    }

    public void loadProfilePicture() {
        try {
            Glide.with(this).load(proPic)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(profilePic);

            profilePic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (thisUser != null) {
                        ImageViewerActivity.Companion.startActivity(context, proPic, thisUser.getProfileBannerURL());
                    } else {
                        ImageViewerActivity.Companion.startActivity(context, proPic);
                    }
                }
            });

            findViewById(R.id.banner).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (thisUser != null) {
                        ImageViewerActivity.Companion.startActivity(context, proPic, thisUser.getProfileBannerURL());
                    } else {
                        ImageViewerActivity.Companion.startActivity(context, proPic);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setBannerImage() {
        try {
            Glide.with(context)
                    .load(thisUser.getProfileBannerURL())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into((ImageView) findViewById(R.id.banner));
        } catch (Exception e) {

        }
    }

    public void getFromIntent() {
        Intent from = getIntent();

        accountId = from.getStringExtra("accountId");
        proPic = from.getStringExtra("proPic");

        if (accountId != null && accountId.equalsIgnoreCase(settings.myId)) {
            isMyProfile = true;
        }
    }

    public void showProfileContent(final User user) {
        proPic = user.getOriginalProfileImageURL();
        loadProfilePicture();

        CoordinatorLayout frameLayout = (CoordinatorLayout)
                findViewById(R.id.dragdismiss_background_view);

        FloatingActionButton fab = new FloatingActionButton(this);

        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                Utils.toDP(56, context), Utils.toDP(56, context));
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.bottomMargin = Utils.toDP(16, context);
        params.rightMargin = Utils.toDP(16, context);
        params.leftMargin = Utils.toDP(16, context);
        fab.setLayoutParams(params);

        fab.setImageResource(R.drawable.ic_fab_pencil);
        ColorExtensionsUtils.accentColor(fab);
        frameLayout.addView(fab);
        fab.setOnClickListener(v -> {
            Intent compose = new Intent(ProfilePager.this, ComposeActivity.class);
            ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                    v.getMeasuredWidth(), v.getMeasuredHeight());
            compose.putExtra("user", "@" + user.getScreenName());
            compose.putExtra("already_animated", true);
            startActivity(compose, opts.toBundle());
        });

        String des = user.getDescription();
        String loc = user.getLocation();
        String web = user.getURL();

        if (des != null && !des.equals("")) {
            description.setText(des);
            HtmlParser.linkifyText(description, null, null, false);
        } else {
            description.setVisibility(View.GONE);
        }
        if (loc != null && !loc.equals("")) {
            location.setVisibility(View.VISIBLE);
            location.setText(loc);
        } else {
            location.setVisibility(View.GONE);
        }
        if (web != null && !web.equals("")) {
            website.setVisibility(View.VISIBLE);
            website.setText(user.getURLEntity().getDisplayURL());
            TextUtils.linkifyText(context, website, null, true, user.getURLEntity().getExpandedURL(), false);

            if (location.getVisibility() == View.GONE) {
                website.setPadding(0, Utils.toDP(16, context), 0, 0);
            }
        } else {
            website.setVisibility(View.GONE);
        }

        TextUtils.linkifyText(context, description, null, true, "", false);

        TextView followingStatus = (TextView) findViewById(R.id.follow_status);
        followButton = (ImageView) findViewById(R.id.follow_button);

        if (isFollowing) {
            followButton.setImageResource(R.drawable.ic_unfollow);
        } else {
            followButton.setImageResource(R.drawable.ic_follow);
        }

        if (isFollowing || !settings.crossAccActions) {
            followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AccentMaterialDialog(
                            context,
                            R.style.MaterialAlertDialogTheme
                    )
                            .setMessage(isFollowing ? R.string.are_you_sure_unfollow : R.string.are_you_sure_follow)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new FollowUser(TYPE_ACC_ONE).execute();
                                }
                            }).show();
                }
            });
        } else if (settings.crossAccActions) {
            followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // dialog for favoriting
                    String[] options = new String[2];
//                    String[] options = new String[3];

                    options[0] = "@" + settings.myScreenName;
                    options[1] = "@" + settings.secondScreenName;
//                    options[2] = context.getString(R.string.both_accounts);

                    new AccentMaterialDialog(
                            context,
                            R.style.MaterialAlertDialogTheme
                    )
                            .setItems(options, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int item) {
                                    new FollowUser(item + 1).execute();
                                }
                            })
                            .create().show();
                }
            });
        }

        if (followingYou) {
            followingStatus.setText(Html.fromHtml("<b>" + getString(R.string.follows_you) + "<b>"));
        } else {
            followingStatus.setText(Html.fromHtml("<b>" + getString(R.string.not_following_you) + "<b>"));
        }

        if (isMyProfile) {
            findViewById(R.id.follow_button).setVisibility(View.GONE);
            findViewById(R.id.follow_status).setVisibility(View.GONE);
        }

        View pictures = findViewById(R.id.media_button);
        View lists = findViewById(R.id.lists_button);

        usersListsPopup = new ProfileUsersListsPopup(context, thisUser);
        lists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usersListsPopup.setExpansionPointForAnim(view);
                usersListsPopup.show();
            }
        });

        picsPopup = new PicturesPopup(context, thisUser);
        pictures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                picsPopup.setExpansionPointForAnim(view);
                picsPopup.show();
            }
        });

        if (user.getFriendsCount() < 1000) {
            followingCount.setText("" + user.getFriendsCount());
        } else {
            followingCount.setText("" + Utils.coolFormat(user.getFriendsCount(), 0));
        }

        if (user.getFollowersCount() < 1000) {
            followerCount.setText("" + user.getFollowersCount());
        } else {
            followerCount.setText("" + Utils.coolFormat(user.getFollowersCount(), 0));
        }

        ImageView verified = (ImageView) findViewById(R.id.verified);
        if (settings.darkTheme) {
            verified.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        } else {
            verified.setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY);
        }

        if (user.isVerified()) {
            verified.setVisibility(View.VISIBLE);
        }

        View openFollowers = findViewById(R.id.followers_button);
        openFollowers.setVisibility(View.VISIBLE);
        TextView followersText = (TextView) findViewById(R.id.followers_text);
        followersText.setText(Html.fromHtml("<b>" + followersText.getText().toString() + "</b>"));

        fol = new ProfileFollowersPopup(context, user);

        openFollowers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fol.setExpansionPointForAnim(view);
                fol.setOnTopOfView(view);
                fol.show();
            }
        });
        openFollowers.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                TopSnackbarUtil.showSnack(ProfilePager.this,
                        getString(R.string.followers) + ": " + user.getFollowersCount(),
                        Toast.LENGTH_SHORT);
                return true;
            }
        });

        View openFriends = findViewById(R.id.following_button);
        openFriends.setVisibility(View.VISIBLE);
        TextView followingText = (TextView) findViewById(R.id.following_text);
        followingText.setText(Html.fromHtml("<b>" + followingText.getText().toString() + "</b>"));

        fri = new ProfileFriendsPopup(context, user);

        openFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fri.setExpansionPointForAnim(view);
                fri.setOnTopOfView(view);
                fri.show();
            }
        });
        openFriends.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                TopSnackbarUtil.showSnack(ProfilePager.this,
                        getString(R.string.following) + ": " + user.getFriendsCount(),
                        Toast.LENGTH_SHORT);
                return true;
            }
        });

        animateIn(profileButtons);
    }

    private void setLongClickChipListener(final int index) {
        chipLayout.getChildAt(index).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                for (int i = 0; i < chipLayout.getChildCount(); i++) {
                    if (i == index) {
                        chipCloud.setChecked(i);
                        chipSelectedState[i] = true;
                    } else {
                        chipCloud.deselectIndex(i);
                        chipSelectedState[i] = false;
                    }

                    chipLayout.getChildAt(i).setEnabled(false);
                }

                chipLayout.setEnabled(false);
                chipLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < chipLayout.getChildCount(); i++) {
                            chipLayout.getChildAt(i).setEnabled(true);
                        }
                    }
                }, 2000);

                addTweetsToLayout(filterTweets());
                return false;
            }
        });
    }

    private void prepareTweetsLayout() {
        chipCloud.addChip(getString(R.string.posts));
        chipCloud.addChip(getString(R.string.replies));
        chipCloud.addChip(getString(R.string.retweets));
        chipCloud.setSelectedIndexes(new int[]{0, 1, 2});

        for (int i = 0; i < 3; i++) {
            setLongClickChipListener(i);
        }


        timelineContent = (LinearLayout) findViewById(R.id.tweets_content);
        TextView tweetsTitle = (TextView) findViewById(R.id.tweets_title_text);
        Button showAllTweets = (Button) findViewById(R.id.show_all_tweets_button);


        if (tweetsTitle == null) {
            return;
        }

        final View tweetsLayout = getLayoutInflater().inflate(R.layout.convo_popup_layout, null, false);
        tweetsPopup = new ProfileTweetsPopup(context, tweetsLayout, thisUser);

        showAllTweets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tweetsPopup.setExpansionPointForAnim(view);
                tweetsPopup.show();
            }
        });
        showAllTweets.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                TopSnackbarUtil.showSnack(ProfilePager.this,
                        getString(R.string.posts) + ": " + thisUser.getStatusesCount(),
                        Toast.LENGTH_SHORT);
                return true;
            }
        });

        if (thisUser.getStatusesCount() < 1000) {
            showAllTweets.setText(getString(R.string.show_all_tweets) + " (" + thisUser.getStatusesCount() + ")");
        } else {
            showAllTweets.setText(getString(R.string.show_all_tweets) + " (" + Utils.coolFormat(thisUser.getStatusesCount(), 0) + ")");
        }

        final View timelineLayout = getLayoutInflater().inflate(R.layout.convo_popup_layout, null, false);
        timelinePopup = new ProfileTimelinePopupLayout(this, timelineLayout, thisUser);

        View showAll = findViewById(R.id.show_all);
        showAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timelinePopup.setExpansionPointForAnim(view);
                timelinePopup.show();
            }
        });

        if (settings.darkTheme && settings.theme == AppSettings.THEME_DARK_BACKGROUND_COLOR) {
            ((TextView) showAll.findViewById(R.id.show_all_text))
                    .setTextColor(ThemeStore.accentColor(context));
        } else if (!settings.darkTheme && settings.theme == AppSettings.THEME_WHITE) {
            ((TextView) showAll.findViewById(R.id.show_all_text))
                    .setTextColor(ThemeStore.accentColor(context));
        }

        addTweetsToLayout(tweets);
    }

    private void addTweetsToLayout(List<Status> statuses) {
        boolean addShowAll = false;
        int size = 0;
        if (statuses.size() >= NUM_TWEETS_ON_TIMELINE) {
            size = NUM_TWEETS_ON_TIMELINE;

            if (statuses.size() > NUM_TWEETS_ON_TIMELINE) {
                addShowAll = true;
            }
        } else {
            size = statuses.size();
        }

        timelineContent.removeAllViews();

        if (size > 0) {
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    View tweetDivider = new View(context);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.toDP(1, context));
                    tweetDivider.setLayoutParams(params);

                    tweetDivider.setBackgroundColor(getResources().getColor(R.color.text_drawer));

                    timelineContent.addView(tweetDivider);
                }

                TweetView t = new TweetView(context, statuses.get(i));
                t.setCurrentUser(thisUser.getScreenName());
                t.setSmallImage(true);
                timelineContent.addView(t.getView());
            }
        }

        View showAll = findViewById(R.id.show_all);

        if (addShowAll) {
            showAll.setVisibility(View.VISIBLE);
            showAll.getLayoutParams().height = Utils.toDP(112, this);
        } else {
            showAll.setVisibility(View.INVISIBLE);
            showAll.getLayoutParams().height = Utils.toDP(16, this);
        }

        showAll.requestLayout();

        animateIn(timelineContent);
    }

    private void animateIn(final View v) {
        if (v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
        }

        ValueAnimator alpha = ValueAnimator.ofFloat(0f, 1f);
        alpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (Float) valueAnimator.getAnimatedValue();
                v.setAlpha(val);
            }
        });
        alpha.setDuration(200);
        alpha.setInterpolator(TimeLineCursorAdapter.ANIMATION_INTERPOLATOR);
        alpha.start();
    }

    public User thisUser;

    public void getUser() {
        TimeoutThread getUser = new TimeoutThread(new Runnable() {
            @Override
            public void run() {

                try {
                    thisUser = new UserJSONImplMastodon(new GetAccountByID(accountId).execSync());
                } catch (Exception e) {
                    thisUser = null;
                }

                if (thisUser == null) {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TopSnackbarUtil.showSnack(context, R.string.error, Toast.LENGTH_SHORT);
                        }
                    });
                }

                try {
                    FollowersDataSource.getInstance(context).createUser(thisUser, sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1));

                } catch (Exception e) {
                    // the user already exists. don't know if this is more efficient than querying the db or not.
                }

                if (thisUser != null) {
                    final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context,
                            MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
                    suggestions.saveRecentQuery("@" + thisUser.getScreenName(), null);
                }

                // set the info to set up the action bar items
                if (isMyProfile) {
                    if (thisUser != null) {
                        // put in the banner and profile pic to shared prefs
                        sharedPrefs.edit().putString("profile_pic_url_" + settings.currentAccount, thisUser.getOriginalProfileImageURL()).commit();
                        sharedPrefs.edit().putString("twitter_background_url_" + settings.currentAccount, thisUser.getProfileBannerURL()).commit();
                        isMuffled = UiUtils.getMuffledUsersKyes(sharedPrefs).contains(accountId);
                    }
                } else {
                    try {

                        Relationship friendship = new GetAccountRelationships(new ArrayList<>() {
                            {
                                add(accountId);
                            }
                        }).execSync().get(0);

                        isFollowing = friendship.following;
                        followingYou = friendship.followedBy;
                        isBlocking = friendship.blocking;
                        isMuted = friendship.muting;
                        isMuffled = UiUtils.getMuffledUsersKyes(sharedPrefs).contains(accountId);
                        isFavorite = FavoriteUsersDataSource.getInstance(context).isFavUser(accountId);

                        isFollowingSet = true;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (thisUser != null) {
                    ((Activity) context).runOnUiThread(() -> {
                        ActionBar actionBar = getSupportActionBar();
                        if (actionBar != null) {
                            actionBar.setTitle(thisUser.getName());
                            //actionBar.setSubtitle("@" + thisUser.getScreenName());
                        }

                        invalidateOptionsMenu();
                        showProfileContent(thisUser);

                        setBannerImage();
                    });
                }

                try {
                    fetchTweets();
                    ((Activity) context).runOnUiThread(() -> prepareTweetsLayout());

                    if (isMyProfile) {
                        fetchFavorites();
                        ((Activity) context).runOnUiThread(() -> {
                            chipCloud.addChip(getString(R.string.favorites));
                            setLongClickChipListener(3);
                        });
                    }


                    if (isMyProfile) {
                        fetchMentions();
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                chipCloud.addChip(getString(R.string.mentions));
                                setLongClickChipListener(4);
                            }
                        });
                    }


                    fetchTweets();
                } catch (Exception e) {
                    if (thisUser != null && thisUser.isProtected()) {
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TopSnackbarUtil.showSnack(context, getString(R.string.protected_account), Toast.LENGTH_SHORT);
                            }
                        });
                    } else {
                        Timber.e("Error fetching tweets: " + e.getMessage());
                    }
                }

            }
        });

        getUser.setPriority(Thread.MAX_PRIORITY);
        getUser.start();
    }

    public List<Status> filterTweets() {
        final int tweetsIndex = 0;
        final int repliesIndex = 1;
        final int retweetsIndex = 2;
        final int likesIndex = 3;
        final int mentionsIndex = 4;

        List<Status> filteredStatuses = new ArrayList<>();

        for (Status status : tweets) {
            if (chipSelectedState[tweetsIndex] && !status.isRetweet() && !status.getText().startsWith("@")) {
                filteredStatuses.add(status);
            } else if (chipSelectedState[retweetsIndex] && status.isRetweet()) {
                filteredStatuses.add(status);
            } else if (chipSelectedState[repliesIndex] && Html.fromHtml(status.getText()).toString().startsWith("@")) {
                filteredStatuses.add(status);
            }
        }

        if (chipSelectedState[likesIndex]) {
            filteredStatuses.addAll(favorites);
        }

        if (chipSelectedState[mentionsIndex]) {
            filteredStatuses.addAll(mentions);
        }

        Collections.sort(filteredStatuses, new Comparator<Status>() {
            public int compare(Status result1, Status result2) {
                return result2.getCreatedAt().compareTo(result1.getCreatedAt());
            }
        });

        return filteredStatuses;
    }

    public boolean fetchTweets() throws Exception {
        if (tweetsPaging != null) {
            HeaderPaginationList<StatusJSONImplMastodon> statuses = StatusJSONImplMastodon.createStatusList(new GetAccountStatuses(thisUser.getId() + "", tweetsPaging, null, null, 20, GetAccountStatuses.Filter.INCLUDE_REPLIES).execSync());
            if (statuses.size() == LOAD_CAPACITY_PER_LIST) {
                tweetsPaging = statuses.getNextCursor();
            } else {
                tweetsPaging = null;
            }

            tweets.addAll(statuses);
            return true;
        }

        return false;
    }

    public boolean fetchFavorites() throws Exception {
        if (favoritesPaging != null) {
            HeaderPaginationList<StatusJSONImplMastodon> statuses =
                    StatusJSONImplMastodon.createStatusList(new GetFavoritedStatuses(thisUser.getId() + "", favoritesPaging, 20).execSync());
            if (statuses.size() == LOAD_CAPACITY_PER_LIST) {
                favoritesPaging = statuses.getNextCursor();
            } else {
                favoritesPaging = null;
            }

            favorites.addAll(statuses);
            return true;
        }

        return false;
    }

    public boolean fetchMentions() throws Exception {
        if (mentionsQuery != null) {
            boolean hasMore = true;
            HeaderPaginationList<StatusJSONImplMastodon> statuses;
            do {
                HeaderPaginationList<Notification> list = new GetNotifications(mentionsQuery, "", 30, EnumSet.of(Notification.Type.MENTION)).execSync();
                statuses = HeaderPaginationList.copyOnlyPage(list);
                if (list != null && list.size() > 0) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).status != null) {
                            statuses.add(new StatusJSONImplMastodon(list.get(i).status, list.get(i).id));
                        }
                    }
                }

                mentionsQuery = list.getNextCursor();

                List<StatusJSONImplMastodon> filteredList = statuses.stream().filter(new StatusFilterPredicate(AppSettings.getInstance(context).mySessionId, Filter.FilterContext.NOTIFICATIONS)).collect(Collectors.toList());

                mentions.addAll(filteredList);

                if (!statuses.hasNext()) {
                    hasMore = false;
                }

            } while (hasMore && mentions.size() < LOAD_CAPACITY_PER_LIST);

            if (!hasMore) {
                mentionsQuery = null;
            }


            return true;
        }

        return false;
    }

    private class GetActionBarInfo extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... urls) {
            if (isMyProfile) {
                if (thisUser != null) {
                    // put in the banner and profile pic to shared prefs
                    sharedPrefs.edit().putString("profile_pic_url_" + sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1), thisUser.getOriginalProfileImageURL()).commit();
                    sharedPrefs.edit().putString("twitter_background_url_" + sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1), thisUser.getProfileBannerURL()).commit();
                }
                return null;
            } else {
                try {
                    Relationship friendship = new GetAccountRelationships(new ArrayList<>() {
                        {
                            add(accountId);
                        }
                    }).execSync().get(0);


                    isFollowing = friendship.following;
                    isBlocking = friendship.blocking;
                    isMuted = friendship.muting;
                    isFavorite = FavoriteUsersDataSource.getInstance(context).isFavUser(accountId);
                    isFollowingSet = true;

                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        protected void onPostExecute(Void none) {
            if (thisUser != null) {
                //actionBar.setTitle(thisUser.getName());
            }
            invalidateOptionsMenu();
        }
    }

    private final int TYPE_ACC_ONE = 1;
    private final int TYPE_ACC_TWO = 2;
    private final int TYPE_BOTH_ACC = 3;

    private class FollowUser extends AsyncTask<String, Void, Boolean> {

        private Exception e = null;

        private int followType;

        FollowUser(int followType) {
            this.followType = followType;
        }

        protected Boolean doInBackground(String... urls) {
            try {
                if (thisUser != null) {
                    boolean useAccount = false;
                    boolean useSecondAccount = false;
                    if (followType == TYPE_ACC_ONE) {
                        useAccount = true;
                    } else if (followType == TYPE_ACC_TWO) {
                        useSecondAccount = true;
                    } else {
                        useAccount = true;
                        useSecondAccount = true;
                    }

                    String accountId = thisUser.getId() + "";
                    boolean isFollowing = false;

                    if (useAccount) {
                        Relationship friendship = new GetAccountRelationships(new ArrayList<>() {
                            {
                                add(accountId);
                            }
                        }).execSync().get(0);
                        isFollowing = friendship.following;
                    }

                    if (isFollowing) {
                        if (useAccount) {
                            new SetAccountFollowed(accountId, false, false).execSync();
                        }

                        if (useSecondAccount) {
                            new SetAccountFollowed(accountId, true, true).execSecondAccountSync();
                        }

                        return false;
                    } else {
                        if (useAccount) {
                            new SetAccountFollowed(accountId, true, true).execSync();
                        }

                        if (useSecondAccount) {
                            new SetAccountFollowed(accountId, true, true).execSecondAccountSync();
                        }

                        FollowersDataSource.getInstance(context).createUser(thisUser, sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1));

                        return true;
                    }
                }

                return null;
            } catch (Exception e) {
                e.printStackTrace();
                this.e = e;
                return null;
            }
        }

        protected void onPostExecute(Boolean created) {
            // add a toast - now following or unfollowed
            // true = followed
            // false = unfollowed
            if (created != null) {
                if (created) {
                    TopSnackbarUtil.showSnack(context, getResources().getString(R.string.followed_user), Toast.LENGTH_SHORT);
                    followButton.setImageResource(R.drawable.ic_unfollow);
                } else {
                    TopSnackbarUtil.showSnack(context, getResources().getString(R.string.unfollowed_user), Toast.LENGTH_SHORT);
                    followButton.setImageResource(R.drawable.ic_follow);
                }
            } else {
                TopSnackbarUtil.showSnack(context, getResources().getString(R.string.error) + ": " + e.getMessage(), Toast.LENGTH_SHORT);
            }

            new GetActionBarInfo().execute();
        }
    }

    private class BlockUser extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            try {
                if (thisUser != null) {

                    String accountId = thisUser.getId() + "";

                    Relationship friendship = new GetAccountRelationships(new ArrayList<>() {
                        {
                            add(accountId);
                        }
                    }).execSync().get(0);

                    boolean isBlocking = friendship.blocking;

                    if (isBlocking) {
                        new SetAccountBlocked(accountId, false).setCallback(new Callback<>() {
                            @Override
                            public void onSuccess(Relationship result) {
                                UiUtils.performUnMuteAction((Activity) context, sharedPrefs, accountId);
                            }

                            @Override
                            public void onError(ErrorResponse error) {

                            }
                        }).exec();
                        return false;
                    } else {
                        new SetAccountBlocked(accountId, true).setCallback(new Callback<>() {
                            @Override
                            public void onSuccess(Relationship result) {
                                UiUtils.performMuteAction((Activity) context, sharedPrefs, accountId, false);
                            }

                            @Override
                            public void onError(ErrorResponse error) {

                            }
                        }).exec();
                        return true;
                    }
                }

                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Boolean isBlocked) {
            // true = followed
            // false = unfollowed
            if (isBlocked != null) {
                if (isBlocked) {
                    TopSnackbarUtil.showSnack(context, getResources().getString(R.string.blocked_user), Toast.LENGTH_SHORT);
                } else {
                    TopSnackbarUtil.showSnack(context, getResources().getString(R.string.unblocked_user), Toast.LENGTH_SHORT);
                }
            } else {
                TopSnackbarUtil.showSnack(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
            }

            new GetActionBarInfo().execute();
        }
    }

    private class FavoriteUser extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            try {
                int currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);
                if (thisUser != null) {
                    if (isFavorite) {
                        // destroy favorite
                        FavoriteUsersDataSource.getInstance(context).deleteUser(thisUser.getId());


                        isFavorite = false;

                        return false;

                    } else {
                        FavoriteUsersDataSource.getInstance(context).createUser(thisUser, currentAccount);


                        isFavorite = true;

                        return true;
                    }
                }

                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Boolean isFavorited) {
            new GetActionBarInfo().execute();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.profile_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        final int MENU_FAVORITE = 0;
        final int MENU_BLOCK = 1;
        final int MENU_UNBLOCK = 2;
        final int MENU_ADD_LIST = 3;
        final int MENU_DM = 4;
        final int MENU_CHANGE_PICTURE = 5;
        final int MENU_CHANGE_BANNER = 6;
        final int MENU_CHANGE_BIO = 7;
        final int MENU_MUTE = 8;
        final int MENU_UNMUTE = 9;
        final int MENU_MUFFLE = 10;
        final int MENU_UNMUFFLE = 11;
        final int MENU_SHARE_LINK = 12;

        if (isFavorite) {
            menu.getItem(MENU_FAVORITE).setIcon(getResources().getDrawable(R.drawable.ic_heart));
            menu.getItem(MENU_FAVORITE).setTitle(getString(R.string.menu_unfavorite));
            menu.getItem(MENU_FAVORITE).setVisible(true);
        } else {
            menu.getItem(MENU_FAVORITE).setIcon(getResources().getDrawable(R.drawable.ic_heart_outline));
            menu.getItem(MENU_FAVORITE).setTitle(getString(R.string.menu_favorite));
            menu.getItem(MENU_FAVORITE).setVisible(true);
        }

        if (isMyProfile) {
            menu.getItem(MENU_BLOCK).setVisible(false);
            menu.getItem(MENU_UNBLOCK).setVisible(false);
            menu.getItem(MENU_ADD_LIST).setVisible(false);
            menu.getItem(MENU_DM).setVisible(false);
        } else {
            if (isFollowingSet) {
                if (isBlocking) {
                    menu.getItem(MENU_BLOCK).setVisible(false);
                } else {
                    menu.getItem(MENU_UNBLOCK).setVisible(false);
                }
            } else {
                menu.getItem(MENU_BLOCK).setVisible(false);
                menu.getItem(MENU_UNBLOCK).setVisible(false);
                menu.getItem(MENU_MUTE).setVisible(false);
                menu.getItem(MENU_UNMUTE).setVisible(false);
                menu.getItem(MENU_MUFFLE).setVisible(false);
                menu.getItem(MENU_UNMUFFLE).setVisible(false);
            }

            menu.getItem(MENU_CHANGE_BIO).setVisible(false);
            menu.getItem(MENU_CHANGE_BANNER).setVisible(false);
            menu.getItem(MENU_CHANGE_PICTURE).setVisible(false);
        }

        if (isFollowingSet || isMyProfile) {
            if (isMuffled) {
                menu.getItem(MENU_MUFFLE).setVisible(false);
            } else {
                menu.getItem(MENU_UNMUFFLE).setVisible(false);
            }
            if (isMuted) {
                menu.getItem(MENU_MUTE).setVisible(false);
            } else {
                menu.getItem(MENU_UNMUTE).setVisible(false);
            }
        } else {
            menu.getItem(MENU_MUFFLE).setVisible(false);
            menu.getItem(MENU_UNMUFFLE).setVisible(false);
            menu.getItem(MENU_MUTE).setVisible(false);
            menu.getItem(MENU_UNMUTE).setVisible(false);
        }

        return true;
    }

    @Override
    public void finish() {
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        // this is used in the onStart() for the home fragment to tell whether or not it should refresh
        // tweetmarker. Since coming out of this will only call onResume(), it isn't needed.
        //sharedPrefs.edit().putBoolean("from_activity", true).commit();

        super.finish();
        overridePendingTransition(0, R.anim.activity_slide_down);

        try {
            if (isMyProfile) {
                AppSettings.invalidate();
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onBackPressed() {
        if (tweetsPopup != null && tweetsPopup.isShowing()) {
            tweetsPopup.hide();
        } else if (usersListsPopup != null && usersListsPopup.isShowing()) {
            usersListsPopup.hide();
        } else if (picsPopup != null && picsPopup.isShowing()) {
            picsPopup.hide();
        } else if (fol != null && fol.isShowing()) {
            fol.hide();
        } else if (fri != null && fri.isShowing()) {
            fri.hide();
        } else if (timelinePopup != null && timelinePopup.isShowing()) {
            timelinePopup.hide();
        } else {
            super.onBackPressed();
        }
    }

    private final int SELECT_PRO_PIC = 57;
    private final int SELECT_BANNER = 58;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_favorite:
                new FavoriteUser().execute();
                return true;

            case R.id.menu_block:
                new BlockUser().execute();
                sharedPrefs.edit().putBoolean("just_muted", true).commit();
                return true;

            case R.id.menu_unblock:
                new BlockUser().execute();
                return true;

            case R.id.menu_add_to_list:
                new GetLists().execute();
                return true;

            case R.id.menu_dm:
/*                Intent dm = new Intent(context, ComposeDMActivity.class);
                dm.putExtra("screenname", accountId);
                startActivity(dm);*/
                return true;

            case R.id.menu_change_picture:
                Intent photoPickerIntent = new Intent();
                photoPickerIntent.setType("image/*");
                photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
                try {
                    startActivityForResult(Intent.createChooser(photoPickerIntent,
                            "Select Picture"), SELECT_PRO_PIC);
                } catch (Throwable t) {
                    // no app to preform this..? hmm, tell them that I guess
                    TopSnackbarUtil.showSnack(context, "No app available to select pictures!", Toast.LENGTH_SHORT);
                }
                return true;

            case R.id.menu_change_banner:
                Intent bannerPickerIntent = new Intent();
                bannerPickerIntent.setType("image/*");
                bannerPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
                try {
                    startActivityForResult(Intent.createChooser(bannerPickerIntent,
                            "Select Picture"), SELECT_BANNER);
                } catch (Throwable t) {
                    // no app to preform this..? hmm, tell them that I guess
                    TopSnackbarUtil.showSnack(context, "No app available to select pictures!", Toast.LENGTH_SHORT);
                }
                return true;

            case R.id.menu_change_bio:
                updateProfile();
                return true;

            case R.id.menu_mute:
                new SetAccountMuted(accountId, true).setCallback(new Callback<>() {
                    @Override
                    public void onSuccess(Relationship result) {
                        UiUtils.performMuteAction(ProfilePager.this, sharedPrefs, accountId, false);
                    }

                    @Override
                    public void onError(ErrorResponse error) {

                    }
                }).exec();

                return true;

            case R.id.menu_unmute:
                new SetAccountMuted(accountId, false).setCallback(new Callback<>() {
                    @Override
                    public void onSuccess(Relationship result) {
                        UiUtils.performUnMuteAction(ProfilePager.this, sharedPrefs, accountId);
                    }

                    @Override
                    public void onError(ErrorResponse error) {

                    }
                }).exec();

                return true;

            case R.id.menu_muffle_user:
                HashMap list = UiUtils.getMuffledUsers(sharedPrefs);
                list.put(accountId, thisUser.getName());
                sharedPrefs.edit().putString(AppSettings.MUFFLED_USERS_ID, JsonHelper.toJSONString(list)).commit();
                sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, true).commit();
                sharedPrefs.edit().putBoolean("just_muted", true).commit();
                finish();
                return true;

            case R.id.menu_unmuffle_user:
                HashMap list2 = UiUtils.getMuffledUsers(sharedPrefs);
                list2.remove(accountId);
                sharedPrefs.edit().putString(AppSettings.MUFFLED_USERS_ID, JsonHelper.toJSONString(list2)).commit();
                sharedPrefs.edit().putBoolean(AppSettings.REFRESH_ME, true).commit();
                sharedPrefs.edit().putBoolean("just_muted", true).commit();
                finish();
                return true;

            case R.id.menu_share_link:
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, thisUser.getURL());

                startActivity(Intent.createChooser(share, "Share with:"));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }



    public void updateProfile() {

        ChangeProfileInfoDialogBinding changeProfileInfoDialogBinding = ChangeProfileInfoDialogBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AccentMaterialDialog(context, R.style.MaterialAlertDialogTheme)
                .setView(changeProfileInfoDialogBinding.getRoot())
                .setTitle(getResources().getString(R.string.change_profile_info))
                .create();


        try {
            changeProfileInfoDialogBinding.name.setText(thisUser.getName());
            changeProfileInfoDialogBinding.location.setText(thisUser.getLocation());
            changeProfileInfoDialogBinding.description.setText(Html.fromHtml(thisUser.getDescription()));
        } catch (Exception e) {

        }

        changeProfileInfoDialogBinding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        changeProfileInfoDialogBinding.change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean ok = true;
                String nameS = null;
                String urlS = null;
                String locationS = null;
                String descriptionS = null;

                if (changeProfileInfoDialogBinding.name.getText().length() <= 20 && ok) {
                    if (changeProfileInfoDialogBinding.name.getText().length() > 0) {
                        nameS = changeProfileInfoDialogBinding.name.getText().toString();
                        sharedPrefs.edit().putString("twitter_users_name_" + sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1), nameS).commit();
                    }
                } else {
                    ok = false;
                    TopSnackbarUtil.showSnack(context, getResources().getString(R.string.name_char_length), Toast.LENGTH_SHORT);
                }

                if (changeProfileInfoDialogBinding.location.getText().length() <= 30 && ok) {
                    if (changeProfileInfoDialogBinding.location.getText().length() > 0) {
                        locationS = changeProfileInfoDialogBinding.location.getText().toString();
                    }
                } else {
                    ok = false;
                    TopSnackbarUtil.showSnack(context, getResources().getString(R.string.location_char_length), Toast.LENGTH_SHORT);
                }

                if (changeProfileInfoDialogBinding.description.getText().length() <= 160 && ok) {
                    if (changeProfileInfoDialogBinding.description.getText().length() > 0) {
                        descriptionS = changeProfileInfoDialogBinding.description.getText().toString();
                    }
                } else {
                    ok = false;
                    TopSnackbarUtil.showSnack(context, getResources().getString(R.string.description_char_length), Toast.LENGTH_SHORT);
                }

                if (ok) {
                    new UpdateInfo(nameS, urlS, locationS, descriptionS).execute();
                    dialog.dismiss();
                }
            }
        });

        dialog.show();
    }

    class UpdateInfo extends AsyncTask<String, Void, Boolean> {

        String name;
        String url;
        String location;
        String description;

        public UpdateInfo(String name, String url, String location, String description) {
            this.name = name;
            this.url = url;
            this.location = location;
            this.description = description;
        }

        protected Boolean doInBackground(String... urls) {
            try {

                new UpdateAccountCredentials(this.name, this.description, (Uri) null, null, null).execSync();

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean added) {
            if (added) {
                TopSnackbarUtil.showSnack(context, getResources().getString(R.string.updated_profile), Toast.LENGTH_SHORT);
            } else {
                TopSnackbarUtil.showSnack(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
            }
        }
    }

    private boolean bannerUpdate = false;

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case UCrop.REQUEST_CROP:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri selectedImage = UCrop.getOutput(imageReturnedIntent);

                        if (bannerUpdate) {
                            new UpdateBanner(selectedImage).execute();
                        } else {
                            new UpdateProPic(selectedImage).execute();
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        TopSnackbarUtil.showSnack(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
                    }
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    final Throwable cropError = UCrop.getError(imageReturnedIntent);
                    cropError.printStackTrace();
                }
                break;
            case SELECT_PRO_PIC:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    bannerUpdate = false;
                    startUcrop(selectedImage);
                }
                break;
            case SELECT_BANNER:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    bannerUpdate = true;
                    startUcrop(selectedImage);
                }
        }
    }

    private Bitmap getBitmapToSend(Uri uri) throws FileNotFoundException, IOException {
        InputStream input = getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither = true;//optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
            return null;

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > 500) ? (originalSize / 500) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither = true;//optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        input = this.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);

        ExifInterface exif = new ExifInterface(IOUtils.getPath(uri, context));
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        input.close();

        return bitmap;
    }

    private static int getPowerOfTwoForSampleRatio(double ratio) {
        int k = Integer.highestOneBit((int) Math.floor(ratio));
        if (k == 0) return 1;
        else return k;
    }

    class UpdateBanner extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        private Uri image = null;

        public UpdateBanner(Uri image) {
            this.image = image;
        }


        protected void onPreExecute() {
            pDialog = AccentProgressDialog.show(context, getResources().getString(R.string.updating_banner_pic) + "...", "", true);

        }

        protected Boolean doInBackground(String... urls) {
            try {

                //create a file to write bitmap data
                File outputDir = context.getCacheDir(); // context being the Activity pointer
                File f = File.createTempFile("compose", ".jpg", outputDir);

                Bitmap bitmap = getBitmapToSend(image);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                byte[] bitmapdata = bos.toByteArray();

                FileOutputStream fos = new FileOutputStream(f);
                fos.write(bitmapdata);
                fos.flush();
                fos.close();

                thisUser = new UserJSONImplMastodon(new UpdateAccountCredentials(null, null, null, f, null).execSync());

                String profileURL = thisUser.getProfileBannerURL();
                sharedPrefs.edit().putString("twitter_background_url_" + sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1), profileURL).commit();

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean uploaded) {

            try {
                pDialog.dismiss();
            } catch (Exception e) {

            }

            if (uploaded) {
                setBannerImage();
                TopSnackbarUtil.showSnack(context, getResources().getString(R.string.uploaded), Toast.LENGTH_SHORT);
            } else {
                TopSnackbarUtil.showSnack(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
            }
        }
    }

    private void startUcrop(Uri sourceUri) {
        try {
            UCrop.Options options = new UCrop.Options();

            options.setToolbarColor(getResources().getColor(R.color.black));
            options.setStatusBarColor(getResources().getColor(R.color.black));
            options.setToolbarWidgetColor(getResources().getColor(R.color.white));

            options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
            options.setCompressionQuality(90);
            options.setFreeStyleCropEnabled(true);

            File destination = File.createTempFile("ucrop", ".jpg", getCacheDir());
            UCrop.of(sourceUri, Uri.fromFile(destination))
                    .withOptions(options)
                    .start(ProfilePager.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class UpdateProPic extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        private Uri image;

        public UpdateProPic(Uri image) {
            this.image = image;
        }

        protected void onPreExecute() {

            pDialog = AccentProgressDialog.show(context, getResources().getString(R.string.updating_pro_pic) + "...", "", true);

        }

        protected Boolean doInBackground(String... urls) {
            try {

                //create a file to write bitmap data
                File outputDir = context.getCacheDir(); // context being the Activity pointer
                File f = File.createTempFile("compose", ".jpg", outputDir);

                Bitmap bitmap = getBitmapToSend(image);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                byte[] bitmapdata = bos.toByteArray();

                FileOutputStream fos = new FileOutputStream(f);
                fos.write(bitmapdata);
                fos.flush();
                fos.close();


                thisUser = new UserJSONImplMastodon(new UpdateAccountCredentials(null, null, f, null, null).execSync());


                proPic = thisUser.getOriginalProfileImageURL();
                sharedPrefs.edit().putString("profile_pic_url_" + sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1), proPic).commit();

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean uploaded) {

            try {
                pDialog.dismiss();
            } catch (Exception e) {

            }

            if (uploaded) {
                loadProfilePicture();
                TopSnackbarUtil.showSnack(context, getResources().getString(R.string.uploaded), Toast.LENGTH_SHORT);
            } else {
                TopSnackbarUtil.showSnack(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
            }
        }
    }

    class GetLists extends AsyncTask<String, Void, List<MastoList>> {

        ProgressDialog pDialog;

        protected void onPreExecute() {

            pDialog = AccentProgressDialog.show(context, getResources().getString(R.string.finding_lists), null, true, false);

        }

        protected List<MastoList> doInBackground(String... urls) {
            try {

                List<MastoList> lists = new allen.town.focus.twitter.api.requests.list.GetLists().execSync();

                return lists;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(final List<MastoList> lists) {

            if (lists != null) {
                Collections.sort(lists, new Comparator<MastoList>() {
                    public int compare(MastoList result1, MastoList result2) {
                        return result1.getTitle().compareTo(result2.getTitle());
                    }
                });

                ArrayList<String> names = new ArrayList<String>();
                for (MastoList l : lists) {
                    names.add(l.getTitle());
                }

                try {
                    pDialog.dismiss();

                    AlertDialog.Builder builder = new AccentMaterialDialog(
                            context,
                            R.style.MaterialAlertDialogTheme
                    );
                    builder.setItems(names.toArray(new CharSequence[lists.size()]), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (thisUser != null) {
                                new CheckList(lists.get(i), thisUser).execute();
                            }
                        }
                    });
                    builder.setTitle(getResources().getString(R.string.choose_list) + ":");
                    builder.create();
                    builder.show();

                } catch (Exception e) {
                    // closed the window
                }


            } else {
                try {
                    pDialog.dismiss();
                } catch (Exception e) {

                }
                TopSnackbarUtil.showSnack(context, context.getResources().getString(R.string.error), Toast.LENGTH_SHORT);
            }
        }
    }

    class CheckList extends AsyncTask<String, Void, List<MastoList>> {

        MastoList list;
        User user;

        public CheckList(MastoList listId, User userId) {
            this.list = listId;
            this.user = userId;
        }

        protected List<MastoList> doInBackground(String... urls) {
            try {
                return new GetAccountInLists(user.getId() + "").execSync();
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(List<MastoList> userLists) {
            if (userLists != null && userLists.contains(list)) {
                new AccentMaterialDialog(
                        context,
                        R.style.MaterialAlertDialogTheme
                )
                        .setMessage(R.string.remove_from_list)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new RemoveFromList(Long.parseLong(list.getId()), user.getId()).execute();
                            }
                        }).show();
                return;
            }
            new AddToList(Long.parseLong(list.getId()), user.getId()).execute();
        }
    }

    class RemoveFromList extends AsyncTask<String, Void, Boolean> {
        long listId;
        long userId;

        public RemoveFromList(long listId, long userId) {
            this.listId = listId;
            this.userId = userId;
        }

        protected Boolean doInBackground(String... urls) {
            try {
                new DeleteListAccount(listId + "", new ArrayList<>() {
                    {
                        add(userId + "");
                    }
                }).execSync();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean added) {
            TopSnackbarUtil.showSnack(context, getResources().getString(added ? R.string.removed_from_list : R.string.error), Toast.LENGTH_SHORT);
        }
    }

    class AddToList extends AsyncTask<String, Void, Boolean> {

        long listId;
        long userId;

        public AddToList(long listId, long userId) {
            this.listId = listId;
            this.userId = userId;
        }

        protected Boolean doInBackground(String... urls) {
            try {
                new AddAccountToList(listId + "", new ArrayList<>() {
                    {
                        add(userId + "");
                    }
                }).execSync();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean added) {
            if (added) {
                TopSnackbarUtil.showSnack(context, getResources().getString(R.string.added_to_list), Toast.LENGTH_SHORT);
            } else {
                TopSnackbarUtil.showSnack(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
            }
        }
    }
}
