package allen.town.focus.twitter.model;


import java.io.Serializable;
import java.util.List;

import allen.town.focus.twitter.api.AllFieldsAreRequired;
import allen.town.focus.twitter.api.ObjectValidationException;

@AllFieldsAreRequired
public class StatusContext extends BaseModel implements Serializable {
    public HeaderPaginationList<Status> ancestors;
    public HeaderPaginationList<Status> descendants;

    @Override
    public void postprocess() throws ObjectValidationException {
        super.postprocess();
        for (Status s : ancestors)
            s.postprocess();
        for (Status s : descendants)
            s.postprocess();
    }
}
