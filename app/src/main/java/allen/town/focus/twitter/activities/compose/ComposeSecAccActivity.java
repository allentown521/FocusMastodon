package allen.town.focus.twitter.activities.compose;

import android.content.Intent;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import allen.town.focus.twitter.R;
import allen.town.focus.twitter.views.widgets.text.FontPrefTextView;
import twitter4j.Status;

public class ComposeSecAccActivity extends ComposeActivity {

    public void setUpReplyText() {

        useAccOne = false;
        useAccTwo = true;

        ImageView pic = (ImageView) findViewById(R.id.profile_pic);
        FontPrefTextView currentName = (FontPrefTextView) findViewById(R.id.current_name);
        Glide.with(this).load(settings.secondProfilePicUrl).into(pic);
        currentName.setText("@" + settings.secondScreenName);

        // for failed notification
        if (!sharedPrefs.getString("draft", "").equals("")) {
            reply.setText(sharedPrefs.getString("draft", ""));
            reply.setSelection(reply.getText().length());
        }

        String to = getIntent().getStringExtra("user") + (isDM ? "" : " ");

        if ((!to.equals("null ") && !isDM) || (isDM && !to.equals("null"))) {
            if(!isDM) {
                Log.v("username_for_noti", "to place: " + to);
                reply.setText(to);
                reply.setSelection(reply.getText().toString().length());
            } else {
                contactEntry.setText(to);
                reply.requestFocus();
            }

            sharedPrefs.edit().putString("draft", "").commit();
        }

        notiId = getIntent().getLongExtra("id", 0);
        replyText = getIntent().getStringExtra("reply_to_text");
        replyStatus = (Status) getIntent().getSerializableExtra("reply_to_status");

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            sharingSomething = true;
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }
        }
    }
}