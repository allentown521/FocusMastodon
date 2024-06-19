package allen.town.focus.twitter.activities.filters

import allen.town.focus.twitter.api.requests.filter.Filter


interface FiltersListener {
    fun deleteFilter(filter: Filter)
    fun updateFilter(updatedFilter: Filter)
}
