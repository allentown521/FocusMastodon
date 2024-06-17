package allen.town.focus.twitter.adapters;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import allen.town.focus.twitter.ui.displayitems.StatusDisplayItem;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class DisplayItemsAdapter extends UsableRecyclerView.Adapter<BindableViewHolder<StatusDisplayItem>> {
    ArrayList<StatusDisplayItem> displayItems = new ArrayList<>();

    public DisplayItemsAdapter() {
        super(null);
    }

    public void setData(ArrayList<StatusDisplayItem> displayItems) {
        this.displayItems = displayItems;
    }

    @NonNull
    @Override
    public BindableViewHolder<StatusDisplayItem> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return (BindableViewHolder<StatusDisplayItem>) StatusDisplayItem.createViewHolder(StatusDisplayItem.Type.values()[viewType & (~0x80000000)], parent.getContext(), parent);
    }

    @Override
    public void onBindViewHolder(BindableViewHolder<StatusDisplayItem> holder, int position) {
        holder.bind(displayItems.get(position));
        super.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position).getType().ordinal() | 0x80000000;
    }

}
