package allen.town.focus.twitter.activities.compose;
/*
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.Vibrator;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

import com.bumptech.glide.Glide;
import com.github.ajalt.reprint.core.Reprint;
import com.yalantis.ucrop.UCrop;

import net.ypresto.androidtranscoder.MediaTranscoder;
import net.ypresto.androidtranscoder.format.AndroidStandardFormatStrategy;
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.activities.GiphySearch;
import allen.town.focus.twitter.activities.MainActivity;
import allen.town.focus.twitter.activities.WhiteToolbarActivity;
import allen.town.focus.twitter.data.sq_lite.QueuedDataSource;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.utils.FingerprintDialog;
import allen.town.focus.twitter.utils.HtmlParser;
import allen.town.focus.twitter.utils.IOUtils;
import allen.town.focus.twitter.utils.ImageUtils;
import allen.town.focus.twitter.utils.NotificationChannelUtil;
import allen.town.focus.twitter.utils.NotificationUtils;
import allen.town.focus.twitter.utils.Utils;
import allen.town.focus.twitter.utils.text.TextUtils;
import allen.town.focus.twitter.views.widgets.EmojiKeyboard;
import allen.town.focus.twitter.views.widgets.ImageKeyboardEditText;
import allen.town.focus.twitter.views.widgets.text.FontPrefTextView;
import allen.town.focus_common.util.Timber;
import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.focus_common.views.AccentMaterialDialog;
import allen.town.focus_common.views.AccentProgressDialog;
import twitter4j.Status;
import uk.co.senab.photoview.PhotoViewAttacher;

public abstract class Compose extends WhiteToolbarActivity implements
        InputConnectionCompat.OnCommitContentListener {

    private static final boolean DEBUG = false;

    public AppSettings settings;
    public Context context;
    public SharedPreferences sharedPrefs;

    public EditText contactEntry;
    public ImageKeyboardEditText reply;
    public ImageView[] attachImage = new ImageView[4];
    public ImageButton[] cancelButton = new ImageButton[4];
    public FrameLayout[] holders = new FrameLayout[4];
    public ImageButton gifButton;
    public ImageButton attachButton;
    public ImageButton pollButton;
    public ImageButton captureButton;
    public ImageButton emojiButton;
    public EmojiKeyboard emojiKeyboard;
    public ImageButton overflow;
    public TextView charRemaining;
    public ListPopupWindow hashtagAutoComplete;
    public FontPrefTextView numberAttached;

    protected boolean useAccOne = true;
    protected boolean useAccTwo = false;

    protected boolean sharingSomething = false;
    protected String attachmentUrl = null; // quoted tweet

    // attach up to four images
    public String[] attachedUri = new String[]{"", "", "", ""};
    public int imagesAttached = 0;

    public PhotoViewAttacher mAttacher;

    public boolean isDM = false;

    public String to = null;
    public long notiId = 0;
    public String replyText = "";
    public String quotingAStatus = null;

    protected boolean attachButtonEnabled = true;

    public int currentAccount;

    final Pattern p = Patterns.WEB_URL;
    public Status replyStatus = null;

    private int getCountFromString(String text) {
        if (AppSettings.isLimitedTweetCharLanguage()) {
            return text.getBytes().length;
        } else {
            return text.length();
        }
    }

    public Handler countHandler;
    public Runnable getCount = new Runnable() {
        @Override
        public void run() {
            String text = reply.getText().toString();

            if (shouldReplaceTo(text)) {
                String replaceable = to.replaceAll("#[a-zA-Z]+ ", "");

                if (!replaceable.equals(" ")) {
                    try {
                        text = text.replaceAll(replaceable, "");
                    } catch (Exception e) {
                    }
                }
            }

            if (!Patterns.WEB_URL.matcher(text).find() && quotingAStatus == null) { // no links, normal tweet
                try {
                    charRemaining.setText(AppSettings.getInstance(context).tweetCharacterCount - getCountFromString(text) + "");
                } catch (Exception e) {
                    charRemaining.setText("0");
                }
            } else {
                int count = getCountFromString(text);
                Matcher m = p.matcher(text);
                while (m.find()) {
                    String url = m.group();
                    count -= url.length(); // take out the length of the url
                    count += 23; // add 23 for the shortened url
                }

                if (quotingAStatus != null) {
                    count += 24;
                }

                charRemaining.setText(AppSettings.getInstance(context).tweetCharacterCount - count + "");
            }

            changeTextColor();
        }

        private int originalTextColor = -1;

        private void changeTextColor() {
            if (originalTextColor == -1) {
                originalTextColor = charRemaining.getCurrentTextColor();
            }

            try {
                if (Integer.parseInt(charRemaining.getText().toString()) <= 10) {
                    charRemaining.setTextColor(getResources().getColor(R.color.red_primary_color_light));
                } else {
                    charRemaining.setTextColor(originalTextColor);
                }
            } catch (Exception e) {
                charRemaining.setTextColor(originalTextColor);
            }
        }
    };


    @Override
    public void finish() {
        super.finish();

        overridePendingTransition(0, R.anim.fade_out);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Reprint.isHardwarePresent() && Reprint.hasFingerprintRegistered() &&
                AppSettings.getInstance(this).fingerprintLock) {
            new FingerprintDialog(this).show();
        }

        Utils.setTaskDescription(this);

        if (!getIntent().getBooleanExtra("already_animated", false)) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }

        countHandler = new Handler();

        settings = AppSettings.getInstance(this);
        context = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);


        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

//        int currentOrientation = getResources().getConfiguration().orientation;
//        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
//        } else {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
//        }

        currentAccount = sharedPrefs.getInt(AppSettings.CURRENT_ACCOUNT, 1);


        Utils.setUpTweetTheme(context, settings);
        setUpWindow();
        setUpLayout();
        setUpActionBar();
        setUpReplyText();

        reply.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.isCtrlPressed()) {

                    findViewById(R.id.send_button).performClick();
                    return true;
                } else {
                    return false;
                }
            }
        });

        if (reply.getText().toString().contains(" RT @")) {
            reply.setSelection(0);
        }

        if (notiId != 0) {
            FontPrefTextView replyTo = findViewById(R.id.reply_to);
            if (quotingAStatus != null) {
                replyTo.setText("<b>" + getString(R.string.quoting) + "</b><br/><br/>" + replyText);
            } else {
                replyTo.setText(replyText);
            }
            HtmlParser.linkifyText(replyTo, replyStatus.getEmoji(), replyStatus.getUserMentionEntitiesList(), false);

            View replyToCard = findViewById(R.id.reply_to_card);

            if (replyToCard != null) {
                replyToCard.setVisibility(View.VISIBLE);
            }

            replyTo.setTextSize(settings.textSize);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String text = reply.getText().toString();

                try {
                    if (!android.text.TextUtils.isEmpty(text) && !(text.startsWith(" RT @") || quotingAStatus != null)) {
                        text = text.replaceAll("  ", " ");
                        reply.setText(text);
                        reply.setSelection(text.length());

                        if (!text.isEmpty() && !text.endsWith(" ")) {
                            reply.append(" ");
                        }

                        if (text.trim().isEmpty()) {
                            reply.setText("");
                        }
                    }
                } catch (Exception e) {

                }

                replyText = reply.getText().toString();
            }
        }, 250);

        if (contactEntry != null) {
            contactEntry.setTextSize(settings.textSize);
        }

        if (reply != null) {
            reply.setTextSize(settings.textSize);
        }

        // change the background color for the cursor
        if (settings.darkTheme && (settings.theme == AppSettings.THEME_BLACK || settings.theme == AppSettings.THEME_DARK_BACKGROUND_COLOR)) {
            if (Utils.isAndroidP()) {
                return;
            }

            try {
                // https://github.com/android/platform_frameworks_base/blob/kitkat-release/core/java/android/widget/TextView.java#L562-564
                Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
                f.setAccessible(true);
                f.set(reply, R.drawable.black_cursor);
            } catch (Exception ignored) {
            }

            try {
                // https://github.com/android/platform_frameworks_base/blob/kitkat-release/core/java/android/widget/TextView.java#L562-564
                Field f = TextView.class.getDeclaredField("mHighlightColor");
                f.setAccessible(true);
                f.set(reply, context.getResources().getColor(R.color.pressed_white));
            } catch (Exception ignored) {
            }
        }
    }

    public void setUpWindow() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        params.dimAmount = .6f;  // set it higher if you want to dim behind the window
        getWindow().setAttributes(params);
        getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        // Gets the display size so that you can set the window to a percent of that
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        // You could also easily used an integer value from the shared preferences to set the percent
        if (height > width) {
            getWindow().setLayout((int) (width * .95), (int) (height * .95));
        } else {
            getWindow().setLayout((int) (width * .7), (int) (height * .8));
        }
    }

    public void setUpActionBar() {
        findViewById(R.id.send_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean close = doneClick();
                        if (close) {
                            onBackPressed();
                        }
                    }
                });
        View discard = findViewById(R.id.discard_button);
        discard.setVisibility(View.GONE);
        discard.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        discardClicked = true;
                        sharedPrefs.edit().putString("draft", "").commit();
                        if (emojiKeyboard.isShowing()) {
                            onBackPressed();
                        }
                        onBackPressed();
                    }
                });

    }

    public void setUpSimilar() {
        attachImage[0] = (ImageView) findViewById(R.id.picture1);
        attachImage[1] = (ImageView) findViewById(R.id.picture2);
        attachImage[2] = (ImageView) findViewById(R.id.picture3);
        attachImage[3] = (ImageView) findViewById(R.id.picture4);

        cancelButton[0] = (ImageButton) findViewById(R.id.cancel1);
        cancelButton[1] = (ImageButton) findViewById(R.id.cancel2);
        cancelButton[2] = (ImageButton) findViewById(R.id.cancel3);
        cancelButton[3] = (ImageButton) findViewById(R.id.cancel4);

        holders[0] = (FrameLayout) findViewById(R.id.holder1);
        holders[1] = (FrameLayout) findViewById(R.id.holder2);
        holders[2] = (FrameLayout) findViewById(R.id.holder3);
        holders[3] = (FrameLayout) findViewById(R.id.holder4);

        attachButton = (ImageButton) findViewById(R.id.attach);
        pollButton = (ImageButton) findViewById(R.id.poll);
        captureButton = (ImageButton) findViewById(R.id.capture);
        gifButton = (ImageButton) findViewById(R.id.gif);
        emojiButton = (ImageButton) findViewById(R.id.emoji);
        emojiKeyboard = (EmojiKeyboard) findViewById(R.id.emojiKeyboard);
        reply = (ImageKeyboardEditText) findViewById(R.id.tweet_content);
        charRemaining = (TextView) findViewById(R.id.char_remaining);

        reply.setCommitContentListener(this);

        gifButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attachGif();
            }
        });

        for (int i = 0; i < cancelButton.length; i++) {
            final int pos = i;
            cancelButton[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    imagesAttached--;

                    List<String> uris = new ArrayList<String>();
                    for (String uri : attachedUri) {
                        uris.add(uri);

                    }
                    uris.remove(pos);

                    for (int i = 0; i < attachImage.length; i++) {
                        attachImage[i].setImageDrawable(null);
                        attachedUri[i] = null;
                        holders[i].setVisibility(View.GONE);
                    }
                    for (int i = 0; i < imagesAttached; i++) {
                        attachImage[i].setImageURI(Uri.parse(uris.get(i)));
                        attachedUri[i] = uris.get(i);
                        holders[i].setVisibility(View.VISIBLE);
                    }

                    attachButton.setEnabled(true);
                    attachButton.setEnabled(true);
                    pollButton.setEnabled(true);
                    attachButtonEnabled = true;
                }
            });
        }

        findViewById(R.id.prompt_pos).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Timber.v("Focus_for_Mastodon_input", "clicked the view");
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(reply, InputMethodManager.SHOW_FORCED);
            }
        });

        ImageView pic = (ImageView) findViewById(R.id.profile_pic);
        FontPrefTextView currentName = (FontPrefTextView) findViewById(R.id.current_name);

        if (!(this instanceof ComposeSecAccActivity))
            Glide.with(this).load(settings.myProfilePicUrl).into(pic);

        currentName.setText("@" + settings.myScreenName);

        //numberAttached.setText("0 " + getString(R.string.attached_images));

        charRemaining.setText(AppSettings.getInstance(this).tweetCharacterCount - reply.getText().length() + "");

        reply.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                countHandler.removeCallbacks(getCount);
                countHandler.postDelayed(getCount, 300);
            }
        });
    }

    public void findGif() {
        Intent gif = new Intent(context, GiphySearch.class);
        startActivityForResult(gif, FIND_GIF);
    }

    public void attachGif() {
        AlertDialog.Builder builder = new AccentMaterialDialog(
                context,
                R.style.MaterialAlertDialogTheme
        );
        builder.setItems(R.array.attach_gif_options, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) { // gif from gallery
                    try {
                        Intent gifIntent = new Intent();
                        gifIntent.setType("image/gif");
                        gifIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                        gifIntent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(gifIntent, SELECT_GIF);
                    } catch (Exception e) {
                        Intent gifIntent = new Intent();
                        gifIntent.setType("image/gif");
                        gifIntent.setAction(Intent.ACTION_PICK);
                        startActivityForResult(gifIntent, SELECT_GIF);
                    }
                } else if (item == 1) { // Giphy
                    findGif();
                }
            }
        });

        builder.create().show();
    }

    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (sharedText != null) {
            if (!isDM) {
                if (subject != null && !subject.equals(sharedText) && !sharedText.contains(subject)) {
                    reply.setText(subject + " - " + sharedText);
                } else {
                    reply.setText(sharedText);
                }
                reply.setSelection(reply.getText().toString().length());
            } else {
                contactEntry.setText(sharedText);
                reply.requestFocus();
            }
        }
    }

    private Bitmap getThumbnail(Uri uri) throws IOException {
        InputStream input = getContentResolver().openInputStream(uri);
        int reqWidth = 150;
        int reqHeight = 150;

        byte[] byteArr = new byte[0];
        byte[] buffer = new byte[1024];
        int len;
        int count = 0;

        try {
            while ((len = input.read(buffer)) > -1) {
                if (len != 0) {
                    if (count + len > byteArr.length) {
                        byte[] newbuf = new byte[(count + len) * 2];
                        System.arraycopy(byteArr, 0, newbuf, 0, count);
                        byteArr = newbuf;
                    }

                    System.arraycopy(buffer, 0, byteArr, count, len);
                    count += len;
                }
            }

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth,
                    reqHeight);
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap b = BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            if (!isAndroidN()) {
                ExifInterface exif = new ExifInterface(IOUtils.getPath(uri, context));
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

                input.close();

                b = ImageUtils.cropSquare(b);
                return rotateBitmap(b, orientation);
            } else {
                input.close();
                b = ImageUtils.cropSquare(b);
                return b;
            }

        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Timber.v("Focus_for_Mastodon_composing_image", "rotation: " + orientation);

        try {
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    return bitmap;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.setScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setRotate(180);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(270);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(270);
                    break;
                default:
                    return bitmap;
            }
            try {
                Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                return bmRotated;
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    void handleSendImage(Intent intent) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            //String filePath = IOUtils.getPath(imageUri, context);
            try {
                attachImage[imagesAttached].setImageURI(imageUri);
                attachedUri[imagesAttached] = imageUri.toString();
                holders[imagesAttached].setVisibility(View.VISIBLE);
                imagesAttached++;
                //numberAttached.setText(imagesAttached + " " + getResources().getString(R.string.attached_images));
                //numberAttached.setVisibility(View.VISIBLE);
            } catch (Throwable e) {
                TopSnackbarUtil.showSnack(Compose.this, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
                //numberAttached.setText("");
                //numberAttached.setVisibility(View.GONE);
            }
        }
    }


    public void displayErrorNotification(final Exception e) {

        if (!DEBUG) {
            return;
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, NotificationChannelUtil.FAILED_TWEETS_CHANNEL)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setContentTitle(getResources().getString(R.string.tweet_failed))
                        .setContentText(e.getMessage());

        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);
        mNotificationManager.cancelAll();
        mNotificationManager.notify(221, mBuilder.build());
    }

    public void makeFailedNotification(String text) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, NotificationChannelUtil.FAILED_TWEETS_CHANNEL)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setContentTitle(getResources().getString(R.string.tweet_failed))
                        .setContentText(notiId != 0 ? getResources().getString(R.string.original_probably_deleted) : getResources().getString(R.string.tap_to_retry));

        Intent resultIntent = new Intent(this, RetryCompose.class);
        QueuedDataSource.getInstance(this).createDraft(text, settings.currentAccount);
        resultIntent.setAction(Intent.ACTION_SEND);
        resultIntent.putExtra("failed_notification_text", text);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        NotificationUtils.generateRandomId(),
                        resultIntent,
                        Utils.withImmutability(PendingIntent.FLAG_UPDATE_CURRENT)
                );

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
        mNotificationManager.notify(5, mBuilder.build());
    }

    public void makeTweetingNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, NotificationChannelUtil.TWEETING_NOTIFICATION_CHANNEL)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setOngoing(true)
                        .setProgress(100, 0, true);

        mBuilder.setContentTitle(getResources().getString(R.string.sending_tweet));

        Intent resultIntent = new Intent(this, MainActivity.class);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        Utils.withImmutability(PendingIntent.FLAG_UPDATE_CURRENT)
                );

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(6, mBuilder.build());
    }

    public void finishedTweetingNotification() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context, NotificationChannelUtil.TWEETING_NOTIFICATION_CHANNEL)
                                    .setSmallIcon(R.drawable.ic_stat_icon)
                                    .setContentTitle(getResources().getString(R.string.tweet_success))
                                    .setOngoing(false)
                                    .setTicker(getResources().getString(R.string.tweet_success));

                    if (settings.vibrate) {
                        Timber.v("Focus_for_Mastodon_vibrate", "vibrate on compose");
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        long[] pattern = {0, 50, 500};
                        v.vibrate(pattern, -1);
                    }

                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(6, mBuilder.build());
                    // cancel it immediately, the ticker will just go off
                    mNotificationManager.cancel(6);
                } catch (Exception e) {
                    // not attached?
                }
            }
        }, 500);

    }


    @Override
    public void onStop() {
        super.onStop();
    }

    private void startUcrop(Uri sourceUri) {
        try {
            UCrop.Options options = new UCrop.Options();

            options.setToolbarColor(getResources().getColor(R.color.black));
            options.setStatusBarColor(getResources().getColor(R.color.black));
            options.setToolbarWidgetColor(getResources().getColor(R.color.white));

//            options.setActiveWidgetColor(ThemeStore.accentColor(context));
            options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
            options.setCompressionQuality(100);
            options.setFreeStyleCropEnabled(true);

            File destination = File.createTempFile("ucrop", ".jpg", getCacheDir());
            UCrop.of(sourceUri, Uri.fromFile(destination))
                    .withOptions(options)
                    .start(Compose.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final int SELECT_PHOTO = 100;
    public static final int CAPTURE_IMAGE = 101;
    public static final int SELECT_GIF = 102;
    public static final int SELECT_VIDEO = 103;
    public static final int FIND_GIF = 104;
    public static final int CAPTURE_VIDEO = 105;

    public String attachmentType = "";

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        Timber.v("Focus_for_Mastodon_image_attach", "got the result, code: " + requestCode);
        switch (requestCode) {
            case UCrop.REQUEST_CROP:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri selectedImage = UCrop.getOutput(imageReturnedIntent);

                        try {
                            Glide.with(this).load(selectedImage).into(attachImage[imagesAttached]);

                            holders[imagesAttached].setVisibility(View.VISIBLE);
                            attachedUri[imagesAttached] = selectedImage.toString();
                            imagesAttached++;
                        } catch (Throwable e) {
                            TopSnackbarUtil.showSnack(Compose.this, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
                        }
                        pollButton.setEnabled(false);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        TopSnackbarUtil.showSnack(Compose.this, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
                    }
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    final Throwable cropError = UCrop.getError(imageReturnedIntent);
                    cropError.printStackTrace();
                }
                countHandler.post(getCount);
                break;
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    startUcrop(imageReturnedIntent.getData());
                }

                break;
            case CAPTURE_IMAGE:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Focus_for_Mastodon/", "photoToTweet.jpg"));
                    startUcrop(selectedImage);
                }

                break;
            case FIND_GIF:
            case SELECT_GIF:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri selectedImage = imageReturnedIntent.getData();

                        attachImage[0].setImageBitmap(getThumbnail(selectedImage));
                        holders[0].setVisibility(View.VISIBLE);
                        attachedUri[0] = selectedImage.toString();
                        imagesAttached = 1;

                        attachmentType = "animated_gif";

                        attachButton.setEnabled(false);
                        pollButton.setEnabled(false);
                        attachButtonEnabled = false;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        TopSnackbarUtil.showSnack(Compose.this, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
                    }
                }
                countHandler.post(getCount);
                break;
            case CAPTURE_VIDEO:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();

                    Timber.v("path to surfaceView on sd card: " + selectedImage);

                    Glide.with(this)
                            .load(selectedImage)
                            .into(attachImage[0]);

                    holders[0].setVisibility(View.VISIBLE);
                    attachedUri[0] = selectedImage.toString();
                    imagesAttached = 1;

                    attachmentType = "video/mp4";
                    attachButton.setEnabled(false);
                    pollButton.setEnabled(false);
                    attachButtonEnabled = false;
                }
                break;
            case SELECT_VIDEO:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri selectedImage = imageReturnedIntent.getData();

                        Timber.v("path to surfaceView on sd card: " + selectedImage);

                        Glide.with(this)
                                .load(selectedImage)
                                .into(attachImage[0]);

                        holders[0].setVisibility(View.VISIBLE);
                        attachedUri[0] = selectedImage.toString();
                        imagesAttached = 1;

                        startVideoEncoding(imageReturnedIntent);

                        attachmentType = "video/mp4";
                        attachButton.setEnabled(false);
                        pollButton.setEnabled(false);
                        attachButtonEnabled = false;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        TopSnackbarUtil.showSnack(Compose.this, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
                    }
                }
                countHandler.post(getCount);
                break;
        }

        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
    }

    @Override
    public void onBackPressed() {
        if (emojiKeyboard.isShowing()) {
            emojiKeyboard.setVisibility(false);

            emojiButton.setImageResource(R.drawable.ic_round_emoji_emotions_24);
            return;
        }

        super.onBackPressed();
    }

    /**
     * 把回复中的@给去掉了
     * @param tweetText
     * @return
     */
    protected boolean shouldReplaceTo(String tweetText) {
        return false;
    }

    public boolean doneClicked = false;
    public boolean discardClicked = false;


    public Bitmap decodeSampledBitmapFromResourceMemOpt(
            InputStream inputStream, int reqWidth, int reqHeight) {

        byte[] byteArr = new byte[0];
        byte[] buffer = new byte[1024];
        int len;
        int count = 0;

        try {
            while ((len = inputStream.read(buffer)) > -1) {
                if (len != 0) {
                    if (count + len > byteArr.length) {
                        byte[] newbuf = new byte[(count + len) * 2];
                        System.arraycopy(byteArr, 0, newbuf, 0, count);
                        byteArr = newbuf;
                    }

                    System.arraycopy(buffer, 0, byteArr, count, len);
                    count += len;
                }
            }

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth,
                    reqHeight);
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            return BitmapFactory.decodeByteArray(byteArr, 0, count, options);

        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options opt, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = opt.outHeight;
        final int width = opt.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public abstract boolean doneClick();

    public abstract void setUpLayout();

    public abstract void setUpReplyText();

    public int toDP(int px) {
        try {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
        } catch (Exception e) {
            return px;
        }
    }

    public static boolean isAndroidN() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.M || Build.VERSION.CODENAME.equals("N");
    }

    public void startVideoEncoding(final Intent data) {
        startVideoEncoding(data, AndroidStandardFormatStrategy.Encoding.HD_720P);
    }

    public void startVideoEncoding(final Intent data, final AndroidStandardFormatStrategy.Encoding encoding) {
        final File file;
        try {
            File outputDir = new File(getExternalFilesDir(null), "outputs");
            outputDir.mkdir();
            file = File.createTempFile("transcode_video", ".mp4", outputDir);
        } catch (IOException e) {
            TopSnackbarUtil.showSnack(this, "Failed to create temporary file.", Toast.LENGTH_LONG);
            return;
        }

        ContentResolver resolver = getContentResolver();
        final ParcelFileDescriptor parcelFileDescriptor;
        try {
            parcelFileDescriptor = resolver.openFileDescriptor(data.getData(), "r");
        } catch (FileNotFoundException e) {
            TopSnackbarUtil.showSnack(this, "File not found.", Toast.LENGTH_LONG);
            return;
        }

        final ProgressDialog progressDialog = AccentProgressDialog.show(context, getString(R.string.preparing_video), "", true);

        final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        MediaTranscoder.Listener listener = new MediaTranscoder.Listener() {
            @Override
            public void onTranscodeCanceled() {
            }

            @Override
            public void onTranscodeFailed(Exception exception) {
                try {
                    progressDialog.cancel();
                } catch (Exception e) {

                }

                attachedUri[0] = data.getData().toString();
            }

            @Override
            public void onTranscodeProgress(double progress) {
            }

            @Override
            public void onTranscodeCompleted() {
                if (file.length() > 15 * 1024 * 1024 && !encoding.equals(AndroidStandardFormatStrategy.Encoding.SD_HIGH)) {
                    startVideoEncoding(data, AndroidStandardFormatStrategy.Encoding.SD_HIGH);
                } else {
                    attachedUri[0] = Uri.fromFile(file).toString();
                }

                try {
                    progressDialog.cancel();
                } catch (Exception e) {

                }
            }
        };

        progressDialog.show();
        MediaTranscoder.getInstance().transcodeVideo(fileDescriptor, file.getAbsolutePath(),
                MediaFormatStrategyPresets.createStandardFormatStrategy(AndroidStandardFormatStrategy.Encoding.HD_720P), listener);

    }

    @Override
    public boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags, Bundle opts) {
        String mime = inputContentInfo.getDescription().getMimeType(0);

        if (mime.equals("image/gif")) {
            try {
                attachImage[0].setImageBitmap(getThumbnail(inputContentInfo.getContentUri()));
                holders[0].setVisibility(View.VISIBLE);
                attachedUri[0] = inputContentInfo.getContentUri().toString();
                imagesAttached = 1;

                attachmentType = "animated_gif";

                attachButton.setEnabled(false);
                pollButton.setEnabled(false);
                attachButtonEnabled = false;
            } catch (Exception e) {

            }
        } else if (mime.contains("image/")) {
            try {
                Glide.with(Compose.this).load(inputContentInfo.getContentUri()).into(attachImage[imagesAttached]);
                holders[imagesAttached].setVisibility(View.VISIBLE);
                attachedUri[imagesAttached] = inputContentInfo.getContentUri().toString();
                imagesAttached++;
            } catch (Throwable e) {
                TopSnackbarUtil.showSnack(Compose.this, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
            }
        } else if (mime.contains("video/mp4")) {
            Glide.with(this)
                    .load(inputContentInfo.getContentUri())
                    .into(attachImage[0]);

            holders[0].setVisibility(View.VISIBLE);
            attachedUri[0] = inputContentInfo.getContentUri().toString();
            imagesAttached = 1;

            attachmentType = "video/mp4";
            attachButton.setEnabled(false);
            pollButton.setEnabled(false);
            attachButtonEnabled = false;
        }

        return true;
    }
}
