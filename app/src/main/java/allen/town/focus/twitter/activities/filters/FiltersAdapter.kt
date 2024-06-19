package allen.town.focus.twitter.activities.filters

import allen.town.focus.twitter.R
import allen.town.focus.twitter.api.requests.filter.Filter
import allen.town.focus.twitter.databinding.ItemRemovableBinding
import allen.town.focus.twitter.utils.BindingHolder
import android.content.Context
import android.text.format.DateUtils.*
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class FiltersAdapter(val listener: FiltersListener, val filters: List<Filter>) :
    RecyclerView.Adapter<BindingHolder<ItemRemovableBinding>>() {

    override fun getItemCount(): Int = filters.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder<ItemRemovableBinding> {
        return BindingHolder(ItemRemovableBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: BindingHolder<ItemRemovableBinding>, position: Int) {
        val binding = holder.binding
        val resources = binding.root.resources
        val actions = resources.getStringArray(R.array.filter_actions)
        val contexts = resources.getStringArray(R.array.filter_contexts)

        val filter = filters[position]
        val context = binding.root.context
        binding.textPrimary.text = if (filter.expiresAt == null) {
            filter.title
        } else {
            context.getString(
                R.string.filter_expiration_format,
                filter.title,
                getRelativeTimeSpanString(binding.root.context, filter.expiresAt.time, System.currentTimeMillis())
            )
        }
        binding.textSecondary.text = context.getString(
            R.string.filter_description_format,
            actions.getOrNull(filter.action.ordinal - 1),
            filter.context.map { contexts.getOrNull(Filter.Kind.from(it).ordinal) }.joinToString("/")
        )

        binding.delete.setOnClickListener {
            listener.deleteFilter(filter)
        }

        binding.root.setOnClickListener {
            listener.updateFilter(filter)
        }
    }

    fun getRelativeTimeSpanString(context: Context, then: Long, now: Long): String {
        var span = now - then
        var future = false
        if (abs(span) < SECOND_IN_MILLIS) {
            return context.getString(R.string.status_created_at_now)
        } else if (span < 0) {
            future = true
            span = -span
        }
        val format: Int
        if (span < MINUTE_IN_MILLIS) {
            span /= SECOND_IN_MILLIS
            format = if (future) {
                R.string.abbreviated_in_seconds
            } else {
                R.string.abbreviated_seconds_ago
            }
        } else if (span < HOUR_IN_MILLIS) {
            span /= MINUTE_IN_MILLIS
            format = if (future) {
                R.string.abbreviated_in_minutes
            } else {
                R.string.abbreviated_minutes_ago
            }
        } else if (span < DAY_IN_MILLIS) {
            span /= HOUR_IN_MILLIS
            format = if (future) {
                R.string.abbreviated_in_hours
            } else {
                R.string.abbreviated_hours_ago
            }
        } else if (span < YEAR_IN_MILLIS) {
            span /= DAY_IN_MILLIS
            format = if (future) {
                R.string.abbreviated_in_days
            } else {
                R.string.abbreviated_days_ago
            }
        } else {
            span /= YEAR_IN_MILLIS
            format = if (future) {
                R.string.abbreviated_in_years
            } else {
                R.string.abbreviated_years_ago
            }
        }
        return context.getString(format, span)
    }
}
