package allen.town.focus.twitter.model;

import java.io.Serializable;

import allen.town.focus.twitter.api.RequiredField;

public class Relationship extends BaseModel implements Serializable {
	@RequiredField
	public String id;
	public boolean following;
	public boolean requested;
	public boolean endorsed;
	public boolean followedBy;
	public boolean muting;
	public boolean mutingNotifications;
	public boolean showingReblogs;
	public boolean notifying;
	public boolean blocking;
	public boolean domainBlocking;
	public boolean blockedBy;
	public String note;

	public boolean canFollow(){
		return !(following || blocking || blockedBy || domainBlocking);
	}

	@Override
	public String toString(){
		return "Relationship{"+
				"id='"+id+'\''+
				", following="+following+
				", requested="+requested+
				", endorsed="+endorsed+
				", followedBy="+followedBy+
				", muting="+muting+
				", mutingNotifications="+mutingNotifications+
				", showingReblogs="+showingReblogs+
				", notifying="+notifying+
				", blocking="+blocking+
				", domainBlocking="+domainBlocking+
				", blockedBy="+blockedBy+
				", note='"+note+'\''+
				'}';
	}
}
