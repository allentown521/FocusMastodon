package allen.town.focus.twitter.utils;


import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import allen.town.focus.twitter.api.session.AccountSessionManager;
import allen.town.focus.twitter.model.Filter;
import allen.town.focus.twitter.model.Status;
import twitter4j.StatusJSONImplMastodon;

public class StatusFilterPredicate implements Predicate<StatusJSONImplMastodon> {
    private final List<Filter> filters;

    public StatusFilterPredicate(List<Filter> filters) {
        this.filters = filters;
    }

    public StatusFilterPredicate(String sessionId, Filter.FilterContext context) {
        filters = AccountSessionManager.getInstance().getAccount(sessionId).wordFilters.stream().filter(f -> f.context.contains(context)).collect(Collectors.toList());
    }

    @Override
    public boolean test(StatusJSONImplMastodon status) {
        for (Filter filter : filters) {
            if (filter.matches(status))
                return false;
        }
        return true;
    }
}
