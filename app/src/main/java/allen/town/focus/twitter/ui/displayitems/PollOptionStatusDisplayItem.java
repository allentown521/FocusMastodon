package allen.town.focus.twitter.ui.displayitems;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.model.Poll;
import allen.town.focus.twitter.twittertext.CustomEmojiHelper;

public class PollOptionStatusDisplayItem extends StatusDisplayItem {
    private CharSequence text;
    public final Poll.Option option;
    private boolean showResults;
    private float votesFraction; // 0..1
    private boolean isMostVoted;
    public final Poll poll;

    public PollOptionStatusDisplayItem(long parentID, Poll poll, Poll.Option option, boolean isSecondAccount, Context context, OnPollChangedListener onPollChangedListener) {
        super(parentID, isSecondAccount, context);
        this.onPollChangedListener = onPollChangedListener;
        this.option = option;
        this.poll = poll;
        showResults = poll.isExpired() || poll.voted;
        int total = poll.votersCount > 0 ? poll.votersCount : poll.votesCount;
        if (showResults && option.votesCount != null && total > 0) {
            votesFraction = (float) option.votesCount / (float) total;
            int mostVotedCount = 0;
            for (Poll.Option opt : poll.options)
                mostVotedCount = Math.max(mostVotedCount, opt.votesCount);
            isMostVoted = option.votesCount == mostVotedCount;
        }
    }

    @Override
    public Type getType() {
        return Type.POLL_OPTION;
    }


    public static class Holder extends StatusDisplayItem.Holder<PollOptionStatusDisplayItem> {
        private final TextView text, percent;
        private final View icon, button;
        private final Drawable progressBg;

        public Holder(Context activity, ViewGroup parent) {
            super(activity, R.layout.display_item_poll_option, parent);
            text = findViewById(R.id.text);
            percent = findViewById(R.id.percent);
            icon = findViewById(R.id.icon);
            button = findViewById(R.id.button);
            progressBg = activity.getResources().getDrawable(R.drawable.bg_poll_option_voted, activity.getTheme()).mutate();
            itemView.setOnClickListener(this::onButtonClick);
        }

        @Override
        public void onBind(PollOptionStatusDisplayItem item) {

            text.setText(CustomEmojiHelper.emojify(item.option.title, item.poll.emojis, text, false));
            icon.setVisibility(item.showResults ? View.GONE : View.VISIBLE);
            percent.setVisibility(item.showResults ? View.VISIBLE : View.GONE);
            itemView.setClickable(!item.showResults);
            if (item.showResults) {
                progressBg.setLevel(Math.round(10000f * item.votesFraction));
                button.setBackground(progressBg);
                itemView.setSelected(item.isMostVoted);
                percent.setText(String.format(Locale.getDefault(), "%d%%", Math.round(item.votesFraction * 100f)));
            } else {
                itemView.setSelected(item.poll.selectedOptions != null && item.poll.selectedOptions.contains(item.option));
                button.setBackgroundResource(R.drawable.bg_poll_option_clickable);
            }
        }


        private void onButtonClick(View v) {
            onPollOptionClick(this);
        }

        public void onPollOptionClick(PollOptionStatusDisplayItem.Holder holder) {
            Poll poll = holder.getItem().poll;
            Poll.Option option = holder.getItem().option;
            if (poll.multiple) {
                if (poll.selectedOptions == null)
                    poll.selectedOptions = new ArrayList<>();
                if (poll.selectedOptions.contains(option)) {
                    poll.selectedOptions.remove(option);
                    holder.itemView.setSelected(false);
                } else {
                    poll.selectedOptions.add(option);
                    holder.itemView.setSelected(true);
                }
                if (item.onPollChangedListener != null) {
                    item.onPollChangedListener.onPollChanged(holder.getItemID(), poll);
                }
                /*for (int i = 0; i < list.getChildCount(); i++) {
                    RecyclerView.ViewHolder vh = list.getChildViewHolder(list.getChildAt(i));
                    if (vh instanceof PollFooterStatusDisplayItem.Holder footer) {
                        if (footer.getItemID().equals(holder.getItemID())) {
                            footer.rebind();
                            break;
                        }
                    }
                }*/
            } else {
                submitPollVote(holder.getItemID(), poll.id, Collections.singletonList(poll.options.indexOf(option)));
            }
        }
    }
}
