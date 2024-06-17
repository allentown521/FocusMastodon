package allen.town.focus.twitter.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus.twitter.R;
import allen.town.focus.twitter.api.MastodonAPIRequest;
import allen.town.focus.twitter.api.requests.polls.SubmitPollVote;
import allen.town.focus.twitter.data.App;
import allen.town.focus.twitter.model.Poll;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;
import twitter4j.Status;

public abstract class StatusDisplayItem {
    public final long parentID;
    public boolean isSecondAccount;
    public boolean inset;
    public int index;
    public OnPollChangedListener onPollChangedListener;

    public StatusDisplayItem(long parentID, boolean isSecondAccount, Context context) {
        this.parentID = parentID;
        this.isSecondAccount = isSecondAccount;
    }

    public abstract Type getType();


    public static BindableViewHolder<? extends StatusDisplayItem> createViewHolder(Type type, Context activity, ViewGroup parent) {
        return switch (type) {
            case POLL_OPTION -> new PollOptionStatusDisplayItem.Holder(activity, parent);
            case POLL_FOOTER -> new PollFooterStatusDisplayItem.Holder(activity, parent);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    public static ArrayList<StatusDisplayItem> buildItems(boolean isSecondAccount, Status status, Context context, OnPollChangedListener onPollChangedListener) {
        long parentID = status.getId();
        ArrayList<StatusDisplayItem> items = new ArrayList<>();
        if (status.getPoll() != null) {
            buildPollItems(parentID, isSecondAccount, status.getPoll(), items, context, onPollChangedListener);
        }
        return items;
    }

    public static void buildPollItems(long parentID, boolean isSecondAccount, Poll poll, List<StatusDisplayItem> items, Context context, OnPollChangedListener onPollChangedListener) {
        for (Poll.Option opt : poll.options) {
            items.add(new PollOptionStatusDisplayItem(parentID, poll, opt, isSecondAccount, context, onPollChangedListener));
        }
        items.add(new PollFooterStatusDisplayItem(parentID, isSecondAccount, poll, context, onPollChangedListener));
    }

    public enum Type {
        HEADER,
        REBLOG_OR_REPLY_LINE,
        TEXT,
        AUDIO,
        POLL_OPTION,
        POLL_FOOTER,
        CARD,
        FOOTER,
        ACCOUNT_CARD,
        ACCOUNT,
        HASHTAG,
        GAP,
        EXTENDED_FOOTER,
        MEDIA_GRID
    }

    public static abstract class Holder<T extends StatusDisplayItem> extends BindableViewHolder<T> implements UsableRecyclerView.DisableableClickable {
        public Context context;

        public Holder(View itemView) {
            super(itemView);
        }

        public Holder(Context context, int layout, ViewGroup parent) {
            super(context, layout, parent);
            this.context = context;
        }

        public long getItemID() {
            return item.parentID;
        }

        @Override
        public void onClick() {
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        protected void submitPollVote(long parentID, String pollID, List<Integer> choices) {
            MastodonAPIRequest<Poll> request = new SubmitPollVote(pollID, choices)
                    .setCallback(new Callback<>() {
                        @Override
                        public void onSuccess(Poll result) {
//                            E.post(new PollUpdatedEvent(accountID, result));
                            if (item.onPollChangedListener != null) {
                                item.onPollChangedListener.onPollChanged(parentID, result);
                            }
                        }

                        @Override
                        public void onError(ErrorResponse error) {
                            error.showToast(App.getInstance());
                        }
                    })
                    .wrapProgress((Activity) context, R.string.loading, true);
            if (getItem().isSecondAccount) {
                request.execSecondAccount();
            } else {
                request.exec();
            }
        }
    }

    public interface OnPollChangedListener {
        void onPollChanged(long statusId, Poll poll);
    }
}
