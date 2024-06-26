package allen.town.focus.twitter.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import allen.town.focus.twitter.R;
import allen.town.focus.twitter.settings.AppSettings;
import allen.town.focus.twitter.activities.profile_viewer.ProfilePager;

import java.util.ArrayList;

import twitter4j.User;

public class FollowersArrayAdapter extends ArrayAdapter<User> {

    public Context context;

    public boolean openFirst = false;

    public ArrayList<User> users;

    public LayoutInflater inflater;
    public AppSettings settings;

    public int layout;
    public XmlResourceParser addonLayout = null;
    public Resources res;
    public int Focus_for_MastodonLayout;
    public int border;

    public Handler mHandler;

    public static class ViewHolder {
        public TextView name;
        public TextView screenName;
        public TextView following;
        public ImageView picture;
        public LinearLayout background;
        public long userId;
    }

    ArrayList<Long> followingIds;

    public FollowersArrayAdapter(Context context, ArrayList<User> users, ArrayList<Long> followingIds) {
        super(context, R.layout.person);

        this.followingIds = followingIds;

        this.context = context;
        this.users = users;

        settings = AppSettings.getInstance(context);
        inflater = LayoutInflater.from(context);

        setUpLayout();

        mHandler = new Handler();
    }

    public void setUpLayout() {

        layout = R.layout.person;

        TypedArray b;
        b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
        border = b.getResourceId(0, 0);
        b.recycle();
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public User getItem(int position) {
        return users.get(position);
    }

    public View newView(ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();

        v = inflater.inflate(layout, viewGroup, false);

        holder.name = (TextView) v.findViewById(R.id.name);
        holder.screenName = (TextView) v.findViewById(R.id.screen_name);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.picture = (ImageView) v.findViewById(R.id.profile_pic);
        holder.following = (TextView) v.findViewById(R.id.following);

        v.setTag(holder);
        return v;
    }

    public void bindView(final View view, int position, final User user) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final long id = user.getId();
        holder.userId = id;

        if (holder.following != null) {
            Log.v("Focus_for_Mastodon", "following not null");
            if (followingIds.contains(id)) {
                holder.following.setVisibility(View.VISIBLE);
            } else {
                holder.following.setVisibility(View.GONE);
            }
        }

        holder.name.setText(user.getName());
        holder.screenName.setText("@" + user.getScreenName());

        String u;
        try {
            u = user.getOriginalProfileImageURL();
        } catch (Exception e) {
            u = user.getProfileImageURL();
        }

        final String url = u;

        try {
            Glide.with(context).load(url).into(holder.picture);
        } catch (Exception e) { }

        holder.background.setOnClickListener(view1 -> ProfilePager.start(context, user));

        if (openFirst && position == 0) {
            holder.background.performClick();
            ((Activity) context).finish();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v;
        if (convertView == null) {

            v = newView(parent);

        } else {
            v = convertView;

            final ViewHolder holder = (ViewHolder) v.getTag();

            holder.picture.setImageDrawable(null);
        }

        bindView(v, position, users.get(position));

        return v;
    }
}