package allen.town.focus.twitter.ui.displayitems;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.stream.Collectors;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.api.requests.polls.SubmitPollVote;
import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.model.Poll;
import allen.town.focus.twitter.utils.UiUtils;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;


public class PollFooterStatusDisplayItem extends StatusDisplayItem {
    public final Poll poll;

    public PollFooterStatusDisplayItem(long parentID, boolean isSecondAccount, Poll poll,Context context,OnPollChangedListener onPollChangedListener) {
        super(parentID, isSecondAccount,context);
        this.poll = poll;
        this.onPollChangedListener = onPollChangedListener;
    }

    @Override
    public Type getType() {
        return Type.POLL_FOOTER;
    }

    public static class Holder extends StatusDisplayItem.Holder<PollFooterStatusDisplayItem> {
        private TextView text;
        private Button button;

        public Holder(Context activity, ViewGroup parent) {
            super(activity, R.layout.display_item_poll_footer, parent);
            text = findViewById(R.id.text);
            button = findViewById(R.id.vote_btn);
            button.setOnClickListener(v -> onPollVoteButtonClick(this));
        }

        @Override
        public void onBind(PollFooterStatusDisplayItem item) {
            String text = App.getInstance().getResources().getQuantityString(R.plurals.x_voters, item.poll.votersCount, item.poll.votersCount);
            if (item.poll.expiresAt != null && !item.poll.isExpired()) {
                text += " · " + UiUtils.formatTimeLeft(itemView.getContext(), item.poll.expiresAt);
            } else if (item.poll.isExpired()) {
                text += " · " + App.getInstance().getString(R.string.poll_closed);
            }
            this.text.setText(text);
            button.setVisibility(item.poll.isExpired() || item.poll.voted || !item.poll.multiple ? View.GONE : View.VISIBLE);
            button.setEnabled(item.poll.selectedOptions != null && !item.poll.selectedOptions.isEmpty());
        }

        public void onPollVoteButtonClick(PollFooterStatusDisplayItem.Holder holder) {
            Poll poll = holder.getItem().poll;
            submitPollVote(holder.getItemID(), poll.id, poll.selectedOptions.stream().map(opt -> poll.options.indexOf(opt)).collect(Collectors.toList()));
        }


    }
}
